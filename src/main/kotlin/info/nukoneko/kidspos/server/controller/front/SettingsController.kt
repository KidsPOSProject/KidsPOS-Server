package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.SettingService
import info.nukoneko.kidspos.server.service.StoreService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/settings")
class SettingsController(
    private val settingService: SettingService,
    private val storeService: StoreService,
) {
    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", "システム設定")
        model.addAttribute("settings", settingService.findVisibleSetting())
        model.addAttribute("stores", storeService.findAll())
        return "settings/index"
    }

    @GetMapping("{key}/edit")
    fun edit(
        @PathVariable key: String,
        model: Model,
    ): String {
        if (settingService.isProtectedKey(key)) {
            return "redirect:/settings"
        }
        val setting = settingService.findSetting(key) ?: return "redirect:/settings"
        model.addAttribute("title", "設定編集")
        model.addAttribute("setting", setting)
        return "settings/edit"
    }

    @PostMapping("{key}")
    fun update(
        @PathVariable key: String,
        @RequestParam value: String,
    ): String {
        if (settingService.isProtectedKey(key)) {
            return "redirect:/settings"
        }
        val setting = settingService.findSetting(key)
        if (setting != null) {
            setting.value = value
            settingService.saveSetting(setting)
        }
        return "redirect:/settings"
    }
}
