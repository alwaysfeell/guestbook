package sumdu.edu.ua.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import sumdu.edu.ua.core.port.CatalogRepositoryPort;
import sumdu.edu.ua.core.port.UserRepositoryPort;
import sumdu.edu.ua.core.security.AppUserDetailsService;
import sumdu.edu.ua.core.service.BookService;
import sumdu.edu.ua.core.service.UserService;

import java.time.Clock;

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
    public UserService userService(UserRepositoryPort userRepo, PasswordEncoder passwordEncoder) {
        return new UserService(userRepo, passwordEncoder);
    }

    @Bean
    public AppUserDetailsService userDetailsService(UserRepositoryPort userRepo) {
        return new AppUserDetailsService(userRepo);
    }
}
