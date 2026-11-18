package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:security-audit;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SecurityAuditListenerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void loginAndLogoutAreLogged(CapturedOutput output) throws Exception {
        MvcResult login = mvc.perform(formLogin("/login").user("olena").password("user123")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(post("/logout").with(csrf()).session(session));

        assertThat(output).contains("AUDIT login success: user='olena', roles=ROLE_USER")
                .contains("request=POST /login")
                .contains("AUDIT logout: user='olena'")
                .contains("request=POST /logout");
    }

    @Test
    void failedLoginIsLogged(CapturedOutput output) throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("wrong"));

        assertThat(output).contains("AUDIT login failure: user='admin', reason=BadCredentialsException");
    }

    @Test
    void deniedAccessIsLogged(CapturedOutput output) throws Exception {
        mvc.perform(get("/api/books"));
        mvc.perform(get("/books/add").with(httpBasic("olena", "user123")));
        MvcResult login = mvc.perform(formLogin("/login").user("olena").password("user123")).andReturn();
        mvc.perform(get("/books/add").session((MockHttpSession) login.getRequest().getSession(false)));

        assertThat(output).contains("AUDIT access denied: user='anonymous', target=GET /api/books")
                .contains("AUDIT access denied: user='olena', target=GET /books/add");
    }
}
