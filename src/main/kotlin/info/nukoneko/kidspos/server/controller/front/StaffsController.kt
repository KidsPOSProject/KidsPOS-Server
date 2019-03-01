package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.StaffService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

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
}