package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.service.SaleExcelReportService
import info.nukoneko.kidspos.server.service.SaleReportService
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.text.SimpleDateFormat
import java.util.*

@RestController
@RequestMapping("/api/reports")
class SaleReportController(
    private val saleReportService: SaleReportService,
    private val saleExcelReportService: SaleExcelReportService,
) {
    private val logger = LoggerFactory.getLogger(SaleReportController::class.java)
    private val dateFormat = SimpleDateFormat("yyyyMMdd")

    @GetMapping("/sales/pdf")
    fun downloadSalesReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: Date,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: Date,
        @RequestParam(required = false) storeId: Int?,
    ): ResponseEntity<ByteArray> {
        logger.info("Generating sales report PDF from {} to {} for store: {}", startDate, endDate, storeId ?: "all")

        return try {
            val pdfBytes =
                if (storeId != null) {
                    saleReportService.generateSalesReportByStore(storeId, startDate, endDate)
                } else {
                    saleReportService.generateSalesReport(startDate, endDate)
                }

            val fileName = buildFileName(startDate, endDate, storeId)

            ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.size.toLong())
                .body(pdfBytes)
        } catch (e: Exception) {
            logger.error("Error generating sales report PDF", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/sales/pdf/today")
    fun downloadTodaySalesReport(
        @RequestParam(required = false) storeId: Int?,
    ): ResponseEntity<ByteArray> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        return downloadSalesReport(startDate, endDate, storeId)
    }

    @GetMapping("/sales/pdf/month")
    fun downloadMonthlySalesReport(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) storeId: Int?,
    ): ResponseEntity<ByteArray> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        logger.info("Generating monthly sales report for {}/{}", year, month)
        return downloadSalesReport(startDate, endDate, storeId)
    }

    @GetMapping("/sales/excel")
    fun downloadSalesExcelReport(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: Date,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: Date,
        @RequestParam(required = false) storeId: Int?,
    ): ResponseEntity<ByteArray> {
        logger.info("Generating sales Excel report from {} to {} for store: {}", startDate, endDate, storeId ?: "all")

        return try {
            val excelBytes =
                if (storeId != null) {
                    saleExcelReportService.generateSalesExcelReportByStore(storeId, startDate, endDate)
                } else {
                    saleExcelReportService.generateSalesExcelReport(startDate, endDate)
                }

            val fileName = buildExcelFileName(startDate, endDate, storeId)

            ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelBytes.size.toLong())
                .body(excelBytes)
        } catch (e: Exception) {
            logger.error("Error generating sales Excel report", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @GetMapping("/sales/excel/today")
    fun downloadTodaySalesExcelReport(
        @RequestParam(required = false) storeId: Int?,
    ): ResponseEntity<ByteArray> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        return downloadSalesExcelReport(startDate, endDate, storeId)
    }

    @GetMapping("/sales/excel/month")
    fun downloadMonthlySalesExcelReport(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) storeId: Int?,
    ): ResponseEntity<ByteArray> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.time

        logger.info("Generating monthly sales Excel report for {}/{}", year, month)
        return downloadSalesExcelReport(startDate, endDate, storeId)
    }

    private fun buildFileName(
        startDate: Date,
        endDate: Date,
        storeId: Int?,
    ): String {
        val startStr = dateFormat.format(startDate)
        val endStr = dateFormat.format(endDate)
        val storeStr = if (storeId != null) "_store$storeId" else ""
        return "sales_report_${startStr}_${endStr}$storeStr.pdf"
    }

    private fun buildExcelFileName(
        startDate: Date,
        endDate: Date,
        storeId: Int?,
    ): String {
        val startStr = dateFormat.format(startDate)
        val endStr = dateFormat.format(endDate)
        val storeStr = if (storeId != null) "_store$storeId" else ""
        return "sales_report_${startStr}_${endStr}$storeStr.xlsx"
    }
}
