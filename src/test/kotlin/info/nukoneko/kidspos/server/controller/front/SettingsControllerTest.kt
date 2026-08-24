package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.entity.SettingEntity
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.service.SettingService
import info.nukoneko.kidspos.server.service.StoreService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@ExtendWith(SpringExtension::class)
@WebMvcTest(SettingsController::class)
@DisplayName("SettingsController")
class SettingsControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var settingService: SettingService

    @MockBean
    private lateinit var storeService: StoreService

    @Test
    @DisplayName("設定一覧を表示する")
    fun showsSettings() {
        val settings = listOf(SettingEntity(key = "printer.host", value = "192.168.1.10"))
        val stores = listOf(StoreEntity(id = 1, name = "本店"))
        whenever(settingService.findVisibleSetting()).thenReturn(settings)
        whenever(storeService.findAll()).thenReturn(stores)

        mockMvc
            .perform(get("/settings"))
            .andExpect(status().isOk)
            .andExpect(view().name("settings/index"))
            .andExpect(model().attribute("title", "システム設定"))
            .andExpect(model().attribute("settings", settings))
            .andExpect(model().attribute("stores", stores))
    }

    @Test
    @DisplayName("設定編集画面を表示する")
    fun showsEditForm() {
        val setting = SettingEntity(key = "printer.host", value = "192.168.1.10")
        whenever(settingService.findSetting(eq("printer.host"))).thenReturn(setting)

        mockMvc
            .perform(get("/settings/printer.host/edit"))
            .andExpect(status().isOk)
            .andExpect(view().name("settings/edit"))
            .andExpect(model().attribute("title", "設定編集"))
            .andExpect(model().attribute("setting", setting))
    }

    @Test
    @DisplayName("存在しない設定の編集は一覧へ戻す")
    fun redirectsWhenSettingIsMissing() {
        whenever(settingService.findSetting(eq("unknown"))).thenReturn(null)

        mockMvc
            .perform(get("/settings/unknown/edit"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settings"))
    }

    @Test
    @DisplayName("設定値を更新して一覧へ戻す")
    fun updatesSetting() {
        whenever(settingService.findSetting(eq("printer.host")))
            .thenReturn(SettingEntity(key = "printer.host", value = "192.168.1.10"))

        mockMvc
            .perform(post("/settings/printer.host").param("value", "192.168.1.20"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settings"))

        val saved = argumentCaptor<SettingEntity>()
        verify(settingService).saveSetting(saved.capture())
        assert(saved.firstValue.value == "192.168.1.20")
    }

    @Test
    @DisplayName("存在しない設定の更新は保存しない")
    fun doesNotSaveMissingSetting() {
        whenever(settingService.findSetting(eq("unknown"))).thenReturn(null)

        mockMvc
            .perform(post("/settings/unknown").param("value", "x"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settings"))

        verify(settingService, never()).saveSetting(any())
    }

    @Test
    @DisplayName("保護された設定の編集画面は一覧へ戻す")
    fun redirectsProtectedSettingEdit() {
        whenever(settingService.isProtectedKey(eq(SettingService.KEY_DANGER_ZONE_PASSWORD))).thenReturn(true)

        mockMvc
            .perform(get("/settings/${SettingService.KEY_DANGER_ZONE_PASSWORD}/edit"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settings"))

        verify(settingService, never()).findSetting(any())
    }

    @Test
    @DisplayName("保護された設定は更新できない")
    fun doesNotUpdateProtectedSetting() {
        whenever(settingService.isProtectedKey(eq(SettingService.KEY_DANGER_ZONE_PASSWORD))).thenReturn(true)

        mockMvc
            .perform(post("/settings/${SettingService.KEY_DANGER_ZONE_PASSWORD}").param("value", "x"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settings"))

        verify(settingService, never()).saveSetting(any())
    }
}
