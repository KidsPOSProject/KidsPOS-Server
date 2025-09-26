package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.request.CreateItemRequest
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.response.ItemResponse
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.service.ItemService
import info.nukoneko.kidspos.server.service.ValidationService
import info.nukoneko.kidspos.server.service.mapper.ItemMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus

@ExtendWith(MockitoExtension::class)
class ItemApiControllerUnitTest {
    @Mock
    private lateinit var itemService: ItemService

    @Mock
    private lateinit var itemMapper: ItemMapper

    @Mock
    private lateinit var validationService: ValidationService

    @InjectMocks
    private lateinit var controller: ItemApiController

    private lateinit var testItem: ItemEntity
    private lateinit var testItemResponse: ItemResponse

    @BeforeEach
    fun setup() {
        testItem =
            ItemEntity(
                id = 1,
                barcode = "123456789",
                name = "Test Item",
                price = 100,
            )

        testItemResponse =
            ItemResponse(
                id = 1,
                barcode = "123456789",
                name = "Test Item",
                price = 100,
            )
    }

    @Test
    fun `should get all items successfully`() {
        // Given
        val items = listOf(testItem)
        val responses = listOf(testItemResponse)

        `when`(itemService.findAll()).thenReturn(items)
        `when`(itemMapper.toResponseList(items)).thenReturn(responses)

        // When
        val result = controller.findAll()

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals(1, result.body?.size)
        assertEquals("Test Item", result.body?.first()?.name)

        verify(itemService).findAll()
        verify(itemMapper).toResponseList(items)
    }

    @Test
    fun `should get item by ID successfully`() {
        // Given
        `when`(itemService.findItem(1)).thenReturn(testItem)
        `when`(itemMapper.toResponse(testItem)).thenReturn(testItemResponse)

        // When
        val result = controller.findById(1)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals(1, result.body?.id)
        assertEquals("Test Item", result.body?.name)

        verify(itemService).findItem(1)
        verify(itemMapper).toResponse(testItem)
    }

    @Test
    fun `should throw exception when item not found by ID`() {
        // Given
        `when`(itemService.findItem(999)).thenReturn(null)

        // When & Then
        try {
            controller.findById(999)
            fail("Should have thrown ItemNotFoundException")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("not found") == true)
        }

        verify(itemService).findItem(999)
        verify(itemMapper, never()).toResponse(testItem)
    }

    @Test
    fun `should get item by barcode successfully`() {
        // Given
        val barcode = "123456789"
        `when`(itemService.findItem(barcode)).thenReturn(testItem)
        `when`(itemMapper.toResponse(testItem)).thenReturn(testItemResponse)

        // When
        val result = controller.findByBarcode(barcode)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals("123456789", result.body?.barcode)

        verify(itemService).findItem(barcode)
        verify(itemMapper).toResponse(testItem)
    }

    @Test
    fun `should throw exception for invalid barcode format`() {
        // Given
        val invalidBarcode = "abc"

        // When & Then
        try {
            controller.findByBarcode(invalidBarcode)
            fail("Should have thrown InvalidBarcodeException")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Invalid barcode") == true)
        }

        verify(itemService, never()).findItem(anyString())
    }

    @Test
    fun `should create item successfully`() {
        // Given
        val request =
            CreateItemRequest(
                name = "New Item",
                barcode = "987654321",
                price = 200,
            )

        val savedItem =
            ItemEntity(
                id = 2,
                barcode = "987654321",
                name = "New Item",
                price = 200,
            )

        val savedResponse =
            ItemResponse(
                id = 2,
                barcode = "987654321",
                name = "New Item",
                price = 200,
            )

        doNothing().`when`(validationService).validateBarcodeUnique(request.barcode)
        doNothing().`when`(validationService).validatePriceRange(request.price)

        // Create expected ItemBean for mocking
        val expectedItemBean =
            ItemBean(
                barcode = request.barcode,
                name = request.name,
                price = request.price,
            )
        `when`(itemService.save(expectedItemBean)).thenReturn(savedItem)
        `when`(itemMapper.toResponse(savedItem)).thenReturn(savedResponse)

        // When
        val result = controller.create(request)

        // Then
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertNotNull(result.body)
        assertEquals(2, result.body?.id)
        assertEquals("New Item", result.body?.name)

        verify(validationService).validateBarcodeUnique(request.barcode)
        verify(validationService).validatePriceRange(request.price)
        verify(itemService).save(expectedItemBean)
        verify(itemMapper).toResponse(savedItem)
    }

    @Test
    fun `should update item successfully`() {
        // Given
        val request =
            CreateItemRequest(
                name = "Updated Item",
                barcode = "123456789",
                price = 150,
            )

        val updatedItem =
            ItemEntity(
                id = 1,
                barcode = "123456789",
                name = "Updated Item",
                price = 150,
            )

        val updatedResponse =
            ItemResponse(
                id = 1,
                barcode = "123456789",
                name = "Updated Item",
                price = 150,
            )

        `when`(itemService.findItem(1)).thenReturn(testItem)
        doNothing().`when`(validationService).validateBarcodeUnique("123456789", 1)
        doNothing().`when`(validationService).validatePriceRange(150)

        // Create expected ItemBean for mocking
        val expectedItemBean =
            ItemBean(
                id = 1,
                barcode = request.barcode,
                name = request.name,
                price = request.price,
            )
        `when`(itemService.save(expectedItemBean)).thenReturn(updatedItem)
        `when`(itemMapper.toResponse(updatedItem)).thenReturn(updatedResponse)

        // When
        val result = controller.update(1, request)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals("Updated Item", result.body?.name)
        assertEquals(150, result.body?.price)

        verify(itemService).findItem(1)
        verify(validationService).validateBarcodeUnique("123456789", 1)
        verify(validationService).validatePriceRange(150)
        verify(itemService).save(expectedItemBean)
        verify(itemMapper).toResponse(updatedItem)
    }

    @Test
    fun `should delete item successfully`() {
        // Given
        doNothing().`when`(validationService).validateItemExists(1)

        // When
        val result = controller.delete(1)

        // Then
        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
        assertNull(result.body)

        verify(validationService).validateItemExists(1)
    }

    @Test
    fun `should throw exception when updating non-existent item`() {
        // Given
        val request =
            CreateItemRequest(
                name = "Updated Item",
                barcode = "123456789",
                price = 150,
            )

        `when`(itemService.findItem(999)).thenReturn(null)

        // When & Then
        try {
            controller.update(999, request)
            fail("Should have thrown ItemNotFoundException")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("not found") == true)
        }

        verify(itemService).findItem(999)
    }

    @Test
    fun `should throw exception when deleting non-existent item`() {
        // Given
        doThrow(RuntimeException("Item not found")).`when`(validationService).validateItemExists(999)

        // When & Then
        try {
            controller.delete(999)
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("not found") == true)
        }

        verify(validationService).validateItemExists(999)
    }
}
