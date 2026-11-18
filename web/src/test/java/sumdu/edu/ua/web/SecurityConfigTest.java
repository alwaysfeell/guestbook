package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:security-config;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void guestIsRedirectedToLoginPage() throws Exception {
        for (String url : new String[]{"/", "/books", "/books/1/comments", "/books/add", "/users", "/users/2/comments"}) {
            mvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    void publicResourcesAreAvailableWithoutLogin() throws Exception {
        mvc.perform(get("/css/style.css")).andExpect(status().isOk());
        mvc.perform(get("/img/logo.png")).andExpect(status().isOk());
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Вхід</h1>"),
                        containsString("action=\"/login\" method=\"post\""),
                        containsString("name=\"username\""),
                        containsString("name=\"password\""),
                        containsString("name=\"_csrf\""),
                        containsString(">Увійти<"),
                        containsString(">Реєстрація<"),
                        not(containsString(">Вийти<")))));
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("auth/register"));
    }

    @Test
    void loginWithValidCredentialsRedirectsToBooks() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("admin123"))
                .andExpect(redirectedUrl("/books"))
                .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"));

        mvc.perform(formLogin("/login").user("olena").password("user123"))
                .andExpect(redirectedUrl("/books"))
                .andExpect(authenticated().withUsername("olena").withRoles("USER"));
    }

    @Test
    void loginWithWrongPasswordReturnsToLoginWithError() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("wrong"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
        mvc.perform(formLogin("/login").user("nobody").password("whatever"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());

        mvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Неправильний логін або пароль.")));
    }

    @Test
    void loginFormRequiresCsrfToken() throws Exception {
        mvc.perform(post("/login").param("username", "admin").param("password", "admin123"))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
    }

    @Test
    void logoutRedirectsToLoginPage() throws Exception {
        mvc.perform(logout("/logout"))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());

        mvc.perform(get("/login").param("logout", ""))
                .andExpect(content().string(containsString("Ви вийшли з системи.")));
    }

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void loggedInUserIsRedirectedFromLoginAndRegister() throws Exception {
        mvc.perform(get("/login")).andExpect(redirectedUrl("/books"));
        mvc.perform(get("/register")).andExpect(redirectedUrl("/books"));
    }

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void userCanViewBooksButWithoutAdminButtons() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<span class=\"principal\">olena</span>"),
                        containsString(">користувач</span>"),
                        containsString("action=\"/logout\""),
                        containsString(">Вийти<"),
                        not(containsString("href=\"/books/add\"")),
                        not(containsString(">адміністратор<")))));

        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("vladyslav"),
                        not(containsString("olena@bookapp.local")))));
    }

    @Test
    @WithMockUser(username = "olena", roles = "USER")
    void userGetsForbiddenForAdminActions() throws Exception {
        mvc.perform(get("/books/add"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/403"));
        mvc.perform(post("/books/add").with(csrf()).param("title", "T").param("author", "A").param("pubYear", "2000"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/books/1/delete").with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/comments/delete").with(csrf())
                        .param("bookId", "12").param("commentId", "10").param("createdAt", "2026-01-01T00:00:00Z"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/info"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/h2-console"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminSeesAdminButtonsAndCanOpenAdminPages() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(content().string(allOf(
                        containsString("<span class=\"principal\">admin</span>"),
                        containsString(">адміністратор</span>"),
                        containsString("href=\"/books/add\""),
                        containsString(">Вийти<"))));
        mvc.perform(get("/books/add")).andExpect(status().isOk()).andExpect(view().name("book-form"));
        mvc.perform(get("/books/12/comments"))
                .andExpect(content().string(containsString("action=\"/books/12/delete\"")));
        mvc.perform(get("/users"))
                .andExpect(content().string(allOf(
                        containsString("olena@bookapp.local"),
                        containsString("активний"))));
        mvc.perform(get("/info")).andExpect(status().isOk());
    }
}
