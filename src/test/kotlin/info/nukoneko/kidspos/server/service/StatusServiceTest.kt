package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.entity.StoreEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import java.util.Properties

@ExtendWith(MockitoExtension::class)
class StatusServiceTest {
    @Mock
    private lateinit var buildPropertiesProvider: ObjectProvider<BuildProperties>

    @Mock
    private lateinit var storeService: StoreService

    @Mock
    private lateinit var printerConnectionChecker: PrinterConnectionChecker

    private val appProperties = AppProperties()

    private fun createService() = StatusService(buildPropertiesProvider, storeService, appProperties, printerConnectionChecker)

    @Test
    fun `getStatus should return build version when BuildProperties is available`() {
        // Arrange
        val buildProperties = BuildProperties(Properties().apply { setProperty("version", "1.2.3") })
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(buildProperties)
        whenever(storeService.findAll()).thenReturn(emptyList())
        val service = createService()

        // Act
        val result = service.getStatus()

        // Assert
        assertEquals(StatusService.STATUS_OK, result.status)
        assertEquals("1.2.3", result.version)
        assertEquals(StatusService.API_VERSION, result.apiVersion)
    }

    @Test
    fun `getStatus should return unknown version when BuildProperties is not available`() {
        // Arrange
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(null)
        whenever(storeService.findAll()).thenReturn(emptyList())
        val service = createService()

        // Act
        val result = service.getStatus()

        // Assert
        assertEquals(StatusService.STATUS_OK, result.status)
        assertEquals(StatusService.UNKNOWN_VERSION, result.version)
        assertEquals(StatusService.API_VERSION, result.apiVersion)
    }

    @Test
    fun `getStatus should report printer not configured when no store has printer`() {
        // Arrange
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(null)
        whenever(storeService.findAll()).thenReturn(emptyList())
        val service = createService()

        // Act
        val printer = service.getStatus().printer!!

        // Assert
        assertFalse(printer.configured)
        assertFalse(printer.reachable)
        assertEquals(0, printer.total)
        assertEquals(0, printer.reachableCount)
    }

    @Test
    fun `getStatus should report reachable when all printers respond`() {
        // Arrange
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(null)
        whenever(storeService.findAll()).thenReturn(
            listOf(
                StoreEntity(1, "Store A", "192.168.1.50"),
                StoreEntity(2, "Store B", "192.168.1.51"),
            ),
        )
        whenever(printerConnectionChecker.isReachable(any(), eq(appProperties.receipt.printer.port), any()))
            .thenReturn(true)
        val service = createService()

        // Act
        val printer = service.getStatus().printer!!

        // Assert
        assertTrue(printer.configured)
        assertTrue(printer.reachable)
        assertEquals(2, printer.total)
        assertEquals(2, printer.reachableCount)
    }

    @Test
    fun `getStatus should report unreachable when some printer does not respond`() {
        // Arrange
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(null)
        whenever(storeService.findAll()).thenReturn(
            listOf(
                StoreEntity(1, "Store A", "192.168.1.50"),
                StoreEntity(2, "Store B", "192.168.1.51"),
            ),
        )
        whenever(printerConnectionChecker.isReachable(eq("192.168.1.50"), eq(appProperties.receipt.printer.port), any()))
            .thenReturn(true)
        whenever(printerConnectionChecker.isReachable(eq("192.168.1.51"), eq(appProperties.receipt.printer.port), any()))
            .thenReturn(false)
        val service = createService()

        // Act
        val printer = service.getStatus().printer!!

        // Assert
        assertTrue(printer.configured)
        assertFalse(printer.reachable)
        assertEquals(2, printer.total)
        assertEquals(1, printer.reachableCount)
    }

    @Test
    fun `getStatus should deduplicate printer hosts and ignore blank ones`() {
        // Arrange
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(null)
        whenever(storeService.findAll()).thenReturn(
            listOf(
                StoreEntity(1, "Store A", "192.168.1.50"),
                StoreEntity(2, "Store B", " 192.168.1.50 "),
                StoreEntity(3, "Store C", " "),
            ),
        )
        whenever(printerConnectionChecker.isReachable(eq("192.168.1.50"), eq(appProperties.receipt.printer.port), any()))
            .thenReturn(true)
        val service = createService()

        // Act
        val printer = service.getStatus().printer!!

        // Assert
        assertTrue(printer.configured)
        assertTrue(printer.reachable)
        assertEquals(1, printer.total)
        assertEquals(1, printer.reachableCount)
    }
}
