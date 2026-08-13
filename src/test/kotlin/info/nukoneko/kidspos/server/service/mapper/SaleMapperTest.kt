package info.nukoneko.kidspos.server.service.mapper

import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.SaleDetailRepository
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SaleMapperTest {
    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var itemRepository: ItemRepository

    @Mock
    private lateinit var saleDetailRepository: SaleDetailRepository

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

    @Test
    fun `saleTime should preserve the exact instant of createdAt`() {
        `when`(storeRepository.findById(10)).thenReturn(Optional.of(StoreEntity(10, "Store 10")))
        `when`(saleDetailRepository.findBySaleId(1)).thenReturn(emptyList())

        val response = mapper.toResponse(sale)

        assertEquals(createdAt, response.saleTime.toInstant())
    }

    @Test
    fun `saleTime should carry the system timezone offset`() {
        `when`(storeRepository.findById(10)).thenReturn(Optional.of(StoreEntity(10, "Store 10")))
        `when`(saleDetailRepository.findBySaleId(1)).thenReturn(emptyList())

        val response = mapper.toResponse(sale)

        assertEquals(
            ZoneId.systemDefault().rules.getOffset(createdAt),
            response.saleTime.offset,
        )
    }

    @Test
    fun `should resolve store name and sale items`() {
        `when`(storeRepository.findById(10)).thenReturn(Optional.of(StoreEntity(10, "Store 10")))
        `when`(saleDetailRepository.findBySaleId(1)).thenReturn(
            listOf(SaleDetailEntity(id = 1, saleId = 1, itemId = 100, price = 150, quantity = 2)),
        )
        `when`(itemRepository.findById(100)).thenReturn(
            Optional.of(ItemEntity(100, "A01000100A", "Item 100", 150)),
        )

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
        `when`(storeRepository.findById(10)).thenReturn(Optional.empty())
        `when`(saleDetailRepository.findBySaleId(1)).thenReturn(
            listOf(SaleDetailEntity(id = 1, saleId = 1, itemId = 100, price = 150, quantity = 1)),
        )
        `when`(itemRepository.findById(100)).thenReturn(Optional.empty())

        val response = mapper.toResponse(sale)

        assertEquals("Unknown Store", response.storeName)
        assertEquals("Unknown", response.items[0].itemName)
        assertEquals("", response.items[0].barcode)
    }

    @Test
    fun `toResponseList should map every entity`() {
        val other = sale.copy(id = 2, storeId = 10)
        `when`(storeRepository.findById(10)).thenReturn(Optional.of(StoreEntity(10, "Store 10")))
        `when`(saleDetailRepository.findBySaleId(1)).thenReturn(emptyList())
        `when`(saleDetailRepository.findBySaleId(2)).thenReturn(emptyList())

        val responses = mapper.toResponseList(listOf(sale, other))

        assertEquals(listOf(1, 2), responses.map { it.id })
        assertEquals(listOf(createdAt, createdAt), responses.map { it.saleTime.toInstant() })
    }
}
