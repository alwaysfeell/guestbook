package sumdu.edu.ua.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;

public class AppInit implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(AppInit.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Beans.init();
        } catch (RuntimeException e) {
            log.error("Application start failed", e);
            throw e;
        }
        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute(BookService.class.getName(), Beans.bookService());
        ctx.setAttribute(CommentService.class.getName(), Beans.commentService());
        log.info("App started at: http://localhost:8080/");
    }
}
