package attune.admin.member.domain.repository;

import attune.user.domain.model.User;
import attune.user.domain.model.UserStatus;
import attune.user.domain.model.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AdminMemberRepositoryTest {

    @Autowired
    private AdminMemberRepository repository;

    @Test
    void searchesByTextStatusAndUuid() {
        User older = repository.save(user(
                "alpha@example.com", "알파", UserStatus.ACTIVE,
                LocalDateTime.of(2026, 1, 1, 0, 0)
        ));
        User newer = repository.save(user(
                "beta@example.com", "베타", UserStatus.WITHDRAWAL,
                LocalDateTime.of(2026, 2, 1, 0, 0)
        ));
        PageRequest pageable = PageRequest.of(
                0, 20, Sort.by(Sort.Direction.DESC, "createdAt")
        );

        assertThat(repository.search(null, "beta", UserStatus.WITHDRAWAL, pageable).getContent())
                .extracting(User::getId)
                .containsExactly(newer.getId());
        assertThat(repository.search(older.getId(), null, null, pageable).getContent())
                .extracting(User::getId)
                .containsExactly(older.getId());
        assertThat(repository.countAllByStatus())
                .extracting(count -> count.getStatus() + ":" + count.getCount())
                .contains("ACTIVE:1", "WITHDRAWAL:1");
    }

    private User user(
            String email,
            String nickname,
            UserStatus status,
            LocalDateTime createdAt
    ) {
        return User.builder()
                .email(email)
                .nickname(nickname)
                .userType(UserType.USER)
                .userStatus(status)
                .createdAt(createdAt)
                .build();
    }
}
