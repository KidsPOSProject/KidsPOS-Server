package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.controller.api.model.StoreBean
import info.nukoneko.kidspos.server.service.StoreService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/stores")
class StoresController {
    @Autowired
    private lateinit var storeService: StoreService

    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", javaClass.simpleName)
        model.addAttribute("data", storeService.findAll())
        return "stores/index"
    }

    @GetMapping("new")
    fun newItem(model: Model): String {
        return "stores/new"
    }

    @GetMapping("{id}/edit")
    fun edit(@PathVariable id: Int, model: Model): String {
        model.addAttribute("store", storeService.findStore(id))
        return "stores/edit"
    }

    @PostMapping
    fun create(@ModelAttribute store: StoreBean): String {
        storeService.save(store)
        return "redirect:/stores"
    }
}