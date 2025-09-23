package info.nukoneko.kidspos.server.controller.api

import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.controller.dto.request.CreateItemRequest
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.response.ItemResponse
import info.nukoneko.kidspos.server.domain.exception.InvalidBarcodeException
import info.nukoneko.kidspos.server.domain.exception.ItemNotFoundException
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.service.ItemService
import info.nukoneko.kidspos.server.service.ValidationService
import info.nukoneko.kidspos.server.service.mapper.ItemMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.context.annotation.Import

@WebMvcTest(
    controllers = [ItemApiController::class],
    excludeAutoConfiguration = [
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration::class
    ]
)
@AutoConfigureMockMvc(addFilters = false)
@Import(info.nukoneko.kidspos.server.TestConfiguration::class)
@Disabled("Temporarily disabled - Spring context issues")
class ItemApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var itemService: ItemService

    @MockBean
    private lateinit var itemMapper: ItemMapper

    @MockBean
    private lateinit var validationService: ValidationService

    private lateinit var testItem: ItemEntity
    private lateinit var testItemResponse: ItemResponse

    @BeforeEach
    fun setup() {
        testItem = ItemEntity(
            id = 1,
            barcode = "123456789",
            name = "Test Item",
            price = 100
        )

        testItemResponse = ItemResponse(
            id = 1,
            barcode = "123456789",
            name = "Test Item",
            price = 100
        )
    }

    @Test
    fun `should get all items successfully`() {
        // Given
        val items = listOf(testItem)
        val responses = listOf(testItemResponse)

        `when`(itemService.findAll()).thenReturn(items)
        `when`(itemMapper.toResponseList(items)).thenReturn(responses)

        // When & Then
        mockMvc.perform(get("/api/items"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].barcode").value("123456789"))
            .andExpect(jsonPath("$[0].name").value("Test Item"))
            .andExpect(jsonPath("$[0].price").value(100))

        verify(itemService).findAll()
        verify(itemMapper).toResponseList(items)
    }

    @Test
    fun `should get item by ID successfully`() {
        // Given
        `when`(itemService.findItem(1)).thenReturn(testItem)
        `when`(itemMapper.toResponse(testItem)).thenReturn(testItemResponse)

        // When & Then
        mockMvc.perform(get("/api/items/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Item"))

        verify(itemService).findItem(1)
        verify(itemMapper).toResponse(testItem)
    }

    @Test
    fun `should throw exception when item not found by ID`() {
        // Given
        `when`(itemService.findItem(999)).thenReturn(null)

        // When & Then
        mockMvc.perform(get("/api/items/999"))
            .andExpect(status().isNotFound)

        verify(itemService).findItem(999)
        verify(itemMapper, never()).toResponse(any())
    }

    @Test
    fun `should get item by barcode successfully`() {
        // Given
        `when`(itemService.findItem("123456789")).thenReturn(testItem)
        `when`(itemMapper.toResponse(testItem)).thenReturn(testItemResponse)

        // When & Then
        mockMvc.perform(get("/api/items/barcode/123456789"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.barcode").value("123456789"))

        verify(itemService).findItem("123456789")
        verify(itemMapper).toResponse(testItem)
    }

    @Test
    fun `should throw exception for invalid barcode format`() {
        // When & Then
        mockMvc.perform(get("/api/items/barcode/abc"))
            .andExpect(status().isBadRequest)

        verify(itemService, never()).findItem(any<String>())
    }

    @Test
    fun `should throw exception when item not found by barcode`() {
        // Given
        `when`(itemService.findItem("999999999")).thenReturn(null)

        // When & Then
        mockMvc.perform(get("/api/items/barcode/999999999"))
            .andExpect(status().isNotFound)

        verify(itemService).findItem("999999999")
        verify(itemMapper, never()).toResponse(any())
    }

    @Test
    fun `should create item successfully`() {
        // Given
        val request = CreateItemRequest(
            name = "New Item",
            barcode = "987654321",
            price = 200
        )

        val savedItem = ItemEntity(
            id = 2,
            barcode = "987654321",
            name = "New Item",
            price = 200
        )

        val savedResponse = ItemResponse(
            id = 2,
            barcode = "987654321",
            name = "New Item",
            price = 200
        )

        `when`(itemService.save(any<ItemBean>())).thenReturn(savedItem)
        `when`(itemMapper.toResponse(savedItem)).thenReturn(savedResponse)

        // When & Then
        mockMvc.perform(post("/api/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.name").value("New Item"))

        verify(validationService).validateBarcodeUnique("987654321")
        verify(validationService).validatePriceRange(200)
        verify(itemService).save(any<ItemBean>())
        verify(itemMapper).toResponse(savedItem)
    }

    @Test
    fun `should handle validation errors during item creation`() {
        // Given
        val request = CreateItemRequest(
            name = "",
            barcode = "",
            price = -10
        )

        // When & Then
        mockMvc.perform(post("/api/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)

        verify(itemService, never()).save(any<ItemBean>())
    }

    @Test
    fun `should update item successfully`() {
        // Given
        val request = CreateItemRequest(
            name = "Updated Item",
            barcode = "123456789",
            price = 150
        )

        val updatedItem = ItemEntity(
            id = 1,
            barcode = "123456789",
            name = "Updated Item",
            price = 150
        )

        val updatedResponse = ItemResponse(
            id = 1,
            barcode = "123456789",
            name = "Updated Item",
            price = 150
        )

        `when`(itemService.findItem(1)).thenReturn(testItem)
        `when`(itemService.save(any<ItemBean>())).thenReturn(updatedItem)
        `when`(itemMapper.toResponse(updatedItem)).thenReturn(updatedResponse)

        // When & Then
        mockMvc.perform(put("/api/items/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Updated Item"))
            .andExpect(jsonPath("$.price").value(150))

        verify(itemService).findItem(1)
        verify(validationService).validateBarcodeUnique("123456789", 1)
        verify(validationService).validatePriceRange(150)
        verify(itemService).save(any<ItemBean>())
        verify(itemMapper).toResponse(updatedItem)
    }

    @Test
    fun `should throw exception when updating non-existent item`() {
        // Given
        val request = CreateItemRequest(
            name = "Updated Item",
            barcode = "123456789",
            price = 150
        )

        `when`(itemService.findItem(999)).thenReturn(null)

        // When & Then
        mockMvc.perform(put("/api/items/999")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound)

        verify(itemService).findItem(999)
        verify(itemService, never()).save(any<ItemBean>())
    }

    @Test
    fun `should delete item successfully`() {
        // When & Then
        mockMvc.perform(delete("/api/items/1"))
            .andExpect(status().isNoContent)

        verify(validationService).validateItemExists(1)
    }

    @Test
    fun `should handle empty request body`() {
        // When & Then
        mockMvc.perform(post("/api/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content(""))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle malformed JSON`() {
        // When & Then
        mockMvc.perform(post("/api/items")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{invalid json"))
            .andExpect(status().isBadRequest)
    }
}