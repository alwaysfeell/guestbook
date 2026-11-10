package sumdu.edu.ua.web.javalin;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.service.BookService;

public class BooksController {
    private static final Logger log = LoggerFactory.getLogger(BooksController.class);

    private final BookService books;

    public BooksController(BookService books) {
        this.books = books;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/books", this::getBooks);
        app.get("/api/books/{id}", this::getBook);
        app.post("/api/books", this::addBook);
    }

    private void getBooks(Context ctx) {
        String q = ctx.queryParam("q");
        String sort = ctx.queryParam("sort");
        int page = Params.intQuery(ctx, "page", 0);
        int size = Params.intQuery(ctx, "size", PageRequest.DEFAULT_SIZE);
        ctx.json(books.search(q, page, size, sort));
    }

    private void getBook(Context ctx) {
        long id = Params.pathId(ctx, "id");
        ctx.json(books.getById(id));
    }

    private void addBook(Context ctx) {
        BookRequest request = Params.body(ctx, BookRequest.class);
        Book book = books.add(request.title(), request.author(), request.pubYear());
        log.info("Book created: id={}, title='{}', author='{}'", book.getId(), book.getTitle(), book.getAuthor());
        ctx.header("Location", "/api/books/" + book.getId());
        ctx.status(201).json(book);
    }
}
