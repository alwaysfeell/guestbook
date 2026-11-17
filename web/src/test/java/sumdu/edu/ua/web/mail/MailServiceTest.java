package sumdu.edu.ua.web.mail;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.service.BookService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest(properties = {
        "app.db.url=jdbc:h2:mem:mail-service;DB_CLOSE_DELAY=-1",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false",
        "app.mail.to=admin@bookapp.test",
        "app.mail.from=bookapp@bookapp.test"
})
@AutoConfigureMockMvc
class MailServiceTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MailService mailService;

    @Autowired
    private BookService books;

    @Test
    void addingBookSendsHtmlEmail() throws Exception {
        mvc.perform(post("/books/add")
                        .param("title", "Intermezzo")
                        .param("author", "Михайло Коцюбинський")
                        .param("pubYear", "1908"))
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("notice", "book.added"))
                .andExpect(flash().attributeCount(2));

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).isEqualTo("Нова книга в каталозі: Intermezzo");
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("bookapp@bookapp.test");
        assertThat(from.getPersonal()).isEqualTo("BookApp");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("admin@bookapp.test");

        String html = htmlPart(message);
        assertThat(html)
                .contains("Нова книга додана до каталогу")
                .contains("Назва: <b>Intermezzo</b>")
                .contains("Раритетне видання!")
                .containsPattern("href=\"http://localhost:8080/books/\\d+/comments\"")
                .contains("raw.githubusercontent.com/alwaysfeell/guestbook/lab7/web/src/main/resources/static/img/logo.png")
                .contains("Effective Java &mdash; Joshua Bloch, 2018");
    }

    @Test
    void modernBookEmailHasNoRareBlock() throws Exception {
        Book book = books.add("Kotlin in Action", "Dmitry Jemerov", 2024);
        mailService.sendNewBookEmail(book);

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();
        String html = htmlPart(greenMail.getReceivedMessages()[0]);
        assertThat(html).contains("Kotlin in Action").doesNotContain("Раритетне");
    }

    private static String htmlPart(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart body = multipart.getBodyPart(i);
                String html = htmlPart(body);
                if (html != null) {
                    return html;
                }
            }
        }
        return null;
    }
}
