package attune.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 헥사고날 아키텍처 규칙 강제(ArchUnit).
 *
 * <p>규칙 설명: docs/architecture/dependency-rules.md, module-rules.md.
 * 여기서 실패하면 계층/레이아웃 위반이다. 불가피한 예외는 규칙을 완화하기 전에
 * docs/exec-plans/tech-debt-tracker.md 에 사유를 남긴다.
 *
 * <p>현재 코드의 실제 관례를 반영해, 공유 커널 {@code attune.common} 과
 * 일부 평면 레이아웃(예: admin/audit 의 domain 패키지)을 허용한다.
 */
class ArchitectureRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages("attune");

    /** 도메인은 바깥(어댑터)을 모른다. */
    @Test
    void domain_should_not_depend_on_adapter() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..");
        rule.check(CLASSES);
    }

    /** 도메인은 애플리케이션 계층도 모른다. */
    @Test
    void domain_should_not_depend_on_application() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..");
        rule.check(CLASSES);
    }

    /** REST 컨트롤러는 adapter.web (또는 공통 web/error)에만 둔다. */
    @Test
    void controllers_should_reside_in_adapter_web() {
        ArchRule rule = classes()
                .that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAnyPackage("..adapter.web..", "attune.common..");
        rule.check(CLASSES);
    }

    /** 서비스는 application (또는 공통 인프라 서비스)에만 둔다. */
    @Test
    void services_should_reside_in_application() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().resideInAnyPackage("..application..", "attune.common..");
        rule.check(CLASSES);
    }

    /** Spring Data Repository 인터페이스는 domain 계층에 둔다. */
    @Test
    void repositories_should_reside_in_domain() {
        ArchRule rule = classes()
                .that().areInterfaces()
                .and().haveSimpleNameEndingWith("Repository")
                .should().resideInAPackage("..domain..");
        rule.check(CLASSES);
    }
}
