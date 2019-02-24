package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.controller.api.model.ItemBean
import info.nukoneko.kidspos.server.service.ItemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*


@Controller
@RequestMapping("/items")
class ItemsController {
    @Autowired
    private lateinit var itemService: ItemService

    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", javaClass.simpleName)
        model.addAttribute("data", itemService.findAll())
        return "items/index"
    }

    @GetMapping("new")
    fun newItem(model: Model): String {
        return "items/new"
    }

    @GetMapping("{id}/edit")
    fun edit(@PathVariable id: Int, model: Model): String {
        val item = itemService.findItem(id)
        model.addAttribute("item", item)
        return "items/edit"
    }

    @PostMapping
    fun create(@ModelAttribute item: ItemBean): String {
        itemService.save(item)
        return "redirect:/items"
    }
}