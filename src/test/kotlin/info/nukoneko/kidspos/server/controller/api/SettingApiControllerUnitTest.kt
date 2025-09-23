package info.nukoneko.kidspos.server.controller.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SettingApiControllerUnitTest {

    @InjectMocks
    private lateinit var controller: SettingApiController

    @Test
    fun `getStatus should return OK status`() {
        // Act
        val result = controller.getStatus()

        // Assert
        assertNotNull(result)
        assertEquals("OK", result.status)
    }

    @Test
    fun `StatusBean should contain correct status value`() {
        // Arrange
        val statusValue = "TEST_STATUS"

        // Act
        val statusBean = SettingApiController.StatusBean(statusValue)

        // Assert
        assertEquals(statusValue, statusBean.status)
    }
}