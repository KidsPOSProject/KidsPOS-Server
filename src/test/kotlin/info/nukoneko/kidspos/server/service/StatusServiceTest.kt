package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.response.PrinterStatusResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import java.util.Properties

@ExtendWith(MockitoExtension::class)
class StatusServiceTest {
    @Mock
    private lateinit var buildPropertiesProvider: ObjectProvider<BuildProperties>

    @Mock
    private lateinit var printerStatusService: PrinterStatusService

    private val printerStatus =
        PrinterStatusResponse(configured = true, reachable = true, total = 2, reachableCount = 2)

    private fun createService() = StatusService(buildPropertiesProvider, printerStatusService)

    @Test
    fun `getStatus should return build version when BuildProperties is available`() {
        // Arrange
        val buildProperties = BuildProperties(Properties().apply { setProperty("version", "1.2.3") })
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(buildProperties)
        whenever(printerStatusService.getStatus()).thenReturn(printerStatus)
        val service = createService()

        // Act
        val result = service.getStatus()

        // Assert
        assertEquals(StatusService.STATUS_OK, result.status)
        assertEquals("1.2.3", result.version)
        assertEquals(StatusService.API_VERSION, result.apiVersion)
    }

    @Test
    fun `getStatus should return build commit when it is recorded`() {
        // Arrange
        val properties =
            Properties().apply {
                setProperty("version", "1.2.3")
                setProperty("commit", "a1b2c3d")
            }
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(BuildProperties(properties))
        whenever(printerStatusService.getStatus()).thenReturn(printerStatus)
        val service = createService()

        // Act
        val result = service.getStatus()

        // Assert
        assertEquals("a1b2c3d", result.commit)
    }

    @Test
    fun `getStatus should return null commit when it is not recorded`() {
        // Arrange
        val buildProperties = BuildProperties(Properties().apply { setProperty("version", "1.2.3") })
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(buildProperties)
        whenever(printerStatusService.getStatus()).thenReturn(printerStatus)
        val service = createService()

        // Act
        val result = service.getStatus()

        // Assert
        assertNull(result.commit)
    }

    @Test
    fun `getStatus should treat unknown commit as absent`() {
        // Arrange
        val properties =
            Properties().apply {
                setProperty("version", "1.2.3")
                setProperty("commit", "unknown")
            }
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(BuildProperties(properties))
        whenever(printerStatusService.getStatus()).thenReturn(printerStatus)
        val service = createService()

        // Act
        val result = service.getStatus()

        // Assert
        assertNull(result.commit)
    }

    @Test
    fun `getStatus should return unknown version when BuildProperties is not available`() {
        // Arrange
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(null)
        whenever(printerStatusService.getStatus()).thenReturn(printerStatus)
        val service = createService()

        // Act
        val result = service.getStatus()

        // Assert
        assertEquals(StatusService.STATUS_OK, result.status)
        assertEquals(StatusService.UNKNOWN_VERSION, result.version)
        assertEquals(StatusService.API_VERSION, result.apiVersion)
    }

    @Test
    fun `getStatus should delegate printer status to PrinterStatusService`() {
        // Arrange
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(null)
        whenever(printerStatusService.getStatus()).thenReturn(printerStatus)
        val service = createService()

        // Act
        val result = service.getStatus()

        // Assert
        assertSame(printerStatus, result.printer)
        verify(printerStatusService).getStatus()
    }
}
