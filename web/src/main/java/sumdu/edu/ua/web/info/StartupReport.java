package sumdu.edu.ua.web.info;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupReport {
    private static final Logger log = LoggerFactory.getLogger(StartupReport.class);

    private final AppInfoService info;
    private final Environment env;

    public StartupReport(AppInfoService info, Environment env) {
        this.info = info;
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        var beans = info.applicationBeans();
        log.info("Application beans loaded ({}):", beans.size());
        beans.forEach((name, type) -> log.info("  {} -> {}", name, type));
        log.info("Started at http://localhost:{}/  (books: /books, info: /info)",
                env.getProperty("local.server.port"));
    }

    @PreDestroy
    public void onShutdown() {
        log.info("Spring context is closing - beans are being destroyed");
    }
}
