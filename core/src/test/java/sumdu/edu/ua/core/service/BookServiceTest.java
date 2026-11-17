package sumdu.edu.ua.core.service;

import org.junit.jupiter.api.Test;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookServiceTest {
    private PageRequest lastRequest;
    private final List<Book> data = new ArrayList<>(List.of(new Book(1, "Kobzar", "Taras Shevchenko", 1840)));

    private final BookService service = new BookService(new CatalogRepositoryPort() {
        public Page<Book> search(String query, PageRequest request) {
            lastRequest = request;
            return new Page<>(List.of(), request, 0);
        }
        public List<Book> findAll() { return List.copyOf(data); }
        public Optional<Book> findById(long id) { return data.stream().filter(b -> b.getId() == id).findFirst(); }
        public Book save(Book book) {
            Book saved = new Book(book.getId() == 0 ? data.size() + 1 : book.getId(),
                    book.getTitle(), book.getAuthor(), book.getPubYear());
            data.removeIf(b -> b.getId() == saved.getId());
            data.add(saved);
            return saved;
        }
        public boolean deleteById(long id) { return data.removeIf(b -> b.getId() == id); }
    });

    @Test
    void parsesSortParameter() {
        service.search(null, 1, 5, "title,desc");
        assertEquals("title", lastRequest.getSortField());
        assertFalse(lastRequest.isAscending());
        assertEquals(5, lastRequest.getOffset());
    }

    @Test
    void rejectsUnknownSortFieldAndBadPaging() {
        assertThrows(ValidationException.class, () -> service.search(null, 0, 10, "price"));
        assertThrows(ValidationException.class, () -> service.search(null, 0, 10, "title,up"));
        assertThrows(ValidationException.class, () -> service.search(null, -1, 10, null));
        assertThrows(ValidationException.class, () -> service.search(null, 0, 0, null));
        assertThrows(ValidationException.class, () -> service.search(null, 0, 101, null));
    }

    @Test
    void missingBookGivesNotFound() {
        assertThrows(NotFoundException.class, () -> service.findById(7));
        assertThrows(NotFoundException.class, () -> service.delete(7));
    }

    @Test
    void rejectsNonPositiveBookId() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.findById(0));
        assertEquals("book id must be a positive integer", e.getMessage());
        assertThrows(ValidationException.class, () -> service.findById(-3));
    }

    @Test
    void validatesNewBook() {
        assertThrows(ValidationException.class, () -> service.add(" ", "Author", 2000));
        assertThrows(ValidationException.class, () -> service.add("Title", "Author", null));
        assertThrows(ValidationException.class, () -> service.add("Title", "Author", 3000));
    }

    @Test
    void addTrimsAndSavesNewBook() {
        Book saved = service.add("  Intermezzo ", " Михайло Коцюбинський ", 1908);
        assertEquals(2, saved.getId());
        assertEquals("Intermezzo", saved.getTitle());
        assertEquals("Михайло Коцюбинський", saved.getAuthor());
        assertEquals(2, service.findAll().size());
    }

    @Test
    void saveUpdatesExistingBookAndRejectsUnknownId() {
        Book updated = service.save(new Book(1, "Кобзар", "Тарас Шевченко", 1840));
        assertEquals("Кобзар", service.findById(1).getTitle());
        assertEquals(1, updated.getId());
        assertThrows(NotFoundException.class, () -> service.save(new Book(99, "T", "A", 2000)));
    }

    @Test
    void deleteRemovesBook() {
        Book deleted = service.delete(1);
        assertEquals("Kobzar", deleted.getTitle());
        assertTrue(service.findAll().isEmpty());
    }
}
