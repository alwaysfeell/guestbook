package sumdu.edu.ua.web.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.service.BookService;

import java.time.Clock;
import java.time.Duration;
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

    private final RestTemplate restTemplate;
    private final EmailTemplateProcessor templateProcessor;
    private final BookService books;
    private final AppMailProperties props;
    private final ResendProperties resend;
    private final Clock clock;

    public MailService(RestTemplateBuilder restTemplateBuilder,
                       EmailTemplateProcessor templateProcessor,
                       BookService books,
                       AppMailProperties props,
                       ResendProperties resend,
                       Clock clock) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.templateProcessor = templateProcessor;
        this.books = books;
        this.props = props;
        this.resend = resend;
        this.clock = clock;
    }

    public void sendNewBookEmail(Book book) {
        String html = templateProcessor.process(NEW_BOOK_TEMPLATE, newBookModel(book));
        sendHtml(props.admin(), "Нова книга в каталозі: " + book.getTitle(), html);
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
        if (isBlank(resend.apiKey())) {
            log.error("Resend API key is not configured (RESEND_API_KEY). Cannot send email '{}'", subject);
            throw new MailDeliveryException("Resend API key is not configured");
        }
        if (isBlank(to)) {
            log.error("Recipient is not configured (APP_MAIL_ADMIN). Cannot send email '{}'", subject);
            throw new MailDeliveryException("Recipient is not configured: set app.mail.admin");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resend.apiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("from", resend.from());
        body.put("to", List.of(to));
        body.put("subject", subject);
        body.put("html", html);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(resend.apiUrl(), new HttpEntity<>(body, headers), String.class);
            log.info("Email sent to {} via Resend with subject '{}', status {}, response {}",
                    to, subject, response.getStatusCode().value(), response.getBody());
        } catch (RestClientResponseException e) {
            log.error("Resend rejected email to {}: status {}, body {}",
                    to, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new MailDeliveryException("Resend API error, status " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            log.error("Failed to send email to {} via Resend: {}", to, e.getMessage());
            throw new MailDeliveryException("Cannot send email to " + to, e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSettings() {
        log.info("Mail: Resend HTTP API {}, api key configured={}, from={}, admin={}",
                resend.apiUrl(), !isBlank(resend.apiKey()), resend.from(), blankAsDash(props.admin()));
        if (isBlank(resend.apiKey())) {
            log.warn("Mail: RESEND_API_KEY is empty - emails will not be sent, confirmation links are written to the log");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankAsDash(String s) {
        return isBlank(s) ? "-" : s;
    }
}
