package sumdu.edu.ua.web.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.service.BookService;

import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    public static final String NEW_BOOK_TEMPLATE = "new_book.ftl";
    public static final String CONFIRM_TEMPLATE = "confirm_account.ftl";
    private static final int RECENT_LIMIT = 3;

    private final JavaMailSender mailSender;
    private final EmailTemplateProcessor templateProcessor;
    private final BookService books;
    private final AppMailProperties props;
    private final MailProperties smtp;
    private final Clock clock;

    public MailService(JavaMailSender mailSender,
                       EmailTemplateProcessor templateProcessor,
                       BookService books,
                       AppMailProperties props,
                       MailProperties smtp,
                       Clock clock) {
        this.mailSender = mailSender;
        this.templateProcessor = templateProcessor;
        this.books = books;
        this.props = props;
        this.smtp = smtp;
        this.clock = clock;
    }

    public void sendNewBookEmail(Book book) {
        String html = templateProcessor.process(NEW_BOOK_TEMPLATE, newBookModel(book));
        sendHtml(recipient(), "Нова книга в каталозі: " + book.getTitle(), html);
    }

    public void sendConfirmationEmail(User user) {
        String html = templateProcessor.process(CONFIRM_TEMPLATE, confirmationModel(user));
        sendHtml(user.getEmail(), "Підтвердження реєстрації в каталозі книг", html);
    }

    public String confirmationLink(User user) {
        return props.baseUrl() + "/confirm?code=" + user.getConfirmationCode();
    }

    public Map<String, Object> confirmationModel(User user) {
        Map<String, Object> model = new HashMap<>();
        model.put("username", user.getUsername());
        model.put("email", user.getEmail());
        model.put("confirmUrl", confirmationLink(user));
        model.put("catalogUrl", props.baseUrl() + "/books");
        model.put("logoUrl", props.logoUrl());
        model.put("registered", Date.from(clock.instant()));
        return model;
    }

    public Map<String, Object> newBookModel(Book book) {
        Page<Book> latest = books.search(null, 0, RECENT_LIMIT + 1, "id,desc");
        List<Book> recent = latest.getItems().stream()
                .filter(b -> b.getId() != book.getId())
                .limit(RECENT_LIMIT)
                .toList();

        Map<String, Object> model = new HashMap<>();
        model.put("id", book.getId());
        model.put("title", book.getTitle());
        model.put("author", book.getAuthor());
        model.put("year", book.getPubYear());
        model.put("added", Date.from(clock.instant()));
        model.put("bookUrl", props.baseUrl() + "/books/" + book.getId() + "/comments");
        model.put("catalogUrl", props.baseUrl() + "/books");
        model.put("logoUrl", props.logoUrl());
        model.put("recentBooks", recent);
        model.put("totalBooks", latest.getTotal());
        return model;
    }

    public void sendHtml(String to, String subject, String html) {
        if (to.isBlank()) {
            throw new MailSendException("Recipient is not configured: set app.mail.to or spring.mail.username");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom(sender(), props.fromName());
            mailSender.send(message);
            log.info("Email sent to {} via {}:{} with subject '{}'", to, smtp.getHost(), smtp.getPort(), subject);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.error("Failed to send email to {} via {}:{}: {}", to, smtp.getHost(), smtp.getPort(),
                    NestedExceptionUtils.getMostSpecificCause(e).getMessage());
            throw new MailSendException("Cannot send email to " + to, e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSettings() {
        boolean auth = Boolean.parseBoolean(smtp.getProperties().getOrDefault("mail.smtp.auth", "false"));
        log.info("Mail: SMTP {}:{}, auth={}, from={}, to={}", smtp.getHost(), smtp.getPort(), auth,
                blankAsDash(sender()), blankAsDash(recipient()));
        if (auth && isBlank(smtp.getUsername())) {
            log.warn("Mail: spring.mail.username is empty - create mail.properties in the project root (see README)");
        }
    }

    String recipient() {
        return isBlank(props.to()) ? nullToEmpty(smtp.getUsername()) : props.to();
    }

    String sender() {
        if (!isBlank(props.from())) {
            return props.from();
        }
        return isBlank(smtp.getUsername()) ? "bookapp@localhost" : smtp.getUsername();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String blankAsDash(String s) {
        return isBlank(s) ? "-" : s;
    }
}
