package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.logging.LogLevel
import info.nukoneko.kidspos.server.service.LogService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * ログ画面コントローラー
 *
 * 直近のログをスタッフが確認するための画面を提供
 */
@Controller
@RequestMapping("/logs")
class LogsController(
    private val logService: LogService,
) {
    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", "ログ")
        model.addAttribute("levels", LogLevel.entries.map { it.name })
        model.addAttribute("capacity", logService.capacity())
        return "logs/index"
    }
}
