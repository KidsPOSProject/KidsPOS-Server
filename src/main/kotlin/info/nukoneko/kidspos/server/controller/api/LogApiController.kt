package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.api.model.LogEntryResponse
import info.nukoneko.kidspos.server.controller.api.model.LogListResponse
import info.nukoneko.kidspos.server.logging.LogLevel
import info.nukoneko.kidspos.server.service.LogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.format.DateTimeFormatter

/**
 * ログAPIコントローラー
 *
 * メモリ上に保持している直近のログを参照・消去するREST APIエンドポイントを提供
 */
@RestController
@RequestMapping("/api/logs")
class LogApiController(
    private val logService: LogService,
) {
    @GetMapping
    fun getLogs(
        @RequestParam(required = false) level: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "200") limit: Int,
    ): ResponseEntity<LogListResponse> {
        val minLevel = LogLevel.from(level)
        val entries =
            logService.findRecent(minLevel, keyword, limit).map {
                LogEntryResponse(
                    timestamp = it.timestamp.format(TIMESTAMP_FORMAT),
                    level = it.level,
                    logger = it.logger,
                    thread = it.thread,
                    message = it.message,
                    stackTrace = it.stackTrace,
                )
            }

        return ResponseEntity.ok(
            LogListResponse(
                entries = entries,
                counts = logService.countsByLevel(),
                total = logService.totalCount(),
                capacity = logService.capacity(),
            ),
        )
    }

    @DeleteMapping
    fun clearLogs(): ResponseEntity<Void> {
        logService.clear()
        return ResponseEntity.noContent().build()
    }

    private companion object {
        val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
