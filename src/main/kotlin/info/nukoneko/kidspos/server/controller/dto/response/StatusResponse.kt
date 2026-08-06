package info.nukoneko.kidspos.server.controller.dto.response

/**
 * サーバーステータスのレスポンスDTO
 */
data class StatusResponse(
    val status: String,
    val version: String,
    val apiVersion: Int,
    val printer: PrinterStatusResponse? = null,
)

/**
 * プリンター到達性のレスポンスDTO
 */
data class PrinterStatusResponse(
    val configured: Boolean,
    val reachable: Boolean,
    val total: Int,
    val reachableCount: Int,
)
