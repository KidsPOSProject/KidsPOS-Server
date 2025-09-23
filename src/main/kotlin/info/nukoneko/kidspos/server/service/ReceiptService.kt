package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.receipt.ReceiptDetail
import info.nukoneko.kidspos.receipt.ReceiptPrinter
import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.*

/**
 * Service responsible for receipt operations
 * Extracted from SaleApiController to improve separation of concerns
 */
@Service
class ReceiptService(
    private val storeService: StoreService,
    private val staffService: StaffService,
    private val appProperties: AppProperties
) {
    private val logger = LoggerFactory.getLogger(ReceiptService::class.java)

    /**
     * Print receipt for a sale
     */
    fun printReceipt(storeId: Int, items: List<ItemBean>, staffBarcode: String, deposit: Int): Boolean {
        logger.debug("Printing receipt for store: {}, items: {}", storeId, items.size)

        return try {
            val receiptDetail = createReceiptDetail(storeId, items, staffBarcode, deposit)
            val printerIp = getPrinterIp(storeId) ?: return false

            sendToPrinter(printerIp, receiptDetail)
            logger.info("Receipt printed successfully for store: {}", storeId)
            true
        } catch (e: Exception) {
            logger.error("Failed to print receipt for store: {}", storeId, e)
            false
        }
    }

    /**
     * Create receipt detail object
     */
    private fun createReceiptDetail(
        storeId: Int,
        items: List<ItemBean>,
        staffBarcode: String,
        deposit: Int
    ): ReceiptDetail {
        val itemEntities = items.map { itemBean ->
            ItemEntity(
                id = itemBean.id!!,
                barcode = itemBean.barcode,
                name = itemBean.name,
                price = itemBean.price
            )
        }

        val storeName = storeService.findStore(storeId)?.name
        val staffName = staffService.findStaff(staffBarcode)?.name

        return ReceiptDetail(
            items = itemEntities,
            storeName = storeName,
            staffName = staffName,
            deposit = deposit,
            transactionId = UUID.randomUUID().toString(),
            createdAt = Date()
        )
    }

    /**
     * Get printer IP for store
     */
    private fun getPrinterIp(storeId: Int): String? {
        val store = storeService.findStore(storeId)
        if (store == null) {
            logger.warn("Store {} not found, cannot print receipt", storeId)
            return null
        }

        val printerIp = store.printerUri
        if (printerIp.isNullOrEmpty()) {
            logger.warn("Store {} - Printer not configured, receipt will not be printed", storeId)
            return null
        }

        return printerIp
    }

    /**
     * Send receipt to thermal printer
     */
    private fun sendToPrinter(printerIp: String, receiptDetail: ReceiptDetail) {
        val printer = ReceiptPrinter(
            printerIp,
            appProperties.receipt.printer.port,
            receiptDetail
        )

        try {
            printer.print()
            logger.debug("Receipt sent to printer at: {}", printerIp)
        } catch (e: IOException) {
            logger.error("Failed to send receipt to printer at {}: {}", printerIp, e.message, e)
            throw e
        }
    }

    /**
     * Generate receipt content as string (for email or display)
     */
    fun generateReceiptContent(
        storeId: Int,
        items: List<ItemBean>,
        staffBarcode: String,
        deposit: Int
    ): String {
        val storeName = storeService.findStore(storeId)?.name ?: "Unknown Store"
        val staffName = staffService.findStaff(staffBarcode)?.name ?: "Unknown Staff"
        val totalAmount = items.sumOf { it.price }
        val change = deposit - totalAmount

        return buildString {
            appendLine("========== RECEIPT ==========")
            appendLine("Store: $storeName")
            appendLine("Staff: $staffName")
            appendLine("Date: ${Date()}")
            appendLine("-----------------------------")
            items.forEach { item ->
                appendLine("${item.name} - ¥${item.price}")
            }
            appendLine("-----------------------------")
            appendLine("Total: ¥$totalAmount")
            appendLine("Deposit: ¥$deposit")
            appendLine("Change: ¥$change")
            appendLine("=============================")
        }
    }

    /**
     * Validate printer configuration for store
     */
    fun validatePrinterConfiguration(storeId: Int): Boolean {
        return getPrinterIp(storeId) != null
    }
}