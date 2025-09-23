package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.service.SaleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/sales")
class SalesController {
    @Autowired
    private lateinit var saleService: SaleService

    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", javaClass.simpleName)
        model.addAttribute("data", saleService.findAllSale())
        return "sales/index"
    }

    @GetMapping("new")
    fun newItem(model: Model): String {
        return "sales/new"
    }

    @GetMapping("{id}/edit")
    fun edit(@PathVariable id: Int, model: Model): String {
        val sale = saleService.findSale(id)
        model.addAttribute("sale", sale)
        return "sales/edit"
    }

    @PostMapping
    fun create(@ModelAttribute sale: SaleBean): String {
//        saleService.save(sale)
        return "redirect:/sales"
    }
}