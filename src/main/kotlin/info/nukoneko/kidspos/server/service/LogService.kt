package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.logging.LogBuffer
import info.nukoneko.kidspos.server.logging.LogEntry
import info.nukoneko.kidspos.server.logging.LogLevel
import org.springframework.stereotype.Service

/**
 * メモリ上に保持している直近のログを参照するサービス
 */
@Service
class LogService {
    fun findRecent(
        minLevel: LogLevel? = null,
        keyword: String? = null,
        limit: Int = DEFAULT_LIMIT,
    ): List<LogEntry> {
        val effectiveLimit = limit.coerceIn(1, MAX_LIMIT)
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotEmpty() }

        return LogBuffer
            .snapshot()
            .asReversed()
            .asSequence()
            .filter { matchesLevel(it, minLevel) }
            .filter { matchesKeyword(it, normalizedKeyword) }
            .take(effectiveLimit)
            .toList()
    }

    fun countsByLevel(): Map<String, Int> {
        val counts = LogBuffer.snapshot().groupingBy { it.level.uppercase() }.eachCount()
        return LogLevel.entries.associate { it.name to (counts[it.name] ?: 0) }
    }

    fun totalCount(): Int = LogBuffer.size()

    fun capacity(): Int = LogBuffer.capacity()

    fun clear() = LogBuffer.clear()

    private fun matchesLevel(
        entry: LogEntry,
        minLevel: LogLevel?,
    ): Boolean {
        if (minLevel == null) return true
        val level = LogLevel.from(entry.level) ?: return true
        return level.severity >= minLevel.severity
    }

    private fun matchesKeyword(
        entry: LogEntry,
        keyword: String?,
    ): Boolean {
        if (keyword == null) return true
        return entry.message.contains(keyword, ignoreCase = true) ||
            entry.logger.contains(keyword, ignoreCase = true) ||
            entry.stackTrace?.contains(keyword, ignoreCase = true) == true
    }

    companion object {
        const val DEFAULT_LIMIT = 200
        const val MAX_LIMIT = 1000
    }
}
