package sumdu.edu.ua.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.exception.ConflictException;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;
import sumdu.edu.ua.core.port.CommentRepositoryPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    private FakeComments comments;
    private CommentService service;

    @BeforeEach
    void setUp() {
        comments = new FakeComments();
        service = new CommentService(comments, new FakeBooks(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void deletesCommentYoungerThan24Hours() {
        comments.put(new Comment(1, 1, "Ivan", "ok", NOW.minus(Duration.ofHours(23))));
        service.delete(1, 1);
        assertTrue(comments.data.isEmpty());
    }

    @Test
    void refusesToDeleteCommentOlderThan24Hours() {
        comments.put(new Comment(2, 1, "Ivan", "old", NOW.minus(Duration.ofHours(24)).minusSeconds(1)));
        assertThrows(ConflictException.class, () -> service.delete(1, 2));
        assertEquals(1, comments.data.size());
    }

    @Test
    void deleteWindowCanBeConfigured() {
        CommentService oneHour = new CommentService(comments, new FakeBooks(),
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(1));
        comments.put(new Comment(3, 1, "Ivan", "two hours old", NOW.minus(Duration.ofHours(2))));
        ConflictException e = assertThrows(ConflictException.class, () -> oneHour.delete(1, 3));
        assertEquals("Comment can be deleted only within 1 hour after creation", e.getMessage());
    }

    @Test
    void deleteOfMissingCommentGivesNotFound() {
        assertThrows(NotFoundException.class, () -> service.delete(1, 99));
    }

    @Test
    void addValidatesAuthorAndText() {
        assertThrows(ValidationException.class, () -> service.add(1, "   ", "text"));
        assertThrows(ValidationException.class, () -> service.add(1, "a".repeat(65), "text"));
        assertThrows(ValidationException.class, () -> service.add(1, "Ivan", ""));
        assertThrows(ValidationException.class, () -> service.add(1, "Ivan", "t".repeat(1001)));
    }

    @Test
    void updatesTextOfFreshComment() {
        comments.put(new Comment(4, 1, "Ivan", "old text", NOW.minus(Duration.ofHours(1))));
        Comment updated = service.update(1, 4, "  new text  ");
        assertEquals("new text", updated.getText());
        assertEquals("Ivan", updated.getAuthor());
        assertEquals("new text", comments.data.get(0).getText());
    }

    @Test
    void refusesToUpdateCommentOlderThan24Hours() {
        comments.put(new Comment(5, 1, "Ivan", "old", NOW.minus(Duration.ofHours(25))));
        ConflictException e = assertThrows(ConflictException.class, () -> service.update(1, 5, "new"));
        assertEquals("Comment can be edited only within 24 hours after creation", e.getMessage());
        assertEquals("old", comments.data.get(0).getText());
    }

    @Test
    void updateValidatesText() {
        comments.put(new Comment(6, 1, "Ivan", "old", NOW));
        assertThrows(ValidationException.class, () -> service.update(1, 6, "  "));
        assertThrows(ValidationException.class, () -> service.update(1, 6, null));
    }

    @Test
    void rejectsNonPositiveIds() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.add(0, "Ivan", "text"));
        assertEquals("bookId must be a positive integer", e.getMessage());
        assertThrows(ValidationException.class, () -> service.list(-1, 0, 10));
        assertThrows(ValidationException.class, () -> service.delete(1, 0));
        assertThrows(ValidationException.class, () -> service.update(-5, 1, "text"));
    }

    @Test
    void addToMissingBookGivesNotFound() {
        assertThrows(NotFoundException.class, () -> service.add(42, "Ivan", "text"));
    }

    static class FakeBooks implements CatalogRepositoryPort {
        public Page<Book> search(String query, PageRequest request) { return new Page<>(List.of(), request, 0); }
        public Optional<Book> findById(long id) {
            return id == 1 ? Optional.of(new Book(1, "Book", "Author", 2000)) : Optional.empty();
        }
        public Book add(String title, String author, int pubYear) { throw new UnsupportedOperationException(); }
    }

    static class FakeComments implements CommentRepositoryPort {
        final List<Comment> data = new ArrayList<>();
        void put(Comment c) { data.add(c); }
        public Comment add(long bookId, String author, String text) {
            Comment c = new Comment(data.size() + 1, bookId, author, text, NOW);
            data.add(c);
            return c;
        }
        public Page<Comment> list(long bookId, String author, Instant since, PageRequest request) {
            return new Page<>(data, request, data.size());
        }
        public Optional<Comment> findById(long bookId, long commentId) {
            return data.stream().filter(c -> c.getId() == commentId && c.getBookId() == bookId).findFirst();
        }
        public boolean update(long bookId, long commentId, String text) {
            for (int i = 0; i < data.size(); i++) {
                Comment c = data.get(i);
                if (c.getId() == commentId && c.getBookId() == bookId) {
                    data.set(i, c.withText(text));
                    return true;
                }
            }
            return false;
        }
        public boolean delete(long bookId, long commentId) {
            return data.removeIf(c -> c.getId() == commentId && c.getBookId() == bookId);
        }
    }
}
