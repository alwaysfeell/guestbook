package sumdu.edu.ua.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;
import sumdu.edu.ua.core.port.CommentRepositoryPort;
import sumdu.edu.ua.core.port.UserRepositoryPort;
import sumdu.edu.ua.core.service.CommentService;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ServiceConfig {

    @Bean
    public CommentService commentService(CommentRepositoryPort commentRepositoryPort,
                                         CatalogRepositoryPort catalogRepositoryPort,
                                         UserRepositoryPort userRepositoryPort,
                                         Clock clock,
                                         @Value("${app.comments.delete-window:24h}") Duration deleteWindow) {
        return new CommentService(commentRepositoryPort, catalogRepositoryPort, userRepositoryPort, clock, deleteWindow);
    }
}
