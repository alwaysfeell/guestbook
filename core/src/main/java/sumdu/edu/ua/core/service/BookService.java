package sumdu.edu.ua.core.service;

import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;

import java.time.Year;
import java.util.List;

public class BookService {
    public static final List<String> SORT_FIELDS = List.of("id", "title", "author", "pubYear");

    private final CatalogRepositoryPort repo;

    public BookService(CatalogRepositoryPort repo) {
        this.repo = repo;
    }

    public Page<Book> search(String q, int page, int size, String sort) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        if (query != null && query.length() > 100) {
            throw new ValidationException("q must be at most 100 characters");
        }

        String field = "id";
        boolean ascending = true;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.trim().split(",");
            field = parts[0].trim();
            if (!SORT_FIELDS.contains(field) || parts.length > 2) {
                throw new ValidationException("sort must be one of " + SORT_FIELDS + " with optional ,asc or ,desc");
            }
            if (parts.length == 2) {
                String dir = parts[1].trim().toLowerCase();
                if (!dir.equals("asc") && !dir.equals("desc")) {
                    throw new ValidationException("sort direction must be asc or desc");
                }
                ascending = dir.equals("asc");
            }
        }
        return repo.search(query, new PageRequest(page, size, field, ascending));
    }

    public Book getById(long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Book with id=" + id + " not found"));
    }

    public Book add(String title, String author, Integer pubYear) {
        String t = title == null ? "" : title.trim();
        String a = author == null ? "" : author.trim();
        if (t.isEmpty() || t.length() > 255) {
            throw new ValidationException("title is required (max 255 chars)");
        }
        if (a.isEmpty() || a.length() > 255) {
            throw new ValidationException("author is required (max 255 chars)");
        }
        int currentYear = Year.now().getValue();
        if (pubYear == null || pubYear < 1 || pubYear > currentYear) {
            throw new ValidationException("pubYear must be between 1 and " + currentYear);
        }
        return repo.add(t, a, pubYear);
    }
}
