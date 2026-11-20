package sumdu.edu.ua.web.mail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import sumdu.edu.ua.core.service.BookService;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ResendSettingsTest {

    private static MailService service(String apiKey, String admin) {
        return new MailService(new RestTemplateBuilder(),
                mock(EmailTemplateProcessor.class),
                mock(BookService.class),
                new AppMailProperties(admin, "http://localhost:8080", ""),
                new ResendProperties("http://localhost:1/emails", apiKey, "BookApp <onboarding@resend.dev>"),
                Clock.systemUTC());
    }

    @Test
    void missingApiKeyIsReportedWithoutHttpCall() {
        assertThatThrownBy(() -> service("", "admin@bookapp.test").sendHtml("admin@bookapp.test", "Test", "<p>Test</p>"))
                .isInstanceOf(MailDeliveryException.class)
                .hasMessage("Resend API key is not configured");
    }

    @Test
    void missingRecipientIsReported() {
        assertThatThrownBy(() -> service("re_test_key", "").sendHtml("", "Test", "<p>Test</p>"))
                .isInstanceOf(MailDeliveryException.class)
                .hasMessage("Recipient is not configured: set app.mail.admin");
    }

    @Test
    void unreachableApiIsReported() {
        assertThatThrownBy(() -> service("re_test_key", "admin@bookapp.test").sendHtml("admin@bookapp.test", "Test", "<p>Test</p>"))
                .isInstanceOf(MailDeliveryException.class)
                .hasMessage("Cannot send email to admin@bookapp.test");
    }
}
