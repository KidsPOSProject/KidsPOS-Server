package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.service.StoreService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class StoreApiControllerUnitTest {

    @Mock
    private lateinit var storeService: StoreService

    @InjectMocks
    private lateinit var controller: StoreApiController

    @Test
    fun `getStores should return list of stores from service`() {
        // Arrange
        val store1 = StoreEntity(
            id = 1,
            name = "Store 1",
            printerUri = "http://printer1.local"
        )
        val store2 = StoreEntity(
            id = 2,
            name = "Store 2",
            printerUri = "http://printer2.local"
        )
        val expectedStores = listOf(store1, store2)
        `when`(storeService.findAll()).thenReturn(expectedStores)

        // Act
        val result = controller.getStores()

        // Assert
        assertNotNull(result)
        assertEquals(200, result.statusCodeValue)
        assertEquals(expectedStores, result.body)
        verify(storeService).findAll()
    }

    @Test
    fun `getStores should return empty list when no stores exist`() {
        // Arrange
        val expectedStores = emptyList<StoreEntity>()
        `when`(storeService.findAll()).thenReturn(expectedStores)

        // Act
        val result = controller.getStores()

        // Assert
        assertNotNull(result)
        assertEquals(200, result.statusCodeValue)
        assertTrue(result.body?.isEmpty() == true)
        verify(storeService).findAll()
    }
}