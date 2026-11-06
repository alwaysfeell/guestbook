package sumdu.edu.ua.persistence;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PersistenceArchitectureTest {
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("sumdu.edu.ua");
    }

    @Test
    void repositoriesShouldResideOnlyInPersistence() {
        classes()
                .that().haveSimpleNameEndingWith("Repository")
                .or().haveSimpleNameEndingWith("Dao")
                .should().resideInAPackage("sumdu.edu.ua.persistence..")
                .check(classes);
    }

    @Test
    void repositoriesShouldImplementCorePorts() {
        classes()
                .that().resideInAPackage("sumdu.edu.ua.persistence..")
                .and().haveSimpleNameEndingWith("Repository")
                .should().implement(resideInAPackage("sumdu.edu.ua.core.port.."))
                .check(classes);
    }

    @Test
    void persistenceShouldNotDependOnWebOrServlet() {
        noClasses()
                .that().resideInAPackage("sumdu.edu.ua.persistence..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("sumdu.edu.ua.web..", "sumdu.edu.ua.config..", "jakarta.servlet..")
                .check(classes);
    }

    @Test
    void persistenceShouldNotUseCoreServices() {
        noClasses()
                .that().resideInAPackage("sumdu.edu.ua.persistence..")
                .should().dependOnClassesThat().resideInAPackage("sumdu.edu.ua.core.service..")
                .because("репозиторії лише зберігають дані, бізнес-правила - у сервісах core")
                .check(classes);
    }
}
