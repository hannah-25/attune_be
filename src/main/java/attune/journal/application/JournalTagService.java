package attune.journal.application;

import attune.common.error.BadRequestException;
import attune.common.error.conflict.DuplicateTagException;
import attune.common.error.notfound.JournalTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.request.CreateJournalTagRequest;
import attune.journal.application.dto.request.UpdateJournalTagPreferenceRequest;
import attune.journal.application.dto.response.JournalTagResponse;
import attune.journal.domain.model.ConditionType;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.TroubleType;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class JournalTagService {

    public record CreateResult(JournalTagResponse response, boolean reactivated) {}

    private final JournalTagRepository journalTagRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public List<JournalTagResponse> getTags(JournalTagCategory category, boolean manage) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        List<JournalTag> systemTags = journalTagRepository
                .findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, category);
        List<JournalTag> userTags = journalTagRepository
                .findAllByScopeAndOwnerUserIdAndCategoryAndIsActiveTrue(JournalTagScope.USER, userId, category);
        Map<Long, UserJournalTagPreference> preferences = preferenceRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserJournalTagPreference::getJournalTagId, Function.identity()));

        return Stream.concat(systemTags.stream(), userTags.stream())
                .map(tag -> toResponse(tag, preferences.get(tag.getId())))
                .filter(r -> manage || (r.enabled() && r.visible()))
                .sorted(Comparator.comparingInt((JournalTagResponse r) -> r.scope().ordinal())
                        .thenComparingLong(JournalTagResponse::tagId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JournalTagResponse> getTroubleTags(UUID userId) {
        List<JournalTag> systemTags = journalTagRepository
                .findAllByScopeAndCategoryAndIsActiveTrue(JournalTagScope.SYSTEM, JournalTagCategory.TROUBLE);
        Map<Long, UserJournalTagPreference> preferences = preferenceRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserJournalTagPreference::getJournalTagId, Function.identity()));

        return systemTags.stream()
                .map(tag -> toResponse(tag, preferences.get(tag.getId())))
                .sorted(Comparator.comparingLong(JournalTagResponse::tagId))
                .toList();
    }

    @Transactional
    public CreateResult createTag(CreateJournalTagRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime now = LocalDateTime.now();
        String normalizedName = normalizeName(request.name());
        validateTagType(request.category(), request.tagType());

        if (journalTagRepository.existsByScopeAndCategoryAndNameAndIsActiveTrue(
                JournalTagScope.SYSTEM, request.category(), normalizedName)) {
            throw new DuplicateTagException("journal tag");
        }

        boolean[] reactivated = {false};
        JournalTag tag = journalTagRepository.findByScopeAndOwnerUserIdAndCategoryAndNameAndTagType(
                        JournalTagScope.USER, userId, request.category(), normalizedName, request.tagType())
                .map(existing -> {
                    if (existing.isActive()) {
                        throw new DuplicateTagException("journal tag");
                    }
                    existing.activate(now);
                    reactivated[0] = true;
                    return existing;
                })
                .orElseGet(() -> journalTagRepository.save(
                        JournalTag.userTag(userId, request.category(), normalizedName, request.tagType(), now)));

        boolean actualVisible = request.visible();
        UserJournalTagPreferenceId prefId = new UserJournalTagPreferenceId(userId, tag.getId());
        UserJournalTagPreference preference = preferenceRepository.findById(prefId)
                .orElseGet(() -> UserJournalTagPreference.create(userId, tag.getId(), true, actualVisible, now));
        preference.update(true, actualVisible, now);
        preferenceRepository.save(preference);

        return new CreateResult(toResponse(tag, preference), reactivated[0]);
    }

    @Transactional
    public JournalTagResponse updatePreference(Long tagId, UpdateJournalTagPreferenceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime now = LocalDateTime.now();
        JournalTag tag = requireAccessibleTag(userId, tagId);

        boolean enabled = request.enabled();
        boolean visible = enabled ? request.visible() : false;

        UserJournalTagPreferenceId prefId = new UserJournalTagPreferenceId(userId, tagId);
        UserJournalTagPreference preference = preferenceRepository.findById(prefId)
                .orElseGet(() -> UserJournalTagPreference.create(userId, tagId, enabled, visible, now));
        preference.update(enabled, visible, now);
        preferenceRepository.save(preference);

        return toResponse(tag, preference);
    }

    @Transactional
    public void deleteTag(Long tagId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        LocalDateTime now = LocalDateTime.now();
        JournalTag tag = requireAccessibleTag(userId, tagId);

        UserJournalTagPreferenceId prefId = new UserJournalTagPreferenceId(userId, tagId);
        UserJournalTagPreference preference = preferenceRepository.findById(prefId)
                .orElseGet(() -> UserJournalTagPreference.create(userId, tagId, false, false, now));
        preference.update(false, false, now);
        preferenceRepository.save(preference);

        if (tag.getScope() == JournalTagScope.USER) {
            tag.deactivate(now);
        }
    }

    @Transactional
    public void bulkSetVisibilityForOnboarding(UUID userId, Set<Long> visibleTagIds) {
        Set<Long> tagIds = visibleTagIds != null ? visibleTagIds : Set.of();
        LocalDateTime now = LocalDateTime.now();
        List<JournalTag> onboardingTags = journalTagRepository
                .findAllByScopeAndCategoryAndIsActiveTrue(
                        JournalTagScope.SYSTEM, JournalTagCategory.TROUBLE);
        Set<Long> onboardingTagIds = onboardingTags.stream()
                .map(JournalTag::getId)
                .collect(Collectors.toSet());
        if (!onboardingTagIds.containsAll(tagIds)) {
            throw new BadRequestException(
                    "visibleTagIds must contain only active system TROUBLE tag IDs");
        }

        Map<Long, UserJournalTagPreference> existingPrefs = preferenceRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(UserJournalTagPreference::getJournalTagId, Function.identity()));

        List<UserJournalTagPreference> preferences = onboardingTags.stream()
                .map(tag -> {
                    boolean visible = tagIds.contains(tag.getId());
                    UserJournalTagPreference pref = existingPrefs.get(tag.getId());
                    if (pref == null) {
                        pref = UserJournalTagPreference.create(userId, tag.getId(), true, visible, now);
                    }
                    pref.update(true, visible, now);
                    return pref;
                })
                .toList();
        preferenceRepository.saveAll(preferences);
    }

    private JournalTag requireAccessibleTag(UUID userId, Long tagId) {
        return journalTagRepository.findById(tagId)
                .filter(JournalTag::isActive)
                .filter(t -> t.getScope() == JournalTagScope.SYSTEM || userId.equals(t.getOwnerUserId()))
                .orElseThrow(JournalTagNotFoundException::new);
    }

    private JournalTagResponse toResponse(JournalTag tag, UserJournalTagPreference preference) {
        boolean enabled = preference == null || preference.isEnabled();
        boolean visible = preference != null ? preference.isVisible() : tag.isDefaultVisible();
        return new JournalTagResponse(
                tag.getId(),
                tag.getCategory(),
                tag.getName(),
                tag.getTagType(),
                tag.getScope(),
                enabled,
                visible
        );
    }

    private String normalizeName(String raw) {
        if (raw == null) {
            throw new BadRequestException("Tag name must not be null");
        }
        String trimmed = raw.strip();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Tag name must not be blank");
        }
        return Normalizer.normalize(trimmed, Normalizer.Form.NFC);
    }

    private void validateTagType(JournalTagCategory category, String tagType) {
        try {
            switch (category) {
                case CONDITION -> ConditionType.valueOf(tagType);
                case SIDE_EFFECT -> {
                    if (!"NONE".equals(tagType)) {
                        throw new BadRequestException("Side effect tag type must be NONE");
                    }
                }
                case TROUBLE -> TroubleType.valueOf(tagType);
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid tag type for category " + category);
        }
    }
}
