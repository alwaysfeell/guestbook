package sumdu.edu.ua.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.web.form.BookRequest;

@RestController
@RequestMapping("/api/books")
public class BooksApiController {
    private static final Logger log = LoggerFactory.getLogger(BooksApiController.class);

    private final BookService books;

    public BooksApiController(BookService books) {
        this.books = books;
    }

    @GetMapping
    public Page<Book> list(@RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "${app.catalog.default-page-size:10}") int size,
                           @RequestParam(required = false) String sort) {
        return books.search(q, page, size, sort);
    }

    @GetMapping("/{id}")
    public Book get(@PathVariable long id) {
        return books.findById(id);
    }

    @PostMapping
    public ResponseEntity<Book> create(@RequestBody BookRequest body,
                                       Authentication authentication,
                                       UriComponentsBuilder uri) {
        Book saved = books.add(body.title(), body.author(), body.pubYear());
        log.info("API: book created by '{}': id={}, title='{}'", authentication.getName(), saved.getId(), saved.getTitle());
        return ResponseEntity.created(uri.path("/api/books/{id}").buildAndExpand(saved.getId()).toUri()).body(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id, Authentication authentication) {
        Book deleted = books.delete(id);
        log.info("API: book deleted by '{}': id={}, title='{}'", authentication.getName(), deleted.getId(), deleted.getTitle());
    }
}
