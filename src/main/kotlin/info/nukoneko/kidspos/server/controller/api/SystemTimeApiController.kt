package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.request.SyncSystemTimeRequest
import info.nukoneko.kidspos.server.controller.dto.response.SystemTimeResponse
import info.nukoneko.kidspos.server.controller.dto.response.SystemTimeSyncResponse
import info.nukoneko.kidspos.server.service.SystemTimeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * システム時刻APIコントローラー
 *
 * サーバー時刻の参照と、クライアント時刻を正とした同期を行うREST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/system/time")
@Tag(name = "System", description = "サーバー時刻API")
class SystemTimeApiController(
    private val systemTimeService: SystemTimeService,
) {
    @GetMapping
    @Operation(summary = "サーバー時刻取得", description = "サーバーの現在時刻とタイムゾーンを取得します")
    fun getTime(): SystemTimeResponse = systemTimeService.currentTime()

    @PostMapping
    @Operation(summary = "サーバー時刻同期", description = "指定された時刻にサーバーのシステム時刻を合わせます")
    fun sync(
        @Valid @RequestBody request: SyncSystemTimeRequest,
    ): ResponseEntity<SystemTimeSyncResponse> {
        val result = systemTimeService.sync(request.epochMillis)
        val status = if (result.success) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR
        return ResponseEntity.status(status).body(result)
    }
}
