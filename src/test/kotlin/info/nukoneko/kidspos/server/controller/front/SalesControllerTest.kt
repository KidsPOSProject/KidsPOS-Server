package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.controller.dto.response.SaleReportDetailData
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.service.SaleAggregationService
import info.nukoneko.kidspos.server.service.SaleService
import info.nukoneko.kidspos.server.service.StoreService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import java.util.Calendar
import java.util.Date

@ExtendWith(SpringExtension::class)
@WebMvcTest(SalesController::class)
@DisplayName("SalesController")
class SalesControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var saleService: SaleService

    @MockBean
    private lateinit var storeService: StoreService

    @MockBean
    private lateinit var saleAggregationService: SaleAggregationService

    @Test
    @DisplayName("売上一覧を新しい順に表示する")
    fun showsSalesOrderedByNewest() {
        whenever(saleService.findAllSale()).thenReturn(
            listOf(
                sale(id = 1, createdAt = dateOf(2026, 8, 20, 10)),
                sale(id = 2, createdAt = dateOf(2026, 8, 21, 10)),
            ),
        )
        whenever(storeService.findAll()).thenReturn(listOf(StoreEntity(id = 1, name = "本店")))
        whenever(saleAggregationService.dailyComparison(any())).thenReturn(comparison())

        val result =
            mockMvc
                .perform(get("/sales"))
                .andExpect(status().isOk)
                .andExpect(view().name("sales/index"))
                .andExpect(model().attribute("title", "売上管理"))
                .andExpect(model().attribute("storeNames", mapOf(1 to "本店")))
                .andExpect(model().attributeExists("stats"))
                .andReturn()

        @Suppress("UNCHECKED_CAST")
        val data = result.modelAndView?.model?.get("data") as List<SaleEntity>
        assert(data.map { it.id } == listOf(2, 1))
    }

    @Test
    @DisplayName("取引詳細を表示する")
    fun showsSaleDetail() {
        whenever(saleAggregationService.findSaleDetail(eq(1))).thenReturn(
            SaleAggregationService.SaleDetailView(
                sale = sale(id = 1, createdAt = dateOf(2026, 8, 21, 10)),
                storeName = "本店",
                details =
                    listOf(
                        SaleReportDetailData(
                            itemId = 1,
                            itemName = "りんご",
                            price = 100,
                            quantity = 2,
                            subtotal = 200,
                        ),
                    ),
            ),
        )

        mockMvc
            .perform(get("/sales/1"))
            .andExpect(status().isOk)
            .andExpect(view().name("sales/detail"))
            .andExpect(model().attribute("title", "取引詳細"))
            .andExpect(model().attributeExists("detail"))
    }

    @Test
    @DisplayName("存在しない取引は一覧へ戻す")
    fun redirectsWhenSaleIsMissing() {
        whenever(saleAggregationService.findSaleDetail(eq(999))).thenReturn(null)

        mockMvc
            .perform(get("/sales/999"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/sales"))
    }

    private fun comparison() =
        SaleAggregationService.DailyComparison(
            date = dateOf(2026, 8, 21, 0),
            totalAmount = 400,
            salesCount = 2,
            averageAmount = 200,
            previousAmount = 200,
            previousSalesCount = 1,
            changeRatio = 1.0,
        )

    private fun sale(
        id: Int,
        createdAt: Date,
    ) = SaleEntity(
        id = id,
        storeId = 1,
        quantity = 1,
        amount = 100,
        deposit = 100,
        createdAt = createdAt,
    )

    private fun dateOf(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
    ): Date =
        Calendar
            .getInstance()
            .apply {
                clear()
                set(year, month - 1, day, hour, 0, 0)
            }.time
}
