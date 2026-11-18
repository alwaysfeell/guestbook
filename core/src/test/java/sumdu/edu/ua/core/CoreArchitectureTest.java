package sumdu.edu.ua.core;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetailsService;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CoreArchitectureTest {
    private static JavaClasses core;

    @BeforeAll
    static void importClasses() {
        core = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("sumdu.edu.ua.core");
    }

    @Test
    void coreShouldNotDependOnServletOrJdbc() {
        noClasses()
                .that().resideInAPackage("sumdu.edu.ua.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("jakarta.servlet..", "javax.servlet..", "java.sql..", "javax.sql..", "org.h2..")
                .because("доменний шар не повинен знати про HTTP та JDBC")
                .check(core);
    }

    @Test
    void coreShouldNotDependOnOtherModules() {
        noClasses()
                .that().resideInAPackage("sumdu.edu.ua.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("sumdu.edu.ua.persistence..", "sumdu.edu.ua.web..", "sumdu.edu.ua.config..")
                .because("залежності спрямовані до core, а не з нього")
                .check(core);
    }

    @Test
    void coreShouldDependOnlyOnSpringSecurityApi() {
        noClasses()
                .that().resideInAPackage("sumdu.edu.ua.core..")
                .should().dependOnClassesThat(
                        resideInAnyPackage("org.springframework..", "jakarta.annotation..", "jakarta.inject..")
                                .and(not(resideInAPackage("org.springframework.security.."))))
                .because("core знає лише API Spring Security (UserDetailsService, PasswordEncoder, @PreAuthorize), "
                        + "а сервіси реєструються як біни в @Configuration-класі")
                .check(core);
    }

    @Test
    void userDetailsServiceShouldResideInCoreSecurity() {
        classes()
                .that().implement(UserDetailsService.class)
                .should().resideInAPackage("sumdu.edu.ua.core.security..")
                .check(core);
    }

    @Test
    void modifyingServiceMethodsShouldBeSecured() {
        methods()
                .that().areDeclaredInClassesThat().resideInAPackage("sumdu.edu.ua.core.service..")
                .and().arePublic()
                .and().haveNameMatching("add|save|update|delete")
                .should().beAnnotatedWith(PreAuthorize.class)
                .check(core);
    }

    @Test
    void onlyServicesAndSecurityUseSpringSecurity() {
        noClasses()
                .that().resideInAnyPackage("sumdu.edu.ua.core.domain..", "sumdu.edu.ua.core.port..",
                        "sumdu.edu.ua.core.exception..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(core);
    }

    @Test
    void coreShouldNotDependOnJpaOrHibernate() {
        noClasses()
                .that().resideInAPackage("sumdu.edu.ua.core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("jakarta.persistence..", "org.hibernate..", "org.flywaydb..")
                .because("сутності JPA і міграції належать модулю persistence")
                .check(core);
    }

    @Test
    void servicesShouldBeTransactional() {
        classes()
                .that().resideInAPackage("sumdu.edu.ua.core.service..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith(jakarta.transaction.Transactional.class)
                .check(core);
    }

    @Test
    void portsShouldBeInterfaces() {
        classes()
                .that().resideInAPackage("sumdu.edu.ua.core.port..")
                .should().beInterfaces()
                .check(core);
    }

    @Test
    void domainShouldNotDependOnServicesOrPorts() {
        noClasses()
                .that().resideInAPackage("sumdu.edu.ua.core.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("sumdu.edu.ua.core.service..", "sumdu.edu.ua.core.port..")
                .check(core);
    }

    @Test
    void coreClassesShouldHavePublicConstructors() {
        constructors()
                .that().areDeclaredInClassesThat().resideInAPackage("sumdu.edu.ua.core..")
                .and().areDeclaredInClassesThat().areNotEnums()
                .should().bePublic()
                .check(core);
    }
}
