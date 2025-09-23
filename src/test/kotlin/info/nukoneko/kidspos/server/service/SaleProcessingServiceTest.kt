package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

@SpringBootTest
@Disabled("Spring context not configured")
class SaleProcessingServiceTest {
    @MockBean
    private lateinit var saleCalculationService: SaleCalculationService

    @MockBean
    private lateinit var saleValidationService: SaleValidationService

    @MockBean
    private lateinit var salePersistenceService: SalePersistenceService

    private lateinit var saleProcessingService: SaleProcessingService

    @BeforeEach
    fun setup() {
        saleProcessingService = SaleProcessingService(
            saleCalculationService,
            saleValidationService,
            salePersistenceService
        )
    }

    @Test
    fun `should process sale with multiple items correctly`() {
        // Given
        val saleBean = SaleBean(storeId = 1, staffBarcode = "1001", itemIds = "1,2,3", deposit = 1000)
        val items = listOf(
            ItemBean(1, "001", "Item 1", 300),
            ItemBean(2, "002", "Item 2", 400),
            ItemBean(3, "003", "Item 3", 200)
        )

        val expectedSale = SaleEntity(
            id = 1,
            storeId = 1,
            staffId = 1,
            quantity = 3,
            amount = 900,
            deposit = 1000,
            createdAt = Date()
        )

        val expectedDetails = listOf(
            SaleDetailEntity(1, 1, 1, 300, 1),
            SaleDetailEntity(2, 1, 2, 400, 1),
            SaleDetailEntity(3, 1, 3, 200, 1)
        )

        `when`(saleValidationService.validateSaleRequest(saleBean, items)).thenReturn(Unit)
        `when`(saleCalculationService.calculateSaleAmount(items)).thenReturn(900)
        `when`(saleCalculationService.calculateChange(900, 1000)).thenReturn(100)
        `when`(salePersistenceService.saveSale(any(), any())).thenReturn(expectedSale)
        `when`(salePersistenceService.saveSaleDetails(any(), any())).thenReturn(expectedDetails)

        // When
        val result = saleProcessingService.processSale(saleBean, items)

        // Then
        assertNotNull(result)
        assertEquals(1, result.id)
        assertEquals(900, result.amount)
        assertEquals(1000, result.deposit)

        verify(saleValidationService).validateSaleRequest(saleBean, items)
        verify(saleCalculationService).calculateSaleAmount(items)
        verify(salePersistenceService).saveSale(any(), any())
        verify(salePersistenceService).saveSaleDetails(any(), any())
    }

    @Test
    fun `should handle duplicate items correctly`() {
        // Given
        val saleBean = SaleBean(storeId = 1, staffBarcode = "1001", itemIds = "1,1,2", deposit = 800)
        val items = listOf(
            ItemBean(1, "001", "Item 1", 300),
            ItemBean(1, "001", "Item 1", 300),
            ItemBean(2, "002", "Item 2", 200)
        )

        `when`(saleValidationService.validateSaleRequest(saleBean, items)).thenReturn(Unit)
        `when`(saleCalculationService.calculateSaleAmount(items)).thenReturn(800)
        `when`(saleCalculationService.groupItemsByType(items)).thenReturn(
            mapOf(
                1 to listOf(items[0], items[1]),
                2 to listOf(items[2])
            )
        )

        // When
        saleProcessingService.processSale(saleBean, items)

        // Then
        verify(saleCalculationService).groupItemsByType(items)
    }

    @Test
    fun `should extract staff ID from barcode correctly`() {
        // Given
        val longBarcode = "123456789001"
        val shortBarcode = "001"

        // When
        val staffId1 = saleProcessingService.extractStaffId(longBarcode)
        val staffId2 = saleProcessingService.extractStaffId(shortBarcode)

        // Then
        assertEquals(1, staffId1)
        assertEquals(0, staffId2)
    }
}

@Disabled("Spring context not configured")
class SaleCalculationServiceTest {
    private lateinit var saleCalculationService: SaleCalculationService

    @BeforeEach
    fun setup() {
        saleCalculationService = SaleCalculationService()
    }

    @Test
    fun `should calculate total amount correctly`() {
        // Given
        val items = listOf(
            ItemBean(1, "001", "Item 1", 100),
            ItemBean(2, "002", "Item 2", 200),
            ItemBean(3, "003", "Item 3", 300)
        )

        // When
        val total = saleCalculationService.calculateSaleAmount(items)

        // Then
        assertEquals(600, total)
    }

    @Test
    fun `should calculate change correctly`() {
        // Given
        val amount = 750
        val deposit = 1000

        // When
        val change = saleCalculationService.calculateChange(amount, deposit)

        // Then
        assertEquals(250, change)
    }

    @Test
    fun `should group items by type correctly`() {
        // Given
        val items = listOf(
            ItemBean(1, "001", "Item 1", 100),
            ItemBean(1, "001", "Item 1", 100),
            ItemBean(2, "002", "Item 2", 200),
            ItemBean(3, "003", "Item 3", 300),
            ItemBean(2, "002", "Item 2", 200)
        )

        // When
        val grouped = saleCalculationService.groupItemsByType(items)

        // Then
        assertEquals(3, grouped.size)
        assertEquals(2, grouped[1]?.size)
        assertEquals(2, grouped[2]?.size)
        assertEquals(1, grouped[3]?.size)
    }
}

@Disabled("Spring context not configured")
class SaleValidationServiceTest {
    private lateinit var saleValidationService: SaleValidationService

    @BeforeEach
    fun setup() {
        saleValidationService = SaleValidationService()
    }

    @Test
    fun `should validate valid sale request`() {
        // Given
        val saleBean = SaleBean(storeId = 1, staffBarcode = "1001", itemIds = "1,2", deposit = 500)
        val items = listOf(
            ItemBean(1, "001", "Item 1", 200),
            ItemBean(2, "002", "Item 2", 250)
        )

        // When & Then - Should not throw exception
        assertDoesNotThrow {
            saleValidationService.validateSaleRequest(saleBean, items)
        }
    }

    @Test
    fun `should throw exception for insufficient deposit`() {
        // Given
        val saleBean = SaleBean(storeId = 1, staffBarcode = "1001", itemIds = "1,2", deposit = 300)
        val items = listOf(
            ItemBean(1, "001", "Item 1", 200),
            ItemBean(2, "002", "Item 2", 250)
        )

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            saleValidationService.validateSaleRequest(saleBean, items)
        }
    }

    @Test
    fun `should throw exception for empty items`() {
        // Given
        val saleBean = SaleBean(storeId = 1, staffBarcode = "1001", itemIds = "", deposit = 1000)
        val items = emptyList<ItemBean>()

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            saleValidationService.validateSaleRequest(saleBean, items)
        }
    }
}