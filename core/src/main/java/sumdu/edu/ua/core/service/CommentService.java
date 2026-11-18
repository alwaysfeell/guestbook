package sumdu.edu.ua.core.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.exception.BookNotFoundException;
import sumdu.edu.ua.core.exception.CommentNotFoundException;
import sumdu.edu.ua.core.exception.CommentTooOldException;
import sumdu.edu.ua.core.exception.ConflictException;
import sumdu.edu.ua.core.exception.ForbiddenWordsException;
import sumdu.edu.ua.core.exception.InvalidCommentDeleteException;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import sumdu.edu.ua.core.port.UserRepositoryPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Transactional
public class CommentService {
    private static final Logger log = LoggerFactory.getLogger(CommentService.class);
    public static final Duration DELETE_WINDOW = Duration.ofHours(24);
    public static final Set<String> FORBIDDEN_WORDS = Set.of("спам", "казино", "ставки", "spam", "casino");

    private final CommentRepositoryPort comments;
    private final CatalogRepositoryPort books;
    private final UserRepositoryPort users;
    private final Clock clock;
    private final Duration deleteWindow;

    public CommentService(CommentRepositoryPort comments, CatalogRepositoryPort books, UserRepositoryPort users) {
        this(comments, books, users, Clock.systemUTC());
    }

    public CommentService(CommentRepositoryPort comments, CatalogRepositoryPort books, UserRepositoryPort users,
                          Clock clock) {
        this(comments, books, users, clock, DELETE_WINDOW);
    }

    public CommentService(CommentRepositoryPort comments, CatalogRepositoryPort books, UserRepositoryPort users,
                          Clock clock, Duration deleteWindow) {
        if (deleteWindow == null || deleteWindow.isNegative() || deleteWindow.isZero()) {
            throw new IllegalArgumentException("deleteWindow must be positive");
        }
        this.comments = comments;
        this.books = books;
        this.users = users;
        this.clock = clock;
        this.deleteWindow = deleteWindow;
    }

    public Duration getDeleteWindow() {
        return deleteWindow;
    }

    public Page<Comment> list(long bookId, int page, int size) {
        BookService.requirePositiveId(bookId, "bookId");
        requireBook(bookId);
        return comments.findByBook(bookId, new PageRequest(page, size));
    }

    @PreAuthorize("isAuthenticated()")
    public Comment add(long bookId, long userId, String text) {
        BookService.requirePositiveId(bookId, "bookId");
        BookService.requirePositiveId(userId, "userId");
        String t = normalizeText(text);
        requireBook(bookId);
        requireUser(userId);
        return comments.add(bookId, userId, t, Instant.now(clock));
    }

    public List<Comment> getCommentsByUser(long userId) {
        BookService.requirePositiveId(userId, "userId");
        requireUser(userId);
        return comments.findByUser(userId);
    }

    public Map<Long, Long> countByUser() {
        return comments.countByUser();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Comment update(long bookId, long commentId, String text) {
        String t = normalizeText(text);
        Comment comment = requireEditable(bookId, commentId, "edited");
        comments.update(bookId, commentId, t);
        return comment.withText(t);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(long bookId, long commentId, Instant createdAt) {
        if (bookId <= 0 || commentId <= 0) {
            log.warn("Invalid delete request: bookId={}, commentId={}", bookId, commentId);
            throw new InvalidCommentDeleteException("Некоректний ідентифікатор книги або коментаря");
        }
        if (createdAt == null) {
            log.warn("Delete request without createdAt: bookId={}, commentId={}", bookId, commentId);
            throw new InvalidCommentDeleteException("Не вказано дату створення коментаря (createdAt)");
        }
        Instant now = Instant.now(clock);
        if (createdAt.isAfter(now)) {
            throw new InvalidCommentDeleteException("Дата створення коментаря не може бути в майбутньому");
        }
        if (Duration.between(createdAt, now).compareTo(deleteWindow) > 0) {
            log.warn("Comment is too old to delete: bookId={}, commentId={}, createdAt={}", bookId, commentId, createdAt);
            throw new CommentTooOldException("Коментар створено більше ніж " + describeUk(deleteWindow)
                    + " тому і не може бути видалений");
        }
        if (books.findById(bookId).isEmpty()) {
            throw new BookNotFoundException("Книгу з id=" + bookId + " не знайдено");
        }
        Comment comment = comments.findById(bookId, commentId)
                .orElseThrow(() -> new CommentNotFoundException(
                        "Коментар з id=" + commentId + " не знайдено у книзі з id=" + bookId));
        if (!comment.getCreatedAt().equals(createdAt)) {
            log.warn("createdAt mismatch: bookId={}, commentId={}, sent={}, stored={}",
                    bookId, commentId, createdAt, comment.getCreatedAt());
            throw new InvalidCommentDeleteException("Дата створення не збігається з датою коментаря");
        }
        comments.delete(bookId, commentId);
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
        requireAllowedWords(t);
        return t;
    }

    private static void requireAllowedWords(String text) {
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (FORBIDDEN_WORDS.contains(word)) {
                log.warn("Comment rejected: forbidden word '{}'", word);
                throw new ForbiddenWordsException("Коментар містить заборонене слово «" + word + "»");
            }
        }
    }

    private static String describe(Duration window) {
        if (window.toMinutesPart() == 0 && window.toSecondsPart() == 0) {
            long hours = window.toHours();
            return hours + (hours == 1 ? " hour" : " hours");
        }
        long minutes = Math.max(1, window.toMinutes());
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }

    private static String describeUk(Duration window) {
        if (window.toMinutesPart() == 0 && window.toSecondsPart() == 0) {
            long hours = window.toHours();
            return hours + " " + plural(hours, "годину", "години", "годин");
        }
        long minutes = Math.max(1, window.toMinutes());
        return minutes + " " + plural(minutes, "хвилину", "хвилини", "хвилин");
    }

    private static String plural(long n, String one, String few, String many) {
        long mod100 = n % 100;
        long mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return many;
        }
        if (mod10 == 1) {
            return one;
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return few;
        }
        return many;
    }

    private void requireBook(long bookId) {
        if (books.findById(bookId).isEmpty()) {
            throw new NotFoundException("Book with id=" + bookId + " not found");
        }
    }

    private void requireUser(long userId) {
        if (users.findById(userId).isEmpty()) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
    }
}
