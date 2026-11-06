package sumdu.edu.ua.web.http;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.service.CommentService;

import java.io.IOException;

public class CommentsServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CommentsServlet.class);

    private CommentService comments;

    @Override
    public void init() {
        comments = HttpSupport.bean(getServletContext(), CommentService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        requireRoot(req);
        long bookId = HttpSupport.requiredId(req, "bookId");
        var page = comments.list(bookId,
                HttpSupport.intParam(req, "page", 0),
                HttpSupport.intParam(req, "size", PageRequest.DEFAULT_SIZE));
        HttpSupport.writeJson(resp, HttpServletResponse.SC_OK, page);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        requireRoot(req);
        long bookId = HttpSupport.requiredId(req, "bookId");
        Comment c = comments.add(bookId, req.getParameter("author"), req.getParameter("text"));
        log.info("Comment created: id={}, bookId={}, author='{}', length={}",
                c.getId(), c.getBookId(), c.getAuthor(), c.getText().length());
        HttpSupport.writeJson(resp, HttpServletResponse.SC_CREATED, c);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        String path = req.getPathInfo();
        if (path == null || path.equals("/") || path.substring(1).contains("/")) {
            throw new NotFoundException("Use DELETE /comments/{id}?bookId={bookId}");
        }
        long commentId = HttpSupport.parseId(path.substring(1), "comment id");
        long bookId = HttpSupport.requiredId(req, "bookId");
        Comment c = comments.delete(bookId, commentId);
        log.info("Comment deleted: id={}, bookId={}, author='{}'", c.getId(), c.getBookId(), c.getAuthor());
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private static void requireRoot(HttpServletRequest req) {
        String path = req.getPathInfo();
        if (path != null && !path.equals("/")) {
            throw new NotFoundException("Unknown path: " + req.getRequestURI());
        }
    }
}
