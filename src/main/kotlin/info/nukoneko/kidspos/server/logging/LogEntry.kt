package info.nukoneko.kidspos.server.logging

import java.time.LocalDateTime

/**
 * メモリ上に保持する1件分のログ記録
 */
data class LogEntry(
    val timestamp: LocalDateTime,
    val level: String,
    val logger: String,
    val thread: String,
    val message: String,
    val stackTrace: String?,
)
