package sumdu.edu.ua.config;

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;

import java.util.Locale;
import java.util.TimeZone;

@org.springframework.context.annotation.Configuration
public class EmailFreemarkerConfig {

    @Bean(name = "emailFreemarkerConfiguration")
    public Configuration emailFreemarkerConfiguration() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "/mail-templates/");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setLocale(Locale.forLanguageTag("uk"));
        cfg.setTimeZone(TimeZone.getDefault());
        cfg.setNumberFormat("computer");
        cfg.setDateTimeFormat("yyyy-MM-dd HH:mm");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        return cfg;
    }
}
