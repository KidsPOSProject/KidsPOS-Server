package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.common.service.IdGenerationService
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

@SpringBootTest
@Disabled("Spring context not configured")
class ItemServiceTest {
    @MockBean
    private lateinit var itemRepository: ItemRepository

    @MockBean
    private lateinit var idGenerationService: IdGenerationService

    private lateinit var itemService: ItemService

    @BeforeEach
    fun setup() {
        itemService = ItemService(itemRepository, idGenerationService)
    }

    @Test
    fun `should find item by ID when item exists`() {
        // Given
        val itemId = 1
        val expectedItem = ItemEntity(
            id = itemId,
            barcode = "123456789",
            name = "Test Item",
            price = 100
        )
        `when`(itemRepository.findById(itemId)).thenReturn(Optional.of(expectedItem))

        // When
        val result = itemService.findItem(itemId)

        // Then
        assertNotNull(result)
        assertEquals(itemId, result?.id)
        assertEquals("123456789", result?.barcode)
        assertEquals("Test Item", result?.name)
        assertEquals(100, result?.price)
        verify(itemRepository).findById(itemId)
    }

    @Test
    fun `should return null when item does not exist by ID`() {
        // Given
        val itemId = 999
        `when`(itemRepository.findById(itemId)).thenReturn(Optional.empty())

        // When
        val result = itemService.findItem(itemId)

        // Then
        assertNull(result)
        verify(itemRepository).findById(itemId)
    }

    @Test
    fun `should find item by barcode when item exists`() {
        // Given
        val barcode = "123456789"
        val expectedItem = ItemEntity(
            id = 1,
            barcode = barcode,
            name = "Test Item",
            price = 100
        )
        `when`(itemRepository.findByBarcode(barcode)).thenReturn(expectedItem)

        // When
        val result = itemService.findItem(barcode)

        // Then
        assertNotNull(result)
        assertEquals(1, result?.id)
        assertEquals(barcode, result?.barcode)
        assertEquals("Test Item", result?.name)
        assertEquals(100, result?.price)
        verify(itemRepository).findByBarcode(barcode)
    }

    @Test
    fun `should return null when item does not exist by barcode`() {
        // Given
        val barcode = "nonexistent"
        `when`(itemRepository.findByBarcode(barcode)).thenReturn(null)

        // When
        val result = itemService.findItem(barcode)

        // Then
        assertNull(result)
        verify(itemRepository).findByBarcode(barcode)
    }

    @Test
    fun `should save item successfully`() {
        // Given
        val itemBean = ItemBean(
            id = null,
            barcode = "123456789",
            name = "New Item",
            price = 200
        )
        val expectedItem = ItemEntity(
            id = 1,
            barcode = "123456789",
            name = "New Item",
            price = 200
        )
        `when`(idGenerationService.generateNextId(itemRepository)).thenReturn(1)
        `when`(itemRepository.save(any<ItemEntity>())).thenReturn(expectedItem)

        // When
        val result = itemService.save(itemBean)

        // Then
        assertNotNull(result)
        assertEquals(1, result.id)
        assertEquals("123456789", result.barcode)
        assertEquals("New Item", result.name)
        assertEquals(200, result.price)
        verify(idGenerationService).generateNextId(itemRepository)
        verify(itemRepository).save(any<ItemEntity>())
    }

    @Test
    fun `should find all items`() {
        // Given
        val expectedItems = listOf(
            ItemEntity(1, "123456789", "Item 1", 100),
            ItemEntity(2, "987654321", "Item 2", 200)
        )
        `when`(itemRepository.findAll()).thenReturn(expectedItems)

        // When
        val result = itemService.findAll()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Item 1", result[0].name)
        assertEquals("Item 2", result[1].name)
        verify(itemRepository).findAll()
    }

    @Test
    fun `should save item with existing ID`() {
        // Given
        val itemBean = ItemBean(
            id = 5,
            barcode = "123456789",
            name = "Existing Item",
            price = 300
        )
        val expectedItem = ItemEntity(
            id = 5,
            barcode = "123456789",
            name = "Existing Item",
            price = 300
        )
        `when`(itemRepository.save(any<ItemEntity>())).thenReturn(expectedItem)

        // When
        val result = itemService.save(itemBean)

        // Then
        assertNotNull(result)
        assertEquals(5, result.id)
        assertEquals("123456789", result.barcode)
        assertEquals("Existing Item", result.name)
        assertEquals(300, result.price)
        verify(itemRepository).save(any<ItemEntity>())
        verify(idGenerationService, never()).generateNextId(any<ItemRepository>())
    }
}