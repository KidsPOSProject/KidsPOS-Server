package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.SettingService
import info.nukoneko.kidspos.server.service.StoreService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/settings")
class SettingsController {
    @Autowired
    private lateinit var settingService: SettingService

    @Autowired
    private lateinit var storeService: StoreService

    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", javaClass.simpleName)
        model.addAttribute("settings", settingService.findAllSetting())
        model.addAttribute("stores", storeService.findAll())
        return "settings/index"
    }

    @GetMapping("{key}/edit")
    fun edit(
        @PathVariable key: String,
        model: Model,
    ): String {
        val setting = settingService.findSetting(key)
        model.addAttribute("setting", setting)
        return "settings/edit"
    }

    @PostMapping("{key}")
    fun update(
        @PathVariable key: String,
        @RequestParam value: String,
    ): String {
        val setting = settingService.findSetting(key)
        if (setting != null) {
            setting.value = value
            settingService.saveSetting(setting)
        }
        return "redirect:/settings"
    }
}
