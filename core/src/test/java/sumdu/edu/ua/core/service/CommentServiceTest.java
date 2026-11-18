package sumdu.edu.ua.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.domain.Role;
import sumdu.edu.ua.core.domain.User;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommentServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final Book BOOK = new Book(1, "Book", "Author", 2000);
    private static final User IVAN = new User(1, "ivan", "x", Role.USER);
    private static final User OLENA = new User(2, "olena", "y", Role.USER);

    private FakeComments comments;
    private CommentService service;

    @BeforeEach
    void setUp() {
        comments = new FakeComments();
        service = new CommentService(comments, new FakeBooks(), new FakeUsers(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Comment comment(long id, User user, String text, Instant createdAt) {
        return new Comment(id, BOOK, user, text, createdAt);
    }

    private static CommentService serviceWith(CommentRepositoryPort repo, CatalogRepositoryPort books) {
        return new CommentService(repo, books, mock(UserRepositoryPort.class), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void delete_withNonPositiveIds_throwsInvalidCommentDeleteException() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CatalogRepositoryPort books = mock(CatalogRepositoryPort.class);
        CommentService service = serviceWith(repo, books);

        InvalidCommentDeleteException e = assertThrows(InvalidCommentDeleteException.class,
                () -> service.delete(-1, 1, NOW));
        assertEquals("Некоректний ідентифікатор книги або коментаря", e.getMessage());
        assertThrows(InvalidCommentDeleteException.class, () -> service.delete(1, 0, NOW));
        assertThrows(InvalidCommentDeleteException.class, () -> service.delete(0, 0, NOW));
        verifyNoInteractions(repo, books);
    }

    @Test
    void delete_withoutCreatedAt_throwsInvalidCommentDeleteException() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CommentService service = serviceWith(repo, mock(CatalogRepositoryPort.class));

        InvalidCommentDeleteException e = assertThrows(InvalidCommentDeleteException.class,
                () -> service.delete(1, 1, null));
        assertEquals("Не вказано дату створення коментаря (createdAt)", e.getMessage());
        verifyNoInteractions(repo);
    }

    @Test
    void delete_withCreatedAtInFuture_throwsInvalidCommentDeleteException() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CommentService service = serviceWith(repo, mock(CatalogRepositoryPort.class));

        assertThrows(InvalidCommentDeleteException.class, () -> service.delete(1, 1, NOW.plusSeconds(60)));
        verifyNoInteractions(repo);
    }

    @Test
    void delete_tooOldComment_throwsCommentTooOldException() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CommentService service = serviceWith(repo, mock(CatalogRepositoryPort.class));
        Instant twoDaysAgo = NOW.minus(Duration.ofHours(48));

        CommentTooOldException e = assertThrows(CommentTooOldException.class,
                () -> service.delete(1, 1, twoDaysAgo));
        assertEquals("Коментар створено більше ніж 24 години тому і не може бути видалений", e.getMessage());
        verifyNoInteractions(repo);
    }

    @Test
    void delete_validData_callsRepositoryDelete() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CatalogRepositoryPort books = mock(CatalogRepositoryPort.class);
        CommentService service = serviceWith(repo, books);
        Instant oneHourAgo = NOW.minus(Duration.ofHours(1));
        Book book = new Book(10, "Clean Code", "Robert C. Martin", 2008);
        when(books.findById(10)).thenReturn(Optional.of(book));
        when(repo.findById(10, 20)).thenReturn(Optional.of(new Comment(20, book, IVAN, "text", oneHourAgo)));

        service.delete(10, 20, oneHourAgo);

        verify(repo).delete(10, 20);
    }

    @Test
    void delete_commentExactly24HoursOld_isAllowed() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CatalogRepositoryPort books = mock(CatalogRepositoryPort.class);
        CommentService service = serviceWith(repo, books);
        Instant dayAgo = NOW.minus(Duration.ofHours(24));
        when(books.findById(1)).thenReturn(Optional.of(BOOK));
        when(repo.findById(1, 2)).thenReturn(Optional.of(comment(2, IVAN, "text", dayAgo)));

        service.delete(1, 2, dayAgo);

        verify(repo).delete(1, 2);
    }

    @Test
    void delete_missingBook_throwsBookNotFoundException() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CatalogRepositoryPort books = mock(CatalogRepositoryPort.class);
        CommentService service = serviceWith(repo, books);
        when(books.findById(42)).thenReturn(Optional.empty());

        BookNotFoundException e = assertThrows(BookNotFoundException.class,
                () -> service.delete(42, 1, NOW.minusSeconds(60)));
        assertEquals("Книгу з id=42 не знайдено", e.getMessage());
        verifyNoInteractions(repo);
    }

    @Test
    void delete_missingComment_throwsCommentNotFoundException() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CatalogRepositoryPort books = mock(CatalogRepositoryPort.class);
        CommentService service = serviceWith(repo, books);
        when(books.findById(1)).thenReturn(Optional.of(BOOK));
        when(repo.findById(1, 99)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> service.delete(1, 99, NOW.minusSeconds(60)));
        verify(repo, never()).delete(anyLong(), anyLong());
    }

    @Test
    void delete_createdAtDiffersFromStored_throwsInvalidCommentDeleteException() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CatalogRepositoryPort books = mock(CatalogRepositoryPort.class);
        CommentService service = serviceWith(repo, books);
        when(books.findById(1)).thenReturn(Optional.of(BOOK));
        when(repo.findById(1, 4)).thenReturn(Optional.of(comment(4, IVAN, "old", NOW.minus(Duration.ofHours(72)))));

        InvalidCommentDeleteException e = assertThrows(InvalidCommentDeleteException.class,
                () -> service.delete(1, 4, NOW.minus(Duration.ofHours(1))));
        assertEquals("Дата створення не збігається з датою коментаря", e.getMessage());
        verify(repo, never()).delete(anyLong(), anyLong());
    }

    @Test
    void delete_tooOldMessageFollowsConfiguredWindow() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        Instant weekAgo = NOW.minus(Duration.ofDays(7));
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        CatalogRepositoryPort books = mock(CatalogRepositoryPort.class);

        assertEquals("Коментар створено більше ніж 1 годину тому і не може бути видалений",
                assertThrows(CommentTooOldException.class, () -> new CommentService(repo, books, users, clock,
                        Duration.ofHours(1)).delete(1, 1, weekAgo)).getMessage());
        assertEquals("Коментар створено більше ніж 5 годин тому і не може бути видалений",
                assertThrows(CommentTooOldException.class, () -> new CommentService(repo, books, users, clock,
                        Duration.ofHours(5)).delete(1, 1, weekAgo)).getMessage());
        assertEquals("Коментар створено більше ніж 30 хвилин тому і не може бути видалений",
                assertThrows(CommentTooOldException.class, () -> new CommentService(repo, books, users, clock,
                        Duration.ofMinutes(30)).delete(1, 1, weekAgo)).getMessage());
        verifyNoInteractions(repo, books);
    }

    @Test
    void add_withForbiddenWord_throwsForbiddenWordsException() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CatalogRepositoryPort books = mock(CatalogRepositoryPort.class);
        CommentService service = serviceWith(repo, books);

        ForbiddenWordsException e = assertThrows(ForbiddenWordsException.class,
                () -> service.add(1, 1, "Найкращі ставки тут!"));
        assertEquals("Коментар містить заборонене слово «ставки»", e.getMessage());
        assertThrows(ForbiddenWordsException.class, () -> service.add(1, 1, "Онлайн-КАЗИНО"));
        assertThrows(ForbiddenWordsException.class, () -> service.add(1, 1, "Buy now, SPAM."));
        verifyNoInteractions(repo, books);
    }

    @Test
    void add_wordThatOnlyContainsForbiddenPart_isAllowed() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CatalogRepositoryPort books = mock(CatalogRepositoryPort.class);
        UserRepositoryPort users = mock(UserRepositoryPort.class);
        CommentService service = new CommentService(repo, books, users, Clock.fixed(NOW, ZoneOffset.UTC));
        when(books.findById(1)).thenReturn(Optional.of(BOOK));
        when(users.findById(1)).thenReturn(Optional.of(IVAN));
        when(repo.add(1, 1, "Спамерам тут не місце", NOW)).thenReturn(comment(7, IVAN, "Спамерам тут не місце", NOW));

        Comment c = service.add(1, 1, "  Спамерам тут не місце ");

        assertEquals(7, c.getId());
        verify(repo).add(1, 1, "Спамерам тут не місце", NOW);
    }

    @Test
    void update_withForbiddenWord_doesNotChangeComment() {
        comments.put(comment(8, IVAN, "old text", NOW.minus(Duration.ofHours(1))));
        assertThrows(ForbiddenWordsException.class, () -> service.update(1, 8, "casino bonus"));
        assertEquals("old text", comments.data.get(0).getText());
    }

    @Test
    void forbiddenWordsExceptionIsValidationError() {
        CommentRepositoryPort repo = mock(CommentRepositoryPort.class);
        CommentService service = serviceWith(repo, mock(CatalogRepositoryPort.class));
        assertThrows(ValidationException.class, () -> service.add(1, 1, "спам"));
        verify(repo, never()).add(anyLong(), anyLong(), anyString(), any(Instant.class));
    }

    @Test
    void customExceptionsKeepBaseTypes() {
        assertTrue(new InvalidCommentDeleteException("x") instanceof ValidationException);
        assertTrue(new CommentTooOldException("x") instanceof ConflictException);
        assertTrue(new BookNotFoundException("x") instanceof NotFoundException);
        assertTrue(new CommentNotFoundException("x") instanceof NotFoundException);
        assertTrue(new ForbiddenWordsException("x") instanceof ValidationException);
    }

    @Test
    void deleteWindowCanBeConfigured() {
        CommentService oneHour = new CommentService(comments, new FakeBooks(), new FakeUsers(),
                Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofHours(1));
        comments.put(comment(3, IVAN, "two hours old", NOW.minus(Duration.ofHours(2))));
        ConflictException e = assertThrows(ConflictException.class, () -> oneHour.update(1, 3, "new"));
        assertEquals("Comment can be edited only within 1 hour after creation", e.getMessage());
    }

    @Test
    void deleteRemovesFreshCommentFromRepository() {
        Instant createdAt = NOW.minus(Duration.ofHours(23));
        comments.put(comment(1, IVAN, "ok", createdAt));
        service.delete(1, 1, createdAt);
        assertTrue(comments.data.isEmpty());
    }

    @Test
    void addStoresCommentOfUserWithCurrentTime() {
        Comment c = service.add(1, 2, "  Чудова книга  ");
        assertEquals("Чудова книга", c.getText());
        assertEquals("olena", c.getAuthor());
        assertEquals(NOW, c.getCreatedAt());
        assertEquals(1, comments.data.size());
    }

    @Test
    void addValidatesText() {
        assertThrows(ValidationException.class, () -> service.add(1, 1, ""));
        assertThrows(ValidationException.class, () -> service.add(1, 1, "t".repeat(1001)));
    }

    @Test
    void addForMissingBookOrUserGivesNotFound() {
        assertThrows(NotFoundException.class, () -> service.add(42, 1, "text"));
        NotFoundException e = assertThrows(NotFoundException.class, () -> service.add(1, 42, "text"));
        assertEquals("User with id=42 not found", e.getMessage());
    }

    @Test
    void returnsCommentsOfUser() {
        comments.put(comment(1, IVAN, "first", NOW.minus(Duration.ofHours(3))));
        comments.put(comment(2, OLENA, "second", NOW.minus(Duration.ofHours(2))));
        comments.put(comment(3, IVAN, "third", NOW.minus(Duration.ofHours(1))));

        List<Comment> ivan = service.getCommentsByUser(1);
        assertEquals(List.of("third", "first"), ivan.stream().map(Comment::getText).toList());
        assertEquals(Map.of(1L, 2L, 2L, 1L), service.countByUser());
        assertThrows(NotFoundException.class, () -> service.getCommentsByUser(7));
    }

    @Test
    void updatesTextOfFreshComment() {
        comments.put(comment(4, IVAN, "old text", NOW.minus(Duration.ofHours(1))));
        Comment updated = service.update(1, 4, "  new text  ");
        assertEquals("new text", updated.getText());
        assertEquals("ivan", updated.getAuthor());
        assertEquals("new text", comments.data.get(0).getText());
    }

    @Test
    void refusesToUpdateCommentOlderThan24Hours() {
        comments.put(comment(5, IVAN, "old", NOW.minus(Duration.ofHours(25))));
        ConflictException e = assertThrows(ConflictException.class, () -> service.update(1, 5, "new"));
        assertEquals("Comment can be edited only within 24 hours after creation", e.getMessage());
        assertEquals("old", comments.data.get(0).getText());
    }

    @Test
    void updateValidatesText() {
        comments.put(comment(6, IVAN, "old", NOW));
        assertThrows(ValidationException.class, () -> service.update(1, 6, "  "));
        assertThrows(ValidationException.class, () -> service.update(1, 6, null));
    }

    @Test
    void rejectsNonPositiveIds() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.add(0, 1, "text"));
        assertEquals("bookId must be a positive integer", e.getMessage());
        assertThrows(ValidationException.class, () -> service.add(1, 0, "text"));
        assertThrows(ValidationException.class, () -> service.list(-1, 0, 10));
        assertThrows(ValidationException.class, () -> service.delete(1, 0, NOW));
        assertThrows(ValidationException.class, () -> service.update(-5, 1, "text"));
        assertThrows(ValidationException.class, () -> service.getCommentsByUser(0));
    }

    static class FakeBooks implements CatalogRepositoryPort {
        public Page<Book> search(String query, PageRequest request) { return new Page<>(List.of(), request, 0); }
        public List<Book> findAll() { return List.of(BOOK); }
        public Optional<Book> findById(long id) { return id == 1 ? Optional.of(BOOK) : Optional.empty(); }
        public Book save(Book book) { throw new UnsupportedOperationException(); }
        public boolean deleteById(long id) { throw new UnsupportedOperationException(); }
    }

    static class FakeUsers implements UserRepositoryPort {
        public List<User> findAll() { return List.of(IVAN, OLENA); }
        public Optional<User> findById(long id) {
            return findAll().stream().filter(u -> u.getId() == id).findFirst();
        }
        public Optional<User> findByUsername(String username) {
            return findAll().stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }
        public Optional<User> findByEmail(String email) { return Optional.empty(); }
        public Optional<User> findByConfirmationCode(String code) { return Optional.empty(); }
        public User save(User user) { throw new UnsupportedOperationException(); }
    }

    static class FakeComments implements CommentRepositoryPort {
        final List<Comment> data = new ArrayList<>();
        void put(Comment c) { data.add(c); }
        public Comment add(long bookId, long userId, String text, Instant createdAt) {
            User user = new FakeUsers().findById(userId).orElseThrow();
            Comment c = new Comment(data.size() + 1, BOOK, user, text, createdAt);
            data.add(c);
            return c;
        }
        public Page<Comment> findByBook(long bookId, PageRequest request) {
            return new Page<>(data, request, data.size());
        }
        public List<Comment> findByUser(long userId) {
            return data.stream().filter(c -> c.getUserId() == userId)
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
        }
        public Map<Long, Long> countByUser() {
            return data.stream().collect(Collectors.groupingBy(Comment::getUserId, Collectors.counting()));
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
