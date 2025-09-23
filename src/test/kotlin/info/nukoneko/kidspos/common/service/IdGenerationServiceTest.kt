package info.nukoneko.kidspos.common.service

import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.SaleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.dao.EmptyResultDataAccessException

class IdGenerationServiceTest {

    @Mock
    private lateinit var itemRepository: ItemRepository

    @Mock
    private lateinit var saleRepository: SaleRepository

    private lateinit var idGenerationService: IdGenerationService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        idGenerationService = IdGenerationService()
    }

    @Test
    fun `should generate ID 1 when repository is empty`() {
        // Given
        `when`(itemRepository.getLastId()).thenThrow(EmptyResultDataAccessException::class.java)

        // When
        val newId = idGenerationService.generateNextId(itemRepository)

        // Then
        assertThat(newId).isEqualTo(1)
    }

    @Test
    fun `should generate next sequential ID when repository has items`() {
        // Given
        `when`(itemRepository.getLastId()).thenReturn(5)

        // When
        val newId = idGenerationService.generateNextId(itemRepository)

        // Then
        assertThat(newId).isEqualTo(6)
    }

    @Test
    fun `should work with different entity types`() {
        // Given - Sale repository with existing data
        `when`(saleRepository.getLastId()).thenReturn(100)

        // When
        val newSaleId = idGenerationService.generateNextId(saleRepository)

        // Then
        assertThat(newSaleId).isEqualTo(101)
    }

    @Test
    fun `should handle null or exceptions gracefully`() {
        // Given
        `when`(itemRepository.getLastId()).thenThrow(RuntimeException("Database error"))

        // When
        val newId = idGenerationService.generateNextId(itemRepository)

        // Then
        assertThat(newId).isEqualTo(1)
    }
}