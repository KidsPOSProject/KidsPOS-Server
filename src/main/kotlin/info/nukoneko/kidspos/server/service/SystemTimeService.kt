package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.response.SystemTimeResponse
import info.nukoneko.kidspos.server.controller.dto.response.SystemTimeSyncResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * サーバー時刻の参照と同期を行うサービス
 *
 * イントラネット内でNTPに到達できない運用のため、クライアントの時刻を正としてOSの時刻を合わせる。
 */
@Service
class SystemTimeService(
    private val systemClockUpdater: SystemClockUpdater,
) {
    private val logger = LoggerFactory.getLogger(SystemTimeService::class.java)

    fun currentTime(): SystemTimeResponse = toResponse(System.currentTimeMillis())

    fun sync(epochMillis: Long): SystemTimeSyncResponse {
        val driftMillis = epochMillis - System.currentTimeMillis()

        if (epochMillis < MIN_EPOCH_MILLIS || epochMillis > MAX_EPOCH_MILLIS) {
            return buildResponse(false, "指定された時刻が有効な範囲外です", epochMillis, driftMillis)
        }

        val result = systemClockUpdater.setTime(epochMillis)
        if (!result.succeeded) {
            logger.warn("システム時刻の同期に失敗しました: {}", result.output)
            val detail = result.output.ifBlank { "詳細不明" }
            return buildResponse(
                false,
                "システム時刻の変更に失敗しました。サーバーに時刻変更の権限がない可能性があります: $detail",
                epochMillis,
                driftMillis,
            )
        }

        logger.info("システム時刻を同期しました 補正={}ms", driftMillis)
        return buildResponse(true, "システム時刻を同期しました", epochMillis, driftMillis)
    }

    private fun buildResponse(
        success: Boolean,
        message: String,
        requestedMillis: Long,
        driftMillis: Long,
    ) = SystemTimeSyncResponse(
        success = success,
        message = message,
        driftMillis = driftMillis,
        requested = toResponse(requestedMillis),
        current = toResponse(System.currentTimeMillis()),
    )

    private fun toResponse(epochMillis: Long): SystemTimeResponse {
        val zone = ZoneId.systemDefault()
        val zoned = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return SystemTimeResponse(
            epochMillis = epochMillis,
            iso = zoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            display = zoned.format(DISPLAY_FORMAT),
            timeZone = zone.id,
        )
    }

    companion object {
        const val MIN_EPOCH_MILLIS = 1_577_836_800_000L
        const val MAX_EPOCH_MILLIS = 4_102_444_800_000L
        private val DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
