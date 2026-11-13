package sumdu.edu.ua.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue("Guestbook") String title,
        @DefaultValue Catalog catalog) {

    public record Catalog(@DefaultValue("10") int defaultPageSize) {
    }
}
