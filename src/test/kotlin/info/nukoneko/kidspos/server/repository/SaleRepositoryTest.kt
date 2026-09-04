package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.SaleEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.util.*

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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

        // Clean up before each test
        saleRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()

        // Create test data
        testSale1 =
            SaleEntity(
                storeId = 1,
                quantity = 2,
                amount = 300,
                deposit = 400,
                createdAt = Date(testDate.time - 86400000), // 1 day ago
            )
        testSale2 =
            SaleEntity(
                storeId = 1,
                quantity = 3,
                amount = 500,
                deposit = 500,
                createdAt = Date(testDate.time - 3600000), // 1 hour ago
            )
        testSale3 =
            SaleEntity(
                storeId = 2,
                quantity = 1,
                amount = 200,
                deposit = 300,
                createdAt = testDate,
            )

        // Persist test data
        testSale1 = entityManager.persistAndFlush(testSale1)
        testSale2 = entityManager.persistAndFlush(testSale2)
        testSale3 = entityManager.persistAndFlush(testSale3)
        entityManager.clear()
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
                storeId = 3,
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
        entityManager.flush()
        entityManager.clear()
        val foundSale = saleRepository.findById(savedSale.id)
        assertTrue(foundSale.isPresent)
        assertEquals(600, foundSale.get().amount)
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
        entityManager.flush()
        entityManager.clear()
        val foundSale = saleRepository.findById(savedSale.id)
        assertTrue(foundSale.isPresent)
        assertEquals(350, foundSale.get().amount)
    }
}
