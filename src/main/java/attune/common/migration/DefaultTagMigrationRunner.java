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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

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

    private static final int BATCH_SIZE = 100;

    @Override
    public void run(ApplicationArguments args) {
        Set<UUID> usersWithPreferences = preferenceRepository.findDistinctUserIds();
        int count = 0;
        int pageNumber = 0;
        Slice<User> page;

        do {
            page = userRepository.findAllByUserStatus(UserStatus.ACTIVE, PageRequest.of(pageNumber++, BATCH_SIZE));
            for (User user : page.getContent()) {
                if (!usersWithPreferences.contains(user.getId())) {
                    defaultTagService.copyDefaultTagsForUser(user.getId());
                    count++;
                }
            }
        } while (page.hasNext());

        if (count > 0) {
            log.info("기본 태그 마이그레이션 완료: {}명", count);
        }
    }
}
