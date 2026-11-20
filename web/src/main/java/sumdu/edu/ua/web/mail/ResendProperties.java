package sumdu.edu.ua.web.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "mail.resend")
public record ResendProperties(
        @DefaultValue("https://api.resend.com/emails") String apiUrl,
        @DefaultValue("") String apiKey,
        @DefaultValue("BookApp <onboarding@resend.dev>") String from) {
}
