package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.entity.StoreEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.net.ServerSocket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest
class ReceiptServiceTest {
    @MockBean
    private lateinit var storeService: StoreService

    @MockBean
    private lateinit var appProperties: AppProperties

    private lateinit var receiptService: ReceiptService

    @BeforeEach
    fun setup() {
        receiptService = ReceiptService(storeService, appProperties)

        // Mock AppProperties - skip for now due to complexity
    }

    @Test
    fun `should validate printer configuration when store has printer URI`() {
        // Given
        val storeId = 1
        val store =
            StoreEntity(
                id = storeId,
                name = "Test Store",
                printerUri = "192.168.1.100",
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
        val store =
            StoreEntity(
                id = storeId,
                name = "Test Store",
                printerUri = "",
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
        val deposit = 1000
        val items =
            listOf(
                ItemBean(1, "123456789", "Item 1", 300),
                ItemBean(2, "987654321", "Item 2", 400),
            )

        val store = StoreEntity(storeId, "Test Store", "192.168.1.100")

        `when`(storeService.findStore(storeId)).thenReturn(store)

        // When
        val result = receiptService.generateReceiptContent(storeId, items, deposit)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Test Store"))
        assertTrue(result.contains("Item 1 - 300リバー"))
        assertTrue(result.contains("Item 2 - 400リバー"))
        assertTrue(result.contains("Total: 700リバー"))
        assertTrue(result.contains("Deposit: 1000リバー"))
        assertTrue(result.contains("Change: 300リバー"))
        verify(storeService).findStore(storeId)
    }

    @Test
    fun `should generate receipt content with unknown store and staff`() {
        // Given
        val storeId = 999
        val deposit = 500
        val items =
            listOf(
                ItemBean(1, "123456789", "Test Item", 200),
            )

        `when`(storeService.findStore(storeId)).thenReturn(null)

        // When
        val result = receiptService.generateReceiptContent(storeId, items, deposit)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Unknown Store"))
        assertTrue(result.contains("Test Item - 200リバー"))
        assertTrue(result.contains("Total: 200リバー"))
        assertTrue(result.contains("Deposit: 500リバー"))
        assertTrue(result.contains("Change: 300リバー"))
        verify(storeService).findStore(storeId)
    }

    @Test
    fun `should handle empty items list in receipt generation`() {
        // Given
        val storeId = 1
        val deposit = 100
        val items = emptyList<ItemBean>()

        val store = StoreEntity(storeId, "Test Store", "192.168.1.100")

        `when`(storeService.findStore(storeId)).thenReturn(store)

        // When
        val result = receiptService.generateReceiptContent(storeId, items, deposit)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Total: 0リバー"))
        assertTrue(result.contains("Deposit: 100リバー"))
        assertTrue(result.contains("Change: 100リバー"))
        verify(storeService).findStore(storeId)
    }

    @Test
    fun `should not request printing when printer is not configured`() {
        val storeId = 1
        `when`(storeService.findStore(storeId)).thenReturn(StoreEntity(storeId, "Test Store", ""))

        val service = receiptServiceWith(port = 9100)

        assertFalse(service.printReceiptAsync(storeId, printableItems, 1000))
    }

    @Test
    fun `should not request printing when store does not exist`() {
        val storeId = 999
        `when`(storeService.findStore(storeId)).thenReturn(null)

        val service = receiptServiceWith(port = 9100)

        assertFalse(service.printReceiptAsync(storeId, printableItems, 1000))
    }

    @Test
    fun `should send receipt to printer in background`() {
        val storeId = 1
        val received = ArrayBlockingQueue<ByteArray>(1)

        ServerSocket(0).use { server ->
            Thread {
                server.accept().use { socket ->
                    received.put(socket.getInputStream().readBytes())
                }
            }.apply { isDaemon = true }.start()

            `when`(storeService.findStore(storeId)).thenReturn(StoreEntity(storeId, "Test Store", "127.0.0.1"))
            val service = receiptServiceWith(port = server.localPort)

            assertTrue(service.printReceiptAsync(storeId, printableItems, 1000))

            val bytes = received.poll(WAIT_LIMIT_MILLIS, TimeUnit.MILLISECONDS)
            assertTrue(bytes != null && bytes.isNotEmpty(), "印字データが送信される")
        }
    }

    @Test
    fun `should return immediately when printer is unreachable`() {
        val storeId = 1
        `when`(storeService.findStore(storeId)).thenReturn(StoreEntity(storeId, "Test Store", UNROUTABLE_HOST))

        val service = receiptServiceWith(port = 9100, connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS)

        val startedAt = System.nanoTime()
        assertTrue(service.printReceiptAsync(storeId, printableItems, 1000))
        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

        assertTrue(elapsedMillis < CONNECT_TIMEOUT_MILLIS, "接続を待たずに返る: ${elapsedMillis}ms")
    }

    private fun receiptServiceWith(
        port: Int,
        connectTimeoutMillis: Int = CONNECT_TIMEOUT_MILLIS,
    ): ReceiptService {
        val properties =
            AppProperties(
                receipt =
                    AppProperties.ReceiptProperties(
                        printer =
                            AppProperties.ReceiptProperties.PrinterProperties(
                                port = port,
                                connectTimeoutMillis = connectTimeoutMillis,
                            ),
                    ),
            )
        return ReceiptService(storeService, properties)
    }

    companion object {
        private const val UNROUTABLE_HOST = "192.0.2.1"
        private const val CONNECT_TIMEOUT_MILLIS = 3_000
        private const val WAIT_LIMIT_MILLIS = 5_000L
        private val printableItems =
            listOf(
                ItemBean(1, "0123456789", "あめ", 100),
                ItemBean(2, "9876543210", "ガム", 200),
            )
    }
}
