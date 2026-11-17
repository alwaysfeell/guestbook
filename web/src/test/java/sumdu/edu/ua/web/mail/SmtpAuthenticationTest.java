package sumdu.edu.ua.web.mail;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smtp-auth;DB_CLOSE_DELAY=-1",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.username=bookapp@bookapp.test",
        "spring.mail.password=wrong-password",
        "spring.mail.properties.mail.smtp.auth=true",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false",
        "app.mail.to=admin@bookapp.test"
})
class SmtpAuthenticationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("bookapp@bookapp.test", "app-password"));

    @Autowired
    private MailService mailService;

    @Test
    void wrongPasswordIsReportedAsAuthenticationError() {
        assertThatThrownBy(() -> mailService.sendHtml("admin@bookapp.test", "Test", "<p>Test</p>"))
                .isInstanceOf(MailSendException.class)
                .hasCauseInstanceOf(MailAuthenticationException.class);
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    @Test
    void senderAndRecipientFallBackToSmtpLogin() {
        assertThat(mailService.sender()).isEqualTo("bookapp@bookapp.test");
        assertThat(mailService.recipient()).isEqualTo("admin@bookapp.test");
    }
}
