package sumdu.edu.ua.web.mail;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.service.BookService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mail-service;DB_CLOSE_DELAY=-1",
        "mail.resend.api-key=re_test_key",
        "mail.resend.from=BookApp <onboarding@resend.dev>",
        "app.mail.admin=admin@bookapp.test"
})
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class MailServiceTest {

    static final ResendStub resend = new ResendStub();

    @DynamicPropertySource
    static void resendUrl(DynamicPropertyRegistry registry) {
        registry.add("mail.resend.api-url", resend::url);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MailService mailService;

    @Autowired
    private BookService books;

    @BeforeEach
    void resetStub() {
        resend.reset();
    }

    @Test
    void addingBookSendsHtmlEmailThroughResendApi() throws Exception {
        mvc.perform(post("/books/add").with(csrf())
                        .param("title", "Intermezzo")
                        .param("author", "Михайло Коцюбинський")
                        .param("pubYear", "1908"))
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("notice", "book.added"))
                .andExpect(flash().attributeCount(2));

        assertThat(resend.requests()).hasSize(1);
        ResendStub.Request request = resend.requests().get(0);
        assertThat(request.authorization()).isEqualTo("Bearer re_test_key");
        assertThat(request.contentType()).startsWith("application/json");

        JsonNode body = request.body();
        assertThat(body.get("from").asText()).isEqualTo("BookApp <onboarding@resend.dev>");
        assertThat(body.get("to")).hasSize(1);
        assertThat(body.get("to").get(0).asText()).isEqualTo("admin@bookapp.test");
        assertThat(body.get("subject").asText()).isEqualTo("Нова книга в каталозі: Intermezzo");
        assertThat(body.get("html").asText())
                .contains("Нова книга додана до каталогу")
                .contains("Назва: <b>Intermezzo</b>")
                .contains("Раритетне видання!")
                .containsPattern("href=\"http://localhost:8080/books/\\d+/comments\"")
                .contains("raw.githubusercontent.com/alwaysfeell/guestbook/lab7/web/src/main/resources/static/img/logo.png")
                .contains("Effective Java &mdash; Joshua Bloch, 2018");
    }

    @Test
    void modernBookEmailHasNoRareBlock() {
        Book book = books.add("Kotlin in Action", "Dmitry Jemerov", 2024);
        mailService.sendNewBookEmail(book);

        assertThat(resend.requests()).hasSize(1);
        assertThat(resend.requests().get(0).body().get("html").asText())
                .contains("Kotlin in Action")
                .doesNotContain("Раритетне");
    }

    @Test
    void resendErrorIsReportedAsMailDeliveryException() {
        resend.respondWith(403, "{\"statusCode\":403,\"message\":\"You can only send testing emails to your own email address\"}");

        assertThatThrownBy(() -> mailService.sendHtml("someone@example.com", "Test", "<p>Test</p>"))
                .isInstanceOf(MailDeliveryException.class)
                .hasMessage("Resend API error, status 403");
        assertThat(resend.requests()).hasSize(1);
    }

    @Test
    void bookIsSavedAndAlertShownWhenResendFails() throws Exception {
        resend.respondWith(500, "{\"message\":\"internal error\"}");

        mvc.perform(post("/books/add").with(csrf())
                        .param("title", "Сад Гетсиманський")
                        .param("author", "Іван Багряний")
                        .param("pubYear", "1950"))
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("notice", "book.added"))
                .andExpect(flash().attribute("alert", "mail.failed"));

        assertThat(books.search("Гетсиманський", 0, 10, "id,asc").getTotal()).isEqualTo(1);
    }
}
