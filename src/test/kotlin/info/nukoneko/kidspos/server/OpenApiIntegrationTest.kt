package info.nukoneko.kidspos.server

import info.nukoneko.kidspos.server.config.OpenApiTestConfiguration
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Tests for OpenAPI/Swagger integration
 * Part of Task 8.2: OpenAPI specification integration
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(OpenApiTestConfiguration::class)
@Disabled("Spring context not configured")
class OpenApiIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `Swagger UI should be accessible`() {
        mockMvc
            .perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk)
    }

    @Test
    fun `OpenAPI JSON specification should be available`() {
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
    }

    @Test
    fun `OpenAPI specification should include all API endpoints`() {
        val result =
            mockMvc
                .perform(get("/v3/api-docs"))
                .andExpect(status().isOk)
                .andReturn()
        val content = result.response.contentAsString

        // Verify key endpoints are documented
        assertTrue(content.contains("/api/item"))
        assertTrue(content.contains("/api/sale"))
        assertTrue(content.contains("/api/store"))
        assertTrue(content.contains("/api/staff"))
    }

    @Test
    fun `OpenAPI specification should include request and response schemas`() {
        val result =
            mockMvc
                .perform(get("/v3/api-docs"))
                .andExpect(status().isOk)
                .andReturn()
        val content = result.response.contentAsString

        // Verify schemas are present
        assertTrue(content.contains("\"components\""))
        assertTrue(content.contains("\"schemas\""))
    }
}
