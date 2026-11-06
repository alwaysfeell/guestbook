package sumdu.edu.ua.web;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class WebArchitectureTest {
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("sumdu.edu.ua");
    }

    @Test
    void webShouldNotDependOnPersistence() {
        noClasses()
                .that().resideInAPackage("sumdu.edu.ua.web..")
                .should().dependOnClassesThat()
                .resideInAPackage("sumdu.edu.ua.persistence..")
                .because("web працює лише з сервісами та портами core")
                .check(classes);
    }

    @Test
    void webShouldNotUseJdbcDirectly() {
        noClasses()
                .that().resideInAPackage("sumdu.edu.ua.web..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("java.sql..", "javax.sql..", "org.h2..")
                .check(classes);
    }

    @Test
    void layersShouldBeRespected() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Core").definedBy("sumdu.edu.ua.core..")
                .layer("Persistence").definedBy("sumdu.edu.ua.persistence..")
                .layer("Web").definedBy("sumdu.edu.ua.web..")
                .layer("Config").definedBy("sumdu.edu.ua.config..")
                .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                .whereLayer("Config").mayNotBeAccessedByAnyLayer()
                .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Config")
                .whereLayer("Core").mayOnlyBeAccessedByLayers("Persistence", "Web", "Config")
                .check(classes);
    }
}
