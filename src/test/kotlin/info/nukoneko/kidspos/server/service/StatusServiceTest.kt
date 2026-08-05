package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import java.util.Properties

@ExtendWith(MockitoExtension::class)
class StatusServiceTest {
    @Mock
    private lateinit var buildPropertiesProvider: ObjectProvider<BuildProperties>

    @Test
    fun `getStatus should return build version when BuildProperties is available`() {
        // Arrange
        val buildProperties = BuildProperties(Properties().apply { setProperty("version", "1.2.3") })
        whenever(buildPropertiesProvider.ifAvailable).thenReturn(buildProperties)
        val service = StatusService(buildPropertiesProvider)

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
        val service = StatusService(buildPropertiesProvider)

        // Act
        val result = service.getStatus()

        // Assert
        assertEquals(StatusService.STATUS_OK, result.status)
        assertEquals(StatusService.UNKNOWN_VERSION, result.version)
        assertEquals(StatusService.API_VERSION, result.apiVersion)
    }
}
