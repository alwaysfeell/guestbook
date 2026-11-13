package sumdu.edu.ua.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.CommentService;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class CoreConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public BookService bookService(CatalogRepositoryPort bookRepo) {
        return new BookService(bookRepo);
    }

    @Bean
    public CommentService commentService(CommentRepositoryPort commentRepo,
        CatalogRepositoryPort bookRepo,
        Clock clock,
        @Value("${app.comments.delete-window:24h}") Duration deleteWindow) {
        return new CommentService(commentRepo, bookRepo, clock, deleteWindow);
    }
}
