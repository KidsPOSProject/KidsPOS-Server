package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.controller.dto.request.StoreBean
import info.nukoneko.kidspos.server.service.StoreService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/stores")
class StoresController(
    private val storeService: StoreService,
) {
    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", "店舗管理")
        model.addAttribute("data", storeService.findAll())
        return "stores/index"
    }

    @GetMapping("new")
    fun newItem(model: Model): String {
        model.addAttribute("title", "店舗作成")
        return "stores/new"
    }

    @GetMapping("{id}/edit")
    fun edit(
        @PathVariable id: Int,
        model: Model,
    ): String {
        val store = storeService.findStore(id) ?: return "redirect:/stores"
        model.addAttribute("title", "店舗編集")
        model.addAttribute("store", store)
        return "stores/edit"
    }

    @PostMapping
    fun create(
        @ModelAttribute store: StoreBean,
    ): String {
        storeService.save(store)
        return "redirect:/stores"
    }

    @PostMapping("{id}/update")
    fun update(
        @PathVariable id: Int,
        @ModelAttribute store: StoreBean,
    ): String {
        val existingStore = storeService.findStore(id)
        if (existingStore != null) {
            val updatedStore =
                StoreBean(
                    id = id,
                    name = store.name,
                    printerUri = store.printerUri,
                )
            storeService.save(updatedStore)
        }
        return "redirect:/stores"
    }

    @PostMapping("{id}/delete")
    fun delete(
        @PathVariable id: Int,
    ): String {
        storeService.delete(id)
        return "redirect:/stores"
    }
}
