package sumdu.edu.ua.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.Page;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.dto.BookDetails;
import sumdu.edu.ua.web.dto.NewBookRequest;

import java.net.URI;

@RestController
@RequestMapping("/books")
public class BooksController {
    private static final Logger log = LoggerFactory.getLogger(BooksController.class);

    private final BookService books;
    private final CommentService comments;

    public BooksController(BookService books, CommentService comments) {
        this.books = books;
        this.comments = comments;
    }

    @GetMapping
    public ResponseEntity<Page<Book>> getBooks(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.catalog.default-page-size:10}") int size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(books.search(q, page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDetails> getBook(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "${app.catalog.default-page-size:10}") int size) {
        Book book = books.getById(id);
        return ResponseEntity.ok(new BookDetails(book, comments.list(id, page, size)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Book> addBook(@RequestBody NewBookRequest request, UriComponentsBuilder uri) {
        Book saved = books.add(request.title(), request.author(), request.pubYear());
        log.info("Book created: id={}, title='{}', author='{}'", saved.getId(), saved.getTitle(), saved.getAuthor());
        URI location = uri.path("/books/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }
}
