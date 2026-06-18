package attune.common.migration;

import attune.journal.application.DefaultTagService;
import attune.journal.domain.repository.ConditionTagRepository;
import attune.journal.domain.repository.SideEffectTagRepository;
import attune.journal.domain.repository.TroubleTagRepository;
import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.migration.default-tags.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DefaultTagMigrationRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ConditionTagRepository conditionTagRepository;
    private final SideEffectTagRepository sideEffectTagRepository;
    private final TroubleTagRepository troubleTagRepository;
    private final DefaultTagService defaultTagService;

    @Override
    public void run(ApplicationArguments args) {
        List<User> allUsers = userRepository.findAll();
        int count = 0;

        for (User user : allUsers) {
            if (user.getUserStatus() != UserStatus.ACTIVE) {
                continue;
            }

            boolean copied = false;

            if (preferenceRepository.findAllByUserId(user.getId()).isEmpty()) {
                defaultTagService.copyDefaultTagsForUser(user.getId());
                copied = true;
            }
            if (sideEffectTagRepository.findAllByUserId(user.getId()).isEmpty()) {
                defaultTagService.copySideEffectTagsForUser(user.getId());
                copied = true;
            }
            if (troubleTagRepository.findAllByUserId(user.getId()).isEmpty()) {
                defaultTagService.copyTroubleTagsForUser(user.getId());
                copied = true;
            }

            if (copied) count++;
        }

        if (count > 0) {
            log.info("기본 태그 마이그레이션 완료: {}명", count);
        }
    }
}
