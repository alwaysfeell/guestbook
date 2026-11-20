package sumdu.edu.ua.web.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(
        @DefaultValue("") String admin,
        @DefaultValue("http://localhost:8080") String baseUrl,
        @DefaultValue("") String logoUrl) {
}
