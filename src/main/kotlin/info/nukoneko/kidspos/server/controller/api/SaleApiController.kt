package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.receipt.ReceiptDetail
import info.nukoneko.kidspos.receipt.ReceiptPrinter
import info.nukoneko.kidspos.server.controller.api.model.ItemBean
import info.nukoneko.kidspos.server.controller.api.model.SaleBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.service.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.util.*

@RestController
@RequestMapping("/api/sale")
class SaleApiController {
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
        val items: List<ItemBean> = sale.itemIds.split(",").map {
            val itemEntity: ItemEntity = itemService.findItem(it.toInt())
                ?: throw IOException("Unknown item.")
            ItemBean(
                itemEntity.id,
                itemEntity.barcode,
                itemEntity.name,
                itemEntity.price
            )
        }

        val entity = service.save(sale, items)

        // レシート印刷
        printReceipt(
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

    private fun printReceipt(storeId: Int, receipt: ReceiptDetail) {
        val printerIp =
            storeService.findStore(storeId)?.printerUri ?: kotlin.run {
                println("$storeId の プリンタは設定されていない可能性があります。")
                println("レシートの印刷はされません")
                return
            }

        if (printerIp.isEmpty()) {
            println("$storeId の プリンタは設定されていない可能性があります。")
            println("レシートの印刷はされません")
            return
        }

        val printer = ReceiptPrinter(printerIp, 9100, receipt)
        try {
            printer.print()
        } catch (e: IOException) {
            println("*** プリント失敗 ***")
            e.localizedMessage
            println("************")
        }
    }
}
