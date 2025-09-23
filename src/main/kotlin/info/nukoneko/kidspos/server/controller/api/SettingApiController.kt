package info.nukoneko.kidspos.server.controller.api

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

/**
 * 設定APIコントローラー
 *
 * アプリケーション設定の管理を行うREST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/setting")
class SettingApiController {
    @RequestMapping("status", method = [RequestMethod.GET])
    fun getStatus(): StatusBean {
        return StatusBean("OK")
    }

    /**
     * ステータス情報を表現するデータクラス
     */
    class StatusBean(val status: String)
}