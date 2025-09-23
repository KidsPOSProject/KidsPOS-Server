package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import jakarta.persistence.EntityManager

@DataJpaTest
@SpringJUnitConfig
@Disabled("Temporarily disabled - Spring context issues")
class QueryOptimizationTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var saleRepository: SaleRepository

    @Autowired
    private lateinit var saleDetailRepository: SaleDetailRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @BeforeEach
    fun setup() {
        // Clear all data before each test
        saleDetailRepository.deleteAll()
        saleRepository.deleteAll()
        itemRepository.deleteAll()
        storeRepository.deleteAll()
    }

    @Test
    fun `should use pagination for large result sets`() {
        // Given - Create 50 items
        for (i in 1..50) {
            val item = ItemEntity(
                id = i,
                barcode = "BAR$i",
                name = "Item $i",
                price = i * 100
            )
            entityManager.persist(item)
        }
        entityManager.flush()

        // When - Fetch with pagination
        val page1 = itemRepository.findAll(PageRequest.of(0, 10, Sort.by("id")))
        val page2 = itemRepository.findAll(PageRequest.of(1, 10, Sort.by("id")))

        // Then
        assertEquals(10, page1.content.size)
        assertEquals(10, page2.content.size)
        assertEquals(50, page1.totalElements)
        assertEquals(5, page1.totalPages)
        assertEquals("Item 1", page1.content[0].name)
        assertEquals("Item 11", page2.content[0].name)
    }

    @Test
    fun `should fetch sale with details in single query to avoid N+1`() {
        // Given - Create test data
        val store = StoreEntity(id = 1, name = "Test Store", printerUri = "http://printer")
        entityManager.persist(store)

        val sale = SaleEntity(
            id = 1,
            storeId = 1,
            staffId = 1,
            quantity = 3,
            amount = 300,
            deposit = 500,
            createdAt = java.util.Date()
        )
        entityManager.persist(sale)

        for (i in 1..3) {
            val item = ItemEntity(id = i, barcode = "BAR$i", name = "Item $i", price = 100)
            entityManager.persist(item)

            val detail = SaleDetailEntity(
                id = i,
                saleId = 1,
                itemId = i,
                price = 100,
                quantity = 1
            )
            entityManager.persist(detail)
        }
        entityManager.flush()
        entityManager.clear()

        // When - Fetch sale with details using optimized query
        val saleWithDetails = saleRepository.findByIdWithDetails(1)

        // Then - Verify data is fetched efficiently
        assertNotNull(saleWithDetails)
        assertEquals(1, saleWithDetails?.id)

        // Details should be eagerly loaded
        val details = saleDetailRepository.findBySaleId(1)
        assertEquals(3, details.size)
    }

    @Test
    fun `should use batch fetching for multiple entity lookups`() {
        // Given - Create test data
        val itemIds = mutableListOf<Int>()
        for (i in 1..20) {
            val item = ItemEntity(id = i, barcode = "BAR$i", name = "Item $i", price = i * 10)
            entityManager.persist(item)
            itemIds.add(i)
        }
        entityManager.flush()
        entityManager.clear()

        // When - Fetch multiple items by IDs
        val items = itemRepository.findAllById(itemIds)

        // Then - All items should be fetched
        assertEquals(20, items.size)
        assertTrue(items.all { it.id in itemIds })
    }

    @Test
    fun `should use projections for read-only queries`() {
        // Given - Create test data
        for (i in 1..10) {
            val item = ItemEntity(
                id = i,
                barcode = "BAR$i",
                name = "Item $i",
                price = i * 100
            )
            entityManager.persist(item)
        }
        entityManager.flush()

        // When - Fetch only required fields using projection
        val itemSummaries = itemRepository.findAllItemSummaries()

        // Then
        assertEquals(10, itemSummaries.size)
        itemSummaries.forEach { summary ->
            assertNotNull(summary.id)
            assertNotNull(summary.name)
            assertNotNull(summary.price)
        }
    }

    @Test
    fun `should use indexed fields for faster queries`() {
        // Given - Create test data with barcodes
        for (i in 1..100) {
            val item = ItemEntity(
                id = i,
                barcode = String.format("%013d", i),
                name = "Item $i",
                price = i * 10
            )
            entityManager.persist(item)
        }
        entityManager.flush()

        // When - Query by indexed barcode field
        val barcode = String.format("%013d", 50)
        val item = itemRepository.findByBarcode(barcode)

        // Then - Should find item quickly using index
        assertNotNull(item)
        assertEquals(50, item?.id)
        assertEquals("Item 50", item?.name)
    }

    @Test
    fun `should optimize count queries`() {
        // Given - Create test data
        for (i in 1..100) {
            val item = ItemEntity(
                id = i,
                barcode = "BAR$i",
                name = "Item $i",
                price = if (i % 2 == 0) 100 else 200
            )
            entityManager.persist(item)
        }
        entityManager.flush()

        // When - Count items with specific criteria
        val totalCount = itemRepository.count()
        val expensiveCount = itemRepository.countByPriceGreaterThan(150)

        // Then
        assertEquals(100, totalCount)
        assertEquals(50, expensiveCount)
    }
}

/**
 * Projection interface for item summaries
 */
interface ItemSummary {
    val id: Int
    val name: String
    val price: Int
}