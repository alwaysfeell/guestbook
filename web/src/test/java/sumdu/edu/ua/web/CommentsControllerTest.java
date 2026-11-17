package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.service.CommentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:comments-controller;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class CommentsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CommentService comments;

    @Test
    void bookPageShowsCommentsAndConditionalDeleteButton() throws Exception {
        mvc.perform(get("/books/11/comments"))
                .andExpect(status().isOk())
                .andExpect(view().name("book-comments"))
                .andExpect(model().attributeExists("book", "comments", "commentForm", "users", "deletableAfter"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Clean Architecture</h1>"),
                        containsString("Відгуків: 2"),
                        containsString("href=\"/users/3/comments\">andriy</a>"),
                        containsString("href=\"/users/2/comments\">olena</a>"),
                        containsString("action=\"/books/11/comments/11/delete\""),
                        containsString("видалення недоступне"),
                        containsString("<option value=\"4\">vladyslav</option>"))));
    }

    @Test
    void emptyCommentListShowsMessage() throws Exception {
        mvc.perform(get("/books/5/comments"))
                .andExpect(content().string(containsString("Відгуків ще немає. Будьте першим!")));
    }

    @Test
    void addAndDeleteCommentThroughForms() throws Exception {
        mvc.perform(post("/books/9/comments").param("userId", "5").param("text", "Great patterns book"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books/9/comments"))
                .andExpect(flash().attribute("notice", "comments.added"));

        Page<Comment> page = comments.list(9, 0, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        Comment saved = page.getItems().get(0);
        assertThat(saved.getAuthor()).isEqualTo("iryna");
        assertThat(saved.getBook().getTitle()).isEqualTo("Head First Design Patterns");

        mvc.perform(get("/books/9/comments").header("Accept-Language", "en"))
                .andExpect(content().string(allOf(
                        containsString("Great patterns book"),
                        containsString("Reviews: 1"),
                        containsString(">iryna</a>"),
                        containsString(">Delete<"))));

        mvc.perform(post("/books/9/comments/{id}/delete", saved.getId()))
                .andExpect(redirectedUrl("/books/9/comments"))
                .andExpect(flash().attribute("notice", "comments.deleted"));
        assertThat(comments.list(9, 0, 10).getTotal()).isZero();
    }

    @Test
    void invalidCommentFormShowsErrors() throws Exception {
        mvc.perform(post("/books/3/comments").param("userId", "").param("text", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("book-comments"))
                .andExpect(model().attributeHasFieldErrors("commentForm", "userId", "text"))
                .andExpect(content().string(allOf(
                        containsString("Оберіть користувача."),
                        containsString("Напишіть текст відгуку."))));

        mvc.perform(post("/books/3/comments").param("userId", "abc").param("text", "text"))
                .andExpect(model().attributeHasFieldErrorCode("commentForm", "userId", "typeMismatch"));

        mvc.perform(post("/books/3/comments").param("userId", "99").param("text", "text"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("User with id=99 not found")));
    }

    @Test
    void oldCommentCannotBeDeleted() throws Exception {
        mvc.perform(post("/books/11/comments/4/delete"))
                .andExpect(redirectedUrl("/books/11/comments"))
                .andExpect(flash().attribute("alert", "comments.deleteForbidden"));

        mvc.perform(get("/books/11/comments").flashAttr("alert", "comments.deleteForbidden"))
                .andExpect(content().string(containsString(
                        "Відгук можна видалити лише протягом 24 годин після створення.")));
    }
}
