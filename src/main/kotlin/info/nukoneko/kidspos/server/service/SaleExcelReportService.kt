package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.response.SaleReportData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportSummary
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

@Service
@Transactional(readOnly = true)
class SaleExcelReportService(
    private val saleAggregationService: SaleAggregationService,
) {
    private val logger = LoggerFactory.getLogger(SaleExcelReportService::class.java)
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")

    fun generateSalesExcelReport(
        startDate: Date,
        endDate: Date,
    ): ByteArray {
        logger.info("Generating sales Excel report from {} to {}", startDate, endDate)

        val aggregation = saleAggregationService.aggregate(startDate, endDate)
        return createExcelReport(aggregation.sales, aggregation.summary)
    }

    fun generateSalesExcelReportByStore(
        storeId: Int,
        startDate: Date,
        endDate: Date,
    ): ByteArray {
        logger.info("Generating sales Excel report for store {} from {} to {}", storeId, startDate, endDate)

        val aggregation = saleAggregationService.aggregate(startDate, endDate, storeId)
        return createExcelReport(aggregation.sales, aggregation.summary)
    }

    private fun createExcelReport(
        reportData: List<SaleReportData>,
        summary: SaleReportSummary,
    ): ByteArray {
        val workbook = XSSFWorkbook()

        try {
            createSummarySheet(workbook, summary)
            createSalesDetailSheet(workbook, reportData)
            createItemSummarySheet(workbook, reportData)
            createDailySummarySheet(workbook, reportData)

            val outputStream = ByteArrayOutputStream()
            workbook.write(outputStream)
            workbook.close()

            logger.info("Excel report generated successfully")
            return outputStream.toByteArray()
        } catch (e: Exception) {
            logger.error("Error generating Excel report", e)
            workbook.close()
            throw RuntimeException("An error occurred while generating the Excel report", e)
        }
    }

    private fun createSummarySheet(
        workbook: XSSFWorkbook,
        summary: SaleReportSummary,
    ) {
        val sheet = workbook.createSheet("サマリー")

        val headerStyle = createHeaderStyle(workbook)
        val titleStyle = createTitleStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)
        val countStyle = createCountStyle(workbook)

        var rowNum = 0

        val titleRow = sheet.createRow(rowNum++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("売上レポート")
        titleCell.cellStyle = titleStyle
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 3))

        val periodRow = sheet.createRow(rowNum++)
        periodRow.createCell(0).setCellValue("期間:")
        periodRow.createCell(1).setCellValue("${dateFormat.format(summary.startDate)} ～ ${dateFormat.format(summary.endDate)}")
        sheet.addMergedRegion(CellRangeAddress(1, 1, 1, 3))

        rowNum++

        val summaryHeaderRow = sheet.createRow(rowNum++)
        val summaryHeader = summaryHeaderRow.createCell(0)
        summaryHeader.setCellValue("集計結果")
        summaryHeader.cellStyle = headerStyle
        sheet.addMergedRegion(CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3))

        val countRow = sheet.createRow(rowNum++)
        countRow.createCell(0).setCellValue("総売上件数")
        val countCell = countRow.createCell(1)
        countCell.setCellValue(summary.totalSales.toDouble())
        countCell.cellStyle = countStyle
        countRow.createCell(2).setCellValue("件")

        val itemCountRow = sheet.createRow(rowNum++)
        itemCountRow.createCell(0).setCellValue("総販売点数")
        val itemCountCell = itemCountRow.createCell(1)
        itemCountCell.setCellValue(summary.totalItemCount.toDouble())
        itemCountCell.cellStyle = countStyle
        itemCountRow.createCell(2).setCellValue("点")

        val amountRow = sheet.createRow(rowNum++)
        amountRow.createCell(0).setCellValue("総売上金額")
        val amountCell = amountRow.createCell(1)
        amountCell.setCellValue(summary.totalAmount.toDouble())
        amountCell.cellStyle = currencyStyle

        val avgRow = sheet.createRow(rowNum++)
        avgRow.createCell(0).setCellValue("平均売上金額")
        val avgCell = avgRow.createCell(1)
        avgCell.setCellValue(summary.averageAmount)
        avgCell.cellStyle = currencyStyle

        for (i in 0..3) {
            sheet.autoSizeColumn(i)
        }
    }

    private fun createSalesDetailSheet(
        workbook: XSSFWorkbook,
        reportData: List<SaleReportData>,
    ) {
        val sheet = workbook.createSheet("売上明細")

        val headerStyle = createHeaderStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)
        val dateStyle = createDateStyle(workbook)

        var rowNum = 0

        val headerRow = sheet.createRow(rowNum++)
        val headers = listOf("売上ID", "日時", "店舗名", "商品数", "金額", "商品明細")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        reportData.forEach { sale ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(sale.saleId.toDouble())

            val dateCell = row.createCell(1)
            dateCell.setCellValue(sale.createdAt)
            dateCell.cellStyle = dateStyle

            row.createCell(2).setCellValue(sale.storeName)
            row.createCell(3).setCellValue(sale.details.sumOf { it.quantity }.toDouble())

            val amountCell = row.createCell(4)
            amountCell.setCellValue(sale.amount.toDouble())
            amountCell.cellStyle = currencyStyle

            val detailText =
                if (sale.details.isNotEmpty()) {
                    sale.details.joinToString(", ") { detail ->
                        "${detail.itemName} x${detail.quantity}"
                    }
                } else {
                    "-"
                }
            row.createCell(5).setCellValue(detailText)
        }

        val totalRow = sheet.createRow(rowNum++)
        totalRow.createCell(2).setCellValue("合計")
        totalRow.createCell(3).setCellValue(reportData.sumOf { sale -> sale.details.sumOf { it.quantity } }.toDouble())
        val totalCell = totalRow.createCell(4)
        totalCell.setCellValue(reportData.sumOf { it.amount }.toDouble())
        totalCell.cellStyle = currencyStyle

        for (i in 0..5) {
            sheet.autoSizeColumn(i)
        }
    }

    private fun createItemSummarySheet(
        workbook: XSSFWorkbook,
        reportData: List<SaleReportData>,
    ) {
        val sheet = workbook.createSheet("商品別集計")

        val headerStyle = createHeaderStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)

        val itemSummary =
            reportData
                .flatMap { it.details }
                .groupBy { it.itemId }
                .map { (_, group) ->
                    ItemSummaryData(
                        itemName = group.first().itemName,
                        totalQuantity = group.sumOf { it.quantity },
                        totalAmount = group.sumOf { it.subtotal },
                        unitPrice = group.first().price,
                    )
                }.sortedByDescending { it.totalAmount }

        var rowNum = 0

        val headerRow = sheet.createRow(rowNum++)
        val headers = listOf("商品名", "単価", "販売数", "売上金額")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        itemSummary.forEach { item ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(item.itemName)

            val priceCell = row.createCell(1)
            priceCell.setCellValue(item.unitPrice.toDouble())
            priceCell.cellStyle = currencyStyle

            row.createCell(2).setCellValue(item.totalQuantity.toDouble())

            val amountCell = row.createCell(3)
            amountCell.setCellValue(item.totalAmount.toDouble())
            amountCell.cellStyle = currencyStyle
        }

        val totalRow = sheet.createRow(rowNum++)
        totalRow.createCell(0).setCellValue("合計")
        totalRow.createCell(2).setCellValue(itemSummary.sumOf { it.totalQuantity }.toDouble())
        val totalCell = totalRow.createCell(3)
        totalCell.setCellValue(itemSummary.sumOf { it.totalAmount }.toDouble())
        totalCell.cellStyle = currencyStyle

        for (i in 0..3) {
            sheet.autoSizeColumn(i)
        }
    }

    private fun createDailySummarySheet(
        workbook: XSSFWorkbook,
        reportData: List<SaleReportData>,
    ) {
        val sheet = workbook.createSheet("日別集計")

        val headerStyle = createHeaderStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)
        val dayFormat = SimpleDateFormat("yyyy/MM/dd")

        var rowNum = 0

        val headerRow = sheet.createRow(rowNum++)
        val headers = listOf("日付", "売上件数", "販売点数", "売上金額")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        reportData
            .groupBy { dayFormat.format(it.createdAt) }
            .toSortedMap()
            .forEach { (date, group) ->
                val row = sheet.createRow(rowNum++)
                row.createCell(0).setCellValue(date)
                row.createCell(1).setCellValue(group.size.toDouble())
                row.createCell(2).setCellValue(group.sumOf { sale -> sale.details.sumOf { it.quantity } }.toDouble())

                val amountCell = row.createCell(3)
                amountCell.setCellValue(group.sumOf { it.amount }.toDouble())
                amountCell.cellStyle = currencyStyle
            }

        for (i in 0..3) {
            sheet.autoSizeColumn(i)
        }
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook): XSSFCellStyle =
        workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            alignment = HorizontalAlignment.CENTER
            val font = workbook.createFont()
            font.bold = true
            setFont(font)
        }

    private fun createTitleStyle(workbook: XSSFWorkbook): XSSFCellStyle =
        workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            val font = workbook.createFont()
            font.bold = true
            font.fontHeightInPoints = 16
            setFont(font)
        }

    private fun createCurrencyStyle(workbook: XSSFWorkbook): XSSFCellStyle =
        workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("¥#,##0")
        }

    private fun createCountStyle(workbook: XSSFWorkbook): XSSFCellStyle =
        workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0")
        }

    private fun createDateStyle(workbook: XSSFWorkbook): XSSFCellStyle =
        workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("yyyy/mm/dd hh:mm")
        }

    private data class ItemSummaryData(
        val itemName: String,
        val totalQuantity: Int,
        val totalAmount: Int,
        val unitPrice: Int,
    )
}
