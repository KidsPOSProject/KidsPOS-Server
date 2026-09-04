package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.SaleAggregationService
import info.nukoneko.kidspos.server.service.SaleProcessingService
import info.nukoneko.kidspos.server.service.StoreService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Date

@Controller
@RequestMapping("/sales")
class SalesController(
    private val saleProcessingService: SaleProcessingService,
    private val storeService: StoreService,
    private val saleAggregationService: SaleAggregationService,
) {
    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", "売上管理")
        model.addAttribute("data", saleProcessingService.findAllSales().sortedByDescending { it.createdAt })
        model.addAttribute("storeNames", storeService.findAll().associate { it.id to it.name })
        model.addAttribute("stats", saleAggregationService.dailyComparison(Date()))
        return "sales/index"
    }

    @GetMapping("{id}")
    fun detail(
        @PathVariable id: Int,
        model: Model,
    ): String {
        val detail = saleAggregationService.findSaleDetail(id) ?: return "redirect:/sales"
        model.addAttribute("title", "取引詳細")
        model.addAttribute("detail", detail)
        return "sales/detail"
    }
}
