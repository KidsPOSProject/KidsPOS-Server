package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.response.StatusResponse
import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.SettingEntity
import info.nukoneko.kidspos.server.service.SettingService
import info.nukoneko.kidspos.server.service.StatusService
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

    @Autowired
    private lateinit var statusService: StatusService

    @RequestMapping("status", method = [RequestMethod.GET])
    fun getStatus(): StatusResponse = statusService.getStatus()

    @GetMapping
    fun getAllSettings(): ResponseEntity<List<SettingEntity>> = ResponseEntity.ok(service.findAllSetting())

    @GetMapping("/{key}")
    fun getSetting(
        @PathVariable key: String,
    ): ResponseEntity<SettingEntity> {
        val setting =
            service.findSetting(key)
                ?: throw ResourceNotFoundException("Setting with key $key not found")
        return ResponseEntity.ok(setting)
    }

    @PostMapping
    fun createSetting(
        @Valid @RequestBody setting: SettingEntity,
    ): ResponseEntity<SettingEntity> {
        val savedSetting = service.saveSetting(setting)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSetting)
    }

    @PutMapping("/{key}")
    fun updateSetting(
        @PathVariable key: String,
        @RequestParam value: String,
    ): ResponseEntity<SettingEntity> {
        val existingSetting =
            service.findSetting(key)
                ?: throw ResourceNotFoundException("Setting with key $key not found")

        existingSetting.value = value
        val savedSetting = service.saveSetting(existingSetting)
        return ResponseEntity.ok(savedSetting)
    }

    @DeleteMapping("/{key}")
    fun deleteSetting(
        @PathVariable key: String,
    ): ResponseEntity<Void> {
        service.findSetting(key)
            ?: throw ResourceNotFoundException("Setting with key $key not found")

        service.deleteSetting(key)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/printer/{storeId}")
    fun savePrinterSettings(
        @PathVariable storeId: Int,
        @RequestBody printerSettings: PrinterSettingsRequest,
    ): ResponseEntity<Map<String, Any>> {
        service.savePrinterHostPort(storeId, printerSettings.host, printerSettings.port)
        return ResponseEntity.ok(
            mapOf(
                "storeId" to storeId,
                "host" to printerSettings.host,
                "port" to printerSettings.port,
                "message" to "Printer settings saved successfully",
            ),
        )
    }

    @GetMapping("/printer/{storeId}")
    fun getPrinterSettings(
        @PathVariable storeId: Int,
    ): ResponseEntity<Map<String, Any>> {
        val settings = service.findPrinterHostPortById(storeId)
        return if (settings != null) {
            ResponseEntity.ok(
                mapOf(
                    "storeId" to storeId,
                    "host" to settings.first,
                    "port" to settings.second,
                ),
            )
        } else {
            throw ResourceNotFoundException("Printer settings for store $storeId not found")
        }
    }

    @PostMapping("/application")
    fun saveApplicationSettings(
        @RequestBody applicationSettings: SettingService.ApplicationSetting,
    ): ResponseEntity<Map<String, Any>> {
        service.saveApplicationSetting(applicationSettings)
        return ResponseEntity.ok(
            mapOf(
                "serverHost" to applicationSettings.serverHost,
                "serverPort" to applicationSettings.serverPort,
                "message" to "Application settings saved successfully",
            ),
        )
    }

    @GetMapping("/application")
    fun getApplicationSettings(): ResponseEntity<SettingService.ApplicationSetting> {
        val settings =
            service.getApplicationSetting()
                ?: throw ResourceNotFoundException("Application settings not found")
        return ResponseEntity.ok(settings)
    }

    /**
     * レシートのテスト印刷を実行
     *
     * @param storeId 店舗ID
     * @return 印刷結果
     */
    @PostMapping("/printer/{storeId}/test")
    fun testPrint(
        @PathVariable storeId: Int,
    ): ResponseEntity<Map<String, Any>> =
        try {
            service.testPrintReceipt(storeId)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to "テスト印刷を送信しました",
                    "storeId" to storeId,
                ),
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "success" to false,
                    "message" to "印刷に失敗しました: ${e.message}",
                    "storeId" to storeId,
                ),
            )
        }

    /**
     * プリンタ設定のリクエストDTO
     */
    data class PrinterSettingsRequest(
        val host: String,
        val port: Int,
    )
}
