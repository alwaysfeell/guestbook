package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.service.CommentService;

import java.time.Instant;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
                        containsString("action=\"/comments/delete\""),
                        containsString("name=\"commentId\" value=\"11\""),
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
                        not(containsString("/comments/delete")),
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

        mvc.perform(deleteRequest(9, saved.getId(), saved.getCreatedAt().toString())
                        .with(user("iryna").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
        assertThat(comments.list(9, 0, 10).getTotal()).isEqualTo(1);

        mvc.perform(deleteRequest(9, saved.getId(), saved.getCreatedAt().toString())
                        .with(user("admin").roles("ADMIN")))
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
    void oldCommentCannotBeDeletedAndErrorIsJson() throws Exception {
        Comment old = comments.list(11, 0, 10).getItems().stream()
                .filter(c -> c.getId() == 4).findFirst().orElseThrow();

        mvc.perform(deleteRequest(11, 4, old.getCreatedAt().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value("Коментар створено більше ніж 24 години тому і не може бути видалений"));

        mvc.perform(deleteRequest(11, 4, Instant.now().minusSeconds(60).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Дата створення не збігається з датою коментаря"));

        assertThat(comments.list(11, 0, 10).getItems()).extracting(Comment::getId).contains(4L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalidDeleteParametersGiveJsonErrors() throws Exception {
        String recent = Instant.now().minusSeconds(60).toString();

        mvc.perform(deleteRequest(0, 11, recent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Некоректний ідентифікатор книги або коментаря"));

        mvc.perform(post("/comments/delete").with(csrf()).param("bookId", "11").param("commentId", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Не вказано дату створення коментаря (createdAt)"));

        mvc.perform(deleteRequest(999, 11, recent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Книгу з id=999 не знайдено"));

        mvc.perform(deleteRequest(11, 777, recent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Коментар з id=777 не знайдено у книзі з id=11"));
    }

    private static MockHttpServletRequestBuilder deleteRequest(long bookId, long commentId, String createdAt) {
        return post("/comments/delete").with(csrf())
                .param("bookId", String.valueOf(bookId))
                .param("commentId", String.valueOf(commentId))
                .param("createdAt", createdAt);
    }
}
