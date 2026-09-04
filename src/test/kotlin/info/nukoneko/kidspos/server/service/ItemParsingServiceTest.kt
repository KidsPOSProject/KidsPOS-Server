package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.ItemEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
class ItemParsingServiceTest {
    @MockBean
    private lateinit var itemService: ItemService

    private lateinit var itemParsingService: ItemParsingService

    @BeforeEach
    fun setup() {
        itemParsingService = ItemParsingService(itemService)
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
}
