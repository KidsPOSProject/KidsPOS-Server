package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.common.service.IdGenerationService
import info.nukoneko.kidspos.server.controller.dto.request.StoreBean
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import java.util.*

@SpringBootTest
class StoreServiceTest {

    @MockBean
    private lateinit var storeRepository: StoreRepository

    @MockBean
    private lateinit var idGenerationService: IdGenerationService

    private lateinit var storeService: StoreService

    @BeforeEach
    fun setup() {
        storeService = StoreService(storeRepository, idGenerationService)
    }

    @Test
    fun `should find store by ID when store exists`() {
        // Given
        val storeId = 1
        val expectedStore = StoreEntity(
            id = storeId,
            name = "Test Store",
            printerUri = "192.168.1.100"
        )
        `when`(storeRepository.findById(storeId)).thenReturn(Optional.of(expectedStore))

        // When
        val result = storeService.findStore(storeId)

        // Then
        assertNotNull(result)
        assertEquals(storeId, result?.id)
        assertEquals("Test Store", result?.name)
        assertEquals("192.168.1.100", result?.printerUri)
        verify(storeRepository).findById(storeId)
    }

    @Test
    fun `should return null when store does not exist by ID`() {
        // Given
        val storeId = 999
        `when`(storeRepository.findById(storeId)).thenReturn(Optional.empty())

        // When
        val result = storeService.findStore(storeId)

        // Then
        assertNull(result)
        verify(storeRepository).findById(storeId)
    }

    @Test
    fun `should save store successfully`() {
        // Given
        val storeBean = StoreBean(
            name = "New Store",
            printerUri = "192.168.1.200"
        )
        val expectedStore = StoreEntity(
            id = 1,
            name = "New Store",
            printerUri = "192.168.1.200"
        )
        `when`(idGenerationService.generateNextId(storeRepository)).thenReturn(1)
        `when`(storeRepository.save(any<StoreEntity>())).thenReturn(expectedStore)

        // When
        val result = storeService.save(storeBean)

        // Then
        assertNotNull(result)
        assertEquals(1, result.id)
        assertEquals("New Store", result.name)
        assertEquals("192.168.1.200", result.printerUri)
        verify(idGenerationService).generateNextId(storeRepository)
        verify(storeRepository).save(any<StoreEntity>())
    }

    @Test
    fun `should find all stores`() {
        // Given
        val expectedStores = listOf(
            StoreEntity(1, "Store 1", "192.168.1.100"),
            StoreEntity(2, "Store 2", "192.168.1.200")
        )
        `when`(storeRepository.findAll()).thenReturn(expectedStores)

        // When
        val result = storeService.findAll()

        // Then
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Store 1", result[0].name)
        assertEquals("Store 2", result[1].name)
        verify(storeRepository).findAll()
    }

    @Test
    fun `should save store with empty printer URI`() {
        // Given
        val storeBean = StoreBean(
            name = "Store without printer",
            printerUri = ""
        )
        val expectedStore = StoreEntity(
            id = 2,
            name = "Store without printer",
            printerUri = ""
        )
        `when`(idGenerationService.generateNextId(storeRepository)).thenReturn(2)
        `when`(storeRepository.save(any<StoreEntity>())).thenReturn(expectedStore)

        // When
        val result = storeService.save(storeBean)

        // Then
        assertNotNull(result)
        assertEquals(2, result.id)
        assertEquals("Store without printer", result.name)
        assertEquals("", result.printerUri)
        verify(idGenerationService).generateNextId(storeRepository)
        verify(storeRepository).save(any<StoreEntity>())
    }
}