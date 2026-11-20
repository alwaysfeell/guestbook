package sumdu.edu.ua.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.domain.Role;
import sumdu.edu.ua.core.domain.User;
import sumdu.edu.ua.core.port.UserRepositoryPort;
import sumdu.edu.ua.web.mail.ResendStub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:registration;DB_CLOSE_DELAY=-1",
        "mail.resend.api-key=re_test_key"
})
@AutoConfigureMockMvc
class RegistrationTest {

    static final ResendStub resend = new ResendStub();

    @DynamicPropertySource
    static void resendUrl(DynamicPropertyRegistry registry) {
        registry.add("mail.resend.api-url", resend::url);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepositoryPort users;

    @BeforeEach
    void resetStub() {
        resend.reset();
    }

    @Test
    void registrationFormHasAllFields() throws Exception {
        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<h1>Реєстрація</h1>"),
                        containsString("action=\"/register\""),
                        containsString("name=\"username\""),
                        containsString("name=\"email\""),
                        containsString("name=\"password\""),
                        containsString("name=\"confirmPassword\""),
                        containsString("name=\"_csrf\""))));
    }

    @Test
    void registerConfirmByEmailAndLogin() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "taras")
                        .param("email", "Taras@Example.com")
                        .param("password", "secret1")
                        .param("confirmPassword", "secret1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register-success"))
                .andExpect(model().attribute("mailSent", true))
                .andExpect(content().string(allOf(
                        containsString("Користувача taras зареєстровано з роллю USER."),
                        containsString("taras@example.com"))));

        User pending = users.findByUsername("taras").orElseThrow();
        assertThat(pending.getRole()).isEqualTo(Role.USER);
        assertThat(pending.isEnabled()).isFalse();
        assertThat(pending.getPassword()).startsWith("$2a$10$").isNotEqualTo("secret1");
        String code = pending.getConfirmationCode();
        assertThat(code).isNotBlank();

        assertThat(resend.requests()).hasSize(1);
        JsonNode email = resend.requests().get(0).body();
        assertThat(email.get("to").get(0).asText()).isEqualTo("taras@example.com");
        assertThat(email.get("subject").asText()).isEqualTo("Підтвердження реєстрації в каталозі книг");
        assertThat(email.get("html").asText())
                .contains("Вітаємо, <b>taras</b>!")
                .contains("href=\"http://localhost:8080/confirm?code=" + code + "\"");

        mvc.perform(formLogin("/login").user("taras").password("secret1"))
                .andExpect(redirectedUrl("/login?disabled"))
                .andExpect(unauthenticated());
        mvc.perform(get("/login").param("disabled", ""))
                .andExpect(content().string(containsString("Акаунт ще не підтверджено.")));

        mvc.perform(get("/confirm").param("code", code))
                .andExpect(redirectedUrl("/login?confirmed"));
        assertThat(users.findByUsername("taras").orElseThrow().isEnabled()).isTrue();

        mvc.perform(formLogin("/login").user("taras").password("secret1"))
                .andExpect(redirectedUrl("/books"))
                .andExpect(authenticated().withUsername("taras").withRoles("USER"));

        mvc.perform(get("/confirm").param("code", code))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("auth/confirm-error"))
                .andExpect(content().string(containsString("недійсне або вже використане")));
    }

    @Test
    void invalidRegistrationShowsFieldErrors() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "olena")
                        .param("email", "OLENA@bookapp.local")
                        .param("password", "secret1")
                        .param("confirmPassword", "secret2"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrorCode("registerForm", "username", "register.usernameTaken"))
                .andExpect(model().attributeHasFieldErrorCode("registerForm", "email", "register.emailTaken"))
                .andExpect(model().attributeHasFieldErrorCode("registerForm", "confirmPassword", "register.passwordMismatch"))
                .andExpect(content().string(allOf(
                        containsString("Цей логін уже зайнятий."),
                        containsString("Користувач з таким email уже зареєстрований."),
                        containsString("Паролі не збігаються."))));

        mvc.perform(post("/register").with(csrf())
                        .param("username", "a b")
                        .param("email", "not-an-email")
                        .param("password", "123")
                        .param("confirmPassword", "123"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "username", "email", "password"))
                .andExpect(content().string(allOf(
                        containsString("Логін може містити лише латинські літери"),
                        containsString("Вкажіть коректний email."),
                        containsString("Пароль має містити від 6 до 72 символів."))));

        assertThat(users.findAll()).hasSize(5);
        assertThat(resend.requests()).isEmpty();
    }

    @Test
    void unknownConfirmationCodeIsRejected() throws Exception {
        mvc.perform(get("/confirm").param("code", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("auth/confirm-error"));
        mvc.perform(get("/confirm"))
                .andExpect(status().isBadRequest());
    }
}
