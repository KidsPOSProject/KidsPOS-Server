package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.receipt.ReceiptDetail
import info.nukoneko.kidspos.receipt.ReceiptPrinter
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class PrintService(private val storeService: StoreService) {
    private val logger = LoggerFactory.getLogger(PrintService::class.java)

    @Async
    fun printReceipt(storeId: Int, receipt: ReceiptDetail) {
        val printerIp =
            storeService.findStore(storeId)?.printerUri ?: kotlin.run {
                logger.warn("$storeId の プリンタは設定されていない可能性があります。")
                logger.warn("レシートの印刷はされません")
                return
            }

        if (printerIp.isEmpty()) {
            logger.warn("$storeId の プリンタは設定されていない可能性があります。")
            logger.warn("レシートの印刷はされません")
            return
        }

        val printer = ReceiptPrinter(printerIp, 9100, receipt)
        try {
            logger.info("レシート印刷開始: 店舗ID=$storeId")
            printer.print()
            logger.info("レシート印刷完了: 店舗ID=$storeId")
        } catch (e: IOException) {
            logger.error("プリント失敗: ${e.message}", e)
        }
    }
}
