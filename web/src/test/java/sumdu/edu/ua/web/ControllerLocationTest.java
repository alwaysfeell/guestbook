package sumdu.edu.ua.web;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ControllerLocationTest {
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("sumdu.edu.ua");
    }

    @Test
    void noHandWrittenServletsOrFilters() {
        noClasses()
                .should().beAssignableTo(HttpServlet.class)
                .orShould().beAssignableTo(Filter.class)
                .because("HTTP-запити обробляє DispatcherServlet і контролери Spring MVC")
                .check(classes);
    }

    @Test
    void controllersShouldResideInWebController() {
        classes()
                .that().areAnnotatedWith(RestController.class)
                .or().areAnnotatedWith(Controller.class)
                .should().resideInAnyPackage("sumdu.edu.ua.web.controller..", "sumdu.edu.ua.web.error..")
                .andShould().haveSimpleNameEndingWith("Controller")
                .check(classes);
    }

    @Test
    void controllerAdviceShouldResideInWebError() {
        classes()
                .that().areAnnotatedWith(RestControllerAdvice.class)
                .should().resideInAPackage("sumdu.edu.ua.web.error..")
                .check(classes);
    }

    @Test
    void repositoriesShouldResideOnlyInPersistence() {
        classes()
                .that().haveSimpleNameEndingWith("Repository")
                .or().haveSimpleNameEndingWith("Dao")
                .should().resideInAPackage("sumdu.edu.ua.persistence..")
                .check(classes);
    }
}
