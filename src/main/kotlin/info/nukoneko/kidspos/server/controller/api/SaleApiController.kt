package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.receipt.ReceiptDetail
import info.nukoneko.kidspos.receipt.ReceiptPrinter
import info.nukoneko.kidspos.server.controller.api.model.ItemBean
import info.nukoneko.kidspos.server.controller.api.model.SaleBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.service.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.util.*

@RestController
@RequestMapping("/api/sale")
class SaleApiController {
    private val logger = LoggerFactory.getLogger(SaleApiController::class.java)
    
    @Autowired
    private lateinit var service: SaleService

    @Autowired
    private lateinit var itemService: ItemService

    @Autowired
    private lateinit var settingService: SettingService

    @Autowired
    private lateinit var storeService: StoreService

    @Autowired
    private lateinit var staffService: StaffService

    @RequestMapping("create", method = [RequestMethod.POST])
    fun createSale(@ModelAttribute sale: SaleBean): SaleEntity {
        // 汚い。時間があるときに直す
        // Preallocate the expected size of the list for memory efficiency
        val itemIds = sale.itemIds.split(",")
        val items = ArrayList<ItemBean>(itemIds.size)
        
        for (itemId in itemIds) {
            val itemEntity: ItemEntity = itemService.findItem(itemId.toInt())
                ?: throw IOException("Unknown item.")
            items.add(ItemBean(
                itemEntity.id,
                itemEntity.barcode,
                itemEntity.name,
                itemEntity.price
            ))
        }

        val entity = service.save(sale, items)

        // レシート印刷 - Now asynchronous
        printReceiptAsync(
            sale.storeId, ReceiptDetail(
                items.map {
                    ItemEntity(
                        it.id!!,
                        it.barcode,
                        it.name,
                        it.price
                    )
                },
                storeService.findStore(sale.storeId)?.name,
                staffService.findStaff(sale.staffBarcode)?.name,
                sale.deposit, null, Date()
            )
        )

        return entity
    }

    @Async
    fun printReceiptAsync(storeId: Int, receipt: ReceiptDetail) {
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
            printer.print()
        } catch (e: IOException) {
            logger.error("*** プリント失敗 ***", e)
            logger.error("************")
        }
    }
}
