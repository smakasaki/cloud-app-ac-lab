package md.utm.cloudapp.quotes

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuoteControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var quoteRepository: QuoteRepository

    @BeforeEach
    fun setup() {
        quoteRepository.deleteAll()
    }

    @Test
    fun `GET quotes returns empty list`() {
        mockMvc.perform(get("/quotes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `POST quotes creates and returns quote`() {
        mockMvc.perform(
            post("/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text": "Test quote", "author": "Test Author"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.text").value("Test quote"))
            .andExpect(jsonPath("$.author").value("Test Author"))
            .andExpect(jsonPath("$.id").isNumber)
    }

    @Test
    fun `GET quotes random returns 404 when empty`() {
        mockMvc.perform(get("/quotes/random"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET quotes stats returns correct totals`() {
        quoteRepository.save(Quote(text = "Quote 1", author = "Author A"))
        quoteRepository.save(Quote(text = "Quote 2", author = "Author A"))
        quoteRepository.save(Quote(text = "Quote 3", author = "Author B"))

        mockMvc.perform(get("/quotes/stats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.uniqueAuthors").value(2))
    }

    @Test
    fun `DELETE quote returns 404 for nonexistent id`() {
        mockMvc.perform(delete("/quotes/999"))
            .andExpect(status().isNotFound)
    }
}