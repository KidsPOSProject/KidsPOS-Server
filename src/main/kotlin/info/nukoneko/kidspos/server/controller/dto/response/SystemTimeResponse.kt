package info.nukoneko.kidspos.server.controller.dto.response

/**
 * サーバー時刻のレスポンスDTO
 */
data class SystemTimeResponse(
    val epochMillis: Long,
    val iso: String,
    val display: String,
    val timeZone: String,
)

/**
 * サーバー時刻同期結果のレスポンスDTO
 */
data class SystemTimeSyncResponse(
    val success: Boolean,
    val message: String,
    val driftMillis: Long,
    val requested: SystemTimeResponse,
    val current: SystemTimeResponse,
)
