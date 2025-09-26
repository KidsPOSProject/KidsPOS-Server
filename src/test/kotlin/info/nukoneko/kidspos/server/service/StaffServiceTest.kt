package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.StaffEntity
import info.nukoneko.kidspos.server.repository.StaffRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

@SpringBootTest
@Disabled("Spring context not configured")
class StaffServiceTest {
    @MockBean
    private lateinit var staffRepository: StaffRepository

    private lateinit var staffService: StaffService

    @BeforeEach
    fun setup() {
        staffService = StaffService(staffRepository)
    }

    @Test
    fun `should find staff by barcode when staff exists`() {
        // Given
        val barcode = "STAFF001"
        val expectedStaff =
            StaffEntity(
                barcode = barcode,
                name = "Test Staff",
            )
        `when`(staffRepository.findById(barcode)).thenReturn(Optional.of(expectedStaff))

        // When
        val result = staffService.findStaff(barcode)

        // Then
        assertNotNull(result)
        assertEquals(barcode, result?.barcode)
        assertEquals("Test Staff", result?.name)
        verify(staffRepository).findById(barcode)
    }

    @Test
    fun `should return null when staff does not exist by barcode`() {
        // Given
        val barcode = "NONEXISTENT"
        `when`(staffRepository.findById(barcode)).thenReturn(Optional.empty())

        // When
        val result = staffService.findStaff(barcode)

        // Then
        assertNull(result)
        verify(staffRepository).findById(barcode)
    }

    @Test
    fun `should find all staff`() {
        // Given
        val expectedStaff =
            listOf(
                StaffEntity(barcode = "STAFF001", name = "Staff Member 1"),
                StaffEntity(barcode = "STAFF002", name = "Staff Member 2"),
            )
        `when`(staffRepository.findAll()).thenReturn(expectedStaff)

        // When
        val result = staffService.findAll()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Staff Member 1", result[0].name)
        assertEquals("Staff Member 2", result[1].name)
        verify(staffRepository).findAll()
    }

    @Test
    fun `should handle empty staff list`() {
        // Given
        `when`(staffRepository.findAll()).thenReturn(emptyList())

        // When
        val result = staffService.findAll()

        // Then
        assertNotNull(result)
        assertTrue(result.isEmpty())
        verify(staffRepository).findAll()
    }

    @Test
    fun `should handle special characters in barcode`() {
        // Given
        val barcodeWithSpecialChars = "STAFF-001_TEST"
        val expectedStaff =
            StaffEntity(
                barcode = barcodeWithSpecialChars,
                name = "Special Staff",
            )
        `when`(staffRepository.findById(barcodeWithSpecialChars)).thenReturn(Optional.of(expectedStaff))

        // When
        val result = staffService.findStaff(barcodeWithSpecialChars)

        // Then
        assertNotNull(result)
        assertEquals(barcodeWithSpecialChars, result?.barcode)
        assertEquals("Special Staff", result?.name)
        verify(staffRepository).findById(barcodeWithSpecialChars)
    }
}
