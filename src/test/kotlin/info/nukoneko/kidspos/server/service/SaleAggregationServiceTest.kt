package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.SaleDetailRepository
import info.nukoneko.kidspos.server.repository.SaleRepository
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date

@ExtendWith(MockitoExtension::class)
class SaleAggregationServiceTest {
    @Mock
    private lateinit var saleRepository: SaleRepository

    @Mock
    private lateinit var saleDetailRepository: SaleDetailRepository

    @Mock
    private lateinit var itemRepository: ItemRepository

    @Mock
    private lateinit var storeRepository: StoreRepository

    private lateinit var service: SaleAggregationService

    @BeforeEach
    fun setUp() {
        service =
            SaleAggregationService(
                saleRepository,
                saleDetailRepository,
                itemRepository,
                storeRepository,
            )
    }

    @Test
    fun `終了日が日付のみでも当日の終端まで検索範囲を広げる`() {
        stubEmptySales()

        service.aggregate(dateOf(2026, 8, 20), dateOf(2026, 8, 21))

        val start = argumentCaptor<Date>()
        val end = argumentCaptor<Date>()
        verify(saleRepository).findByDateRange(start.capture(), end.capture())

        assertEquals(dateOf(2026, 8, 20, 0, 0, 0, 0), start.firstValue)
        assertEquals(dateOf(2026, 8, 21, 23, 59, 59, 999), end.firstValue)
    }

    @Test
    fun `終了日当日の売上が集計に含まれる`() {
        val saleOnEndDate =
            SaleEntity(
                id = 2,
                storeId = 1,
                quantity = 1,
                amount = 100,
                deposit = 100,
                createdAt = dateOf(2026, 8, 21, 23, 30, 0, 0),
            )
        stubSales(listOf(saleOnEndDate))
        stubDetails(listOf(detail(id = 10, saleId = 2, itemId = 1, price = 100, quantity = 1)))
        stubItems(listOf(ItemEntity(id = 1, barcode = "0001", name = "りんご", price = 100)))
        stubStores(listOf(StoreEntity(id = 1, name = "本店")))

        val aggregation = service.aggregate(dateOf(2026, 8, 20), dateOf(2026, 8, 21))

        assertEquals(1, aggregation.summary.totalSales)
        assertEquals(100, aggregation.summary.totalAmount)
        assertEquals(1, aggregation.sales.size)
    }

    @Test
    fun `合計金額と販売点数と平均客単価を明細から集計する`() {
        stubAllSales()

        val summary = service.aggregate(dateOf(2026, 8, 20), dateOf(2026, 8, 21)).summary

        assertEquals(3, summary.totalSales)
        assertEquals(6, summary.totalItemCount)
        assertEquals(650, summary.totalAmount)
        assertEquals(650.0 / 3, summary.averageAmount, 0.001)
    }

    @Test
    fun `明細の小計は単価かける数量で計算される`() {
        stubAllSales()

        val sale = service.aggregate(dateOf(2026, 8, 20), dateOf(2026, 8, 21)).sales.first { it.saleId == 1 }
        val detail = sale.details.first { it.itemId == 1 }

        assertEquals(100, detail.price)
        assertEquals(2, detail.quantity)
        assertEquals(200, detail.subtotal)
    }

    @Test
    fun `店舗を指定すると該当店舗の売上だけを集計する`() {
        stubSales(allSales())
        stubDetails(allDetails().filter { it.saleId == 3 })
        stubItems(allItems())
        stubStores(allStores())

        val aggregation = service.aggregate(dateOf(2026, 8, 20), dateOf(2026, 8, 21), storeId = 2)

        assertEquals(1, aggregation.summary.totalSales)
        assertEquals(250, aggregation.summary.totalAmount)
        assertTrue(aggregation.sales.all { it.storeId == 2 })
    }

    @Test
    fun `売上が無い期間では平均客単価が 0 になり明細を読み込まない`() {
        stubEmptySales()

        val aggregation = service.aggregate(dateOf(2026, 8, 20), dateOf(2026, 8, 21))

        assertEquals(0, aggregation.summary.totalSales)
        assertEquals(0, aggregation.summary.totalItemCount)
        assertEquals(0, aggregation.summary.totalAmount)
        assertEquals(0.0, aggregation.summary.averageAmount, 0.001)
        verify(saleDetailRepository, never()).findBySaleIdIn(any())
    }

    @Test
    fun `商品別集計は金額の多い順に並ぶ`() {
        stubAllSales()

        val items = service.summaryResponse(dateOf(2026, 8, 20), dateOf(2026, 8, 21)).items

        assertEquals(listOf(1, 2, 3), items.map { it.itemId })
        assertEquals(listOf(300, 200, 150), items.map { it.amount })
        assertEquals(listOf(3, 2, 1), items.map { it.quantity })
        assertEquals("りんご", items.first().itemName)
    }

