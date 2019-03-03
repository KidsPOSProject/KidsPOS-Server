package info.nukoneko.kidspos.server.controller.front

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

    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", "top")
        model.addAttribute("canonicalHostName", InetAddress.getLocalHost().canonicalHostName)
        model.addAttribute("address", InetAddress.getLocalHost().address)
        model.addAttribute("hostName", InetAddress.getLocalHost().hostName)
        model.addAttribute("hostAddress", InetAddress.getLocalHost().hostAddress)
        model.addAttribute("running_port", environment.getProperty("local.server.port"))
        return "index"
    }
}