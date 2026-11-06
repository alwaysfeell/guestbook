package sumdu.edu.ua.persistence;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.persistence.jdbc.DbInit;
import sumdu.edu.ua.persistence.jdbc.JdbcBookRepository;
import sumdu.edu.ua.persistence.jdbc.JdbcCommentRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcRepositoriesTest {
    private static final JdbcBookRepository books = new JdbcBookRepository();
    private static final JdbcCommentRepository comments = new JdbcCommentRepository();

    @BeforeAll
    static void initDb() {
        DbInit.init();
    }

    @Test
    void searchFiltersSortsAndPaginates() {
        Page<Book> page = books.search("martin", new PageRequest(0, 1, "pubYear", false));
        assertEquals(3, page.getTotal());
        assertEquals(1, page.getItems().size());
        assertEquals("Clean Architecture", page.getItems().get(0).getTitle());

        Page<Book> second = books.search(null, new PageRequest(1, 5, "id", true));
        assertTrue(second.getTotal() >= 12);
        assertEquals(6, second.getItems().get(0).getId());
    }

    @Test
    void addListAndDeleteComment() {
        Book book = books.add("Test Book", "Tester", 2026);
        Comment c = comments.add(book.getId(), "Влад", "Тестовий відгук");

        assertEquals(1, comments.list(book.getId(), null, null, new PageRequest(0, 10)).getTotal());
        assertTrue(comments.findById(book.getId(), c.getId()).isPresent());

        assertTrue(comments.delete(book.getId(), c.getId()));
        assertFalse(comments.findById(book.getId(), c.getId()).isPresent());
    }
}
