package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:api-security;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ApiSecurityTest {
    private static final String BOOK_JSON = "{\"title\":\"Intermezzo\",\"author\":\"Михайло Коцюбинський\",\"pubYear\":1908}";

    @Autowired
    private MockMvc mvc;

    @Test
    void guestGetsUnauthorizedJson() throws Exception {
        MvcResult result = mvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Basic")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/books"))
                .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();

        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/books/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordGetsUnauthorized() throws Exception {
        mvc.perform(get("/api/books").with(httpBasic("admin", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void userCanReadButNotModify() throws Exception {
        mvc.perform(get("/api/books").param("size", "5").with(httpBasic("olena", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.items[0].title").value("Кобзар"));
        mvc.perform(get("/api/books/12").with(httpBasic("olena", "user123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Effective Java"));

        mvc.perform(post("/api/books").with(httpBasic("olena", "user123"))
                        .contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: ADMIN role required"));
        mvc.perform(delete("/api/books/1").with(httpBasic("olena", "user123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminHasFullAccess() throws Exception {
        MvcResult created = mvc.perform(post("/api/books").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(BOOK_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/books/13")))
                .andExpect(jsonPath("$.id").value(13))
                .andExpect(jsonPath("$.title").value("Intermezzo"))
                .andReturn();
        assertThat(created.getRequest().getSession(false)).isNull();

        mvc.perform(delete("/api/books/13").with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/books/13").with(httpBasic("admin", "admin123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book with id=13 not found"));
    }

    @Test
    void invalidRequestsGetJsonErrors() throws Exception {
        mvc.perform(post("/api/books").with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"author\":\"A\",\"pubYear\":2000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title is required (max 255 chars)"));
        mvc.perform(get("/api/books/abc").with(httpBasic("admin", "admin123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
