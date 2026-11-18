package sumdu.edu.ua.web.mail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sumdu.edu.ua.config.EmailFreemarkerConfig;
import sumdu.edu.ua.core.domain.Book;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTemplateProcessorTest {
    private static final String LOGO = "https://raw.githubusercontent.com/alwaysfeell/guestbook/lab7/web/src/main/resources/static/img/logo.png";

    private static EmailTemplateProcessor processor;

    @BeforeAll
    static void setUp() {
        processor = new EmailTemplateProcessor(new EmailFreemarkerConfig().emailFreemarkerConfiguration());
    }

    private static Map<String, Object> model(String title, String author, int year) {
        Map<String, Object> model = new HashMap<>();
        model.put("id", 13L);
        model.put("title", title);
        model.put("author", author);
        model.put("year", year);
        model.put("added", Date.from(LocalDateTime.of(2026, 11, 16, 21, 5).atZone(ZoneId.systemDefault()).toInstant()));
        model.put("bookUrl", "http://localhost:8080/books/13/comments");
        model.put("catalogUrl", "http://localhost:8080/books");
        model.put("logoUrl", LOGO);
        model.put("recentBooks", List.of(new Book(12, "Effective Java", "Joshua Bloch", 2018)));
        model.put("totalBooks", 13L);
        return model;
    }

    @Test
    void rendersLayoutVariablesAndFormattedDate() {
        String html = processor.process("new_book.ftl", model("Intermezzo", "Михайло Коцюбинський", 1908));

        assertThat(html)
                .startsWith("<!DOCTYPE html>")
                .endsWith("</html>\n")
                .contains("<img src=\"" + LOGO + "\" style=\"width:150px;margin-bottom:20px;\" alt=\"BookApp\">")
                .contains("Назва: <b>Intermezzo</b>")
                .contains("Автор: <b>Михайло Коцюбинський</b>")
                .contains("Рік видання: <b>1908</b>")
                .contains("Дата додавання: 2026-11-16 21:05")
                .contains("href=\"http://localhost:8080/books/13/comments\"")
                .contains("BookApp &copy; ")
                .contains("Effective Java &mdash; Joshua Bloch, 2018")
                .contains("Усього книг у каталозі: <b>13</b>");
    }

    @Test
    void oldBookIsMarkedAsRare() {
        String html = processor.process("new_book.ftl", model("Кобзар", "Тарас Шевченко", 1840));
        assertThat(html).contains("<b>Раритетне видання!</b> Книзі вже 186 р.");
    }

    @Test
    void modernBookIsNotMarkedAsRare() {
        String html = processor.process("new_book.ftl", model("Clean Code", "Robert C. Martin", 2008));
        assertThat(html).doesNotContain("Раритетне");
    }

    @Test
    void yearWithoutThousandsSeparator() {
        String html = processor.process("new_book.ftl", model("Patterns", "Martin Fowler", 2002));
        assertThat(html).contains("<b>2002</b>").doesNotContain("2,002").doesNotContain("2 002");
    }

    @Test
    void htmlInDataIsEscaped() {
        String html = processor.process("new_book.ftl", model("<script>alert(1)</script>", "A & B", 2020));
        assertThat(html)
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .contains("A &amp; B")
                .doesNotContain("<script>");
    }

    @Test
    void missingVariableFailsRendering() {
        Map<String, Object> model = model("Title", "Author", 2000);
        model.remove("title");
        assertThatThrownBy(() -> processor.process("new_book.ftl", model))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("new_book.ftl");
    }

    @Test
    void rendersConfirmationEmailWithLink() {
        Map<String, Object> model = new HashMap<>();
        model.put("username", "taras");
        model.put("email", "taras@example.com");
        model.put("confirmUrl", "http://localhost:8080/confirm?code=abc-123");
        model.put("catalogUrl", "http://localhost:8080/books");
        model.put("logoUrl", LOGO);
        model.put("registered", Date.from(LocalDateTime.of(2026, 11, 18, 15, 30).atZone(ZoneId.systemDefault()).toInstant()));

        String html = processor.process("confirm_account.ftl", model);
        assertThat(html)
                .contains("Підтвердження реєстрації")
                .contains("Вітаємо, <b>taras</b>!")
                .contains("href=\"http://localhost:8080/confirm?code=abc-123\"")
                .contains("Дата реєстрації: 2026-11-18 15:30")
                .contains("BookApp &copy;");
    }
}
