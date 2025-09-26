package info.nukoneko.kidspos.server.service

import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportDetailData
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportSummary
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Service
@Transactional(readOnly = true)
class SaleReportService(
    private val saleRepository: SaleRepository,
    private val saleDetailRepository: SaleDetailRepository,
    private val itemRepository: ItemRepository,
    private val storeRepository: StoreRepository,
    private val staffRepository: StaffRepository,
) {
    private val logger = LoggerFactory.getLogger(SaleReportService::class.java)
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")
    private val numberFormat = NumberFormat.getInstance(Locale.JAPAN)

    fun generateSalesReport(
        startDate: Date,
        endDate: Date,
    ): ByteArray {
        logger.info("Generating sales report from {} to {}", startDate, endDate)

        val sales = saleRepository.findByDateRange(startDate, endDate)
        val reportData = prepareSalesReportData(sales)
        val summary = calculateSummary(reportData, startDate, endDate)

        return createPdfReport(reportData, summary)
    }

    fun generateSalesReportByStore(
        storeId: Int,
        startDate: Date,
        endDate: Date,
    ): ByteArray {
        logger.info("Generating sales report for store {} from {} to {}", storeId, startDate, endDate)

        val sales =
            saleRepository
                .findByDateRange(startDate, endDate)
                .filter { it.storeId == storeId }
        val reportData = prepareSalesReportData(sales)
        val summary = calculateSummary(reportData, startDate, endDate)

        return createPdfReport(reportData, summary)
    }

    private fun prepareSalesReportData(sales: List<SaleEntity>): List<SaleReportData> =
        sales.map { sale ->
            val store = storeRepository.findById(sale.storeId).orElse(null)
            val staff =
                if (sale.staffId > 0) {
                    staffRepository.findById(sale.staffId.toString()).orElse(null)
                } else {
                    null
                }
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
                staffId = sale.staffId,
                staffName = staff?.name ?: "不明なスタッフ",
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

    private fun createPdfReport(
        reportData: List<SaleReportData>,
        summary: SaleReportSummary,
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writer = PdfWriter(outputStream)
        val pdf = PdfDocument(writer)
        val document = Document(pdf, PageSize.A4)

        try {
            // ヘッダー
            addHeader(document, summary)

            // サマリー
            addSummary(document, summary)

            // 売上明細テーブル
            addSalesTable(document, reportData)

            document.close()
            logger.info("PDF report generated successfully")

            return outputStream.toByteArray()
        } catch (e: Exception) {
            logger.error("Error generating PDF report", e)
            document.close()
            throw RuntimeException("An error occurred while generating the PDF report", e)
        }
    }

    private fun addHeader(
        document: Document,
        summary: SaleReportSummary,
    ) {
        val title =
            Paragraph("売上レポート")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20f)
                .setBold()

        val period =
            Paragraph("期間: ${dateFormat.format(summary.startDate)} ～ ${dateFormat.format(summary.endDate)}")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(12f)

        document.add(title)
        document.add(period)
        document.add(Paragraph("\n"))
    }

    private fun addSummary(
        document: Document,
        summary: SaleReportSummary,
    ) {
        val summaryTitle =
            Paragraph("集計結果")
                .setFontSize(16f)
                .setBold()

        document.add(summaryTitle)

        val summaryTable =
            Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                .useAllAvailableWidth()

        summaryTable.addCell(createCell("総売上件数:", false))
        summaryTable.addCell(createCell("${numberFormat.format(summary.totalSales)} 件", true))

        summaryTable.addCell(createCell("総売上金額:", false))
        summaryTable.addCell(createCell("¥${numberFormat.format(summary.totalAmount)}", true))

        summaryTable.addCell(createCell("平均売上金額:", false))
        summaryTable.addCell(createCell("¥${numberFormat.format(summary.averageAmount.toInt())}", true))

        document.add(summaryTable)
        document.add(Paragraph("\n"))
    }

    private fun addSalesTable(
        document: Document,
        reportData: List<SaleReportData>,
    ) {
        val tableTitle =
            Paragraph("売上明細")
                .setFontSize(16f)
                .setBold()

        document.add(tableTitle)

        val table =
            Table(UnitValue.createPercentArray(floatArrayOf(10f, 20f, 15f, 15f, 15f, 15f, 10f)))
                .useAllAvailableWidth()

        // ヘッダー行
        table.addHeaderCell(createHeaderCell("売上ID"))
        table.addHeaderCell(createHeaderCell("日時"))
        table.addHeaderCell(createHeaderCell("店舗"))
        table.addHeaderCell(createHeaderCell("スタッフ"))
        table.addHeaderCell(createHeaderCell("商品数"))
        table.addHeaderCell(createHeaderCell("金額"))
        table.addHeaderCell(createHeaderCell("詳細"))

        // データ行
        reportData.forEach { sale ->
            table.addCell(createDataCell(sale.saleId.toString()))
            table.addCell(createDataCell(dateFormat.format(sale.createdAt)))
            table.addCell(createDataCell(sale.storeName))
            table.addCell(createDataCell(sale.staffName))
            table.addCell(createDataCell(sale.quantity.toString()))
            table.addCell(createDataCell("¥${numberFormat.format(sale.amount)}"))

            // 詳細セル
            val detailText =
                if (sale.details.isNotEmpty()) {
                    sale.details.joinToString("\n") { detail ->
                        "${detail.itemName} x${detail.quantity}"
                    }
                } else {
                    "-"
                }
            table.addCell(createDataCell(detailText))
        }

        document.add(table)
    }

    private fun createHeaderCell(text: String): Cell =
        Cell()
            .add(Paragraph(text).setBold())
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(5f)

    private fun createDataCell(text: String): Cell =
        Cell()
            .add(Paragraph(text))
            .setTextAlignment(TextAlignment.LEFT)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setPadding(3f)
            .setFontSize(10f)

    private fun createCell(
        text: String,
        isValue: Boolean,
    ): Cell {
        val cell =
            Cell()
                .add(Paragraph(text))
                .setPadding(5f)

        if (!isValue) {
            cell.setBold()
        }

        return cell
    }
}
