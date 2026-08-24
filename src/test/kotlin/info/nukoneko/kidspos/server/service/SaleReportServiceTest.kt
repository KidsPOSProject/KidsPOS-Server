package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.response.SaleReportData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportDetailData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Calendar
import java.util.Date

@ExtendWith(MockitoExtension::class)
class SaleReportServiceTest {
    @Mock
    private lateinit var saleAggregationService: SaleAggregationService

    private lateinit var service: SaleReportService

    @BeforeEach
    fun setUp() {
        service = SaleReportService(saleAggregationService)
    }

    @Test
    fun `売上がある期間の PDF を生成できる`() {
        stubAggregation(sales(), summary(totalSales = 1, totalItemCount = 3, totalAmount = 400))

        val pdf = service.generateSalesReport(startDate(), endDate())

        assertTrue(pdf.isNotEmpty())
        assertEquals("%PDF", String(pdf.copyOfRange(0, 4)))
    }

    @Test
    fun `売上が無い期間でも PDF を生成できる`() {
        stubAggregation(emptyList(), summary(totalSales = 0, totalItemCount = 0, totalAmount = 0))

        val pdf = service.generateSalesReport(startDate(), endDate())

        assertEquals("%PDF", String(pdf.copyOfRange(0, 4)))
    }

    @Test
    fun `店舗指定の PDF は店舗を絞って集計する`() {
        whenever(saleAggregationService.aggregate(any(), any(), anyOrNull()))
            .thenReturn(aggregation(sales(), summary(totalSales = 1, totalItemCount = 3, totalAmount = 400)))

        service.generateSalesReportByStore(2, startDate(), endDate())

        verify(saleAggregationService).aggregate(any(), any(), eq(2))
    }

    private fun stubAggregation(
        data: List<SaleReportData>,
        summary: SaleReportSummary,
    ) {
        whenever(saleAggregationService.aggregate(any(), any(), anyOrNull()))
            .thenReturn(aggregation(data, summary))
    }

    private fun aggregation(
        data: List<SaleReportData>,
        summary: SaleReportSummary,
    ) = SaleAggregationService.Aggregation(
        startDate = startDate(),
        endDate = endDate(),
        sales = data,
        summary = summary,
    )

    private fun sales() =
        listOf(
            SaleReportData(
                saleId = 1,
                storeId = 1,
                storeName = "本店",
                quantity = 2,
                amount = 400,
                createdAt = startDate(),
                details =
                    listOf(
                        SaleReportDetailData(
                            itemId = 1,
                            itemName = "りんご",
                            price = 100,
                            quantity = 2,
                            subtotal = 200,
                        ),
                        SaleReportDetailData(
                            itemId = 2,
                            itemName = "みかん",
                            price = 200,
                            quantity = 1,
                            subtotal = 200,
                        ),
                    ),
            ),
        )

    private fun summary(
        totalSales: Int,
        totalItemCount: Int,
        totalAmount: Int,
    ) = SaleReportSummary(
        totalSales = totalSales,
        totalItemCount = totalItemCount,
        totalAmount = totalAmount,
        averageAmount = if (totalSales > 0) totalAmount.toDouble() / totalSales else 0.0,
        startDate = startDate(),
        endDate = endDate(),
    )

    private fun startDate(): Date = dateOf(2026, 8, 20)

    private fun endDate(): Date = dateOf(2026, 8, 21)

    private fun dateOf(
        year: Int,
        month: Int,
        day: Int,
    ): Date =
        Calendar
            .getInstance()
            .apply {
                clear()
                set(year, month - 1, day, 0, 0, 0)
            }.time
}
