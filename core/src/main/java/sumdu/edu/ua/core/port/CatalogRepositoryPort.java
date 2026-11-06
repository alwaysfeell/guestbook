package sumdu.edu.ua.core.port;

import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;

import java.util.Optional;

public interface CatalogRepositoryPort {
    Page<Book> search(String query, PageRequest request);

    Optional<Book> findById(long id);

    Book add(String title, String author, int pubYear);
}
