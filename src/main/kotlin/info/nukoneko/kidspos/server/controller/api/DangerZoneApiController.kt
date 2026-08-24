package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.request.ChangeDangerZonePasswordRequest
import info.nukoneko.kidspos.server.controller.dto.request.ClearDangerZonePasswordRequest
import info.nukoneko.kidspos.server.controller.dto.request.VerifyDangerZonePasswordRequest
import info.nukoneko.kidspos.server.controller.dto.response.DangerZonePasswordResponse
import info.nukoneko.kidspos.server.controller.dto.response.DangerZoneStatusResponse
import info.nukoneko.kidspos.server.controller.dto.response.DangerZoneVerifyResponse
import info.nukoneko.kidspos.server.service.DangerZonePasswordService
import info.nukoneko.kidspos.server.service.DangerZoneVerifyRateLimiter
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Danger Zone パスワードAPIコントローラー
 *
 * 危険操作を保護するパスワードの設定・解除・照合を行うREST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/setting/danger-zone")
@Tag(name = "DangerZone", description = "危険操作を保護するパスワードAPI")
class DangerZoneApiController(
    private val dangerZonePasswordService: DangerZonePasswordService,
    private val verifyRateLimiter: DangerZoneVerifyRateLimiter,
) {
    @GetMapping("/status")
    @Operation(summary = "設定状態取得", description = "Danger Zone パスワードが設定済みかを取得します")
    fun getStatus(): DangerZoneStatusResponse = DangerZoneStatusResponse(dangerZonePasswordService.isConfigured())

    @PostMapping("/password")
    @Operation(summary = "パスワード設定", description = "Danger Zone パスワードを設定または変更します")
    fun changePassword(
        @Valid @RequestBody request: ChangeDangerZonePasswordRequest,
    ): ResponseEntity<DangerZonePasswordResponse> {
        val result = dangerZonePasswordService.changePassword(request.currentPassword, request.newPassword)
        return toResponse(result)
    }

    @PostMapping("/password/clear")
    @Operation(summary = "パスワード解除", description = "Danger Zone パスワードを削除します")
    fun clearPassword(
        @Valid @RequestBody request: ClearDangerZonePasswordRequest,
    ): ResponseEntity<DangerZonePasswordResponse> {
        val result = dangerZonePasswordService.clearPassword(request.currentPassword)
        return toResponse(result)
    }

    @PostMapping("/verify")
    @Operation(summary = "パスワード照合", description = "入力されたパスワードがDanger Zone パスワードと一致するかを判定します")
    fun verify(
        @Valid @RequestBody request: VerifyDangerZonePasswordRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<DangerZoneVerifyResponse> {
        val clientKey = clientKeyOf(httpRequest)
        val retryAfter = verifyRateLimiter.retryAfterSeconds(clientKey)
        if (retryAfter > 0) {
            return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, retryAfter.toString())
                .body(
                    DangerZoneVerifyResponse(
                        valid = false,
                        configured = dangerZonePasswordService.isConfigured(),
                        message = "試行回数が多すぎます。${retryAfter}秒後にもう一度お試しください",
                    ),
                )
        }

        val result = dangerZonePasswordService.verify(request.password)
        when {
            result.valid -> verifyRateLimiter.recordSuccess(clientKey)
            result.configured -> verifyRateLimiter.recordFailure(clientKey)
        }
        return ResponseEntity.ok(DangerZoneVerifyResponse(result.valid, result.configured, result.message))
    }

    /**
     * イントラネット内で直接受けるため、詐称できるX-Forwarded-Forは見ずに接続元アドレスだけを使う。
     */
    private fun clientKeyOf(request: HttpServletRequest): String = request.remoteAddr ?: "unknown"

    private fun toResponse(result: DangerZonePasswordService.ChangeResult): ResponseEntity<DangerZonePasswordResponse> {
        val status = if (result.succeeded) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity
            .status(status)
            .body(DangerZonePasswordResponse(result.succeeded, result.message, result.configured))
    }
}
