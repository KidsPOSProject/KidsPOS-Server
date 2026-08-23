package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.response.SaleReportData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportDetailData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportSummary
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import java.io.ByteArrayInputStream
import java.util.Calendar
import java.util.Date

@ExtendWith(MockitoExtension::class)
class SaleExcelReportServiceTest {
    @Mock
    private lateinit var saleAggregationService: SaleAggregationService

    private lateinit var service: SaleExcelReportService

    @BeforeEach
    fun setUp() {
        service = SaleExcelReportService(saleAggregationService)
    }

    @Test
    fun `サマリーと明細と商品別と日別のシートを出力する`() {
        stubAggregation(sales(), summary(totalSales = 2, totalItemCount = 4, totalAmount = 700))

        val workbook = openWorkbook(service.generateSalesExcelReport(startDate(), endDate()))

        assertEquals(4, workbook.numberOfSheets)
        assertNotNull(workbook.getSheet("サマリー"))
        assertNotNull(workbook.getSheet("売上明細"))
        assertNotNull(workbook.getSheet("商品別集計"))
        assertNotNull(workbook.getSheet("日別集計"))
        workbook.close()
    }

    @Test
    fun `日別集計シートは日付ごとの件数と販売点数と金額を出力する`() {
        stubAggregation(sales(), summary(totalSales = 2, totalItemCount = 4, totalAmount = 700))

        val workbook = openWorkbook(service.generateSalesExcelReport(startDate(), endDate()))
        val sheet = workbook.getSheet("日別集計")

        assertEquals("2026/08/20", sheet.getRow(1).getCell(0).stringCellValue)
        assertEquals(1.0, sheet.getRow(1).getCell(1).numericCellValue)
        assertEquals(3.0, sheet.getRow(1).getCell(2).numericCellValue)
        assertEquals(400.0, sheet.getRow(1).getCell(3).numericCellValue)

        assertEquals("2026/08/21", sheet.getRow(2).getCell(0).stringCellValue)
        assertEquals(1.0, sheet.getRow(2).getCell(1).numericCellValue)
        assertEquals(1.0, sheet.getRow(2).getCell(2).numericCellValue)
        assertEquals(300.0, sheet.getRow(2).getCell(3).numericCellValue)
        workbook.close()
    }

    @Test
    fun `商品別集計シートは金額の多い順に出力する`() {
        stubAggregation(sales(), summary(totalSales = 2, totalItemCount = 4, totalAmount = 700))

        val workbook = openWorkbook(service.generateSalesExcelReport(startDate(), endDate()))
        val sheet = workbook.getSheet("商品別集計")

        assertEquals("みかん", sheet.getRow(1).getCell(0).stringCellValue)
        assertEquals("りんご", sheet.getRow(2).getCell(0).stringCellValue)
        workbook.close()
    }

    @Test
    fun `売上が無い期間でもシートを出力する`() {
        stubAggregation(emptyList(), summary(totalSales = 0, totalItemCount = 0, totalAmount = 0))

        val workbook = openWorkbook(service.generateSalesExcelReport(startDate(), endDate()))

        assertEquals(4, workbook.numberOfSheets)
        workbook.close()
    }

    @Test
    fun `店舗指定の Excel は店舗を絞って集計する`() {
        stubAggregation(sales(), summary(totalSales = 2, totalItemCount = 4, totalAmount = 700))

        service.generateSalesExcelReportByStore(3, startDate(), endDate())

        verify(saleAggregationService).aggregate(any(), any(), eq(3))
    }

    private fun openWorkbook(bytes: ByteArray) = XSSFWorkbook(ByteArrayInputStream(bytes))

    private fun stubAggregation(
        data: List<SaleReportData>,
        summary: SaleReportSummary,
    ) {
        whenever(saleAggregationService.aggregate(any(), any(), anyOrNull()))
            .thenReturn(
                SaleAggregationService.Aggregation(
                    startDate = startDate(),
                    endDate = endDate(),
                    sales = data,
                    summary = summary,
                ),
            )
    }

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
            SaleReportData(
                saleId = 2,
                storeId = 1,
                storeName = "本店",
                quantity = 1,
                amount = 300,
                createdAt = endDate(),
                details =
                    listOf(
                        SaleReportDetailData(
                            itemId = 2,
                            itemName = "みかん",
                            price = 300,
                            quantity = 1,
                            subtotal = 300,
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
