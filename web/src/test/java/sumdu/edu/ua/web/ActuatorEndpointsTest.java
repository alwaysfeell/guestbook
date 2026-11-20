package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:actuator;DB_CLOSE_DELAY=-1",
        "spring.h2.console.enabled=false"
})
@AutoConfigureMockMvc
class ActuatorEndpointsTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void healthIsPublicAndHidesDetailsFromGuests() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void adminSeesDatabaseHealthDetails() throws Exception {
        mvc.perform(get("/actuator/health").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.db.details.database").value("H2"));
    }

    @Test
    void userDoesNotSeeHealthDetails() throws Exception {
        mvc.perform(get("/actuator/health").with(user("olena").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void infoIsPublic() throws Exception {
        mvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("guestbook"));
    }

    @Test
    void actuatorRootIsForAdminOnly() throws Exception {
        mvc.perform(get("/actuator"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/actuator").with(user("olena").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void pagesWorkWhenH2ConsoleIsDisabled() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk());
        mvc.perform(get("/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/books").with(user("olena").roles("USER")))
                .andExpect(status().isOk());
    }
}
