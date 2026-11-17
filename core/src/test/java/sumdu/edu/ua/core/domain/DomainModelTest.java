package sumdu.edu.ua.core.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelTest {
    private static final Instant CREATED = Instant.parse("2026-11-10T10:00:00Z");
    private static final Book BOOK = new Book(1, "Kobzar", "Taras Shevchenko", 1840);
    private static final User IVAN = new User(3, "ivan", "$2a$10$hash", Role.USER);

    @Test
    void booksWithSameDataAreEqual() {
        Book b = new Book(1, "Kobzar", "Taras Shevchenko", 1840);
        assertEquals(BOOK, b);
        assertEquals(BOOK.hashCode(), b.hashCode());
        assertNotEquals(BOOK, new Book(2, "Kobzar", "Taras Shevchenko", 1840));
        assertTrue(BOOK.toString().contains("title='Kobzar'"));
    }

    @Test
    void commentReferencesBookAndUser() {
        Comment c = new Comment(5, BOOK, IVAN, "text", CREATED);
        assertEquals(1, c.getBookId());
        assertEquals(3, c.getUserId());
        assertEquals("ivan", c.getAuthor());
        assertEquals("Kobzar", c.getBook().getTitle());
    }

    @Test
    void commentWithTextKeepsOtherFields() {
        Comment c = new Comment(5, BOOK, IVAN, "old", CREATED);
        Comment changed = c.withText("new");
        assertNotSame(c, changed);
        assertEquals("old", c.getText());
        assertEquals(new Comment(5, BOOK, IVAN, "new", CREATED), changed);
    }

    @Test
    void userToStringHidesPassword() {
        assertFalse(IVAN.toString().contains("$2a$10$hash"));
        assertFalse(IVAN.isAdmin());
        assertTrue(new User(1, "admin", "x", Role.ADMIN).isAdmin());
    }
}
