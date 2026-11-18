package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.test.web.servlet.MockMvc;
import sumdu.edu.ua.core.port.UserRepositoryPort;
import sumdu.edu.ua.web.mail.MailService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:registration-mail-failure;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class RegistrationMailFailureTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepositoryPort users;

    @MockBean
    private MailService mailService;

    @Test
    void userIsSavedEvenIfEmailIsNotSent() throws Exception {
        doThrow(new MailSendException("535 Authentication failed")).when(mailService).sendConfirmationEmail(any());

        mvc.perform(post("/register").with(csrf())
                        .param("username", "maria")
                        .param("email", "maria@example.com")
                        .param("password", "secret1")
                        .param("confirmPassword", "secret1"))
                .andExpect(view().name("auth/register-success"))
                .andExpect(model().attribute("mailSent", false))
                .andExpect(content().string(containsString("Лист не вдалося надіслати.")));

        assertThat(users.findByUsername("maria")).get().matches(u -> !u.isEnabled());
    }
}
