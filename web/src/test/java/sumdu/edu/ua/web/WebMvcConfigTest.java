package sumdu.edu.ua.web;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import sumdu.edu.ua.config.WebConfig;
import sumdu.edu.ua.web.error.ApiErrorController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.db.url=jdbc:h2:mem:mvc-config;DB_CLOSE_DELAY=-1")
class WebMvcConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestRestTemplate http;

    @Test
    void webConfigEnablesMvcExplicitly() {
        assertThat(WebConfig.class).hasAnnotation(EnableWebMvc.class);
        assertThat(context.getBean(WebConfig.class)).isNotNull();
        assertThat(context.getBeanNamesForType(WebMvcAutoConfiguration.class)).isEmpty();
    }

    @Test
    void dispatcherServletIsRegisteredAutomatically() {
        assertThat(context.getBean(DispatcherServlet.class)).isNotNull();
        DispatcherServletRegistrationBean reg = context.getBean(DispatcherServletRegistrationBean.class);
        assertThat(reg.getPath()).isEqualTo("/");
        assertThat(reg.getUrlMappings()).containsExactly("/");
    }

    @Test
    void homeControllerReturnsViewResolvedToStaticPage() {
        ResponseEntity<String> page = http.getForEntity("/", String.class);
        assertThat(page.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(page.getBody()).contains("<h1>Каталог книг</h1>");

        assertThat(http.getForEntity("/book.html", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void jsonDatesAreIsoStringsThanksToBootObjectMapper() {
        ResponseEntity<Map> book = http.getForEntity("/books/11", Map.class);
        Map<?, ?> comments = (Map<?, ?>) book.getBody().get("comments");
        Map<?, ?> first = (Map<?, ?>) ((java.util.List<?>) comments.get("items")).get(0);
        assertThat(first.get("createdAt")).isInstanceOf(String.class);
        assertThat((String) first.get("createdAt")).matches("\\d{4}-\\d{2}-\\d{2}T.*Z");
    }

    @Test
    void unknownUrlGivesApiError() {
        ResponseEntity<Map> resp = http.getForEntity("/nope", Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).containsEntry("status", 404).containsEntry("path", "/nope");
    }

    @Test
    void errorPathUsesApiErrorFormat() throws Exception {
        assertThat(context.getBean(ErrorController.class)).isInstanceOf(ApiErrorController.class);

        MockMvc mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
        mvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/books")
                        .requestAttr(RequestDispatcher.ERROR_MESSAGE, "Boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value("/books"));
    }
}
