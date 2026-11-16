package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.ClassUtils;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.info.AppInfoService;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.db.url=jdbc:h2:mem:boot-test;DB_CLOSE_DELAY=-1")
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

        assertThat(ClassUtils.getUserClass(context.getBean(CatalogRepositoryPort.class)).getSimpleName())
                .isEqualTo("JdbcBookRepository");
        assertThat(ClassUtils.getUserClass(context.getBean(CommentRepositoryPort.class)).getSimpleName())
                .isEqualTo("JdbcCommentRepository");
        assertThat(context.containsBean("db")).isTrue();
        assertThat(context.containsBean("dbInit")).isTrue();
    }

    @Test
    void beansAreSingletons() {
        assertThat(context.getBean(BookService.class)).isSameAs(context.getBean(BookService.class));
        assertThat(context.getBean(CatalogRepositoryPort.class)).isSameAs(context.getBean(CatalogRepositoryPort.class));
    }

    @Test
    void mvcControllersAreRegistered() {
        assertThat(context.getBeansWithAnnotation(Controller.class))
                .containsKeys("booksController", "commentsController", "homeController", "infoController");
        assertThat(context.getBeansWithAnnotation(RestController.class)).isEmpty();
        assertThat(context.getBeansOfType(ServletRegistrationBean.class)).containsOnlyKeys("dispatcherServletRegistration");
        assertThat(context.getBean(AppInfoService.class).endpoints())
                .anyMatch(e -> e.startsWith("GET") && e.contains("/books/add"))
                .anyMatch(e -> e.startsWith("POST") && e.contains("/books/{bookId}/comments"))
                .anyMatch(e -> e.startsWith("POST") && e.contains("/books/{bookId}/comments/{commentId}/delete"));
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
    void httpRequestsReturnRenderedPages() {
        ResponseEntity<String> books = http.getForEntity("/books?size=5", String.class);
        assertThat(books.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(books.getBody()).contains("Кобзар").contains("Сторінка 1 з 3");

        ResponseEntity<String> missing = http.getForEntity("/books/999/comments", String.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).contains("Book with id=999 not found");

        ResponseEntity<Map> info = http.getForEntity("/info", Map.class);
        assertThat(info.getBody()).containsEntry("application", "guestbook");
    }
}
