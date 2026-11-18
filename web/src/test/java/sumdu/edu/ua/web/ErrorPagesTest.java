package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:error-pages;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ErrorPagesTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void forbiddenPageShowsCurrentUser() throws Exception {
        mvc.perform(get("/403"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Доступ заборонено</h1>"),
                        containsString("Код відповіді: 403"),
                        containsString("Користувач olena не має прав на цю дію."))));
    }

    @Test
    void errorPagesArePublic() throws Exception {
        mvc.perform(get("/404"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("<h1>Сторінку не знайдено</h1>")));

        mvc.perform(get("/403").header("Accept-Language", "en"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(allOf(
                        containsString("<h1>Access denied</h1>"),
                        containsString("Please log in to continue."))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unknownUrlRendersNotFoundPage() throws Exception {
        mvc.perform(get("/nope"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(allOf(
                        containsString("Сторінку не знайдено"),
                        containsString("No route for GET /nope"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unsupportedMethodIsHandledByErrorTemplate() throws Exception {
        mvc.perform(delete("/books").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(view().name("error"))
                .andExpect(content().string(allOf(
                        containsString("Method Not Allowed"),
                        containsString("Код відповіді: 405"))));
    }
}
