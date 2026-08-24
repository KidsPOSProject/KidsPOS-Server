package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.receipt.ReceiptDetail
import info.nukoneko.kidspos.receipt.ReceiptPrinter
import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Service responsible for receipt operations
 * Extracted from SaleApiController to improve separation of concerns
 */
@Service
class ReceiptService(
    private val storeService: StoreService,
    private val appProperties: AppProperties,
) {
    private val logger = LoggerFactory.getLogger(ReceiptService::class.java)

    private val executor =
        ThreadPoolExecutor(
            PRINT_THREADS,
            PRINT_THREADS,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(QUEUE_CAPACITY),
            { runnable -> Thread(runnable, "receipt-printer").apply { isDaemon = true } },
        ).apply { allowCoreThreadTimeOut(true) }

    /**
     * レシート印刷を依頼する
     *
     * 到達できないプリンターは接続タイムアウトまで待たされるため、
     * 送信は別スレッドに任せて会計のレスポンスを待たせない。
     * 戻り値は印刷を実際に依頼できたかどうかで、印刷の成否ではない。
     */
    fun printReceiptAsync(
        storeId: Int,
        items: List<ItemBean>,
        deposit: Int,
    ): Boolean {
        logger.debug("Printing receipt for store: {}, items: {}", storeId, items.size)

        val printerIp = getPrinterIp(storeId) ?: return false

        val receiptDetail =
            try {
                createReceiptDetail(storeId, items, deposit)
            } catch (e: Exception) {
                logger.error("Failed to build receipt for store: {}", storeId, e)
                return false
            }

        return try {
            executor.execute {
                try {
                    sendToPrinter(printerIp, receiptDetail)
                    logger.info("Receipt printed successfully for store: {}", storeId)
                } catch (e: Exception) {
                    logger.error("Failed to print receipt for store: {}", storeId, e)
                }
            }
            true
        } catch (e: RejectedExecutionException) {
            logger.error("Print queue is full, receipt discarded for store: {}", storeId, e)
            false
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    /**
     * Create receipt detail object
     */
    private fun createReceiptDetail(
        storeId: Int,
        items: List<ItemBean>,
        deposit: Int,
    ): ReceiptDetail {
        val itemEntities =
            items.map { itemBean ->
                ItemEntity(
                    id = itemBean.id!!,
                    barcode = itemBean.barcode ?: "",
                    name = itemBean.name,
                    price = itemBean.price,
                )
            }

        val storeName = storeService.findStore(storeId)?.name

        return ReceiptDetail(
            items = itemEntities,
            storeName = storeName,
            deposit = deposit,
            transactionId = UUID.randomUUID().toString(),
            createdAt = Date(),
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
    private fun sendToPrinter(
        printerIp: String,
        receiptDetail: ReceiptDetail,
    ) {
        val printerProperties = appProperties.receipt.printer
        val printer =
            ReceiptPrinter(
                printerIp,
                printerProperties.port,
                receiptDetail,
            )

        try {
            printer.print(printerProperties.connectTimeoutMillis)
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
        deposit: Int,
    ): String {
        val storeName = storeService.findStore(storeId)?.name ?: "Unknown Store"
        val totalAmount = items.sumOf { it.price }
        val change = deposit - totalAmount

        return buildString {
            appendLine("========== RECEIPT ==========")
            appendLine("Store: $storeName")
            appendLine("Date: ${Date()}")
            appendLine("-----------------------------")
            items.forEach { item ->
                appendLine("${item.name} - ${item.price}リバー")
            }
            appendLine("-----------------------------")
            appendLine("Total: ${totalAmount}リバー")
            appendLine("Deposit: ${deposit}リバー")
            appendLine("Change: ${change}リバー")
            appendLine("=============================")
        }
    }

    /**
     * Validate printer configuration for store
     */
    fun validatePrinterConfiguration(storeId: Int): Boolean = getPrinterIp(storeId) != null

    companion object {
        const val PRINT_THREADS = 2
        const val KEEP_ALIVE_SECONDS = 60L
        const val QUEUE_CAPACITY = 64
    }
}
