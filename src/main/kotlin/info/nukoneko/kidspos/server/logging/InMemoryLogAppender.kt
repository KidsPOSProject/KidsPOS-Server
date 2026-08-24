package info.nukoneko.kidspos.server.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 直近のログを [LogBuffer] に蓄える logback appender
 *
 * ログ画面から参照するため、ファイル出力とは別にメモリ上へ保持する。
 */
class InMemoryLogAppender : AppenderBase<ILoggingEvent>() {
    var capacity: Int = LogBuffer.DEFAULT_CAPACITY

    override fun start() {
        LogBuffer.configureCapacity(capacity)
        super.start()
    }

    override fun append(eventObject: ILoggingEvent) {
        LogBuffer.add(toEntry(eventObject))
    }

    private fun toEntry(event: ILoggingEvent): LogEntry =
        LogEntry(
            timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.timeStamp), ZoneId.systemDefault()),
            level = event.level.levelStr,
            logger = event.loggerName,
            thread = event.threadName,
            message = event.formattedMessage.orEmpty().take(MAX_MESSAGE_LENGTH),
            stackTrace =
                event.throwableProxy?.let {
                    ThrowableProxyUtil.asString(it).take(MAX_STACK_TRACE_LENGTH)
                },
        )

    private companion object {
        const val MAX_MESSAGE_LENGTH = 2000
        const val MAX_STACK_TRACE_LENGTH = 8000
    }
}
