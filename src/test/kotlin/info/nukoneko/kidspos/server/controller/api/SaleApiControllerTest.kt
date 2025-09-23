package info.nukoneko.kidspos.server.controller.api

import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.controller.dto.request.CreateSaleRequest
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.controller.dto.response.SaleResponse
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.service.*
import info.nukoneko.kidspos.server.service.mapper.SaleMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@WebMvcTest(
    controllers = [SaleApiController::class],
    excludeAutoConfiguration = [
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration::class
    ]
)
@AutoConfigureMockMvc(addFilters = false)
@Import(info.nukoneko.kidspos.server.TestConfiguration::class)
@Disabled("Spring context not configured")
class SaleApiControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var saleProcessingService: SaleProcessingService

    @MockBean
    private lateinit var itemParsingService: ItemParsingService

    @MockBean
    private lateinit var receiptService: ReceiptService

    @MockBean
    private lateinit var saleMapper: SaleMapper

    private lateinit var testSale: SaleEntity
    private lateinit var testItems: List<ItemBean>

    @BeforeEach
    fun setup() {
        testSale = SaleEntity(
            id = 1,
            storeId = 1,
            staffId = 1,
            quantity = 2,
            amount = 300,
            deposit = 400,
            createdAt = Date()
        )

        testItems = listOf(
            ItemBean(1, "123456789", "Test Item 1", 100),
            ItemBean(2, "987654321", "Test Item 2", 200)
        )
    }

    @Test
    fun `should create sale successfully`() {
        // Given
        val request = CreateSaleRequest(
            storeId = 1,
            staffBarcode = "STAFF001",
            itemIds = "1,2",
            deposit = 400
        )

        val summary = SaleSummary(
            totalAmount = 300,
            deposit = 400,
            change = 100,
            itemCount = 2,
            uniqueItems = 2,
            itemQuantities = mapOf(1 to 1, 2 to 1)
        )

        `when`(itemParsingService.parseItemsFromIds("1,2")).thenReturn(testItems)
        `when`(saleProcessingService.processSaleWithValidation(any(), any()))
            .thenReturn(SaleResult.Success(testSale, summary))
        `when`(receiptService.printReceipt(any(), any(), any(), any())).thenReturn(true)

        // When & Then
        mockMvc.perform(post("/api/sales")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))

        verify(itemParsingService).parseItemsFromIds("1,2")
        verify(saleProcessingService).processSaleWithValidation(any(), any())
    }

    @Test
    fun `should return bad request for validation error`() {
        // Given
        val request = CreateSaleRequest(
            storeId = 1,
            staffBarcode = "STAFF001",
            itemIds = "1,2",
            deposit = 100
        )

        `when`(itemParsingService.parseItemsFromIds("1,2")).thenReturn(testItems)
        `when`(saleProcessingService.processSaleWithValidation(any(), any()))
            .thenReturn(SaleResult.ValidationError("Insufficient deposit"))

        // When & Then
        mockMvc.perform(post("/api/sales")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest)

        verify(itemParsingService).parseItemsFromIds("1,2")
        verify(saleProcessingService).processSaleWithValidation(any(), any())
        verify(receiptService, never()).printReceipt(any(), any(), any(), any())
    }

    @Test
    fun `should return internal server error for processing error`() {
        // Given
        val request = CreateSaleRequest(
            storeId = 1,
            staffBarcode = "STAFF001",
            itemIds = "1,2",
            deposit = 400
        )

        `when`(itemParsingService.parseItemsFromIds("1,2")).thenReturn(testItems)
        `when`(saleProcessingService.processSaleWithValidation(any(), any()))
            .thenReturn(SaleResult.ProcessingError("Database error"))

        // When & Then
        mockMvc.perform(post("/api/sales")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError)

        verify(itemParsingService).parseItemsFromIds("1,2")
        verify(saleProcessingService).processSaleWithValidation(any(), any())
    }

    @Test
    fun `should handle exception during sale creation`() {
        // Given
        val request = CreateSaleRequest(
            storeId = 1,
            staffBarcode = "STAFF001",
            itemIds = "1,2",
            deposit = 400
        )

        `when`(itemParsingService.parseItemsFromIds("1,2"))
            .thenThrow(RuntimeException("Parsing error"))

        // When & Then
        mockMvc.perform(post("/api/sales")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError)

        verify(itemParsingService).parseItemsFromIds("1,2")
        verify(saleProcessingService, never()).processSaleWithValidation(any(), any())
    }

    @Test
    fun `should get all sales successfully`() {
        // Given
        val sales = listOf(testSale)
        val saleResponse = SaleResponse(
            id = 1,
            storeId = 1,
            storeName = "Store 1",
            staffId = "STAFF001",
            staffName = "Test Staff",
            totalAmount = 300,
            deposit = 400,
            change = 100,
            saleTime = java.time.LocalDateTime.now(),
            items = emptyList()
        )

        `when`(saleProcessingService.findAllSales()).thenReturn(sales)
        `when`(saleMapper.toResponseList(sales)).thenReturn(listOf(saleResponse))

        // When & Then
        mockMvc.perform(get("/api/sales"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].id").value(1))

        verify(saleProcessingService).findAllSales()
        verify(saleMapper).toResponseList(sales)
    }

    @Test
    fun `should get sale by ID successfully`() {
        // Given
        val saleResponse = SaleResponse(
            id = 1,
            storeId = 1,
            storeName = "Store 1",
            staffId = "STAFF001",
            staffName = "Test Staff",
            totalAmount = 300,
            deposit = 400,
            change = 100,
            saleTime = java.time.LocalDateTime.now(),
            items = emptyList()
        )

        `when`(saleProcessingService.findSaleById(1)).thenReturn(testSale)
        `when`(saleMapper.toResponse(testSale)).thenReturn(saleResponse)

        // When & Then
        mockMvc.perform(get("/api/sales/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))

        verify(saleProcessingService).findSaleById(1)
        verify(saleMapper).toResponse(testSale)
    }

    @Test
    fun `should return not found when sale does not exist`() {
        // Given
        `when`(saleProcessingService.findSaleById(999)).thenReturn(null)

        // When & Then
        mockMvc.perform(get("/api/sales/999"))
            .andExpect(status().isNotFound)

        verify(saleProcessingService).findSaleById(999)
        verify(saleMapper, never()).toResponse(any())
    }

    @Test
    fun `should validate printer configuration successfully`() {
        // When & Then
        mockMvc.perform(get("/api/sales/printer/validate"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return false for invalid printer configuration`() {
        // Given
        `when`(receiptService.validatePrinterConfiguration(1))
            .thenReturn(false)

        // When & Then
        mockMvc.perform(get("/api/sales/printer/validate"))
            .andExpect(status().isOk)

        verify(receiptService).validatePrinterConfiguration(1)
    }

    @Test
    fun `should handle validation errors for missing parameters`() {
        // Given
        val invalidRequest = """
            {
                "storeId": null,
                "staffBarcode": "",
                "itemIds": "",
                "deposit": -100
            }
        """.trimIndent()

        // When & Then
        mockMvc.perform(post("/api/sales")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequest))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle validation errors for invalid parameters`() {
        // Given
        val invalidRequest = CreateSaleRequest(
            storeId = -1,
            staffBarcode = "",
            itemIds = "",
            deposit = -100
        )

        // When & Then
        mockMvc.perform(post("/api/sales")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest)

        verify(itemParsingService, never()).parseItemsFromIds(any<String>())
    }
}