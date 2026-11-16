package sumdu.edu.ua.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Configuration
public class LanguageConfig implements WebMvcConfigurer {

    public static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("uk");
    public static final List<Locale> SUPPORTED_LOCALES = List.of(DEFAULT_LOCALE, Locale.ENGLISH);

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("lang");
        resolver.setCookieMaxAge(Duration.ofDays(30));
        resolver.setDefaultLocaleFunction(LanguageConfig::fromAcceptLanguage);
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true);
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    static Locale fromAcceptLanguage(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (header == null || header.isBlank()) {
            return DEFAULT_LOCALE;
        }
        try {
            Locale match = Locale.lookup(Locale.LanguageRange.parse(header), SUPPORTED_LOCALES);
            return match != null ? match : DEFAULT_LOCALE;
        } catch (IllegalArgumentException e) {
            return DEFAULT_LOCALE;
        }
    }
}
