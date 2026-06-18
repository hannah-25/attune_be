package attune.common.migration;

import attune.journal.application.DefaultTagService;
import attune.journal.domain.repository.UserJournalTagPreferenceRepository;
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
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.migration.default-tags.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DefaultTagMigrationRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserJournalTagPreferenceRepository preferenceRepository;
    private final DefaultTagService defaultTagService;

    @Override
    public void run(ApplicationArguments args) {
        List<User> allUsers = userRepository.findAll();
        Set<UUID> usersWithPreferences = preferenceRepository.findDistinctUserIds();
        int count = 0;

        for (User user : allUsers) {
            if (user.getUserStatus() != UserStatus.ACTIVE) {
                continue;
            }

            if (!usersWithPreferences.contains(user.getId())) {
                defaultTagService.copyDefaultTagsForUser(user.getId());
                count++;
            }
        }

        if (count > 0) {
            log.info("기본 태그 마이그레이션 완료: {}명", count);
        }
    }
}
