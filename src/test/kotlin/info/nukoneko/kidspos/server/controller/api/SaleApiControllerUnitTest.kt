package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.request.CreateSaleRequest
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.service.*
import info.nukoneko.kidspos.server.service.mapper.SaleMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import java.util.*

@ExtendWith(MockitoExtension::class)
class SaleApiControllerUnitTest {
    @Mock
    private lateinit var saleProcessingService: SaleProcessingService

    @Mock
    private lateinit var itemParsingService: ItemParsingService

    @Mock
    private lateinit var receiptService: ReceiptService

    @Mock
    private lateinit var saleMapper: SaleMapper

    @InjectMocks
    private lateinit var controller: SaleApiController

    private lateinit var testSale: SaleEntity
    private lateinit var testItems: List<ItemBean>

    @BeforeEach
    fun setup() {
        testSale =
            SaleEntity(
                id = 1,
                storeId = 1,
                staffId = 1,
                quantity = 2,
                amount = 300,
                deposit = 400,
                createdAt = Date(),
            )

        testItems =
            listOf(
                ItemBean(1, "123456789", "Test Item 1", 100),
                ItemBean(2, "987654321", "Test Item 2", 200),
            )
    }

    @Test
    fun `should create sale successfully`() {
        // Given
        val request =
            CreateSaleRequest(
                storeId = 1,
                staffBarcode = "STAFF001",
                itemIds = "1,2",
                deposit = 400,
            )

        val summary =
            SaleSummary(
                totalAmount = 300,
                deposit = 400,
                change = 100,
                itemCount = 2,
                uniqueItems = 2,
                itemQuantities = mapOf(1 to 1, 2 to 1),
            )

        val expectedSaleBean =
            SaleBean(
                storeId = request.storeId,
                staffBarcode = request.staffBarcode,
                itemIds = request.itemIds,
                deposit = request.deposit,
            )

        `when`(itemParsingService.parseItemsFromIds("1,2")).thenReturn(testItems)
        `when`(saleProcessingService.processSaleWithValidation(expectedSaleBean, testItems))
            .thenReturn(SaleResult.Success(testSale, summary))
        `when`(receiptService.printReceipt(1, testItems, "STAFF001", 400)).thenReturn(true)

        // When
        val result = controller.createSale(request)

        // Then
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertNotNull(result.body)

        val body = result.body as Map<String, Any>
        assertEquals(1, body["id"])
        assertEquals(300, body["amount"])
        assertEquals(2, body["quantity"])
        assertEquals(400, body["deposit"])
        assertEquals(100, body["change"])

        verify(itemParsingService).parseItemsFromIds("1,2")
        verify(saleProcessingService).processSaleWithValidation(expectedSaleBean, testItems)
        verify(receiptService).printReceipt(1, testItems, "STAFF001", 400)
    }

    @Test
    fun `should return bad request for validation error`() {
        // Given
        val request =
            CreateSaleRequest(
                storeId = 1,
                staffBarcode = "STAFF001",
                itemIds = "1,2",
                deposit = 100,
            )

        val expectedSaleBean =
            SaleBean(
                storeId = request.storeId,
                staffBarcode = request.staffBarcode,
                itemIds = request.itemIds,
                deposit = request.deposit,
            )

        `when`(itemParsingService.parseItemsFromIds("1,2")).thenReturn(testItems)
        `when`(saleProcessingService.processSaleWithValidation(expectedSaleBean, testItems))
            .thenReturn(SaleResult.ValidationError("Insufficient deposit"))

        // When
        val result = controller.createSale(request)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertNotNull(result.body)

        val body = result.body as Map<String, Any>
        assertEquals("Insufficient deposit", body["error"])

        verify(itemParsingService).parseItemsFromIds("1,2")
        verify(saleProcessingService).processSaleWithValidation(expectedSaleBean, testItems)
        verifyNoInteractions(receiptService)
    }

    @Test
    fun `should return internal server error for processing error`() {
        // Given
        val request =
            CreateSaleRequest(
                storeId = 1,
                staffBarcode = "STAFF001",
                itemIds = "1,2",
                deposit = 400,
            )

        val expectedSaleBean =
            SaleBean(
                storeId = request.storeId,
                staffBarcode = request.staffBarcode,
                itemIds = request.itemIds,
                deposit = request.deposit,
            )

        `when`(itemParsingService.parseItemsFromIds("1,2")).thenReturn(testItems)
        `when`(saleProcessingService.processSaleWithValidation(expectedSaleBean, testItems))
            .thenReturn(SaleResult.ProcessingError("Database error"))

        // When
        val result = controller.createSale(request)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.statusCode)
        assertNotNull(result.body)

        val body = result.body as Map<String, Any>
        assertEquals("Database error", body["error"])

        verify(itemParsingService).parseItemsFromIds("1,2")
        verify(saleProcessingService).processSaleWithValidation(expectedSaleBean, testItems)
        verifyNoInteractions(receiptService)
    }

    @Test
    fun `should handle exception during sale creation`() {
        // Given
        val request =
            CreateSaleRequest(
                storeId = 1,
                staffBarcode = "STAFF001",
                itemIds = "1,2",
                deposit = 400,
            )

        `when`(itemParsingService.parseItemsFromIds("1,2"))
            .thenThrow(RuntimeException("Parsing error"))

        // When
        val result = controller.createSale(request)

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.statusCode)
        assertNotNull(result.body)

        val body = result.body as Map<String, Any>
        assertEquals("Internal server error", body["error"])

        verify(itemParsingService).parseItemsFromIds("1,2")
        verifyNoInteractions(saleProcessingService)
        verifyNoInteractions(receiptService)
    }

    @Test
    fun `should return error for generic sale error`() {
        // Given
        val request =
            CreateSaleRequest(
                storeId = 1,
                staffBarcode = "STAFF001",
                itemIds = "1,2",
                deposit = 400,
            )

        val expectedSaleBean =
            SaleBean(
                storeId = request.storeId,
                staffBarcode = request.staffBarcode,
                itemIds = request.itemIds,
                deposit = request.deposit,
            )

        `when`(itemParsingService.parseItemsFromIds("1,2")).thenReturn(testItems)
        `when`(saleProcessingService.processSaleWithValidation(expectedSaleBean, testItems))
            .thenReturn(SaleResult.Error("General error"))

        // When
        val result = controller.createSale(request)

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertNotNull(result.body)

        val body = result.body as Map<String, Any>
        assertEquals("General error", body["error"])

        verify(itemParsingService).parseItemsFromIds("1,2")
        verify(saleProcessingService).processSaleWithValidation(expectedSaleBean, testItems)
        verifyNoInteractions(receiptService)
    }
}
