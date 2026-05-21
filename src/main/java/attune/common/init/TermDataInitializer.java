package attune.common.init;

import attune.term.domain.model.Term;
import attune.term.domain.model.TermType;
import attune.term.domain.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class TermDataInitializer implements ApplicationRunner {

    private final TermRepository termRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime now = LocalDateTime.now();

        Arrays.stream(TermType.values())
                .filter(type -> termRepository.findTopByTypeOrderByVersionDesc(type).isEmpty())
                .forEach(type -> termRepository.save(
                        Term.builder()
                                .version(1)
                                .type(type)
                                .content(testContent(type))
                                .effectiveAt(now)
                                .createdAt(now)
                                .build()
                ));
    }

    private String testContent(TermType type) {
        return switch (type) {
            case TERMS_OF_SERVICE -> """
                    [테스트] 이용약관

                    제1조 (목적)
                    본 약관은 Attune 서비스 이용에 관한 기본 사항을 규정합니다.

                    제2조 (이용계약 체결)
                    이용계약은 회원가입 시 약관에 동의함으로써 성립됩니다.

                    제3조 (서비스 이용)
                    회원은 본 약관 및 관계 법령을 준수하여 서비스를 이용해야 합니다.
                    """;
            case PRIVACY_POLICY -> """
                    [테스트] 개인정보 처리방침

                    1. 수집하는 개인정보 항목
                    이메일, 닉네임, 비밀번호, 복약 정보

                    2. 개인정보 수집 및 이용 목적
                    서비스 제공 및 회원 관리

                    3. 개인정보 보유 및 이용 기간
                    회원 탈퇴 시까지 보관하며, 탈퇴 후 즉시 파기합니다.
                    """;
            case MARKETING_CONSENT -> """
                    [테스트] 마케팅 정보 수신 동의 (선택)

                    Attune의 새로운 기능 안내, 이벤트, 프로모션 등 마케팅 정보를
                    이메일로 수신하는 것에 동의합니다.

                    * 동의하지 않아도 서비스 이용에 제한이 없습니다.
                    """;
        };
    }
}
