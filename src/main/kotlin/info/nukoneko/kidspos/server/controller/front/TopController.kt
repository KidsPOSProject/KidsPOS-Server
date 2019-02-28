package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.SettingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.net.InetAddress


@Controller
@RequestMapping("/")
class TopController {

    @Autowired
    private lateinit var environment: Environment

    @Autowired
    private lateinit var settingService: SettingService

    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", "top")
        model.addAttribute("running_canonicalHostName", InetAddress.getLocalHost().canonicalHostName)
        model.addAttribute("running_address", InetAddress.getLocalHost().address.joinToString(","))
        model.addAttribute("running_host", InetAddress.getLocalHost().hostName)
        model.addAttribute("running_port", environment.getProperty("local.server.port"))
        return "index"
    }
}