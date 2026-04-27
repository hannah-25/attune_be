package attune.term.domain.model;

import attune.user.domain.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "user_term_agreements")
public class UserTermAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;


    private boolean termsOfService;

    private boolean privacyPolicy;

    private boolean marketingConsent;

    //약관 고지일
    private LocalDateTime notifiedAt;
    //약관 동의일
    private LocalDateTime agreedAt;

}