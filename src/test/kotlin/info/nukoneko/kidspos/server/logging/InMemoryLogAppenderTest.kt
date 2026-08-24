package info.nukoneko.kidspos.server.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@DisplayName("InMemoryLogAppender")
class InMemoryLogAppenderTest {
    private lateinit var appender: InMemoryLogAppender

    @BeforeEach
    fun setUp() {
        LogBuffer.configureCapacity(LogBuffer.DEFAULT_CAPACITY)
        LogBuffer.clear()
        appender = InMemoryLogAppender()
        appender.context = LoggerContext()
    }

    private fun event(
        level: Level = Level.ERROR,
        message: String = "問題が発生しました",
        throwable: Throwable? = null,
    ): LoggingEvent {
        val logged = LoggingEvent()
        logged.level = level
        logged.loggerName = "info.nukoneko.kidspos.server.service.SaleService"
        logged.threadName = "http-nio-8080-exec-1"
        logged.message = message
        logged.timeStamp = 1_756_000_000_000
        if (throwable != null) {
            logged.setThrowableProxy(ThrowableProxy(throwable))
        }
        return logged
    }

    @Test
    @DisplayName("ログイベントをバッファへ変換して積む")
    fun appendsEventAsEntry() {
        appender.start()

        appender.doAppend(event())

        val entries = LogBuffer.snapshot()
        assertEquals(1, entries.size)
        val entry = entries.first()
        assertEquals("ERROR", entry.level)
        assertEquals("info.nukoneko.kidspos.server.service.SaleService", entry.logger)
        assertEquals("http-nio-8080-exec-1", entry.thread)
        assertEquals("問題が発生しました", entry.message)
        assertNull(entry.stackTrace)
    }

    @Test
    @DisplayName("例外があればスタックトレースを保持する")
    fun keepsStackTrace() {
        appender.start()

        appender.doAppend(event(throwable = IllegalStateException("在庫がありません")))

        val entry = LogBuffer.snapshot().first()
        assertNotNull(entry.stackTrace)
        assertTrue(entry.stackTrace!!.contains("IllegalStateException"))
        assertTrue(entry.stackTrace!!.contains("在庫がありません"))
    }

    @Test
    @DisplayName("start で設定した容量がバッファへ反映される")
    fun appliesConfiguredCapacityOnStart() {
        appender.capacity = 3

        appender.start()

        assertEquals(3, LogBuffer.capacity())
        repeat(5) { appender.doAppend(event(message = it.toString())) }
        assertEquals(3, LogBuffer.size())
    }

    @Test
    @DisplayName("長すぎるメッセージは切り詰める")
    fun truncatesLongMessage() {
        appender.start()

        appender.doAppend(event(message = "あ".repeat(5000)))

        val entry = LogBuffer.snapshot().first()
        assertEquals(2000, entry.message.length)
    }

    @Test
    @DisplayName("タイムスタンプをローカル日時へ変換する")
    fun convertsTimestamp() {
        appender.start()

        appender.doAppend(event())

        val expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(1_756_000_000_000), ZoneId.systemDefault())
        val entry = LogBuffer.snapshot().first()
        assertEquals(expected, entry.timestamp)
    }
}
