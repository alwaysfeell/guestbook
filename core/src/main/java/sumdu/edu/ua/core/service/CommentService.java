package sumdu.edu.ua.core.service;

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

public class CommentService {
    public static final Duration DELETE_WINDOW = Duration.ofHours(24);

    private final CommentRepositoryPort comments;
    private final CatalogRepositoryPort books;
    private final Clock clock;
    private final Duration deleteWindow;

    public CommentService(CommentRepositoryPort comments, CatalogRepositoryPort books) {
        this(comments, books, Clock.systemUTC());
    }

    public CommentService(CommentRepositoryPort comments, CatalogRepositoryPort books, Clock clock) {
        this(comments, books, clock, DELETE_WINDOW);
    }

    public CommentService(CommentRepositoryPort comments, CatalogRepositoryPort books,
                          Clock clock, Duration deleteWindow) {
        if (deleteWindow == null || deleteWindow.isNegative() || deleteWindow.isZero()) {
            throw new IllegalArgumentException("deleteWindow must be positive");
        }
        this.comments = comments;
        this.books = books;
        this.clock = clock;
        this.deleteWindow = deleteWindow;
    }

    public Duration getDeleteWindow() {
        return deleteWindow;
    }

    public Page<Comment> list(long bookId, int page, int size) {
        BookService.requirePositiveId(bookId, "bookId");
        requireBook(bookId);
        return comments.list(bookId, null, null, new PageRequest(page, size));
    }

    public Comment add(long bookId, String author, String text) {
        BookService.requirePositiveId(bookId, "bookId");
        String a = author == null ? "" : author.trim();
        if (a.isEmpty() || a.length() > 64) {
            throw new ValidationException("author is required (max 64 chars)");
        }
        String t = normalizeText(text);
        requireBook(bookId);
        return comments.add(bookId, a, t);
    }

    public Comment update(long bookId, long commentId, String text) {
        String t = normalizeText(text);
        Comment comment = requireEditable(bookId, commentId, "edited");
        comments.update(bookId, commentId, t);
        return comment.withText(t);
    }

    public Comment delete(long bookId, long commentId) {
        Comment comment = requireEditable(bookId, commentId, "deleted");
        comments.delete(bookId, commentId);
        return comment;
    }

    private Comment requireEditable(long bookId, long commentId, String action) {
        BookService.requirePositiveId(bookId, "bookId");
        BookService.requirePositiveId(commentId, "comment id");
        Comment comment = comments.findById(bookId, commentId)
                .orElseThrow(() -> new NotFoundException(
                        "Comment with id=" + commentId + " not found for book id=" + bookId));

        Duration age = Duration.between(comment.getCreatedAt(), Instant.now(clock));
        if (age.compareTo(deleteWindow) > 0) {
            throw new ConflictException("Comment can be " + action + " only within "
                    + describe(deleteWindow) + " after creation");
        }
        return comment;
    }

    private static String normalizeText(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty() || t.length() > 1000) {
            throw new ValidationException("text is required (max 1000 chars)");
        }
        return t;
    }

    private static String describe(Duration window) {
        if (window.toMinutesPart() == 0 && window.toSecondsPart() == 0) {
            long hours = window.toHours();
            return hours + (hours == 1 ? " hour" : " hours");
        }
        long minutes = Math.max(1, window.toMinutes());
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }

    private void requireBook(long bookId) {
        if (books.findById(bookId).isEmpty()) {
            throw new NotFoundException("Book with id=" + bookId + " not found");
        }
    }
}
