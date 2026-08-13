package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.response.StatusResponse
import info.nukoneko.kidspos.server.service.SettingService
import info.nukoneko.kidspos.server.service.StatusService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SettingApiControllerUnitTest {
    @Mock
    private lateinit var settingService: SettingService

    @Mock
    private lateinit var statusService: StatusService

    @InjectMocks
    private lateinit var controller: SettingApiController

    @Test
    fun `getStatus should return status from service`() {
        // Arrange
        val expected = StatusResponse(status = "OK", version = "1.0.0", apiVersion = 1)
        whenever(statusService.getStatus()).thenReturn(expected)

        // Act
        val result = controller.getStatus()

        // Assert
        assertNotNull(result)
        assertEquals("OK", result.status)
        assertEquals(expected, result)
    }
}
