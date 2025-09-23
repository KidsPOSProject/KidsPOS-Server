package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.service.StaffService
import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        assertEquals(200, result.statusCodeValue)
        val body = result.body
        assertNotNull(body)
        assertEquals(barcode, body?.barcode)
        assertEquals("Test Staff", body?.name)
        verify(staffService).findStaff(barcode)
    }

    @Test
    fun `getStaff should throw ResourceNotFoundException when staff not found`() {
        // Arrange
        val barcode = "NONEXISTENT"
        `when`(staffService.findStaff(barcode)).thenReturn(null)

        // Act & Assert
        val exception = assertThrows<ResourceNotFoundException> {
            controller.getStaff(barcode)
        }
        assertEquals("Staff with barcode $barcode not found", exception.message)
        verify(staffService).findStaff(barcode)
    }

    @Test
    fun `getStaff should throw ResourceNotFoundException for empty barcode`() {
        // Arrange
        val barcode = ""
        `when`(staffService.findStaff(barcode)).thenReturn(null)

        // Act & Assert
        val exception = assertThrows<ResourceNotFoundException> {
            controller.getStaff(barcode)
        }
        assertEquals("Staff with barcode  not found", exception.message)
        verify(staffService).findStaff(barcode)
    }
}