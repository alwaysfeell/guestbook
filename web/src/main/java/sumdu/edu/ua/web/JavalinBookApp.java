package sumdu.edu.ua.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sumdu.edu.ua.config.Beans;
import sumdu.edu.ua.core.exception.ConflictException;
import sumdu.edu.ua.core.exception.NotFoundException;
import sumdu.edu.ua.core.exception.ValidationException;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;
import sumdu.edu.ua.web.error.ApiError;
import sumdu.edu.ua.web.javalin.BooksController;
import sumdu.edu.ua.web.javalin.CommentsController;

public class JavalinBookApp {
    private static final Logger log = LoggerFactory.getLogger(JavalinBookApp.class);
    private static final int PORT = 8080;

    public static void main(String[] args) {
        Beans.init();

        Javalin app = create(Beans.bookService(), Beans.commentService());
        app.start(PORT);
        log.info("Javalin app started at: http://localhost:{}/", PORT);
    }

    public static Javalin create(BookService bookService, CommentService commentService) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, false));
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.http.prefer405over404 = true;
        });

        app.before(ctx -> log.info("{} {}", ctx.method(), fullPath(ctx)));
        app.after(ctx -> log.info("{} {} -> {}", ctx.method(), fullPath(ctx), ctx.statusCode()));

        new BooksController(bookService).registerRoutes(app);
        new CommentsController(commentService).registerRoutes(app);

        app.exception(ValidationException.class, (e, ctx) -> error(ctx, 400, e.getMessage()));
        app.exception(NotFoundException.class, (e, ctx) -> error(ctx, 404, e.getMessage()));
        app.exception(ConflictException.class, (e, ctx) -> error(ctx, 409, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            log.error("{} {} -> 500: {}", ctx.method(), fullPath(ctx), e.getMessage(), e);
            ctx.status(500).json(new ApiError(500, "Internal server error", ctx.path()));
        });

        return app;
    }

    private static void error(Context ctx, int status, String message) {
        log.warn("{} {} -> {}: {}", ctx.method(), fullPath(ctx), status, message);
        ctx.status(status).json(new ApiError(status, message, ctx.path()));
    }

    private static String fullPath(Context ctx) {
        String query = ctx.queryString();
        return query == null ? ctx.path() : ctx.path() + "?" + query;
    }
}
