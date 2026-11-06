package sumdu.edu.ua.core;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
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
                .should().bePublic()
                .check(core);
    }
}
