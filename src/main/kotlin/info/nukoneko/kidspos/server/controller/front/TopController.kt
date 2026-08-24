package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.NetworkHostService
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/")
class TopController(
    private val environment: Environment,
    private val networkHostService: NetworkHostService,
) {
    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", "ダッシュボード")
        model.addAttribute("hosts", networkHostService.findHosts())
        model.addAttribute("port", environment.getProperty("local.server.port"))
        return "index"
    }
}
