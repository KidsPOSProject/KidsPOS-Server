package info.nukoneko.kidspos.server.controller.advice

import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.controller.api.ItemApiController
import info.nukoneko.kidspos.server.controller.dto.request.CreateItemRequest
import info.nukoneko.kidspos.server.domain.exception.ItemNotFoundException
import info.nukoneko.kidspos.server.service.BarcodePdfService
import info.nukoneko.kidspos.server.service.ItemService
import info.nukoneko.kidspos.server.service.ValidationService
import info.nukoneko.kidspos.server.service.mapper.ItemMapper
import org.junit.jupiter.api.Assertions.assertFalse
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
class GlobalExceptionHandlerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var itemService: ItemService

    @MockBean
    private lateinit var itemMapper: ItemMapper

    @MockBean
    private lateinit var validationService: ValidationService

    @MockBean
    private lateinit var barcodePdfService: BarcodePdfService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should handle ItemNotFoundException with proper error response`() {
        // Given
        `when`(itemService.findItem(999)).thenThrow(ItemNotFoundException(id = 999))

        // When & Then
        mockMvc
            .perform(get("/api/item/999"))
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
                post("/api/item")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.details.fieldErrors").isArray)
    }

    @Test
    fun `should handle generic exceptions without exposing sensitive info`() {
        // Given
        `when`(itemService.findItem(1))
            .thenThrow(RuntimeException("Database connection failed at 192.168.1.100"))

        // When & Then
        val result =
            mockMvc
                .perform(get("/api/item/1"))
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(
                    jsonPath("$.message").value("データベース接続エラーが発生しました。しばらく待ってから再試行してください"),
                ).andReturn()

        val body = result.response.contentAsString
        assertFalse(body.contains("192.168.1.100"), "内部の接続先情報を露出してはいけない")
    }

    @Test
    fun `should handle business exceptions with appropriate status codes`() {
        // When & Then
        mockMvc
            .perform(get("/api/item/barcode/invalid"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_BARCODE"))
            .andExpect(jsonPath("$.message").value("Invalid barcode format: invalid"))
    }
}
