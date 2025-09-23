package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.domain.exception.ItemNotFoundException
import info.nukoneko.kidspos.server.entity.ItemEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
@Disabled("Temporarily disabled - Spring context issues")
class ItemParsingServiceTest {

    @MockBean
    private lateinit var itemService: ItemService

    private lateinit var itemParsingService: ItemParsingService

    @BeforeEach
    fun setup() {
        itemParsingService = ItemParsingService(itemService)
    }

    @Test
    fun `should parse single item ID successfully`() {
        // Given
        val itemId = 1
        val itemEntity = ItemEntity(itemId, "123456789", "Test Item", 100)
        `when`(itemService.findItem(itemId)).thenReturn(itemEntity)

        // When
        val result = itemParsingService.parseItemFromId(itemId)

        // Then
        assertNotNull(result)
        assertEquals(itemId, result.id)
        assertEquals("123456789", result.barcode)
        assertEquals("Test Item", result.name)
        assertEquals(100, result.price)
        verify(itemService).findItem(itemId)
    }

    @Test
    fun `should throw exception when item not found by ID`() {
        // Given
        val itemId = 999
        `when`(itemService.findItem(itemId)).thenReturn(null)

        // When & Then
        assertThrows(ItemNotFoundException::class.java) {
            itemParsingService.parseItemFromId(itemId)
        }
        verify(itemService).findItem(itemId)
    }

    @Test
    fun `should parse multiple item IDs successfully`() {
        // Given
        val itemIds = "1,2,3"
        val item1 = ItemEntity(1, "123456789", "Item 1", 100)
        val item2 = ItemEntity(2, "987654321", "Item 2", 200)
        val item3 = ItemEntity(3, "555666777", "Item 3", 300)

        `when`(itemService.findItem(1)).thenReturn(item1)
        `when`(itemService.findItem(2)).thenReturn(item2)
        `when`(itemService.findItem(3)).thenReturn(item3)

        // When
        val result = itemParsingService.parseItemsFromIds(itemIds)

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("Item 1", result[0].name)
        assertEquals("Item 2", result[1].name)
        assertEquals("Item 3", result[2].name)
        verify(itemService).findItem(1)
        verify(itemService).findItem(2)
        verify(itemService).findItem(3)
    }

    @Test
    fun `should handle duplicate item IDs`() {
        // Given
        val itemIds = "1,1,2"
        val item1 = ItemEntity(1, "123456789", "Item 1", 100)
        val item2 = ItemEntity(2, "987654321", "Item 2", 200)

        `when`(itemService.findItem(1)).thenReturn(item1)
        `when`(itemService.findItem(2)).thenReturn(item2)

        // When
        val result = itemParsingService.parseItemsFromIds(itemIds)

        // Then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals("Item 1", result[0].name)
        assertEquals("Item 1", result[1].name)
        assertEquals("Item 2", result[2].name)
        verify(itemService, times(2)).findItem(1)
        verify(itemService).findItem(2)
    }

    @Test
    fun `should throw exception for empty item IDs`() {
        // Given
        val emptyItemIds = ""

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            itemParsingService.parseItemsFromIds(emptyItemIds)
        }
    }

    @Test
    fun `should throw exception for blank item IDs`() {
        // Given
        val blankItemIds = "   "

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            itemParsingService.parseItemsFromIds(blankItemIds)
        }
    }

    @Test
    fun `should throw exception for invalid item ID format`() {
        // Given
        val invalidItemIds = "1,abc,3"
        val item1 = ItemEntity(1, "123456789", "Item 1", 100)
        `when`(itemService.findItem(1)).thenReturn(item1)

        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            itemParsingService.parseItemsFromIds(invalidItemIds)
        }
    }

    @Test
    fun `should parse items from barcodes successfully`() {
        // Given
        val barcodes = "123456789,987654321"
        val item1 = ItemEntity(1, "123456789", "Item 1", 100)
        val item2 = ItemEntity(2, "987654321", "Item 2", 200)

        `when`(itemService.findItem("123456789")).thenReturn(item1)
        `when`(itemService.findItem("987654321")).thenReturn(item2)

        // When
        val result = itemParsingService.parseItemsFromBarcodes(barcodes)

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Item 1", result[0].name)
        assertEquals("Item 2", result[1].name)
        verify(itemService).findItem("123456789")
        verify(itemService).findItem("987654321")
    }

    @Test
    fun `should validate item IDs format correctly`() {
        // Given & When & Then
        assertTrue(itemParsingService.validateItemIdsFormat("1,2,3"))
        assertTrue(itemParsingService.validateItemIdsFormat("123"))
        assertFalse(itemParsingService.validateItemIdsFormat(""))
        assertFalse(itemParsingService.validateItemIdsFormat("1,abc,3"))
        assertFalse(itemParsingService.validateItemIdsFormat("1,,3"))
    }

    @Test
    fun `should count items from IDs correctly`() {
        // Given & When & Then
        assertEquals(3, itemParsingService.countItemsFromIds("1,2,3"))
        assertEquals(1, itemParsingService.countItemsFromIds("123"))
        assertEquals(0, itemParsingService.countItemsFromIds(""))
        assertEquals(2, itemParsingService.countItemsFromIds("1, ,2"))
    }

    @Test
    fun `should get unique item count correctly`() {
        // Given & When & Then
        assertEquals(3, itemParsingService.getUniqueItemCount("1,2,3"))
        assertEquals(2, itemParsingService.getUniqueItemCount("1,1,2"))
        assertEquals(1, itemParsingService.getUniqueItemCount("1,1,1"))
        assertEquals(0, itemParsingService.getUniqueItemCount(""))
    }
}