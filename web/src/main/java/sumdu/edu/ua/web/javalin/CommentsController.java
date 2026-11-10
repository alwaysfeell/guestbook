package sumdu.edu.ua.web.javalin;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sumdu.edu.ua.core.domain.Comment;
import sumdu.edu.ua.core.domain.PageRequest;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.service.CommentService;

public class CommentsController {
    private static final Logger log = LoggerFactory.getLogger(CommentsController.class);

    private final CommentService comments;

    public CommentsController(CommentService comments) {
        this.comments = comments;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/books/{bookId}/comments", this::getComments);
        app.post("/api/comments", this::addComment);
        app.delete("/api/comments/{id}", this::deleteComment);
    }

    private void getComments(Context ctx) {
        long bookId = Params.pathId(ctx, "bookId");
        int page = Params.intQuery(ctx, "page", 0);
        int size = Params.intQuery(ctx, "size", PageRequest.DEFAULT_SIZE);
        ctx.json(comments.list(bookId, page, size));
    }

    private void addComment(Context ctx) {
        CommentRequest request = Params.body(ctx, CommentRequest.class);
        if (request.bookId() == null || request.bookId() <= 0) {
            throw new ValidationException("bookId must be a positive integer");
        }
        Comment c = comments.add(request.bookId(), request.author(), request.text());
        log.info("Comment created: id={}, bookId={}, author='{}', length={}",
                c.getId(), c.getBookId(), c.getAuthor(), c.getText().length());
        ctx.status(201).json(c);
    }

    private void deleteComment(Context ctx) {
        long commentId = Params.pathId(ctx, "id");
        long bookId = Params.requiredQueryId(ctx, "bookId");
        Comment c = comments.delete(bookId, commentId);
        log.info("Comment deleted: id={}, bookId={}, author='{}'", c.getId(), c.getBookId(), c.getAuthor());
        ctx.status(204);
    }
}
