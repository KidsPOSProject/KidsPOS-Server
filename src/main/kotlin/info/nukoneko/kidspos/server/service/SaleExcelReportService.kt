package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.response.SaleReportData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportDetailData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportSummary
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.repository.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Service
@Transactional(readOnly = true)
class SaleExcelReportService(
    private val saleRepository: SaleRepository,
    private val saleDetailRepository: SaleDetailRepository,
    private val itemRepository: ItemRepository,
    private val storeRepository: StoreRepository,
) {
    private val logger = LoggerFactory.getLogger(SaleExcelReportService::class.java)
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")
    private val numberFormat = NumberFormat.getInstance(Locale.JAPAN)

    fun generateSalesExcelReport(
        startDate: Date,
        endDate: Date,
    ): ByteArray {
        logger.info("Generating sales Excel report from {} to {}", startDate, endDate)

        val sales = saleRepository.findByDateRange(startDate, endDate)
        val reportData = prepareSalesReportData(sales)
        val summary = calculateSummary(reportData, startDate, endDate)

        return createExcelReport(reportData, summary)
    }

    fun generateSalesExcelReportByStore(
        storeId: Int,
        startDate: Date,
        endDate: Date,
    ): ByteArray {
        logger.info("Generating sales Excel report for store {} from {} to {}", storeId, startDate, endDate)

        val sales =
            saleRepository
                .findByDateRange(startDate, endDate)
                .filter { it.storeId == storeId }
        val reportData = prepareSalesReportData(sales)
        val summary = calculateSummary(reportData, startDate, endDate)

        return createExcelReport(reportData, summary)
    }

    private fun prepareSalesReportData(sales: List<SaleEntity>): List<SaleReportData> =
        sales.map { sale ->
            val store = storeRepository.findById(sale.storeId).orElse(null)
            val details =
                saleDetailRepository.findBySaleId(sale.id).map { detail ->
                    val item = itemRepository.findById(detail.itemId).orElse(null)
                    SaleReportDetailData(
                        itemId = detail.itemId,
                        itemName = item?.name ?: "不明な商品",
                        price = detail.price,
                        quantity = detail.quantity,
                        subtotal = detail.price * detail.quantity,
                    )
                }

            SaleReportData(
                saleId = sale.id,
                storeId = sale.storeId,
                storeName = store?.name ?: "不明な店舗",
                quantity = sale.quantity,
                amount = sale.amount,
                createdAt = sale.createdAt,
                details = details,
            )
        }

    private fun calculateSummary(
        reportData: List<SaleReportData>,
        startDate: Date,
        endDate: Date,
    ): SaleReportSummary {
        val totalSales = reportData.size
        val totalAmount = reportData.sumOf { it.amount }
        val averageAmount = if (totalSales > 0) totalAmount.toDouble() / totalSales else 0.0

        return SaleReportSummary(
            totalSales = totalSales,
            totalAmount = totalAmount,
            averageAmount = averageAmount,
            startDate = startDate,
            endDate = endDate,
        )
    }

    private fun createExcelReport(
        reportData: List<SaleReportData>,
        summary: SaleReportSummary,
    ): ByteArray {
        val workbook = XSSFWorkbook()

        try {
            // サマリーシート
            createSummarySheet(workbook, summary)

            // 売上明細シート
            createSalesDetailSheet(workbook, reportData)

            // 商品別集計シート
            createItemSummarySheet(workbook, reportData)

            // バイト配列に変換
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

        // スタイルの作成
        val headerStyle = createHeaderStyle(workbook)
        val titleStyle = createTitleStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)

        var rowNum = 0

        // タイトル
        val titleRow = sheet.createRow(rowNum++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("売上レポート")
        titleCell.cellStyle = titleStyle
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 3))

        // 期間
        val periodRow = sheet.createRow(rowNum++)
        periodRow.createCell(0).setCellValue("期間:")
        periodRow.createCell(1).setCellValue("${dateFormat.format(summary.startDate)} ～ ${dateFormat.format(summary.endDate)}")
        sheet.addMergedRegion(CellRangeAddress(1, 1, 1, 3))

        // 空白行
        rowNum++

        // サマリーヘッダー
        val summaryHeaderRow = sheet.createRow(rowNum++)
        val summaryHeader = summaryHeaderRow.createCell(0)
        summaryHeader.setCellValue("集計結果")
        summaryHeader.cellStyle = headerStyle
        sheet.addMergedRegion(CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3))

        // 総売上件数
        val countRow = sheet.createRow(rowNum++)
        countRow.createCell(0).setCellValue("総売上件数")
        val countCell = countRow.createCell(1)
        countCell.setCellValue(summary.totalSales.toDouble())
        countCell.cellStyle =
            workbook.createCellStyle().apply {
                dataFormat = workbook.createDataFormat().getFormat("#,##0")
            }
        countRow.createCell(2).setCellValue("件")

        // 総売上金額
        val amountRow = sheet.createRow(rowNum++)
        amountRow.createCell(0).setCellValue("総売上金額")
        val amountCell = amountRow.createCell(1)
        amountCell.setCellValue(summary.totalAmount.toDouble())
        amountCell.cellStyle = currencyStyle

        // 平均売上金額
        val avgRow = sheet.createRow(rowNum++)
        avgRow.createCell(0).setCellValue("平均売上金額")
        val avgCell = avgRow.createCell(1)
        avgCell.setCellValue(summary.averageAmount)
        avgCell.cellStyle = currencyStyle

        // 列幅の自動調整
        for (i in 0..3) {
            sheet.autoSizeColumn(i)
        }
    }

    private fun createSalesDetailSheet(
        workbook: XSSFWorkbook,
        reportData: List<SaleReportData>,
    ) {
        val sheet = workbook.createSheet("売上明細")

        // スタイルの作成
        val headerStyle = createHeaderStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)
        val dateStyle = createDateStyle(workbook)

        var rowNum = 0

        // ヘッダー行
        val headerRow = sheet.createRow(rowNum++)
        val headers = listOf("売上ID", "日時", "店舗名", "商品数", "金額", "商品明細")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        // データ行
        reportData.forEach { sale ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(sale.saleId.toDouble())

            val dateCell = row.createCell(1)
            dateCell.setCellValue(sale.createdAt)
            dateCell.cellStyle = dateStyle

            row.createCell(2).setCellValue(sale.storeName)
            row.createCell(3).setCellValue(sale.quantity.toDouble())

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

        // 合計行
        val totalRow = sheet.createRow(rowNum++)
        totalRow.createCell(2).setCellValue("合計")
        val totalCell = totalRow.createCell(4)
        totalCell.setCellValue(reportData.sumOf { it.amount }.toDouble())
        totalCell.cellStyle = currencyStyle

        // 列幅の自動調整
        for (i in 0..5) {
            sheet.autoSizeColumn(i)
        }
    }

    private fun createItemSummarySheet(
        workbook: XSSFWorkbook,
        reportData: List<SaleReportData>,
    ) {
        val sheet = workbook.createSheet("商品別集計")

        // スタイルの作成
        val headerStyle = createHeaderStyle(workbook)
        val currencyStyle = createCurrencyStyle(workbook)

        // 商品別に集計
        val itemSummary = mutableMapOf<String, ItemSummaryData>()
        reportData.forEach { sale ->
            sale.details.forEach { detail ->
                val existing =
                    itemSummary.getOrDefault(
                        detail.itemName,
                        ItemSummaryData(detail.itemName, 0, 0, detail.price),
                    )
                itemSummary[detail.itemName] =
                    ItemSummaryData(
                        itemName = existing.itemName,
                        totalQuantity = existing.totalQuantity + detail.quantity,
                        totalAmount = existing.totalAmount + detail.subtotal,
                        unitPrice = detail.price,
                    )
            }
        }

        var rowNum = 0

        // ヘッダー行
        val headerRow = sheet.createRow(rowNum++)
        val headers = listOf("商品名", "単価", "販売数", "売上金額")
        headers.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        // データ行（売上金額の降順でソート）
        itemSummary.values.sortedByDescending { it.totalAmount }.forEach { item ->
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

        // 合計行
        val totalRow = sheet.createRow(rowNum++)
        totalRow.createCell(0).setCellValue("合計")
        totalRow.createCell(2).setCellValue(itemSummary.values.sumOf { it.totalQuantity }.toDouble())
        val totalCell = totalRow.createCell(3)
        totalCell.setCellValue(itemSummary.values.sumOf { it.totalAmount }.toDouble())
        totalCell.cellStyle = currencyStyle

        // 列幅の自動調整
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
