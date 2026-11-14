package sumdu.edu.ua.core.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelTest {
    private static final Instant CREATED = Instant.parse("2026-11-10T10:00:00Z");

    @Test
    void booksWithSameDataAreEqual() {
        Book a = new Book(1, "Kobzar", "Taras Shevchenko", 1840);
        Book b = new Book(1, "Kobzar", "Taras Shevchenko", 1840);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new Book(2, "Kobzar", "Taras Shevchenko", 1840));
        assertTrue(a.toString().contains("title='Kobzar'"));
    }

    @Test
    void commentWithTextKeepsOtherFields() {
        Comment c = new Comment(5, 1, "Ivan", "old", CREATED);
        Comment changed = c.withText("new");
        assertNotSame(c, changed);
        assertEquals("old", c.getText());
        assertEquals(new Comment(5, 1, "Ivan", "new", CREATED), changed);
    }
}
