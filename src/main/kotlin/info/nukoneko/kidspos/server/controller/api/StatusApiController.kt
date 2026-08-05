package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.response.StatusResponse
import info.nukoneko.kidspos.server.service.StatusService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ステータスAPIコントローラー
 *
 * サーバーの稼働状態とバージョン情報を提供
 */
@RestController
@RequestMapping("/api/status")
@Tag(name = "ステータス", description = "サーバーステータス・バージョン情報API")
class StatusApiController(
    private val statusService: StatusService,
) {
    @GetMapping
    @Operation(summary = "ステータス取得", description = "サーバーの稼働状態とバージョン情報を取得します")
    fun getStatus(): StatusResponse = statusService.getStatus()
}
