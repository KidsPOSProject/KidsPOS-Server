package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.logging.LogBuffer
import info.nukoneko.kidspos.server.logging.LogEntry
import info.nukoneko.kidspos.server.logging.LogLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("LogService")
class LogServiceTest {
    private val service = LogService()

    @BeforeEach
    fun setUp() {
        LogBuffer.configureCapacity(LogBuffer.DEFAULT_CAPACITY)
        LogBuffer.clear()
    }

    private fun add(
        level: String,
        message: String,
        logger: String = "info.nukoneko.kidspos.Sample",
        stackTrace: String? = null,
    ) {
        LogBuffer.add(
            LogEntry(
                timestamp = LocalDateTime.of(2026, 8, 24, 12, 0, 0),
                level = level,
                logger = logger,
                thread = "main",
                message = message,
                stackTrace = stackTrace,
            ),
        )
    }

    @Test
    @DisplayName("新しいログから順に返す")
    fun returnsNewestFirst() {
        add("INFO", "1")
        add("INFO", "2")
        add("INFO", "3")

        assertEquals(listOf("3", "2", "1"), service.findRecent().map { it.message })
    }

    @Test
    @DisplayName("指定レベル以上だけを返す")
    fun filtersByMinimumLevel() {
        add("DEBUG", "debug")
        add("INFO", "info")
        add("WARN", "warn")
        add("ERROR", "error")

        val result = service.findRecent(minLevel = LogLevel.WARN).map { it.message }

        assertEquals(listOf("error", "warn"), result)
    }

    @Test
    @DisplayName("レベル未指定なら全件返す")
    fun returnsAllWhenLevelIsNull() {
        add("DEBUG", "debug")
        add("ERROR", "error")

        assertEquals(2, service.findRecent(minLevel = null).size)
    }

    @Test
    @DisplayName("メッセージのキーワードで絞り込む")
    fun filtersByMessageKeyword() {
        add("ERROR", "在庫がありません")
        add("ERROR", "印刷に失敗しました")

        val result = service.findRecent(keyword = "在庫").map { it.message }

        assertEquals(listOf("在庫がありません"), result)
    }

    @Test
    @DisplayName("ロガー名とスタックトレースもキーワード検索の対象にする")
    fun filtersByLoggerAndStackTrace() {
        add("ERROR", "失敗", logger = "info.nukoneko.kidspos.server.service.SaleService")
        add("ERROR", "失敗", logger = "other", stackTrace = "java.lang.IllegalStateException")

        assertEquals(1, service.findRecent(keyword = "SaleService").size)
        assertEquals(1, service.findRecent(keyword = "IllegalStateException").size)
    }

    @Test
    @DisplayName("キーワードは大文字小文字を区別しない")
    fun keywordIsCaseInsensitive() {
        add("ERROR", "Printer Not Found")

        assertEquals(1, service.findRecent(keyword = "printer not found").size)
    }

    @Test
    @DisplayName("空白だけのキーワードは無視する")
    fun ignoresBlankKeyword() {
        add("INFO", "1")

        assertEquals(1, service.findRecent(keyword = "   ").size)
    }

    @Test
    @DisplayName("件数上限を超えたら新しい方から切り取る")
    fun appliesLimit() {
        repeat(10) { add("INFO", it.toString()) }

        val result = service.findRecent(limit = 3).map { it.message }

        assertEquals(listOf("9", "8", "7"), result)
    }

    @Test
    @DisplayName("不正な件数は許容範囲に丸める")
    fun clampsLimit() {
        repeat(5) { add("INFO", it.toString()) }

        assertEquals(1, service.findRecent(limit = 0).size)
        assertEquals(5, service.findRecent(limit = 99999).size)
    }

    @Test
    @DisplayName("レベル別の件数を返す")
    fun countsByLevel() {
        add("ERROR", "1")
        add("ERROR", "2")
        add("WARN", "3")

        val counts = service.countsByLevel()

        assertEquals(2, counts["ERROR"])
        assertEquals(1, counts["WARN"])
        assertEquals(0, counts["INFO"])
        assertTrue(counts.keys.containsAll(LogLevel.entries.map { it.name }))
    }

    @Test
    @DisplayName("保持件数と上限を返す")
    fun exposesBufferState() {
        LogBuffer.configureCapacity(7)
        add("INFO", "1")

        assertEquals(1, service.totalCount())
        assertEquals(7, service.capacity())
    }

    @Test
    @DisplayName("clear で保持しているログを消す")
    fun clearsBuffer() {
        add("INFO", "1")

        service.clear()

        assertEquals(0, service.totalCount())
    }

    @Test
    @DisplayName("未知のレベルは絞り込みで除外しない")
    fun keepsUnknownLevel() {
        add("FATAL", "未知のレベル")

        assertEquals(1, service.findRecent(minLevel = LogLevel.ERROR).size)
    }
}
