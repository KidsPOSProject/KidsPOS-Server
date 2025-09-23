package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.service.StaffService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class StaffApiControllerUnitTest {

    @Mock
    private lateinit var staffService: StaffService

    @InjectMocks
    private lateinit var controller: StaffApiController

    @Test
    fun `getStaff should return staff when found`() {
        // Arrange
        val barcode = "ST123456"
        val expectedStaff = StaffEntity(
            barcode = barcode,
            name = "Test Staff"
        )
        `when`(staffService.findStaff(barcode)).thenReturn(expectedStaff)

        // Act
        val result = controller.getStaff(barcode)

        // Assert
        assertNotNull(result)
        assertEquals(expectedStaff, result)
        assertEquals(barcode, result?.barcode)
        assertEquals("Test Staff", result?.name)
        verify(staffService).findStaff(barcode)
    }

    @Test
    fun `getStaff should return null when staff not found`() {
        // Arrange
        val barcode = "NONEXISTENT"
        `when`(staffService.findStaff(barcode)).thenReturn(null)

        // Act
        val result = controller.getStaff(barcode)

        // Assert
        assertNull(result)
        verify(staffService).findStaff(barcode)
    }

    @Test
    fun `getStaff should handle empty barcode`() {
        // Arrange
        val barcode = ""
        `when`(staffService.findStaff(barcode)).thenReturn(null)

        // Act
        val result = controller.getStaff(barcode)

        // Assert
        assertNull(result)
        verify(staffService).findStaff(barcode)
    }
}