package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.db.url=jdbc:h2:mem:mvc-config;DB_CLOSE_DELAY=-1")
class WebMvcConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestRestTemplate http;

    @Test
    void springBootMvcAutoConfigurationIsActive() {
        assertThat(context.getBeanNamesForType(WebMvcAutoConfiguration.class)).isNotEmpty();
        assertThat(context.getBean(ThymeleafViewResolver.class)).isNotNull();
        assertThat(context.getBean(LocaleResolver.class)).isInstanceOf(CookieLocaleResolver.class);
    }

    @Test
    void dispatcherServletIsRegisteredAutomatically() {
        assertThat(context.getBean(DispatcherServlet.class)).isNotNull();
        DispatcherServletRegistrationBean reg = context.getBean(DispatcherServletRegistrationBean.class);
        assertThat(reg.getPath()).isEqualTo("/");
    }

    @Test
    void rootRedirectsToCatalogPage() {
        ResponseEntity<String> page = http.getForEntity("/", String.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(page.getBody()).contains("<h1>Каталог книг</h1>");
    }

    @Test
    void staticStylesheetIsServed() {
        ResponseEntity<String> css = http.getForEntity("/css/style.css", String.class);
        assertThat(css.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(css.getHeaders().getContentType().isCompatibleWith(MediaType.valueOf("text/css"))).isTrue();
    }

    @Test
    void unknownUrlRendersHtmlErrorPage() {
        ResponseEntity<String> resp = http.getForEntity("/nope", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("Сторінку не знайдено").contains("No route for GET /nope");
    }

    @Test
    void unsupportedMethodIsHandledByErrorTemplate() {
        ResponseEntity<String> resp = http.exchange("/books", HttpMethod.DELETE, null, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(resp.getBody()).contains("Method Not Allowed").contains("Код відповіді: 405");
    }
}
