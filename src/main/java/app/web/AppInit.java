package app.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppInit implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(AppInit.class);

    @Override
    public void contextInitialized(ServletContextEvent e) {
        try {
            new CommentDao().init();
            log.info("Database initialized: table 'comments' is ready");
        } catch (Exception ex) {
            log.error("Database initialization failed", ex);
            throw new RuntimeException(ex);
        }
    }
}
