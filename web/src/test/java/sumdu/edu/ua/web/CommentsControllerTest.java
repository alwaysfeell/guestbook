package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.service.CommentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
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
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminSeesCommentsWithDeleteButtons() throws Exception {
        mvc.perform(get("/books/11/comments"))
                .andExpect(status().isOk())
                .andExpect(view().name("book-comments"))
                .andExpect(model().attributeExists("book", "comments", "commentForm", "deletableAfter"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Clean Architecture</h1>"),
                        containsString("Відгуків: 2"),
                        containsString("href=\"/users/3/comments\">andriy</a>"),
                        containsString("href=\"/users/2/comments\">olena</a>"),
                        containsString("action=\"/books/11/comments/11/delete\""),
                        containsString("видалення недоступне"),
                        containsString("action=\"/books/11/delete\""),
                        containsString("Автор відгуку:"),
                        not(containsString("<select")))));
    }

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void userSeesCommentsWithoutAdminButtons() throws Exception {
        mvc.perform(get("/books/11/comments"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>Clean Architecture</h1>"),
                        containsString("<strong>olena</strong>"),
                        containsString("action=\"/books/11/comments\""),
                        not(containsString("/books/11/comments/11/delete")),
                        not(containsString("action=\"/books/11/delete\"")),
                        not(containsString("видалення недоступне")))));
    }

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void emptyCommentListShowsMessage() throws Exception {
        mvc.perform(get("/books/5/comments"))
                .andExpect(content().string(containsString("Відгуків ще немає. Будьте першим!")));
    }

    @Test
    void commentAuthorIsTheLoggedInUser() throws Exception {
        mvc.perform(post("/books/9/comments").with(user("iryna").roles("USER")).with(csrf())
                        .param("text", "Great patterns book"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books/9/comments"))
                .andExpect(flash().attribute("notice", "comments.added"));

        Page<Comment> page = comments.list(9, 0, 10);
        assertThat(page.getTotal()).isEqualTo(1);
        Comment saved = page.getItems().get(0);
        assertThat(saved.getAuthor()).isEqualTo("iryna");
        assertThat(saved.getBook().getTitle()).isEqualTo("Head First Design Patterns");

        mvc.perform(get("/books/9/comments").with(user("admin").roles("ADMIN")).header("Accept-Language", "en"))
                .andExpect(content().string(allOf(
                        containsString("Great patterns book"),
                        containsString("Reviews: 1"),
                        containsString(">iryna</a>"),
                        containsString(">Delete<"))));

        mvc.perform(post("/books/9/comments/{id}/delete", saved.getId()).with(user("iryna").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
        assertThat(comments.list(9, 0, 10).getTotal()).isEqualTo(1);

        mvc.perform(post("/books/9/comments/{id}/delete", saved.getId()).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/books/9/comments"))
                .andExpect(flash().attribute("notice", "comments.deleted"));
        assertThat(comments.list(9, 0, 10).getTotal()).isZero();
    }

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void invalidCommentFormShowsErrors() throws Exception {
        mvc.perform(post("/books/3/comments").with(csrf()).param("text", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("book-comments"))
                .andExpect(model().attributeHasFieldErrors("commentForm", "text"))
                .andExpect(content().string(containsString("Напишіть текст відгуку.")));

        mvc.perform(post("/books/99/comments").with(csrf()).param("text", "text"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Book with id=99 not found")));
    }

    @Test
    void commentFormWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/books/3/comments").with(user("olena").roles("USER")).param("text", "text"))
                .andExpect(status().isForbidden());
        assertThat(comments.list(3, 0, 10).getTotal()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void oldCommentCannotBeDeleted() throws Exception {
        mvc.perform(post("/books/11/comments/4/delete").with(csrf()))
                .andExpect(redirectedUrl("/books/11/comments"))
                .andExpect(flash().attribute("alert", "comments.deleteForbidden"));

        mvc.perform(get("/books/11/comments").flashAttr("alert", "comments.deleteForbidden"))
                .andExpect(content().string(containsString(
                        "Відгук можна видалити лише протягом 24 годин після створення.")));
    }
}
