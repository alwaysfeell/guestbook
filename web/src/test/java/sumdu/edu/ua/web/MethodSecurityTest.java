package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:method-security;DB_CLOSE_DELAY=-1")
class MethodSecurityTest {

    @Autowired
    private BookService books;

    @Autowired
    private CommentService comments;

    @Test
    void anonymousCallIsRejectedByPreAuthorize() {
        assertThatThrownBy(() -> books.delete(1))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        assertThatThrownBy(() -> comments.add(1, 2, "text"))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void userCannotModifyCatalog() {
        assertThat(books.findById(1).getTitle()).isEqualTo("Кобзар");
        assertThatThrownBy(() -> books.add("T", "A", 2000)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> books.delete(1)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> comments.delete(12, 10)).isInstanceOf(AccessDeniedException.class);
        assertThat(books.findById(1)).isNotNull();
    }

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void userCanAddComment() {
        assertThat(comments.add(5, 2, "Роман про Київ").getAuthor()).isEqualTo("olena");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanAddAndDeleteBooks() {
        Book saved = books.add("Intermezzo", "Михайло Коцюбинський", 1908);
        assertThat(books.delete(saved.getId()).getTitle()).isEqualTo("Intermezzo");
    }
}
