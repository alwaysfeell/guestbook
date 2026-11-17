package sumdu.edu.ua.web.mail;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.Map;

@Component
public class EmailTemplateProcessor {

    private final Configuration cfg;

    public EmailTemplateProcessor(@Qualifier("emailFreemarkerConfiguration") Configuration cfg) {
        this.cfg = cfg;
    }

    public String process(String templateName, Map<String, Object> model) {
        try {
            Template template = cfg.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot render email template " + templateName, e);
        }
    }
}
