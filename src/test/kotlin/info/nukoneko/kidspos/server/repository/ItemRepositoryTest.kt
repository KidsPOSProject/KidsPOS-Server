package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.ItemEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ItemRepository Integration Tests")
class ItemRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var itemRepository: ItemRepository

    private lateinit var testItem1: ItemEntity
    private lateinit var testItem2: ItemEntity
    private lateinit var testItem3: ItemEntity

    @BeforeEach
    fun setup() {
        // Clean up before each test
        itemRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()

        // Create test data with unique IDs
        testItem1 = ItemEntity(
            id = 1001,
            barcode = "123456789",
            name = "Test Item 1",
            price = 100
        )
        testItem2 = ItemEntity(
            id = 1002,
            barcode = "987654321",
            name = "Test Item 2",
            price = 200
        )
        testItem3 = ItemEntity(
            id = 1003,
            barcode = "555666777",
            name = "Expensive Item",
            price = 1000
        )

        // Persist test data
        testItem1 = itemRepository.save(testItem1)
        testItem2 = itemRepository.save(testItem2)
        testItem3 = itemRepository.save(testItem3)
        entityManager.flush()
        entityManager.clear()
    }

    @Test
    fun `should find item by barcode`() {
        // When
        val foundItem = itemRepository.findByBarcode("123456789")

        // Then
        assertNotNull(foundItem)
        assertEquals("123456789", foundItem?.barcode)
        assertEquals("Test Item 1", foundItem?.name)
        assertEquals(100, foundItem?.price)
    }

    @Test
    fun `should return null when item not found by barcode`() {
        // When
        val foundItem = itemRepository.findByBarcode("nonexistent")

        // Then
        assertNull(foundItem)
    }

    @Test
    fun `should find all items`() {
        // When
        val allItems = itemRepository.findAll()

        // Then
        assertNotNull(allItems)
        assertEquals(3, allItems.size)

        val itemNames = allItems.map { it.name }.sorted()
        assertTrue(itemNames.contains("Test Item 1"))
        assertTrue(itemNames.contains("Test Item 2"))
        assertTrue(itemNames.contains("Expensive Item"))
    }

    @Test
    fun `should save new item`() {
        // Given
        val newItem = ItemEntity(
            id = 2001,
            barcode = "111222333",
            name = "New Item",
            price = 300
        )

        // When
        val savedItem = itemRepository.save(newItem)

        // Then
        assertNotNull(savedItem)
        assertEquals(2001, savedItem.id)
        assertEquals("111222333", savedItem.barcode)
        assertEquals("New Item", savedItem.name)
        assertEquals(300, savedItem.price)

        // Verify persistence
        entityManager.clear()
        val foundItem = itemRepository.findById(savedItem.id)
        assertTrue(foundItem.isPresent)
        assertEquals("New Item", foundItem.get().name)
    }

    @Test
    fun `should update existing item by creating new instance`() {
        // Given - fetch and update existing item
        val existingItem = itemRepository.findById(testItem1.id).orElseThrow()
        val updatedItem = ItemEntity(
            id = existingItem.id,
            barcode = existingItem.barcode,
            name = existingItem.name,
            price = 150
        )

        // When
        val savedItem = itemRepository.save(updatedItem)

        // Then
        assertNotNull(savedItem)
        assertEquals(testItem1.id, savedItem.id)
        assertEquals(150, savedItem.price)

        // Verify persistence
        entityManager.clear()
        val foundItem = itemRepository.findById(savedItem.id)
        assertTrue(foundItem.isPresent)
        assertEquals(150, foundItem.get().price)
    }

    @Test
    fun `should delete item by id`() {
        // Given
        val itemIdToDelete = testItem1.id

        // When
        itemRepository.deleteById(itemIdToDelete)

        // Then
        val foundItem = itemRepository.findById(itemIdToDelete)
        assertFalse(foundItem.isPresent)

        // Verify other items still exist
        val remainingItems = itemRepository.findAll()
        assertEquals(2, remainingItems.size)
    }

    @Test
    fun `should find items with pagination`() {
        // Given
        val pageRequest = PageRequest.of(0, 2)

        // When
        val itemPage = itemRepository.findAll(pageRequest)

        // Then
        assertNotNull(itemPage)
        assertEquals(2, itemPage.size)
        assertEquals(3, itemPage.totalElements)
        assertEquals(2, itemPage.totalPages)
        assertTrue(itemPage.hasNext())
        assertFalse(itemPage.hasPrevious())
    }

    @Test
    fun `should find items by price range using custom query`() {
        // When
        val priceRangeItems = itemRepository.findByPriceRange(100, 500)

        // Then
        assertNotNull(priceRangeItems)
        assertEquals(2, priceRangeItems.size)
        assertTrue(priceRangeItems.all { it.price in 100..500 })
    }

    @Test
    fun `should count items with price greater than threshold`() {
        // When
        val expensiveItemCount = itemRepository.countByPriceGreaterThan(150)

        // Then
        assertEquals(2, expensiveItemCount) // testItem2 (200) and testItem3 (1000)
    }

    @Test
    fun `should find all item summaries using projection`() {
        // When
        val itemSummaries = itemRepository.findAllItemSummaries()

        // Then
        assertNotNull(itemSummaries)
        assertEquals(3, itemSummaries.size)
    }

    @Test
    fun `should batch fetch items by IDs`() {
        // Given
        val idsToFetch = listOf(testItem1.id, testItem3.id)

        // When
        val batchItems = itemRepository.findAllByIdsBatch(idsToFetch)

        // Then
        assertNotNull(batchItems)
        assertEquals(2, batchItems.size)
        assertTrue(batchItems.any { it.id == testItem1.id })
        assertTrue(batchItems.any { it.id == testItem3.id })
        assertFalse(batchItems.any { it.id == testItem2.id })
    }

    @Test
    fun `should get last ID correctly`() {
        // When
        val lastId = itemRepository.getLastId()

        // Then
        assertTrue(lastId > 0)
        // Should be the highest ID (testItem3.id = 1003)
        assertEquals(1003, lastId)
    }
}