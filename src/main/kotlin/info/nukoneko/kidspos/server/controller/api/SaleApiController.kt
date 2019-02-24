package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.receipt.ReceiptDetail
import info.nukoneko.kidspos.receipt.ReceiptPrinter
import info.nukoneko.kidspos.server.controller.api.model.SaleBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.service.SaleService
import info.nukoneko.kidspos.server.service.SettingService
import info.nukoneko.kidspos.server.service.StaffService
import info.nukoneko.kidspos.server.service.StoreService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
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
    private lateinit var settingService: SettingService

    @Autowired
    private lateinit var storeService: StoreService

    @Autowired
    private lateinit var staffService: StaffService

    @RequestMapping(method = [RequestMethod.POST])
    fun createSale(@Validated @RequestBody sale: SaleBean): SaleEntity {
        val entity = service.save(sale)

        // レシート印刷
        printReceipt(sale.store_id, ReceiptDetail(
                sale.items.map { ItemEntity(it.id!!, it.name, it.price) },
                storeService.findStore(sale.store_id)?.name,
                staffService.findStaff(sale.staff_id)?.name,
                sale.deposit, null, Date()
        ))

        return entity
    }

    private fun printReceipt(storeId: Int, receipt: ReceiptDetail) {
        val ip = settingService.findPrinterHostPortById(storeId)
        if (ip == null) {
            println("$storeId の プリンタは設定されていない可能性があります。")
            println("レシートの印刷はされません")
            return
        }

        val printer = ReceiptPrinter(ip.first, ip.second, receipt)
        try {
            printer.print()
        } catch (e: IOException) {
            println("*** プリント失敗 ***")
            e.localizedMessage
            println("************")
        }
    }
}