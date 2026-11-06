package sumdu.edu.ua.core.service;

import org.junit.jupiter.api.Test;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookServiceTest {
    private PageRequest lastRequest;

    private final BookService service = new BookService(new CatalogRepositoryPort() {
        public Page<Book> search(String query, PageRequest request) {
            lastRequest = request;
            return new Page<>(List.of(), request, 0);
        }
        public Optional<Book> findById(long id) { return Optional.empty(); }
        public Book add(String title, String author, int pubYear) { return new Book(1, title, author, pubYear); }
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
        assertThrows(NotFoundException.class, () -> service.getById(7));
    }

    @Test
    void validatesNewBook() {
        assertThrows(ValidationException.class, () -> service.add(" ", "Author", 2000));
        assertThrows(ValidationException.class, () -> service.add("Title", "Author", null));
        assertThrows(ValidationException.class, () -> service.add("Title", "Author", 3000));
    }
}
