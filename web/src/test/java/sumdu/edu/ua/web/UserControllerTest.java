package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.domain.Role;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.port.UserRepositoryPort;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:user-controller;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepositoryPort users;

    @Test
    void usersPageListsUsersWithCommentCounts() throws Exception {
        mvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users"))
                .andExpect(model().attribute("users", hasSize(5)))
                .andExpect(model().attributeExists("commentCounts"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Користувачі</h1>"),
                        containsString("vladyslav"),
                        containsString("адміністратор"),
                        containsString("href=\"/users/4/comments\""),
                        containsString("Усього користувачів: 5"))));
    }

    @Test
    void userCommentsPageShowsCommentsWithBookLinks() throws Exception {
        mvc.perform(get("/users/4/comments"))
                .andExpect(status().isOk())
                .andExpect(view().name("user-comments"))
                .andExpect(model().attribute("username", "vladyslav"))
                .andExpect(model().attribute("comments", hasSize(3)))
                .andExpect(content().string(allOf(
                        containsString("<h1>Коментарі користувача: vladyslav</h1>"),
                        containsString("Відгуків: 3"),
                        containsString("Розділ про builder і immutable-класи використав у лабораторних."),
                        containsString("href=\"/books/12\">Effective Java</a>"),
                        containsString("href=\"/books/8\">Domain-Driven Design</a>"),
                        containsString("href=\"/books/10\">Clean Code</a>"))));
    }

    @Test
    void userCommentsPageIsLocalized() throws Exception {
        mvc.perform(get("/users/1/comments").header("Accept-Language", "en"))
                .andExpect(content().string(allOf(
                        containsString("<h1>Comments by user: admin</h1>"),
                        containsString("administrator"),
                        containsString("Book:"))));
    }

    @Test
    void userWithoutCommentsSeesEmptyState() throws Exception {
        User maria = users.save(new User(0, "maria", "$2a$10$hash", Role.USER));

        mvc.perform(get("/users/{id}/comments", maria.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("comments", hasSize(0)))
                .andExpect(content().string(allOf(
                        containsString("Користувач ще не залишив жодного відгуку."),
                        not(containsString("<ul class=\"comments\">")))));
    }

    @Test
    void unknownUserGivesNotFoundPage() throws Exception {
        mvc.perform(get("/users/99/comments"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(content().string(containsString("User with id=99 not found")));

        mvc.perform(get("/users/abc/comments"))
                .andExpect(status().isBadRequest());
    }
}