    @Test
    fun `店舗別集計は金額の多い順に並ぶ`() {
        stubAllSales()

        val stores = service.summaryResponse(dateOf(2026, 8, 20), dateOf(2026, 8, 21)).stores

        assertEquals(listOf(1, 2), stores.map { it.storeId })
        assertEquals(listOf(400, 250), stores.map { it.amount })
        assertEquals(listOf(2, 1), stores.map { it.salesCount })
        assertEquals(listOf(4, 2), stores.map { it.itemCount })
    }

    @Test
    fun `日別集計は日付の昇順に並ぶ`() {
        stubAllSales()

        val daily = service.summaryResponse(dateOf(2026, 8, 20), dateOf(2026, 8, 21)).daily

        assertEquals(listOf("2026-08-20", "2026-08-21"), daily.map { it.date })
        assertEquals(listOf(1, 2), daily.map { it.salesCount })
        assertEquals(listOf(300, 350), daily.map { it.amount })
        assertEquals(listOf(3, 3), daily.map { it.itemCount })
    }

    @Test
    fun `マスタから消えた商品と店舗は不明として扱う`() {
        stubSales(listOf(allSales().first()))
        stubDetails(allDetails().filter { it.saleId == 1 })
        stubItems(emptyList())
        stubStores(emptyList())

        val response = service.summaryResponse(dateOf(2026, 8, 20), dateOf(2026, 8, 21))

        assertTrue(response.items.all { it.itemName == "不明な商品" })
        assertTrue(response.stores.all { it.storeName == "不明な店舗" })
    }

    @Test
    fun `summaryResponse は補正済みの期間を返す`() {
        stubEmptySales()

        val response = service.summaryResponse(dateOf(2026, 8, 20), dateOf(2026, 8, 21))

        assertEquals(dateOf(2026, 8, 20, 0, 0, 0, 0), response.startDate)
        assertEquals(dateOf(2026, 8, 21, 23, 59, 59, 999), response.endDate)
    }

    private fun stubAllSales() {
        stubSales(allSales())
        stubDetails(allDetails())
        stubItems(allItems())
        stubStores(allStores())
    }

    private fun stubEmptySales() {
        whenever(saleRepository.findByDateRange(any(), any())).thenReturn(emptyList())
    }

    private fun stubSales(sales: List<SaleEntity>) {
        whenever(saleRepository.findByDateRange(any(), any())).thenReturn(sales)
    }

    private fun stubDetails(details: List<SaleDetailEntity>) {
        whenever(saleDetailRepository.findBySaleIdIn(any())).thenReturn(details)
    }

    private fun stubItems(items: List<ItemEntity>) {
        whenever(itemRepository.findAllById(any())).thenReturn(items)
    }

    private fun stubStores(stores: List<StoreEntity>) {
        whenever(storeRepository.findAllById(any())).thenReturn(stores)
    }

    private fun allSales() =
        listOf(
            SaleEntity(
                id = 1,
                storeId = 1,
                quantity = 3,
                amount = 300,
                deposit = 500,
                createdAt = dateOf(2026, 8, 20, 10, 0, 0, 0),
            ),
            SaleEntity(
                id = 2,
                storeId = 1,
                quantity = 1,
                amount = 100,
                deposit = 100,
                createdAt = dateOf(2026, 8, 21, 23, 30, 0, 0),
            ),
            SaleEntity(
                id = 3,
                storeId = 2,
                quantity = 2,
                amount = 250,
                deposit = 300,
                createdAt = dateOf(2026, 8, 21, 9, 0, 0, 0),
            ),
        )

    private fun allDetails() =
        listOf(
            detail(id = 1, saleId = 1, itemId = 1, price = 100, quantity = 2),
            detail(id = 2, saleId = 1, itemId = 2, price = 100, quantity = 1),
            detail(id = 3, saleId = 2, itemId = 1, price = 100, quantity = 1),
            detail(id = 4, saleId = 3, itemId = 2, price = 100, quantity = 1),
            detail(id = 5, saleId = 3, itemId = 3, price = 150, quantity = 1),
        )

    private fun allItems() =
        listOf(
            ItemEntity(id = 1, barcode = "0001", name = "りんご", price = 100),
            ItemEntity(id = 2, barcode = "0002", name = "みかん", price = 100),
            ItemEntity(id = 3, barcode = "0003", name = "ぶどう", price = 150),
        )

    private fun allStores() =
        listOf(
            StoreEntity(id = 1, name = "本店"),
            StoreEntity(id = 2, name = "支店"),
        )

    private fun detail(
        id: Int,
        saleId: Int,
        itemId: Int,
        price: Int,
        quantity: Int,
    ) = SaleDetailEntity(id = id, saleId = saleId, itemId = itemId, price = price, quantity = quantity)

    private fun dateOf(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        millis: Int = 0,
    ): Date =
        Calendar
            .getInstance()
            .apply {
                clear()
                set(year, month - 1, day, hour, minute, second)
                set(Calendar.MILLISECOND, millis)
            }.time
}
