package sumdu.edu.ua.web.mail;

import freemarker.cache.ClassTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.servlet.ViewResolver;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:mail-config;DB_CLOSE_DELAY=-1")
class MailConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    @Qualifier("emailFreemarkerConfiguration")
    private Configuration emailConfiguration;

    @Test
    void gmailSmtpSettingsAreApplied() {
        JavaMailSenderImpl sender = (JavaMailSenderImpl) context.getBean(JavaMailSender.class);
        assertThat(sender.getHost()).isEqualTo("smtp.gmail.com");
        assertThat(sender.getPort()).isEqualTo(587);
        assertThat(sender.getDefaultEncoding()).isEqualTo("UTF-8");
        assertThat(sender.getJavaMailProperties())
                .containsEntry("mail.smtp.auth", "true")
                .containsEntry("mail.smtp.starttls.enable", "true")
                .containsEntry("mail.smtp.ssl.trust", "smtp.gmail.com");
    }

    @Test
    void emailTemplatesHaveSeparateFreemarkerConfiguration() {
        assertThat(context.getBeansOfType(Configuration.class))
                .containsKey("emailFreemarkerConfiguration")
                .hasSizeGreaterThanOrEqualTo(2);
        assertThat(emailConfiguration.getTemplateLoader()).isInstanceOf(ClassTemplateLoader.class);
        assertThat(((ClassTemplateLoader) emailConfiguration.getTemplateLoader()).getBasePackagePath())
                .isEqualTo("mail-templates/");
        assertThat(emailConfiguration.getOutputFormat()).isEqualTo(HTMLOutputFormat.INSTANCE);
        assertThat(emailConfiguration.getDefaultEncoding()).isEqualTo("UTF-8");
    }

    @Test
    void thymeleafStillRendersPages() {
        assertThat(context.getBeansOfType(ViewResolver.class))
                .containsKeys("thymeleafViewResolver", "freeMarkerViewResolver");
        AppMailProperties props = context.getBean(AppMailProperties.class);
        assertThat(props.fromName()).isEqualTo("BookApp");
        assertThat(props.baseUrl()).isEqualTo("http://localhost:8080");
        assertThat(props.logoUrl()).endsWith("/lab7/web/src/main/resources/static/img/logo.png");
    }
}
