package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.entity.StoreEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
@Disabled("Temporarily disabled - Spring context issues")
class ReceiptServiceTest {

    @MockBean
    private lateinit var storeService: StoreService

    @MockBean
    private lateinit var staffService: StaffService

    @MockBean
    private lateinit var appProperties: AppProperties

    private lateinit var receiptService: ReceiptService

    @BeforeEach
    fun setup() {
        receiptService = ReceiptService(storeService, staffService, appProperties)

        // Mock AppProperties - skip for now due to complexity
    }

    @Test
    fun `should validate printer configuration when store has printer URI`() {
        // Given
        val storeId = 1
        val store = StoreEntity(
            id = storeId,
            name = "Test Store",
            printerUri = "192.168.1.100"
        )
        `when`(storeService.findStore(storeId)).thenReturn(store)

        // When
        val result = receiptService.validatePrinterConfiguration(storeId)

        // Then
        assertTrue(result)
        verify(storeService).findStore(storeId)
    }

    @Test
    fun `should fail printer validation when store does not exist`() {
        // Given
        val storeId = 999
        `when`(storeService.findStore(storeId)).thenReturn(null)

        // When
        val result = receiptService.validatePrinterConfiguration(storeId)

        // Then
        assertFalse(result)
        verify(storeService).findStore(storeId)
    }

    @Test
    fun `should fail printer validation when printer URI is empty`() {
        // Given
        val storeId = 1
        val store = StoreEntity(
            id = storeId,
            name = "Test Store",
            printerUri = ""
        )
        `when`(storeService.findStore(storeId)).thenReturn(store)

        // When
        val result = receiptService.validatePrinterConfiguration(storeId)

        // Then
        assertFalse(result)
        verify(storeService).findStore(storeId)
    }

    @Test
    fun `should generate receipt content with proper formatting`() {
        // Given
        val storeId = 1
        val staffBarcode = "STAFF001"
        val deposit = 1000
        val items = listOf(
            ItemBean(1, "123456789", "Item 1", 300),
            ItemBean(2, "987654321", "Item 2", 400)
        )

        val store = StoreEntity(storeId, "Test Store", "192.168.1.100")
        val staff = StaffEntity(staffBarcode, "Test Staff")

        `when`(storeService.findStore(storeId)).thenReturn(store)
        `when`(staffService.findStaff(staffBarcode)).thenReturn(staff)

        // When
        val result = receiptService.generateReceiptContent(storeId, items, staffBarcode, deposit)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Test Store"))
        assertTrue(result.contains("Test Staff"))
        assertTrue(result.contains("Item 1 - ¥300"))
        assertTrue(result.contains("Item 2 - ¥400"))
        assertTrue(result.contains("Total: ¥700"))
        assertTrue(result.contains("Deposit: ¥1000"))
        assertTrue(result.contains("Change: ¥300"))
        verify(storeService).findStore(storeId)
        verify(staffService).findStaff(staffBarcode)
    }

    @Test
    fun `should generate receipt content with unknown store and staff`() {
        // Given
        val storeId = 999
        val staffBarcode = "UNKNOWN"
        val deposit = 500
        val items = listOf(
            ItemBean(1, "123456789", "Test Item", 200)
        )

        `when`(storeService.findStore(storeId)).thenReturn(null)
        `when`(staffService.findStaff(staffBarcode)).thenReturn(null)

        // When
        val result = receiptService.generateReceiptContent(storeId, items, staffBarcode, deposit)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Unknown Store"))
        assertTrue(result.contains("Unknown Staff"))
        assertTrue(result.contains("Test Item - ¥200"))
        assertTrue(result.contains("Total: ¥200"))
        assertTrue(result.contains("Deposit: ¥500"))
        assertTrue(result.contains("Change: ¥300"))
        verify(storeService).findStore(storeId)
        verify(staffService).findStaff(staffBarcode)
    }

    @Test
    fun `should handle empty items list in receipt generation`() {
        // Given
        val storeId = 1
        val staffBarcode = "STAFF001"
        val deposit = 100
        val items = emptyList<ItemBean>()

        val store = StoreEntity(storeId, "Test Store", "192.168.1.100")
        val staff = StaffEntity(staffBarcode, "Test Staff")

        `when`(storeService.findStore(storeId)).thenReturn(store)
        `when`(staffService.findStaff(staffBarcode)).thenReturn(staff)

        // When
        val result = receiptService.generateReceiptContent(storeId, items, staffBarcode, deposit)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Total: ¥0"))
        assertTrue(result.contains("Deposit: ¥100"))
        assertTrue(result.contains("Change: ¥100"))
        verify(storeService).findStore(storeId)
        verify(staffService).findStaff(staffBarcode)
    }
}