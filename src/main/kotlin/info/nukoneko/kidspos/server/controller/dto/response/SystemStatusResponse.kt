package info.nukoneko.kidspos.server.controller.dto.response

data class SystemStatusResponse(
    val status: String,
    val apiVersion: String,
    val printer: PrinterStatusResponse,
)

data class PrinterStatusResponse(
    val configured: Boolean,
    val reachable: Boolean,
    val total: Int,
    val reachableCount: Int,
)
