package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.mail.MailService;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:books-controller;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class BooksControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MailService mailService;

    @Autowired
    private CommentService comments;

    @Test
    void listRendersBooksTemplateWithModel() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("books"))
                .andExpect(model().attribute("books", hasSize(10)))
                .andExpect(model().attributeExists("page"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Каталог книг</h1>"),
                        containsString("Кобзар"),
                        containsString("href=\"/books/1/comments\""),
                        containsString("Сторінка 1 з 2"),
                        containsString("Усього книг: "))));
    }

    @Test
    void searchWithoutResultsShowsConditionalMessage() throws Exception {
        mvc.perform(get("/books").param("q", "martin"))
                .andExpect(model().attribute("books", hasSize(3)))
                .andExpect(content().string(not(containsString("class=\"empty\""))));

        mvc.perform(get("/books").param("q", "zzz"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("books", hasSize(0)))
                .andExpect(content().string(allOf(
                        containsString("За запитом «zzz» нічого не знайдено."),
                        not(containsString("<table")))));
    }

    @Test
    void addFormIsBoundToBookForm() throws Exception {
        mvc.perform(get("/books/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("book-form"))
                .andExpect(model().attributeExists("bookForm"))
                .andExpect(content().string(allOf(
                        containsString("action=\"/books/add\""),
                        containsString("id=\"title\" name=\"title\""),
                        containsString("id=\"author\" name=\"author\""),
                        containsString("id=\"pubYear\" name=\"pubYear\""))));
    }

    @Test
    void validFormAddsBookAndRedirects() throws Exception {
        mvc.perform(post("/books/add")
                        .param("title", "Intermezzo")
                        .param("author", "Михайло Коцюбинський")
                        .param("pubYear", "1908"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("notice", "book.added"))
                .andExpect(flash().attribute("noticeArg", "Intermezzo"));

        verify(mailService).sendNewBookEmail(argThat(b -> b.getTitle().equals("Intermezzo") && b.getPubYear() == 1908));

        mvc.perform(get("/books").param("q", "Intermezzo"))
                .andExpect(model().attribute("books", hasSize(1)));
    }

    @Test
    void mailFailureKeepsBookAndShowsAlert() throws Exception {
        doThrow(new MailSendException("535 Authentication failed")).when(mailService).sendNewBookEmail(any());

        mvc.perform(post("/books/add")
                        .param("title", "Тигролови")
                        .param("author", "Іван Багряний")
                        .param("pubYear", "1944"))
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("notice", "book.added"))
                .andExpect(flash().attribute("alert", "mail.failed"));

        mvc.perform(get("/books").param("q", "Тигролови"))
                .andExpect(model().attribute("books", hasSize(2)));
    }

    @Test
    void invalidFormIsRenderedAgainWithLocalizedErrors() throws Exception {
        mvc.perform(post("/books/add")
                        .param("title", " ")
                        .param("author", "Author")
                        .param("pubYear", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("book-form"))
                .andExpect(model().attributeHasFieldErrors("bookForm", "title", "pubYear"))
                .andExpect(content().string(allOf(
                        containsString("Вкажіть назву книги."),
                        containsString("Рік видання має бути цілим числом."),
                        containsString("value=\"Author\""))));

        int next = Year.now().getValue() + 1;
        mvc.perform(post("/books/add")
                        .header("Accept-Language", "en")
                        .param("title", "Future")
                        .param("author", "Author")
                        .param("pubYear", String.valueOf(next)))
                .andExpect(model().attributeHasFieldErrorCode("bookForm", "pubYear", "book.pubYear.future"))
                .andExpect(content().string(containsString(
                        "Publication year cannot be greater than " + (next - 1) + ".")));

        verify(mailService, never()).sendNewBookEmail(any());
    }

    @Test
    void errorsAreRenderedAsHtmlPage() throws Exception {
        mvc.perform(get("/books/999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(content().string(allOf(
                        containsString("Сторінку не знайдено"),
                        containsString("Book with id=999 not found"))));

        mvc.perform(get("/books/abc/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("bookId must be an integer, got &#39;abc&#39;")));

        mvc.perform(get("/books").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("page must be &gt;= 0")));
    }

    @Test
    void bookUrlRedirectsToBookPage() throws Exception {
        mvc.perform(get("/books/12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books/12/comments"));

        mvc.perform(get("/books/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingBookRemovesItsCommentsByCascade() throws Exception {
        assertThat(comments.list(2, 0, 10).getTotal()).isEqualTo(1);

        mvc.perform(post("/books/2/delete"))
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attribute("notice", "book.deleted"))
                .andExpect(flash().attribute("noticeArg", "Захар Беркут"));

        mvc.perform(get("/books/2/comments"))
                .andExpect(status().isNotFound());
        assertThat(comments.countByUser().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(11);

        mvc.perform(post("/books/2/delete"))
                .andExpect(status().isNotFound());
    }
}
