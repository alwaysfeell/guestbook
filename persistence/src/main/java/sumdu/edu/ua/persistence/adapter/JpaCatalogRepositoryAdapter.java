package sumdu.edu.ua.persistence.adapter;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;
import sumdu.edu.ua.persistence.entity.BookEntity;
import sumdu.edu.ua.persistence.repository.BookRepository;

import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class JpaCatalogRepositoryAdapter implements CatalogRepositoryPort {
    private final BookRepository repo;

    public JpaCatalogRepositoryAdapter(BookRepository repo) {
        this.repo = repo;
    }

    @Override
    public Page<Book> search(String query, PageRequest request) {
        Sort.Direction direction = request.isAscending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, property(request.getSortField())).and(Sort.by("id"));
        Pageable pageable = org.springframework.data.domain.PageRequest.of(request.getPage(), request.getSize(), sort);

        var page = query == null || query.isBlank()
                ? repo.findAll(pageable)
                : repo.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query, pageable);

        List<Book> items = page.getContent().stream().map(EntityMapper::toDomain).toList();
        return new Page<>(items, request, page.getTotalElements());
    }

    @Override
    public List<Book> findAll() {
        return repo.findAll(Sort.by("id")).stream().map(EntityMapper::toDomain).toList();
    }

    @Override
    public Optional<Book> findById(long id) {
        return repo.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    @Transactional
    public Book save(Book book) {
        BookEntity entity = book.getId() == 0
                ? new BookEntity(book.getTitle(), book.getAuthor(), book.getPubYear())
                : repo.findById(book.getId()).orElseThrow();
        entity.setTitle(book.getTitle());
        entity.setAuthor(book.getAuthor());
        entity.setYear(book.getPubYear());
        return EntityMapper.toDomain(repo.save(entity));
    }

    @Override
    @Transactional
    public boolean deleteById(long id) {
        Optional<BookEntity> entity = repo.findById(id);
        entity.ifPresent(repo::delete);
        return entity.isPresent();
    }

    private static String property(String sortField) {
        return switch (sortField) {
            case "title" -> "title";
            case "author" -> "author";
            case "pubYear" -> "year";
            default -> "id";
        };
    }
}
