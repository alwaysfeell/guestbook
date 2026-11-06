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
        public boolean delete(long bookId, long commentId) {
            return data.removeIf(c -> c.getId() == commentId && c.getBookId() == bookId);
        }
    }
}
