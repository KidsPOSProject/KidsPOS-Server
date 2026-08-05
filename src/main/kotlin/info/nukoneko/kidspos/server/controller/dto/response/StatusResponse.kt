package info.nukoneko.kidspos.server.controller.dto.response

/**
 * サーバーステータスのレスポンスDTO
 */
data class StatusResponse(
    val status: String,
    val version: String,
    val apiVersion: Int,
)
