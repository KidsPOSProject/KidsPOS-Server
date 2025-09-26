package info.nukoneko.kidspos.server.controller.advice

import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.controller.api.ItemApiController
import info.nukoneko.kidspos.server.controller.dto.request.CreateItemRequest
import info.nukoneko.kidspos.server.domain.exception.InvalidBarcodeException
import info.nukoneko.kidspos.server.domain.exception.ItemNotFoundException
import info.nukoneko.kidspos.server.service.ItemService
import info.nukoneko.kidspos.server.service.ValidationService
import info.nukoneko.kidspos.server.service.mapper.ItemMapper
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
    controllers = [ItemApiController::class],
    includeFilters = [
        org.springframework.context.annotation.ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
            classes = [GlobalExceptionHandler::class],
        ),
    ],
    excludeAutoConfiguration = [
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration::class,
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@Import(info.nukoneko.kidspos.server.TestConfiguration::class)
@Disabled("Spring context not configured")
class GlobalExceptionHandlerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var itemService: ItemService

    @MockBean
    private lateinit var itemMapper: ItemMapper

    @MockBean
    private lateinit var validationService: ValidationService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should handle ItemNotFoundException with proper error response`() {
        // Given
        `when`(itemService.findItem(999)).thenThrow(ItemNotFoundException(id = 999))

        // When & Then
        mockMvc
            .perform(get("/api/items/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Item with ID 999 not found"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `should handle validation errors with detailed messages`() {
        // Given
        val invalidRequest =
            CreateItemRequest(
                name = "", // Invalid: empty name
                barcode = "abc", // Invalid: not numeric
                price = -100, // Invalid: negative price
            )

        // When & Then
        mockMvc
            .perform(
                post("/api/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `should handle generic exceptions without exposing sensitive info`() {
        // Given
        `when`(itemService.findItem(1))
            .thenThrow(RuntimeException("Database connection failed at 192.168.1.100"))

        // When & Then
        mockMvc
            .perform(get("/api/items/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
    }

    @Test
    fun `should handle business exceptions with appropriate status codes`() {
        // Given
        `when`(itemService.findItem("invalid"))
            .thenThrow(InvalidBarcodeException("invalid"))

        // When & Then
        mockMvc
            .perform(get("/api/items/barcode/invalid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_BARCODE"))
            .andExpect(jsonPath("$.message").value("Invalid barcode format: invalid"))
    }
}
