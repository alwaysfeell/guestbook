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

@SpringBootTest(properties = "app.db.url=jdbc:h2:mem:comments-controller;DB_CLOSE_DELAY=-1")
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
                .andExpect(model().attributeExists("book", "comments", "commentForm", "deletableAfter"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Clean Architecture</h1>"),
                        containsString("Відгуків: 2"),
                        containsString("Андрій"),
                        containsString("action=\"/books/11/comments/2/delete\""),
                        containsString("видалення недоступне"))));
    }

    @Test
    void emptyCommentListShowsMessage() throws Exception {
        mvc.perform(get("/books/5/comments"))
                .andExpect(content().string(containsString("Відгуків ще немає. Будьте першим!")));
    }

    @Test
    void addAndDeleteCommentThroughForms() throws Exception {
        mvc.perform(post("/books/3/comments").param("author", "Student").param("text", "Great play"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books/3/comments"))
                .andExpect(flash().attribute("notice", "comments.added"));

        Page<Comment> page = comments.list(3, 0, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        long id = page.getItems().get(0).getId();

        mvc.perform(get("/books/3/comments").header("Accept-Language", "en"))
                .andExpect(content().string(allOf(
                        containsString("Great play"),
                        containsString("Reviews: 1"),
                        containsString(">Delete<"))));

        mvc.perform(post("/books/3/comments/{id}/delete", id))
                .andExpect(redirectedUrl("/books/3/comments"))
                .andExpect(flash().attribute("notice", "comments.deleted"));
        assertThat(comments.list(3, 0, 10).getTotal()).isZero();
    }

    @Test
    void invalidCommentFormShowsErrors() throws Exception {
        mvc.perform(post("/books/3/comments").param("author", "   ").param("text", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("book-comments"))
                .andExpect(model().attributeHasFieldErrors("commentForm", "author", "text"))
                .andExpect(content().string(allOf(
                        containsString("Вкажіть своє ім&#39;я."),
                        containsString("Напишіть текст відгуку."))));
    }

    @Test
    void oldCommentCannotBeDeleted() throws Exception {
        mvc.perform(post("/books/11/comments/1/delete"))
                .andExpect(redirectedUrl("/books/11/comments"))
                .andExpect(flash().attribute("alert", "comments.deleteForbidden"));

        mvc.perform(get("/books/11/comments").flashAttr("alert", "comments.deleteForbidden"))
                .andExpect(content().string(containsString(
                        "Відгук можна видалити лише протягом 24 годин після створення.")));
    }
}
