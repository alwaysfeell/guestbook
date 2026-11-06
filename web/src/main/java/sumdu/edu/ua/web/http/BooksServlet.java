package sumdu.edu.ua.web.http;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sumdu.edu.ua.core.domain.Book;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BooksServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(BooksServlet.class);

    private BookService books;
    private CommentService comments;

    @Override
    public void init() {
        books = HttpSupport.bean(getServletContext(), BookService.class);
        comments = HttpSupport.bean(getServletContext(), CommentService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        int page = HttpSupport.intParam(req, "page", 0);
        int size = HttpSupport.intParam(req, "size", PageRequest.DEFAULT_SIZE);

        if (path == null || path.equals("/")) {
            var result = books.search(req.getParameter("q"), page, size, req.getParameter("sort"));
            HttpSupport.writeJson(resp, HttpServletResponse.SC_OK, result);
            return;
        }

        long id = bookIdFromPath(path);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("book", books.getById(id));
        body.put("comments", comments.list(id, page, size));
        HttpSupport.writeJson(resp, HttpServletResponse.SC_OK, body);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path != null && !path.equals("/")) {
            throw new NotFoundException("Unknown path: " + req.getRequestURI());
        }
        Book book = books.add(
                req.getParameter("title"),
                req.getParameter("author"),
                HttpSupport.optionalInt(req, "pubYear"));
        log.info("Book created: id={}, title='{}', author='{}'", book.getId(), book.getTitle(), book.getAuthor());
        resp.setHeader("Location", req.getContextPath() + "/books/" + book.getId());
        HttpSupport.writeJson(resp, HttpServletResponse.SC_CREATED, book);
    }

    private static long bookIdFromPath(String path) {
        String[] parts = path.substring(1).split("/");
        if (parts.length != 1) {
            throw new NotFoundException("Unknown path: /books" + path);
        }
        return HttpSupport.parseId(parts[0], "book id");
    }
}
