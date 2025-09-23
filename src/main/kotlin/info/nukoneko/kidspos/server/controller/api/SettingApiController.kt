package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.SettingEntity
import info.nukoneko.kidspos.server.service.SettingService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 設定APIコントローラー
 *
 * アプリケーション設定の管理を行うREST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/setting")
class SettingApiController {
    @Autowired
    private lateinit var service: SettingService

    @RequestMapping("status", method = [RequestMethod.GET])
    fun getStatus(): StatusBean {
        return StatusBean("OK")
    }

    @GetMapping
    fun getAllSettings(): ResponseEntity<List<SettingEntity>> {
        return ResponseEntity.ok(service.findAllSetting())
    }

    @GetMapping("/{key}")
    fun getSetting(@PathVariable key: String): ResponseEntity<SettingEntity> {
        val setting = service.findSetting(key)
            ?: throw ResourceNotFoundException("Setting with key $key not found")
        return ResponseEntity.ok(setting)
    }

    @PostMapping
    fun createSetting(@Valid @RequestBody setting: SettingEntity): ResponseEntity<SettingEntity> {
        val savedSetting = service.saveSetting(setting)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSetting)
    }

    @PutMapping("/{key}")
    fun updateSetting(
        @PathVariable key: String,
        @RequestParam value: String
    ): ResponseEntity<SettingEntity> {
        val existingSetting = service.findSetting(key)
            ?: throw ResourceNotFoundException("Setting with key $key not found")

        existingSetting.value = value
        val savedSetting = service.saveSetting(existingSetting)
        return ResponseEntity.ok(savedSetting)
    }

    @DeleteMapping("/{key}")
    fun deleteSetting(@PathVariable key: String): ResponseEntity<Void> {
        val existingSetting = service.findSetting(key)
            ?: throw ResourceNotFoundException("Setting with key $key not found")

        service.deleteSetting(key)
        return ResponseEntity.noContent().build()
    }

    /**
     * ステータス情報を表現するデータクラス
     */
    class StatusBean(val status: String)
}