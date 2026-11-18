package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.FlashMap;
import sumdu.edu.ua.core.service.CommentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:mvc-exception-handler;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@WithMockUser(username = "olena", roles = "USER")
class MvcExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CommentService comments;

    @Test
    void forbiddenWordRedirectsToProblemPage() throws Exception {
        MvcResult result = mvc.perform(post("/books/3/comments").with(csrf()).param("text", "Найкраще казино міста"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/problem"))
                .andExpect(flash().attribute("problemMessage", "Коментар містить заборонене слово «казино»"))
                .andExpect(flash().attribute("problemBack", "/books/3/comments"))
                .andReturn();
        assertThat(comments.list(3, 0, 10).getTotal()).isEqualTo(1);

        FlashMap flash = result.getFlashMap();
        mvc.perform(get("/problem").flashAttrs(flash))
                .andExpect(status().isOk())
                .andExpect(view().name("error/problem"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Дію не виконано</h1>"),
                        containsString("Коментар містить заборонене слово «казино»"),
                        containsString("href=\"/books/3/comments\""))));
    }

    @Test
    void problemPageWithoutMessageShowsDefaultText() throws Exception {
        mvc.perform(get("/problem").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>Action not completed</h1>"),
                        containsString("An error occurred while performing the action."),
                        containsString("href=\"/books\""))));
    }

    @Test
    void allowedCommentIsStillSaved() throws Exception {
        mvc.perform(post("/books/5/comments").with(csrf()).param("text", "Спамерам тут не місце, гарна книга"))
                .andExpect(redirectedUrl("/books/5/comments"))
                .andExpect(flash().attribute("notice", "comments.added"));
        assertThat(comments.list(5, 0, 10).getTotal()).isEqualTo(1);
    }
}
