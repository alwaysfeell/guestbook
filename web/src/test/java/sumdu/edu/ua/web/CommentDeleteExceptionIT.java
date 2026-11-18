package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.exception.BookNotFoundException;
import sumdu.edu.ua.core.exception.CommentTooOldException;
import sumdu.edu.ua.core.exception.InvalidCommentDeleteException;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.AppInit;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AppInit.class,
        properties = "spring.datasource.url=jdbc:h2:mem:comment-delete-it;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class CommentDeleteExceptionIT {
    private static final String TOO_OLD = "Коментар створено більше ніж 24 години тому і не може бути видалений";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Test
    void tooOldCommentGivesBadRequestWithJsonError() throws Exception {
        doThrow(new CommentTooOldException(TOO_OLD))
                .when(commentService).delete(eq(1L), eq(2L), any(Instant.class));

        mockMvc.perform(post("/comments/delete").with(csrf())
                        .param("bookId", "1")
                        .param("commentId", "2")
                        .param("createdAt", "2024-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(TOO_OLD));

        verify(commentService).delete(1L, 2L, Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void invalidDataGivesBadRequestWithJsonError() throws Exception {
        doThrow(new InvalidCommentDeleteException("Некоректний ідентифікатор книги або коментаря"))
                .when(commentService).delete(eq(-1L), eq(2L), any(Instant.class));

        mockMvc.perform(post("/comments/delete").with(csrf())
                        .param("bookId", "-1")
                        .param("commentId", "2")
                        .param("createdAt", "2024-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Некоректний ідентифікатор книги або коментаря"));
    }

    @Test
    void missingBookGivesNotFoundWithJsonError() throws Exception {
        doThrow(new BookNotFoundException("Книгу з id=99 не знайдено"))
                .when(commentService).delete(eq(99L), eq(2L), any(Instant.class));

        mockMvc.perform(post("/comments/delete").with(csrf())
                        .param("bookId", "99")
                        .param("commentId", "2")
                        .param("createdAt", "2024-01-01T00:00:00Z"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Книгу з id=99 не знайдено"));
    }

    @Test
    void successfulDeleteRedirectsToBookPage() throws Exception {
        mockMvc.perform(post("/comments/delete").with(csrf())
                        .param("bookId", "1")
                        .param("commentId", "2")
                        .param("createdAt", "2026-11-18T10:00:00Z"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books/1/comments"))
                .andExpect(flash().attribute("notice", "comments.deleted"));

        verify(commentService).delete(1L, 2L, Instant.parse("2026-11-18T10:00:00Z"));
    }

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void userCannotDeleteComments() throws Exception {
        mockMvc.perform(post("/comments/delete").with(csrf())
                        .param("bookId", "1")
                        .param("commentId", "2")
                        .param("createdAt", "2026-11-18T10:00:00Z"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(commentService);
    }
}
