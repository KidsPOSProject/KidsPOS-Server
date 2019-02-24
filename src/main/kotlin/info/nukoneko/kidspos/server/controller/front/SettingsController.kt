package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.SettingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/settings")
class SettingsController {
    @Autowired
    private lateinit var settingService: SettingService

    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", javaClass.simpleName)
        model.addAttribute("settings", settingService.findAllSetting())
        return "settings/index"
    }

    @GetMapping("{key}/edit")
    fun edit(@PathVariable key: String, model: Model): String {
        val setting = settingService.findSetting(key)
        model.addAttribute("setting", setting)
        return "settings/edit"
    }
}