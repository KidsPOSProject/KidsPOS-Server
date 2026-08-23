package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.entity.StoreEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class PrinterStatusServiceTest {
    @Mock
    private lateinit var storeService: StoreService

    @Mock
    private lateinit var printerConnectionChecker: PrinterConnectionChecker

    private var service: PrinterStatusService? = null

    @AfterEach
    fun tearDown() {
        service?.shutdown()
    }

    private fun createService(cacheSeconds: Int = DEFAULT_CACHE_SECONDS): PrinterStatusService {
        val appProperties =
            AppProperties(
                receipt =
                    AppProperties.ReceiptProperties(
                        printer = AppProperties.ReceiptProperties.PrinterProperties(statusCacheSeconds = cacheSeconds),
                    ),
            )
        return PrinterStatusService(storeService, appProperties, printerConnectionChecker).also { service = it }
    }

    private fun port() = AppProperties().receipt.printer.port

    @Test
    fun `should report not configured when no store has printer`() {
        // Arrange
        whenever(storeService.findAll()).thenReturn(emptyList())

        // Act
        val status = createService().getStatus()

        // Assert
        assertFalse(status.configured)
        assertFalse(status.reachable)
        assertEquals(0, status.total)
        assertEquals(0, status.reachableCount)
    }

    @Test
    fun `should report reachable when all printers respond`() {
        // Arrange
        whenever(storeService.findAll()).thenReturn(
            listOf(
                StoreEntity(1, "Store A", "192.168.1.50"),
                StoreEntity(2, "Store B", "192.168.1.51"),
            ),
        )
        whenever(printerConnectionChecker.isReachable(any(), eq(port()), any())).thenReturn(true)

        // Act
        val status = createService().getStatus()

        // Assert
        assertTrue(status.configured)
        assertTrue(status.reachable)
        assertEquals(2, status.total)
        assertEquals(2, status.reachableCount)
    }

    @Test
    fun `should report unreachable when some printer does not respond`() {
        // Arrange
        whenever(storeService.findAll()).thenReturn(
            listOf(
                StoreEntity(1, "Store A", "192.168.1.50"),
                StoreEntity(2, "Store B", "192.168.1.51"),
            ),
        )
        whenever(printerConnectionChecker.isReachable(eq("192.168.1.50"), eq(port()), any())).thenReturn(true)
        whenever(printerConnectionChecker.isReachable(eq("192.168.1.51"), eq(port()), any())).thenReturn(false)

        // Act
        val status = createService().getStatus()

        // Assert
        assertTrue(status.configured)
        assertFalse(status.reachable)
        assertEquals(2, status.total)
        assertEquals(1, status.reachableCount)
    }

    @Test
    fun `should deduplicate printer hosts and ignore blank ones`() {
        // Arrange
        whenever(storeService.findAll()).thenReturn(
            listOf(
                StoreEntity(1, "Store A", "192.168.1.50"),
                StoreEntity(2, "Store B", " 192.168.1.50 "),
                StoreEntity(3, "Store C", " "),
            ),
        )
        whenever(printerConnectionChecker.isReachable(eq("192.168.1.50"), eq(port()), any())).thenReturn(true)

        // Act
        val status = createService().getStatus()

        // Assert
        assertTrue(status.configured)
        assertTrue(status.reachable)
        assertEquals(1, status.total)
        assertEquals(1, status.reachableCount)
    }

    @Test
    fun `should treat check failure as unreachable`() {
        // Arrange
        whenever(storeService.findAll()).thenReturn(listOf(StoreEntity(1, "Store A", "192.168.1.50")))
        whenever(printerConnectionChecker.isReachable(any(), eq(port()), any()))
            .thenThrow(IllegalStateException("boom"))

        // Act
        val status = createService().getStatus()

        // Assert
        assertTrue(status.configured)
        assertFalse(status.reachable)
        assertEquals(1, status.total)
        assertEquals(0, status.reachableCount)
    }

    @Test
    fun `should reuse cached result within cache duration`() {
        // Arrange
        whenever(storeService.findAll()).thenReturn(listOf(StoreEntity(1, "Store A", "192.168.1.50")))
        whenever(printerConnectionChecker.isReachable(any(), eq(port()), any())).thenReturn(true)
        val target = createService()

        // Act
        val first = target.getStatus()
        val second = target.getStatus()

        // Assert
        assertEquals(first, second)
        verify(printerConnectionChecker, times(1)).isReachable(any(), eq(port()), any())
        verify(storeService, times(1)).findAll()
    }

    @Test
    fun `should check again when cache duration is zero`() {
        // Arrange
        whenever(storeService.findAll()).thenReturn(listOf(StoreEntity(1, "Store A", "192.168.1.50")))
        whenever(printerConnectionChecker.isReachable(any(), eq(port()), any())).thenReturn(true)
        val target = createService(cacheSeconds = 0)

        // Act
        target.getStatus()
        target.getStatus()

        // Assert
        verify(printerConnectionChecker, times(2)).isReachable(any(), eq(port()), any())
    }

    @Test
    fun `should check printers in parallel`() {
        // Arrange
        val hosts = listOf("192.168.1.50", "192.168.1.51", "192.168.1.52")
        whenever(storeService.findAll()).thenReturn(
            hosts.mapIndexed { index, host -> StoreEntity(index + 1, "Store $index", host) },
        )
        // 逐次実行だと最初の1台が待ち切れずに false を返すため、並列であることを結果で判定できる
        val latch = CountDownLatch(hosts.size)
        whenever(printerConnectionChecker.isReachable(any(), eq(port()), any())).thenAnswer {
            latch.countDown()
            latch.await(PARALLEL_WAIT_SECONDS, TimeUnit.SECONDS)
        }

        // Act
        val status = createService().getStatus()

        // Assert
        assertTrue(status.reachable)
        assertEquals(hosts.size, status.reachableCount)
    }

    companion object {
        private const val DEFAULT_CACHE_SECONDS = 15
        private const val PARALLEL_WAIT_SECONDS = 2L
    }
}
