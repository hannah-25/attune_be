package attune.journal.application;

import attune.common.util.SecurityUtils;
import attune.common.error.BadRequestException;
import attune.common.error.conflict.DuplicateTagException;
import attune.common.error.notfound.JournalTagNotFoundException;
import attune.journal.application.dto.request.CreateCatalogJournalTagRequest;
import attune.journal.application.dto.request.UpdateCatalogTagPreferenceRequest;
import attune.journal.application.dto.response.CatalogJournalTagResponse;
import attune.journal.domain.model.ConditionTag;
import attune.journal.domain.model.ConditionType;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.LegacyJournalTagMapping;
import attune.journal.domain.model.SideEffectTag;
import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.model.TroubleType;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.LegacyJournalTagMappingRepository;
import attune.journal.domain.repository.ConditionTagRepository;
import attune.journal.domain.repository.ConditionLogRepository;
import attune.journal.domain.repository.SideEffectTagRepository;
import attune.journal.domain.repository.SideEffectLogRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import attune.journal.domain.repository.TroubleLogRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class JournalTagCatalogService {

    private final JournalTagRepository journalTagRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;
    private final LegacyJournalTagMappingRepository mappingRepository;
    private final ConditionTagRepository conditionTagRepository;
    private final SideEffectTagRepository sideEffectTagRepository;
    private final TroubleTagRepository troubleTagRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final SideEffectLogRepository sideEffectLogRepository;
    private final TroubleLogRepository troubleLogRepository;

    @Transactional(readOnly = true)
    public List<CatalogJournalTagResponse> getTags(JournalTagCategory category) {
        return buildTagResponse(SecurityUtils.getCurrentUserUuid(), category);
    }

    @Transactional(readOnly = true)
    public List<CatalogJournalTagResponse> getTroubleTags(UUID userId) {
        return buildTagResponse(userId, JournalTagCategory.TROUBLE);
    }

    @Transactional
    public void bulkSetVisibilityForOnboarding(UUID userId, Set<Long> visibleCatalogTagIds) {
        List<JournalTag> catalogTags = Stream.concat(
                journalTagRepository.findAllByScopeAndIsActiveTrue(JournalTagScope.SYSTEM).stream(),
                journalTagRepository.findAllByScopeAndOwnerUserIdAndIsActiveTrue(
                        JournalTagScope.USER, userId).stream()
        ).toList();
        Map<Long, UserJournalTagPreference> preferencesByTagId = preferenceRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserJournalTagPreference::getJournalTagId, Function.identity()));

        List<UserJournalTagPreference> preferences = catalogTags.stream()
                .map(tag -> {
                    boolean visible = visibleCatalogTagIds.contains(tag.getId());
                    UserJournalTagPreference preference = preferencesByTagId.get(tag.getId());
                    if (preference == null) {
                        preference = UserJournalTagPreference.create(userId, tag.getId(), true, visible);
                    }
                    preference.update(true, visible);
                    return preference;
                })
                .toList();
        preferenceRepository.saveAll(preferences);

        Map<Long, Boolean> visibilityByCatalogTagId = catalogTags.stream()
                .collect(Collectors.toMap(
                        JournalTag::getId,
                        tag -> visibleCatalogTagIds.contains(tag.getId())
                ));
        syncLegacyTagsForOnboarding(userId, visibilityByCatalogTagId);
    }

    private List<CatalogJournalTagResponse> buildTagResponse(UUID userId, JournalTagCategory category) {
        List<JournalTag> systemTags = journalTagRepository
                .findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, category);
        List<JournalTag> userTags = journalTagRepository
                .findAllByScopeAndOwnerUserIdAndCategoryAndIsActiveTrue(JournalTagScope.USER, userId, category);
        Map<Long, UserJournalTagPreference> preferences = preferenceRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserJournalTagPreference::getJournalTagId, Function.identity()));
        Map<Long, Long> legacyIdsByCatalogId = representativeLegacyIds(userId, category);

        return Stream.concat(systemTags.stream(), userTags.stream())
                .map(tag -> toResponse(tag, preferences.get(tag.getId()), legacyIdsByCatalogId.get(tag.getId())))
                .sorted(Comparator.comparing(CatalogJournalTagResponse::scope)
                        .thenComparing(CatalogJournalTagResponse::catalogTagId))
                .toList();
    }

    @Transactional
    public CatalogJournalTagResponse createTag(CreateCatalogJournalTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        validateTagType(request.category(), request.tagType());
        journalTagRepository.findByScopeAndCategoryAndNameAndTagType(
                        JournalTagScope.SYSTEM, request.category(), request.name(), request.tagType())
                .filter(JournalTag::isActive)
                .ifPresent(tag -> {
                    throw new DuplicateTagException("journal catalog");
                });

        JournalTag tag = journalTagRepository.findByScopeAndOwnerUserIdAndCategoryAndNameAndTagType(
                        JournalTagScope.USER, userId, request.category(), request.name(), request.tagType())
                .map(existing -> {
                    if (existing.isActive()) {
                        throw new DuplicateTagException("journal catalog");
                    }
                    existing.activate();
                    return existing;
                })
                .orElseGet(() -> journalTagRepository.save(JournalTag.userTag(
                        userId, request.category(), request.name(), request.tagType())));

        UserJournalTagPreferenceId preferenceId = new UserJournalTagPreferenceId(userId, tag.getId());
        UserJournalTagPreference preference = preferenceRepository.findById(preferenceId)
                .orElseGet(() -> UserJournalTagPreference.create(userId, tag.getId(), true, request.visible()));
        preference.update(true, request.visible());
        preferenceRepository.save(preference);

        Long legacyTagId = representativeLegacyIds(userId, request.category()).get(tag.getId());
        if (legacyTagId == null) {
            legacyTagId = createLegacyCompatibilityTag(userId, tag, request.visible());
        } else {
            syncLegacyTags(userId, request.category(), tag.getId(), true, request.visible());
        }
        return toResponse(tag, preference, legacyTagId);
    }

    @Transactional
    public CatalogJournalTagResponse updatePreference(Long catalogTagId, UpdateCatalogTagPreferenceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        JournalTag tag = journalTagRepository.findById(catalogTagId)
                .filter(JournalTag::isActive)
                .filter(found -> found.getScope() == JournalTagScope.SYSTEM || userId.equals(found.getOwnerUserId()))
                .orElseThrow(JournalTagNotFoundException::new);

        UserJournalTagPreferenceId id = new UserJournalTagPreferenceId(userId, catalogTagId);
        UserJournalTagPreference preference = preferenceRepository.findById(id)
                .orElseGet(() -> UserJournalTagPreference.create(
                        userId, catalogTagId, request.enabled(), request.visible()));
        preference.update(request.enabled(), request.visible());
        preferenceRepository.save(preference);
        syncLegacyTags(userId, tag.getCategory(), catalogTagId, request.enabled(), request.visible());

        Long legacyTagId = representativeLegacyIds(userId, tag.getCategory()).get(catalogTagId);
        return toResponse(tag, preference, legacyTagId);
    }

    @Transactional
    public void deleteTag(Long catalogTagId, LocalDate journalDate) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        JournalTag tag = journalTagRepository.findById(catalogTagId)
                .filter(JournalTag::isActive)
                .filter(found -> found.getScope() == JournalTagScope.SYSTEM || userId.equals(found.getOwnerUserId()))
                .orElseThrow(JournalTagNotFoundException::new);

        UserJournalTagPreferenceId id = new UserJournalTagPreferenceId(userId, catalogTagId);
        UserJournalTagPreference preference = preferenceRepository.findById(id)
                .orElseGet(() -> UserJournalTagPreference.create(userId, catalogTagId, false, false));
        preference.update(false, false);
        preferenceRepository.save(preference);

        List<Long> legacyTagIds = syncLegacyTags(userId, tag.getCategory(), catalogTagId, false, false);

        if (tag.getScope() == JournalTagScope.USER) {
            tag.deactivate();
        }

        LocalDateTime startAt = journalDate.atStartOfDay();
        switch (tag.getCategory()) {
            case CONDITION -> {
                conditionLogRepository.deleteAllByCatalogTagFromDate(userId, catalogTagId, startAt);
                legacyTagIds.forEach(legacyId -> conditionLogRepository.deleteAllByTagFromDate(legacyId, startAt));
            }
            case SIDE_EFFECT -> {
                sideEffectLogRepository.deleteAllByCatalogTagFromDate(userId, catalogTagId, startAt);
                legacyTagIds.forEach(legacyId -> sideEffectLogRepository.deleteAllByTagFromDate(legacyId, startAt));
            }
            case TROUBLE -> {
                troubleLogRepository.deleteAllByCatalogTagFromDate(userId, catalogTagId, startAt);
                legacyTagIds.forEach(legacyId -> troubleLogRepository.deleteAllByTagFromDate(legacyId, startAt));
            }
        }
    }

    private Map<Long, Long> representativeLegacyIds(UUID userId, JournalTagCategory category) {
        Map<Long, Long> result = new HashMap<>();
        for (LegacyJournalTagMapping mapping : mappingRepository.findAllByUserIdAndLegacyCategory(userId, category)) {
            result.merge(mapping.getJournalTagId(), mapping.getLegacyTagId(), Math::min);
        }
        return result;
    }

    private List<Long> syncLegacyTags(
            UUID userId, JournalTagCategory category, Long catalogTagId, boolean enabled, boolean visible
    ) {
        List<Long> legacyIds = mappingRepository.findByUserIdAndLegacyCategoryAndJournalTagId(userId, category, catalogTagId).stream()
                .map(LegacyJournalTagMapping::getLegacyTagId)
                .toList();

        switch (category) {
            case CONDITION -> conditionTagRepository.findAllById(legacyIds).stream()
                    .filter(tag -> userId.equals(tag.getUserId()))
                    .forEach(tag -> tag.changePreference(enabled, visible));
            case SIDE_EFFECT -> sideEffectTagRepository.findAllById(legacyIds).stream()
                    .filter(tag -> userId.equals(tag.getUserId()))
                    .forEach(tag -> tag.changePreference(enabled, visible));
            case TROUBLE -> troubleTagRepository.findAllById(legacyIds).stream()
                    .filter(tag -> userId.equals(tag.getUserId()))
                    .forEach(tag -> tag.changePreference(enabled, visible));
        }
        return legacyIds;
    }

    private void syncLegacyTagsForOnboarding(UUID userId, Map<Long, Boolean> visibilityByCatalogTagId) {
        Map<JournalTagCategory, Map<Long, Long>> catalogTagIdsByLegacyId = mappingRepository.findAllByUserId(userId)
                .stream()
                .filter(mapping -> visibilityByCatalogTagId.containsKey(mapping.getJournalTagId()))
                .collect(Collectors.groupingBy(
                        LegacyJournalTagMapping::getLegacyCategory,
                        Collectors.toMap(
                                LegacyJournalTagMapping::getLegacyTagId,
                                LegacyJournalTagMapping::getJournalTagId
                        )
                ));

        syncConditionTags(
                userId,
                catalogTagIdsByLegacyId.getOrDefault(JournalTagCategory.CONDITION, Map.of()),
                visibilityByCatalogTagId
        );
        syncSideEffectTags(
                userId,
                catalogTagIdsByLegacyId.getOrDefault(JournalTagCategory.SIDE_EFFECT, Map.of()),
                visibilityByCatalogTagId
        );
        syncTroubleTags(
                userId,
                catalogTagIdsByLegacyId.getOrDefault(JournalTagCategory.TROUBLE, Map.of()),
                visibilityByCatalogTagId
        );
    }

    private void syncConditionTags(
            UUID userId, Map<Long, Long> catalogTagIdsByLegacyId, Map<Long, Boolean> visibilityByCatalogTagId
    ) {
        if (catalogTagIdsByLegacyId.isEmpty()) {
            return;
        }
        conditionTagRepository.findAllById(catalogTagIdsByLegacyId.keySet()).stream()
                .filter(tag -> userId.equals(tag.getUserId()))
                .forEach(tag -> tag.changePreference(
                        true,
                        visibilityByCatalogTagId.get(catalogTagIdsByLegacyId.get(tag.getId()))
                ));
    }

    private void syncSideEffectTags(
            UUID userId, Map<Long, Long> catalogTagIdsByLegacyId, Map<Long, Boolean> visibilityByCatalogTagId
    ) {
        if (catalogTagIdsByLegacyId.isEmpty()) {
            return;
        }
        sideEffectTagRepository.findAllById(catalogTagIdsByLegacyId.keySet()).stream()
                .filter(tag -> userId.equals(tag.getUserId()))
                .forEach(tag -> tag.changePreference(
                        true,
                        visibilityByCatalogTagId.get(catalogTagIdsByLegacyId.get(tag.getId()))
                ));
    }

    private void syncTroubleTags(
            UUID userId, Map<Long, Long> catalogTagIdsByLegacyId, Map<Long, Boolean> visibilityByCatalogTagId
    ) {
        if (catalogTagIdsByLegacyId.isEmpty()) {
            return;
        }
        troubleTagRepository.findAllById(catalogTagIdsByLegacyId.keySet()).stream()
                .filter(tag -> userId.equals(tag.getUserId()))
                .forEach(tag -> tag.changePreference(
                        true,
                        visibilityByCatalogTagId.get(catalogTagIdsByLegacyId.get(tag.getId()))
                ));
    }

    private Long createLegacyCompatibilityTag(UUID userId, JournalTag tag, boolean visible) {
        Long legacyTagId = switch (tag.getCategory()) {
            case CONDITION -> conditionTagRepository.save(ConditionTag.builder()
                    .userId(userId)
                    .condition(tag.getName())
                    .conditionType(ConditionType.valueOf(tag.getTagType()))
                    .isActive(true)
                    .visible(visible)
                    .build()).getId();
            case SIDE_EFFECT -> sideEffectTagRepository.save(SideEffectTag.builder()
                    .userId(userId)
                    .sideEffect(tag.getName())
                    .isActive(true)
                    .visible(visible)
                    .build()).getId();
            case TROUBLE -> troubleTagRepository.save(TroubleTag.builder()
                    .userId(userId)
                    .trouble(tag.getName())
                    .type(TroubleType.valueOf(tag.getTagType()))
                    .isActive(true)
                    .visible(visible)
                    .build()).getId();
        };
        mappingRepository.save(LegacyJournalTagMapping.create(tag.getCategory(), legacyTagId, userId, tag.getId()));
        return legacyTagId;
    }

    private void validateTagType(JournalTagCategory category, String tagType) {
        try {
            switch (category) {
                case CONDITION -> ConditionType.valueOf(tagType);
                case SIDE_EFFECT -> {
                    if (!"NONE".equals(tagType)) {
                        throw new BadRequestException("Side effect catalog tag type must be NONE");
                    }
                }
                case TROUBLE -> TroubleType.valueOf(tagType);
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid journal catalog tag type");
        }
    }

    private CatalogJournalTagResponse toResponse(
            JournalTag tag, UserJournalTagPreference preference, Long legacyTagId
    ) {
        boolean enabled = preference == null || preference.isEnabled();
        boolean visible = preference != null ? preference.isVisible() : tag.isDefaultVisible();
        return new CatalogJournalTagResponse(
                tag.getId(),
                legacyTagId,
                tag.getCategory(),
                tag.getName(),
                tag.getTagType(),
                tag.getScope(),
                enabled,
                visible
        );
    }

}
