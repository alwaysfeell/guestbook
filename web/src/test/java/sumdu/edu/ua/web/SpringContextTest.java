package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.ClassUtils;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import sumdu.edu.ua.core.port.UserRepositoryPort;
import sumdu.edu.ua.core.security.AppUserDetailsService;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.core.service.UserService;
import sumdu.edu.ua.web.info.AppInfoService;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:boot-test;DB_CLOSE_DELAY=-1")
class SpringContextTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestRestTemplate http;

    @Test
    void allApplicationBeansAreLoaded() {
        assertThat(context.getBean(BookService.class)).isNotNull();
        assertThat(context.getBean(CommentService.class)).isNotNull();
        assertThat(context.getBean(Clock.class)).isEqualTo(Clock.systemUTC());
        assertThat(context.getBean(AppInfoService.class)).isNotNull();

        assertThat(context.getBean(UserService.class)).isNotNull();

        assertThat(ClassUtils.getUserClass(context.getBean(CatalogRepositoryPort.class)).getSimpleName())
                .isEqualTo("JpaCatalogRepositoryAdapter");
        assertThat(ClassUtils.getUserClass(context.getBean(CommentRepositoryPort.class)).getSimpleName())
                .isEqualTo("JpaCommentRepositoryAdapter");
        assertThat(ClassUtils.getUserClass(context.getBean(UserRepositoryPort.class)).getSimpleName())
                .isEqualTo("JpaUserRepositoryAdapter");
        assertThat(context.containsBean("entityManagerFactory")).isTrue();
        assertThat(context.containsBean("flyway")).isTrue();
        assertThat(context.containsBean("db")).isFalse();
        assertThat(context.containsBean("dbInit")).isFalse();
    }

    @Test
    void servicesAreWrappedInTransactionalProxies() {
        assertThat(AopUtils.isAopProxy(context.getBean(BookService.class))).isTrue();
        assertThat(AopUtils.isAopProxy(context.getBean(CommentService.class))).isTrue();
        assertThat(AopUtils.isAopProxy(context.getBean(UserService.class))).isTrue();
        assertThat(context.getBean(PlatformTransactionManager.class)).isInstanceOf(JpaTransactionManager.class);
    }

    @Test
    void beansAreSingletons() {
        assertThat(context.getBean(BookService.class)).isSameAs(context.getBean(BookService.class));
        assertThat(context.getBean(CatalogRepositoryPort.class)).isSameAs(context.getBean(CatalogRepositoryPort.class));
    }

    @Test
    void mvcControllersAreRegistered() {
        assertThat(context.getBeansWithAnnotation(Controller.class))
                .containsKeys("booksController", "commentsController", "userController", "homeController", "infoController");
        assertThat(context.getBeansWithAnnotation(RestController.class)).containsOnlyKeys("booksApiController");
        assertThat(context.getBeansWithAnnotation(Controller.class))
                .containsKeys("authController", "errorPagesController", "booksApiController");
        assertThat(context.getBeansOfType(ServletRegistrationBean.class)).containsOnlyKeys("dispatcherServletRegistration", "h2Console");
        assertThat(context.getBean(AppInfoService.class).endpoints())
                .anyMatch(e -> e.startsWith("GET") && e.contains("/books/add"))
                .anyMatch(e -> e.startsWith("POST") && e.contains("/books/{bookId}/comments"))
                .anyMatch(e -> e.startsWith("POST") && e.contains("/books/{bookId}/comments/{commentId}/delete"))
                .anyMatch(e -> e.startsWith("GET") && e.contains("/users/{id}/comments"))
                .anyMatch(e -> e.startsWith("POST") && e.contains("/books/{id}/delete"));
    }

    @Test
    void securityBeansAreConfigured() {
        assertThat(context.getBean(PasswordEncoder.class)).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(context.getBean(UserDetailsService.class)).isInstanceOf(AppUserDetailsService.class);
        assertThat(context.getBeansOfType(SecurityFilterChain.class))
                .containsOnlyKeys("apiSecurityFilterChain", "webSecurityFilterChain");
        assertThat(context.getBean(AppInfoService.class).endpoints())
                .anyMatch(e -> e.startsWith("GET") && e.contains("/login"))
                .anyMatch(e -> e.startsWith("POST") && e.contains("/register"))
                .anyMatch(e -> e.startsWith("GET") && e.contains("/confirm"))
                .anyMatch(e -> e.startsWith("DELETE") && e.contains("/api/books/{id}"));
    }

    @Test
    void propertiesFromYamlAreApplied() {
        AppProperties props = context.getBean(AppProperties.class);
        assertThat(props.title()).isEqualTo("Каталог книг і відгуки");
        assertThat(props.catalog().defaultPageSize()).isEqualTo(10);
        assertThat(context.getBean(CommentService.class).getDeleteWindow()).isEqualTo(Duration.ofHours(24));
        assertThat(context.getEnvironment().getProperty("spring.application.name")).isEqualTo("guestbook");
    }

    @Test
    void constructorAndFieldInjectionWorkTogether() {
        Map<String, Object> info = context.getBean(AppInfoService.class).info();
        assertThat(info.get("application")).isEqualTo("guestbook");
        assertThat(info.get("booksTotal")).isEqualTo(12L);
        assertThat(info.get("beansInContext")).isInstanceOf(Integer.class);
    }

    @Test
    void guestSeesLoginPageInsteadOfCatalog() {
        ResponseEntity<String> books = http.getForEntity("/books?size=5", String.class);
        assertThat(books.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(books.getBody()).contains("<h1>Вхід</h1>").doesNotContain("Кобзар");
    }

    @Test
    void apiRequestsOverRealHttpUseBasicAuthentication() {
        ResponseEntity<Map> guest = http.getForEntity("/api/books", Map.class);
        assertThat(guest.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(guest.getBody()).containsEntry("status", 401);

        ResponseEntity<Map> admin = http.withBasicAuth("admin", "admin123").getForEntity("/api/books?size=5", Map.class);
        assertThat(admin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(admin.getBody()).containsEntry("total", 12);

        ResponseEntity<Map> missing = http.withBasicAuth("olena", "user123").getForEntity("/api/books/999", Map.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).containsEntry("message", "Book with id=999 not found");
    }
}
