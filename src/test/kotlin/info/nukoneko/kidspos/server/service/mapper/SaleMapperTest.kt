package info.nukoneko.kidspos.server.service.mapper

import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.service.ItemService
import info.nukoneko.kidspos.server.service.SalePersistenceService
import info.nukoneko.kidspos.server.service.StoreService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.time.Instant
import java.time.ZoneId
import java.util.Date

@ExtendWith(MockitoExtension::class)
class SaleMapperTest {
    @Mock
    private lateinit var storeService: StoreService

    @Mock
    private lateinit var itemService: ItemService

    @Mock
    private lateinit var salePersistenceService: SalePersistenceService

    @InjectMocks
    private lateinit var mapper: SaleMapper

    private val createdAt: Instant = Instant.parse("2026-08-13T05:43:22.947Z")

    private lateinit var sale: SaleEntity

    @BeforeEach
    fun setup() {
        sale =
            SaleEntity(
                id = 1,
                storeId = 10,
                quantity = 3,
                amount = 300,
                deposit = 500,
                createdAt = Date.from(createdAt),
            )
    }

    private fun stubStore(vararg stores: StoreEntity) {
        `when`(storeService.findAll()).thenReturn(stores.toList())
    }

    private fun stubItems(vararg items: ItemEntity) {
        `when`(itemService.findAll()).thenReturn(items.toList())
    }

    private fun stubDetails(vararg details: SaleDetailEntity) {
        `when`(salePersistenceService.findSaleDetails(any())).thenReturn(details.toList())
    }

    @Test
    fun `saleTime should preserve the exact instant of createdAt`() {
        stubStore(StoreEntity(10, "Store 10"))
        stubItems()
        stubDetails()

        val response = mapper.toResponse(sale)

        assertEquals(createdAt, response.saleTime.toInstant())
    }

    @Test
    fun `saleTime should carry the system timezone offset`() {
        stubStore(StoreEntity(10, "Store 10"))
        stubItems()
        stubDetails()

        val response = mapper.toResponse(sale)

        assertEquals(
            ZoneId.systemDefault().rules.getOffset(createdAt),
            response.saleTime.offset,
        )
    }

    @Test
    fun `should resolve store name and sale items`() {
        stubStore(StoreEntity(10, "Store 10"))
        stubItems(ItemEntity(100, "A01000100A", "Item 100", 150))
        stubDetails(SaleDetailEntity(id = 1, saleId = 1, itemId = 100, price = 150, quantity = 2))

        val response = mapper.toResponse(sale)

        assertEquals("Store 10", response.storeName)
        assertEquals(200, response.change)
        assertEquals(1, response.items.size)
        assertEquals("Item 100", response.items[0].itemName)
        assertEquals("A01000100A", response.items[0].barcode)
        assertEquals(300, response.items[0].subtotal)
        assertEquals(2, response.totalItems)
    }

    @Test
    fun `should fall back to placeholders when store and item are missing`() {
        stubStore()
        stubItems()
        stubDetails(SaleDetailEntity(id = 1, saleId = 1, itemId = 100, price = 150, quantity = 1))

        val response = mapper.toResponse(sale)

        assertEquals("Unknown Store", response.storeName)
        assertEquals("Unknown", response.items[0].itemName)
        assertEquals("", response.items[0].barcode)
    }

    @Test
    fun `toResponseList should map every entity`() {
        stubStore(StoreEntity(10, "Store 10"))
        stubItems()
        stubDetails()

        val responses = mapper.toResponseList(listOf(sale, sale.copy(id = 2, storeId = 10)))

        assertEquals(listOf(1, 2), responses.map { it.id })
        assertEquals(listOf(createdAt, createdAt), responses.map { it.saleTime.toInstant() })
    }

    @Test
    fun `toResponseList should fetch each relation only once`() {
        stubStore(StoreEntity(10, "Store 10"))
        stubItems()
        stubDetails()

        mapper.toResponseList(listOf(sale, sale.copy(id = 2), sale.copy(id = 3)))

        verify(salePersistenceService, times(1)).findSaleDetails(any())
        verify(storeService, times(1)).findAll()
        verify(itemService, times(1)).findAll()
    }
}
