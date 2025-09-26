package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.domain.exception.ValidationException
import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.StaffRepository
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ValidationServiceUnitTest {
    @Mock
    private lateinit var itemRepository: ItemRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var staffRepository: StaffRepository

    @InjectMocks
    private lateinit var validationService: ValidationService

    @Test
    fun `validateItemExists should pass when item exists`() {
        // Arrange
        val itemId = 1
        `when`(itemRepository.existsById(itemId)).thenReturn(true)

        // Act & Assert
        assertDoesNotThrow {
            validationService.validateItemExists(itemId)
        }
        verify(itemRepository).existsById(itemId)
    }

    @Test
    fun `validateItemExists should throw ValidationException when item does not exist`() {
        // Arrange
        val itemId = 999
        `when`(itemRepository.existsById(itemId)).thenReturn(false)

        // Act & Assert
        val exception =
            assertThrows<ValidationException> {
                validationService.validateItemExists(itemId)
            }
        assertEquals("Item with ID $itemId does not exist", exception.message)
        verify(itemRepository).existsById(itemId)
    }

    @Test
    fun `validateItemExists should handle zero ID`() {
        // Arrange
        val itemId = 0
        `when`(itemRepository.existsById(itemId)).thenReturn(false)

        // Act & Assert
        val exception =
            assertThrows<ValidationException> {
                validationService.validateItemExists(itemId)
            }
        assertEquals("Item with ID 0 does not exist", exception.message)
        verify(itemRepository).existsById(itemId)
    }

    @Test
    fun `validateItemExists should handle negative ID`() {
        // Arrange
        val itemId = -1
        `when`(itemRepository.existsById(itemId)).thenReturn(false)

        // Act & Assert
        val exception =
            assertThrows<ValidationException> {
                validationService.validateItemExists(itemId)
            }
        assertEquals("Item with ID -1 does not exist", exception.message)
        verify(itemRepository).existsById(itemId)
    }
}
