package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
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
        properties = "spring.datasource.url=jdbc:h2:mem:mvc-config;DB_CLOSE_DELAY=-1")
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
    void guestIsRedirectedFromRootToLoginPage() {
        ResponseEntity<String> page = http.getForEntity("/", String.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(page.getBody()).contains("<h1>Вхід</h1>").contains("name=\"_csrf\"");
    }

    @Test
    void staticStylesheetIsServedWithoutLogin() {
        ResponseEntity<String> css = http.getForEntity("/css/style.css", String.class);
        assertThat(css.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(css.getHeaders().getContentType().isCompatibleWith(MediaType.valueOf("text/css"))).isTrue();
    }

    @Test
    void loginAndRegisterPagesArePublic() {
        assertThat(http.getForEntity("/login", String.class).getBody()).contains("action=\"/login\"");
        assertThat(http.getForEntity("/register", String.class).getBody()).contains("<h1>Реєстрація</h1>");
    }
}
