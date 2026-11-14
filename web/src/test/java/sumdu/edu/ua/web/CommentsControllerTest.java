package sumdu.edu.ua.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.db.url=jdbc:h2:mem:comments-controller;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class CommentsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void fullCrudCycle() throws Exception {
        String created = mvc.perform(post("/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":3,\"author\":\"Student\",\"text\":\"Great play\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(3))
                .andExpect(jsonPath("$.author").value("Student"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(created);
        long id = node.get("id").asLong();

        mvc.perform(get("/comments").param("bookId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id));

        mvc.perform(put("/comments/{id}", id).param("bookId", "3").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great play, read it twice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Great play, read it twice"))
                .andExpect(jsonPath("$.author").value("Student"));

        mvc.perform(delete("/comments/{id}", id).param("bookId", "3"))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/comments/{id}", id).param("bookId", "3"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/comments").param("bookId", "3"))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void businessRulesFromCoreBecomeHttpStatuses() throws Exception {
        mvc.perform(post("/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":3,\"author\":\"   \",\"text\":\"text\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("author is required (max 64 chars)"));

        mvc.perform(post("/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"Student\",\"text\":\"no book\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bookId must be a positive integer"));

        mvc.perform(post("/comments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":999,\"author\":\"Student\",\"text\":\"text\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book with id=999 not found"));

        mvc.perform(delete("/comments/1").param("bookId", "11"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Comment can be deleted only within 24 hours after creation"));
        mvc.perform(put("/comments/1").param("bookId", "11").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"new\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Comment can be edited only within 24 hours after creation"));
    }

    @Test
    void missingRequestParameterGives400() throws Exception {
        mvc.perform(get("/comments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bookId is required"));
        mvc.perform(delete("/comments/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bookId is required"));
    }
}
