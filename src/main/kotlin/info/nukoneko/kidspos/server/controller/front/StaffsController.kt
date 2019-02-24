package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.controller.api.model.StaffBean
import info.nukoneko.kidspos.server.service.StaffService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/staffs")
class StaffsController {
    @Autowired
    private lateinit var staffService: StaffService

    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", javaClass.simpleName)
        model.addAttribute("data", staffService.findAll())
        return "staffs/index"
    }

    @GetMapping("new")
    fun newItem(model: Model): String {
        return "staffs/new"
    }

    @GetMapping("{id}/edit")
    fun edit(@PathVariable id: Int, model: Model): String {
        model.addAttribute("staff", staffService.findStaff(id))
        return "staffs/edit"
    }

    @PostMapping
    fun create(@ModelAttribute staff: StaffBean): String {
        staffService.save(staff)
        return "redirect:/staffs"
    }
}