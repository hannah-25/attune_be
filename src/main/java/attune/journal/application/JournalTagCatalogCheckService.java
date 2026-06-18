package attune.journal.application;

import attune.common.error.BadRequestException;
import attune.common.error.notfound.JournalTagNotFoundException;
import attune.common.util.SecurityUtils;
import attune.journal.application.dto.response.CatalogTagCheckResponse;
import attune.journal.domain.model.ConditionLog;
import attune.journal.domain.model.ConditionTag;
import attune.journal.domain.model.ConditionType;
import attune.journal.domain.model.JournalTag;
import attune.journal.domain.model.JournalTagCategory;
import attune.journal.domain.model.JournalTagScope;
import attune.journal.domain.model.LegacyJournalTagMapping;
import attune.journal.domain.model.SideEffectLog;
import attune.journal.domain.model.SideEffectTag;
import attune.journal.domain.model.TroubleLog;
import attune.journal.domain.model.TroubleTag;
import attune.journal.domain.model.TroubleType;
import attune.journal.domain.model.UserJournalTagPreference;
import attune.journal.domain.model.UserJournalTagPreferenceId;
import attune.journal.domain.repository.ConditionLogRepository;
import attune.journal.domain.repository.ConditionTagRepository;
import attune.journal.domain.repository.JournalTagRepository;
import attune.journal.domain.repository.LegacyJournalTagMappingRepository;
import attune.journal.domain.repository.SideEffectLogRepository;
import attune.journal.domain.repository.SideEffectTagRepository;
import attune.journal.domain.repository.TroubleLogRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JournalTagCatalogCheckService {

    private final JournalTagRepository journalTagRepository;
    private final LegacyJournalTagMappingRepository mappingRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;
    private final ConditionTagRepository conditionTagRepository;
    private final SideEffectTagRepository sideEffectTagRepository;
    private final TroubleTagRepository troubleTagRepository;
    private final ConditionLogRepository conditionLogRepository;
    private final SideEffectLogRepository sideEffectLogRepository;
    private final TroubleLogRepository troubleLogRepository;

    @Transactional
    public CatalogTagCheckResponse check(Long catalogTagId) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        JournalTag tag = requireEnabledTag(userId, catalogTagId);
        Long legacyTagId = findOrCreateLegacyTag(userId, tag);
        LocalDateTime checkedAt = LocalDateTime.now();

        switch (tag.getCategory()) {
            case CONDITION -> conditionLogRepository.save(ConditionLog.builder()
                    .conditionTagId(legacyTagId)
                    .userId(userId)
                    .journalTagId(tag.getId())
                    .checkedAt(checkedAt)
                    .build());
            case SIDE_EFFECT -> sideEffectLogRepository.save(SideEffectLog.builder()
                    .sideEffectTagId(legacyTagId)
                    .userId(userId)
                    .journalTagId(tag.getId())
                    .checkedAt(checkedAt)
                    .build());
            case TROUBLE -> troubleLogRepository.save(TroubleLog.builder()
                    .troubleTagId(legacyTagId)
                    .userId(userId)
                    .journalTagId(tag.getId())
                    .checkedAt(checkedAt)
                    .build());
        }

        return new CatalogTagCheckResponse(tag.getId(), tag.getCategory(), checkedAt);
    }

    @Transactional
    public void uncheck(Long catalogTagId, LocalDate date) {
        UUID userId = SecurityUtils.getCurrentUserUuid();
        JournalTag tag = requireAccessibleTag(userId, catalogTagId);
        LocalDateTime startAt = date.atStartOfDay();
        LocalDateTime endAt = date.plusDays(1).atStartOfDay();

        switch (tag.getCategory()) {
            case CONDITION -> conditionLogRepository.deleteAllByCatalogTagAndDate(
                    userId, catalogTagId, startAt, endAt);
            case SIDE_EFFECT -> sideEffectLogRepository.deleteAllByCatalogTagAndDate(
                    userId, catalogTagId, startAt, endAt);
            case TROUBLE -> troubleLogRepository.deleteAllByCatalogTagAndDate(
                    userId, catalogTagId, startAt, endAt);
        }
    }

    private JournalTag requireEnabledTag(UUID userId, Long catalogTagId) {
        JournalTag tag = requireAccessibleTag(userId, catalogTagId);
        boolean enabled = preferenceRepository.findById(new UserJournalTagPreferenceId(userId, catalogTagId))
                .map(UserJournalTagPreference::isEnabled)
                .orElse(true);
        if (!enabled) {
            throw new BadRequestException("Journal catalog tag is disabled");
        }
        return tag;
    }

    private JournalTag requireAccessibleTag(UUID userId, Long catalogTagId) {
        return journalTagRepository.findById(catalogTagId)
                .filter(JournalTag::isActive)
                .filter(tag -> tag.getScope() == JournalTagScope.SYSTEM || userId.equals(tag.getOwnerUserId()))
                .orElseThrow(JournalTagNotFoundException::new);
    }

    private Long findOrCreateLegacyTag(UUID userId, JournalTag tag) {
        return mappingRepository.findAllByUserIdAndLegacyCategory(userId, tag.getCategory()).stream()
                .filter(mapping -> mapping.getJournalTagId().equals(tag.getId()))
                .map(LegacyJournalTagMapping::getLegacyTagId)
                .min(Long::compareTo)
                .orElseGet(() -> createLegacyCompatibilityTag(userId, tag));
    }

    private Long createLegacyCompatibilityTag(UUID userId, JournalTag tag) {
        boolean visible = effectiveVisible(userId, tag);
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
        ensurePreference(userId, tag.getId(), visible);
        return legacyTagId;
    }

    private void ensurePreference(UUID userId, Long journalTagId, boolean visible) {
        UserJournalTagPreferenceId id = new UserJournalTagPreferenceId(userId, journalTagId);
        UserJournalTagPreference preference = preferenceRepository.findById(id)
                .orElseGet(() -> UserJournalTagPreference.create(userId, journalTagId, true, visible));
        preference.update(true, visible);
        preferenceRepository.save(preference);
    }

    private boolean effectiveVisible(UUID userId, JournalTag tag) {
        return preferenceRepository.findById(new UserJournalTagPreferenceId(userId, tag.getId()))
                .map(UserJournalTagPreference::isVisible)
                .orElse(tag.isDefaultVisible());
    }
}
