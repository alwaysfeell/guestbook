package sumdu.edu.ua.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.db.url=jdbc:h2:mem:books-controller;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class BooksControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listReturnsJsonPage() throws Exception {
        mvc.perform(get("/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.items[0].title").value("Кобзар"));
    }

    @Test
    void searchSortAndPagingAreBoundFromRequestParams() throws Exception {
        mvc.perform(get("/books").param("q", "martin").param("sort", "pubYear,desc").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].title").value("Clean Architecture"))
                .andExpect(jsonPath("$.items[1].title").value("Clean Code"));
    }

    @Test
    void bookByIdContainsComments() throws Exception {
        mvc.perform(get("/books/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.book.title").value("Clean Architecture"))
                .andExpect(jsonPath("$.comments.items", hasSize(2)))
                .andExpect(jsonPath("$.comments.items[0].author").value("Андрій"))
                .andExpect(jsonPath("$.comments.items[0].createdAt", containsString("T")));
    }

    @Test
    void addBookReturns201WithLocation() throws Exception {
        mvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Intermezzo\",\"author\":\"Mykhailo Kotsiubynskyi\",\"pubYear\":1908}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/books/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Intermezzo"));
    }

    @Test
    void errorsHaveStatusAndApiErrorBody() throws Exception {
        mvc.perform(get("/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Book with id=999 not found"))
                .andExpect(jsonPath("$.path").value("/books/999"));

        mvc.perform(get("/books/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be an integer, got 'abc'"));

        mvc.perform(get("/books/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("book id must be a positive integer"));

        mvc.perform(get("/books").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
    }

    @Test
    void invalidRequestsToPostBooks() throws Exception {
        mvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"author\":\"A\",\"pubYear\":2000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title is required (max 255 chars)"));

        mvc.perform(post("/books").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));

        mvc.perform(post("/books").contentType(MediaType.APPLICATION_FORM_URLENCODED).content("title=T"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));

        mvc.perform(put("/books"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));
    }
}
