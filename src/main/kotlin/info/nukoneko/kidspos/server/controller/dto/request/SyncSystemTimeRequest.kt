package info.nukoneko.kidspos.server.controller.dto.request

import info.nukoneko.kidspos.server.service.SystemTimeService
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * サーバー時刻同期のリクエストDTO
 */
data class SyncSystemTimeRequest(
    @field:Min(value = SystemTimeService.MIN_EPOCH_MILLIS, message = "時刻が古すぎます")
    @field:Max(value = SystemTimeService.MAX_EPOCH_MILLIS, message = "時刻が未来すぎます")
    val epochMillis: Long,
)
