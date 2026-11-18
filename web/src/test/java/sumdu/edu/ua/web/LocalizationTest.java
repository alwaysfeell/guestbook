package sumdu.edu.ua.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:localization;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class LocalizationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MessageSource messages;

    @Test
    void ukrainianIsTheDefaultLanguage() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(content().string(allOf(
                        containsString("<html lang=\"uk\">"),
                        containsString("<h1>Каталог книг</h1>"),
                        containsString(">Додати книгу<"))));
    }

    @Test
    void acceptLanguageHeaderSelectsTranslation() throws Exception {
        mvc.perform(get("/books").header("Accept-Language", "en-US,en;q=0.9"))
                .andExpect(content().string(allOf(
                        containsString("<html lang=\"en\">"),
                        containsString("<h1>Books catalog</h1>"),
                        containsString("Total books: 12"))));

        mvc.perform(get("/books/add").header("Accept-Language", "de-DE,de;q=0.9,en;q=0.8"))
                .andExpect(content().string(allOf(
                        containsString("<h1>New book</h1>"),
                        containsString(">Title<"),
                        containsString(">Author<"))));

        mvc.perform(get("/books").header("Accept-Language", "uk-UA,uk;q=0.9,en;q=0.8"))
                .andExpect(content().string(containsString("<h1>Каталог книг</h1>")));

        mvc.perform(get("/books").header("Accept-Language", "de-DE,de;q=0.9"))
                .andExpect(content().string(containsString("<h1>Каталог книг</h1>")));
    }

    @Test
    void langParameterChangesLanguageAndIsRememberedInCookie() throws Exception {
        mvc.perform(get("/books").param("lang", "en"))
                .andExpect(cookie().value("lang", "en"))
                .andExpect(content().string(containsString("<h1>Books catalog</h1>")));

        mvc.perform(get("/books").cookie(new Cookie("lang", "uk")).header("Accept-Language", "en"))
                .andExpect(content().string(containsString("<h1>Каталог книг</h1>")));
    }

    @Test
    void messagesAreResolvedForEveryLanguage() {
        assertThat(messages.getMessage("label.noBooks", null, Locale.forLanguageTag("uk"))).isEqualTo("Ще немає книг.");
        assertThat(messages.getMessage("label.noBooks", null, Locale.ENGLISH)).isEqualTo("No books yet.");
        assertThat(messages.getMessage("form.add", null, Locale.GERMAN)).isEqualTo("Додати");
    }

    @Test
    void englishBundleHasTheSameKeys() throws IOException {
        Properties uk = load("messages.properties");
        assertThat(load("messages_en.properties").keySet()).isEqualTo(uk.keySet());
    }

    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (InputStream in = LocalizationTest.class.getClassLoader().getResourceAsStream(name)) {
            p.load(in);
        }
        return p;
    }
}
