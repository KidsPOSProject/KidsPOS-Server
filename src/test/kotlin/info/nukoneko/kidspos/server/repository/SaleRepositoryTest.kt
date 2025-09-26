package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.SaleEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import java.util.*

@DataJpaTest
@Disabled("Spring context not configured")
class SaleRepositoryTest {
    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var saleRepository: SaleRepository

    private lateinit var testSale1: SaleEntity
    private lateinit var testSale2: SaleEntity
    private lateinit var testSale3: SaleEntity
    private lateinit var testDate: Date

    @BeforeEach
    fun setup() {
        testDate = Date()

        // Create test data
        testSale1 =
            SaleEntity(
                id = 0,
                storeId = 1,
                staffId = 1,
                quantity = 2,
                amount = 300,
                deposit = 400,
                createdAt = Date(testDate.time - 86400000), // 1 day ago
            )
        testSale2 =
            SaleEntity(
                id = 0,
                storeId = 1,
                staffId = 2,
                quantity = 3,
                amount = 500,
                deposit = 500,
                createdAt = Date(testDate.time - 3600000), // 1 hour ago
            )
        testSale3 =
            SaleEntity(
                id = 0,
                storeId = 2,
                staffId = 1,
                quantity = 1,
                amount = 200,
                deposit = 300,
                createdAt = testDate,
            )

        // Persist test data
        testSale1 = entityManager.persistAndFlush(testSale1)
        testSale2 = entityManager.persistAndFlush(testSale2)
        testSale3 = entityManager.persistAndFlush(testSale3)
    }

    @Test
    fun `should find all sales`() {
        // When
        val allSales = saleRepository.findAll()

        // Then
        assertNotNull(allSales)
        assertEquals(3, allSales.size)
    }

    @Test
    fun `should save new sale`() {
        // Given
        val newSale =
            SaleEntity(
                id = 0,
                storeId = 3,
                staffId = 3,
                quantity = 4,
                amount = 600,
                deposit = 700,
                createdAt = Date(),
            )

        // When
        val savedSale = saleRepository.save(newSale)

        // Then
        assertNotNull(savedSale)
        assertTrue(savedSale.id > 0)
        assertEquals(3, savedSale.storeId)
        assertEquals(600, savedSale.amount)

        // Verify persistence
        entityManager.clear()
        val foundSale = saleRepository.findById(savedSale.id)
        assertTrue(foundSale.isPresent)
        assertEquals(600, foundSale.get().amount)
    }

    @Test
    fun `should find sale by ID with details`() {
        // When
        val foundSale = saleRepository.findByIdWithDetails(testSale1.id)

        // Then
        assertNotNull(foundSale)
        assertEquals(testSale1.id, foundSale?.id)
        assertEquals(300, foundSale?.amount)
        assertEquals(1, foundSale?.storeId)
    }

    @Test
    fun `should return null when sale not found by ID with details`() {
        // When
        val foundSale = saleRepository.findByIdWithDetails(9999)

        // Then
        assertNull(foundSale)
    }

    @Test
    fun `should find sales by store ID with pagination`() {
        // Given
        val pageRequest = PageRequest.of(0, 2)

        // When
        val salesPage = saleRepository.findByStoreId(1, pageRequest)

        // Then
        assertNotNull(salesPage)
        assertEquals(2, salesPage.size) // Both testSale1 and testSale2 have storeId = 1
        assertEquals(2, salesPage.totalElements)
        assertEquals(1, salesPage.totalPages)
        assertFalse(salesPage.hasNext())
    }

    @Test
    fun `should find sales by date range`() {
        // Given
        val startDate = Date(testDate.time - 86400000 * 2) // 2 days ago
        val endDate = Date(testDate.time + 3600000) // 1 hour from now

        // When
        val salesInRange = saleRepository.findByDateRange(startDate, endDate)

        // Then
        assertNotNull(salesInRange)
        assertEquals(3, salesInRange.size)

        // Should be ordered by createdAt DESC
        assertTrue(salesInRange[0].createdAt >= salesInRange[1].createdAt)
        assertTrue(salesInRange[1].createdAt >= salesInRange[2].createdAt)
    }

    @Test
    fun `should find sales summary by store`() {
        // Given
        val fromDate = Date(testDate.time - 86400000 * 2) // 2 days ago

        // When
        val salesSummaries = saleRepository.findSalesSummaryByStore(fromDate)

        // Then
        assertNotNull(salesSummaries)
        assertTrue(salesSummaries.isNotEmpty())

        val store1Summary = salesSummaries.find { it.storeId == 1 }
        assertNotNull(store1Summary)
        assertEquals(2, store1Summary?.totalSales) // testSale1 and testSale2
        assertEquals(800L, store1Summary?.totalAmount) // 300 + 500

        val store2Summary = salesSummaries.find { it.storeId == 2 }
        assertNotNull(store2Summary)
        assertEquals(1, store2Summary?.totalSales) // testSale3
        assertEquals(200L, store2Summary?.totalAmount)
    }

    @Test
    fun `should count sales by store ID`() {
        // When
        val store1Count = saleRepository.countByStoreId(1)
        val store2Count = saleRepository.countByStoreId(2)
        val store3Count = saleRepository.countByStoreId(3)

        // Then
        assertEquals(2, store1Count)
        assertEquals(1, store2Count)
        assertEquals(0, store3Count)
    }

    @Test
    fun `should get last ID correctly`() {
        // When
        val lastId = saleRepository.getLastId()

        // Then
        assertTrue(lastId > 0)
        // Should be one of our test sale IDs
        assertTrue(lastId == testSale1.id || lastId == testSale2.id || lastId == testSale3.id)
    }

    @Test
    fun `should delete sale by ID`() {
        // Given
        val saleIdToDelete = testSale1.id

        // When
        saleRepository.deleteById(saleIdToDelete)

        // Then
        val foundSale = saleRepository.findById(saleIdToDelete)
        assertFalse(foundSale.isPresent)

        // Verify other sales still exist
        val remainingSales = saleRepository.findAll()
        assertEquals(2, remainingSales.size)
    }

    @Test
    fun `should update existing sale by creating new instance`() {
        // Given - create a copy with updated amount
        val updatedSale =
            SaleEntity(
                id = testSale1.id,
                storeId = testSale1.storeId,
                staffId = testSale1.staffId,
                quantity = testSale1.quantity,
                amount = 350,
                deposit = testSale1.deposit,
                createdAt = testSale1.createdAt,
            )

        // When
        val savedSale = saleRepository.save(updatedSale)

        // Then
        assertNotNull(savedSale)
        assertEquals(testSale1.id, savedSale.id)
        assertEquals(350, savedSale.amount)

        // Verify persistence
        entityManager.clear()
        val foundSale = saleRepository.findById(savedSale.id)
        assertTrue(foundSale.isPresent)
        assertEquals(350, foundSale.get().amount)
    }
}
