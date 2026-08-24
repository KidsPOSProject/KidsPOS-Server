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
import info.nukoneko.kidspos.server.controller.dto.response.SaleReportSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Service
@Transactional(readOnly = true)
class SaleReportService(
    private val saleAggregationService: SaleAggregationService,
) {
    private val logger = LoggerFactory.getLogger(SaleReportService::class.java)
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm")
    private val numberFormat = NumberFormat.getInstance(Locale.JAPAN)

    fun generateSalesReport(
        startDate: Date,
        endDate: Date,
    ): ByteArray {
        logger.info("Generating sales report from {} to {}", startDate, endDate)

        val aggregation = saleAggregationService.aggregate(startDate, endDate)
        return createPdfReport(aggregation.sales, aggregation.summary)
    }

    fun generateSalesReportByStore(
        storeId: Int,
        startDate: Date,
        endDate: Date,
    ): ByteArray {
        logger.info("Generating sales report for store {} from {} to {}", storeId, startDate, endDate)

        val aggregation = saleAggregationService.aggregate(startDate, endDate, storeId)
        return createPdfReport(aggregation.sales, aggregation.summary)
    }

    private fun createPdfReport(
        reportData: List<SaleReportData>,
        summary: SaleReportSummary,
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writer = PdfWriter(outputStream)
        val pdf = PdfDocument(writer)
        val document = Document(pdf, PageSize.A4)
        document.setFont(JapanesePdfFont.create())

        try {
            addHeader(document, summary)
            addSummary(document, summary)
            addItemSummaryTable(document, reportData)
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
        document.add(
            Paragraph("集計結果")
                .setFontSize(16f)
                .setBold(),
        )

        val summaryTable =
            Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                .useAllAvailableWidth()

        summaryTable.addCell(createCell("総売上件数:", false))
        summaryTable.addCell(createCell("${numberFormat.format(summary.totalSales)} 件", true))

        summaryTable.addCell(createCell("総販売点数:", false))
        summaryTable.addCell(createCell("${numberFormat.format(summary.totalItemCount)} 点", true))

        summaryTable.addCell(createCell("総売上金額:", false))
        summaryTable.addCell(createCell("¥${numberFormat.format(summary.totalAmount)}", true))

        summaryTable.addCell(createCell("平均売上金額:", false))
        summaryTable.addCell(createCell("¥${numberFormat.format(summary.averageAmount.toInt())}", true))

        document.add(summaryTable)
        document.add(Paragraph("\n"))
    }

    private fun addItemSummaryTable(
        document: Document,
        reportData: List<SaleReportData>,
    ) {
        val items =
            reportData
                .flatMap { it.details }
                .groupBy { it.itemId }
                .map { (_, group) ->
                    Triple(
                        group.first().itemName,
                        group.sumOf { it.quantity },
                        group.sumOf { it.subtotal },
                    )
                }.sortedByDescending { it.third }

        if (items.isEmpty()) {
            return
        }

        document.add(
            Paragraph("商品別集計")
                .setFontSize(16f)
                .setBold(),
        )

        val table =
            Table(UnitValue.createPercentArray(floatArrayOf(50f, 20f, 30f)))
                .useAllAvailableWidth()

        table.addHeaderCell(createHeaderCell("商品名"))
        table.addHeaderCell(createHeaderCell("販売数"))
        table.addHeaderCell(createHeaderCell("売上金額"))

        items.forEach { (name, quantity, amount) ->
            table.addCell(createDataCell(name))
            table.addCell(createDataCell("${numberFormat.format(quantity)} 点"))
            table.addCell(createDataCell("¥${numberFormat.format(amount)}"))
        }

        document.add(table)
        document.add(Paragraph("\n"))
    }

    private fun addSalesTable(
        document: Document,
        reportData: List<SaleReportData>,
    ) {
        document.add(
            Paragraph("売上明細")
                .setFontSize(16f)
                .setBold(),
        )

        if (reportData.isEmpty()) {
            document.add(Paragraph("対象期間の売上はありません").setFontSize(12f))
            return
        }

        val table =
            Table(UnitValue.createPercentArray(floatArrayOf(10f, 20f, 20f, 15f, 15f, 20f)))
                .useAllAvailableWidth()

        table.addHeaderCell(createHeaderCell("売上ID"))
        table.addHeaderCell(createHeaderCell("日時"))
        table.addHeaderCell(createHeaderCell("店舗"))
        table.addHeaderCell(createHeaderCell("商品数"))
        table.addHeaderCell(createHeaderCell("金額"))
        table.addHeaderCell(createHeaderCell("詳細"))

        reportData.forEach { sale ->
            table.addCell(createDataCell(sale.saleId.toString()))
            table.addCell(createDataCell(dateFormat.format(sale.createdAt)))
            table.addCell(createDataCell(sale.storeName))
            table.addCell(createDataCell(sale.details.sumOf { it.quantity }.toString()))
            table.addCell(createDataCell("¥${numberFormat.format(sale.amount)}"))

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
