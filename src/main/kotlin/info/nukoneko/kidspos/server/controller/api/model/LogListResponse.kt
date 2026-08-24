package info.nukoneko.kidspos.server.controller.api.model

data class LogEntryResponse(
    val timestamp: String,
    val level: String,
    val logger: String,
    val thread: String,
    val message: String,
    val stackTrace: String? = null,
)

data class LogListResponse(
    val entries: List<LogEntryResponse>,
    val counts: Map<String, Int>,
    val total: Int,
    val capacity: Int,
)
