package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.Optional

@SpringBootTest
@EnableCaching
class CacheServiceTest {

    @Autowired
    private lateinit var itemService: ItemService

    @Autowired
    private lateinit var storeService: StoreService

    @Autowired
    private lateinit var cacheManager: CacheManager

    @MockBean
    private lateinit var itemRepository: ItemRepository

    @MockBean
    private lateinit var storeRepository: StoreRepository

    @BeforeEach
    fun setup() {
        // Clear all caches before each test
        cacheManager.cacheNames.forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
        }
    }

    @Test
    fun `should cache item lookups by ID`() {
        // Given
        val itemId = 1
        val item = ItemEntity(id = itemId, name = "Test Item", barcode = "1234", price = 100)
        `when`(itemRepository.findById(itemId)).thenReturn(Optional.of(item))

        // When - First call should hit the repository
        val result1 = itemService.findItem(itemId)
        val result2 = itemService.findItem(itemId)

        // Then - Repository should only be called once due to caching
        verify(itemRepository, times(1)).findById(itemId)
        assertEquals(result1, result2)
        assertNotNull(result1)
        assertEquals("Test Item", result1?.name)
    }

    @Test
    fun `should cache item lookups by barcode`() {
        // Given
        val barcode = "123456789"
        val item = ItemEntity(id = 1, name = "Test Item", barcode = barcode, price = 100)
        `when`(itemRepository.findByBarcode(barcode)).thenReturn(item)

        // When - Multiple calls with same barcode
        val result1 = itemService.findItem(barcode)
        val result2 = itemService.findItem(barcode)
        val result3 = itemService.findItem(barcode)

        // Then - Repository should only be called once
        verify(itemRepository, times(1)).findByBarcode(barcode)
        assertEquals(result1, result2)
        assertEquals(result2, result3)
    }

    @Test
    fun `should evict cache when item is saved`() {
        // Given
        val itemId = 1
        val originalItem = ItemEntity(id = itemId, name = "Original", barcode = "1234", price = 100)
        val updatedItem = ItemEntity(id = itemId, name = "Updated", barcode = "1234", price = 200)

        `when`(itemRepository.findById(itemId)).thenReturn(Optional.of(originalItem))

        // When - First retrieve to populate cache
        val cached = itemService.findItem(itemId)
        assertEquals("Original", cached?.name)

        // Update the item (should evict cache)
        `when`(itemRepository.save(any(ItemEntity::class.java))).thenReturn(updatedItem)
        `when`(itemRepository.findById(itemId)).thenReturn(Optional.of(updatedItem))

        itemService.save(ItemBean(id = itemId, name = "Updated", barcode = "1234", price = 200))

        // Get item again (should not use cache)
        val afterUpdate = itemService.findItem(itemId)

        // Then - Repository should be called twice (once before update, once after)
        verify(itemRepository, times(2)).findById(itemId)
        assertEquals("Updated", afterUpdate?.name)
    }

    @Test
    fun `should cache all items list`() {
        // Given
        val items = listOf(
            ItemEntity(id = 1, name = "Item 1", barcode = "001", price = 100),
            ItemEntity(id = 2, name = "Item 2", barcode = "002", price = 200),
            ItemEntity(id = 3, name = "Item 3", barcode = "003", price = 300)
        )
        `when`(itemRepository.findAll()).thenReturn(items)

        // When - Multiple calls to findAll
        val result1 = itemService.findAll()
        val result2 = itemService.findAll()
        val result3 = itemService.findAll()

        // Then - Repository should only be called once
        verify(itemRepository, times(1)).findAll()
        assertEquals(result1.size, result2.size)
        assertEquals(result2.size, result3.size)
        assertEquals(3, result1.size)
    }

    @Test
    fun `should cache store lookups`() {
        // Given
        val storeId = 1
        val store = StoreEntity(id = storeId, name = "Test Store", printerUri = "http://printer")
        `when`(storeRepository.findById(storeId)).thenReturn(Optional.of(store))

        // When - Multiple calls
        val result1 = storeService.findStore(storeId)
        val result2 = storeService.findStore(storeId)

        // Then - Repository should only be called once
        verify(storeRepository, times(1)).findById(storeId)
        assertEquals(result1, result2)
        assertEquals("Test Store", result1?.name)
    }

    @Test
    fun `should have separate caches for different entity types`() {
        // Given
        val id = 1
        val item = ItemEntity(id = id, name = "Item", barcode = "123", price = 100)
        val store = StoreEntity(id = id, name = "Store", printerUri = "http://printer")

        `when`(itemRepository.findById(id)).thenReturn(Optional.of(item))
        `when`(storeRepository.findById(id)).thenReturn(Optional.of(store))

        // When - Get both with same ID
        val itemResult = itemService.findItem(id)
        val storeResult = storeService.findStore(id)

        // Then - Both should be cached separately
        assertEquals("Item", itemResult?.name)
        assertEquals("Store", storeResult?.name)
        verify(itemRepository, times(1)).findById(id)
        verify(storeRepository, times(1)).findById(id)
    }
}