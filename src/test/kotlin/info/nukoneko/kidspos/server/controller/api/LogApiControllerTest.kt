package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.logging.LogEntry
import info.nukoneko.kidspos.server.logging.LogLevel
import info.nukoneko.kidspos.server.service.LogService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@WebMvcTest(LogApiController::class)
@DisplayName("LogApiController")
class LogApiControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var logService: LogService

    private val entry =
        LogEntry(
            timestamp = LocalDateTime.of(2026, 8, 24, 12, 34, 56, 789_000_000),
            level = "ERROR",
            logger = "info.nukoneko.kidspos.server.service.SaleService",
            thread = "http-nio-8080-exec-1",
            message = "印刷に失敗しました",
            stackTrace = "java.lang.IllegalStateException",
        )

    private fun stubDefaults() {
        whenever(logService.countsByLevel()).thenReturn(mapOf("ERROR" to 1))
        whenever(logService.totalCount()).thenReturn(1)
        whenever(logService.capacity()).thenReturn(500)
    }

    @Test
    @DisplayName("直近のログをJSONで返す")
    fun returnsLogsAsJson() {
        stubDefaults()
        whenever(logService.findRecent(anyOrNull(), anyOrNull(), any())).thenReturn(listOf(entry))

        mockMvc
            .perform(get("/api/logs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.entries[0].level").value("ERROR"))
            .andExpect(jsonPath("$.entries[0].timestamp").value("2026-08-24 12:34:56.789"))
            .andExpect(jsonPath("$.entries[0].message").value("印刷に失敗しました"))
            .andExpect(jsonPath("$.entries[0].stackTrace").value("java.lang.IllegalStateException"))
            .andExpect(jsonPath("$.counts.ERROR").value(1))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.capacity").value(500))
    }

    @Test
    @DisplayName("既定の件数は200件")
    fun usesDefaultLimit() {
        stubDefaults()
        whenever(logService.findRecent(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        mockMvc.perform(get("/api/logs")).andExpect(status().isOk)

        verify(logService).findRecent(eq(null), eq(null), eq(200))
    }

    @Test
    @DisplayName("レベル・キーワード・件数をサービスへ渡す")
    fun passesQueryParameters() {
        stubDefaults()
        whenever(logService.findRecent(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        mockMvc
            .perform(get("/api/logs").param("level", "warn").param("keyword", "印刷").param("limit", "50"))
            .andExpect(status().isOk)

        verify(logService).findRecent(eq(LogLevel.WARN), eq("印刷"), eq(50))
    }

    @Test
    @DisplayName("未知のレベルはフィルタなしとして扱う")
    fun ignoresUnknownLevel() {
        stubDefaults()
        whenever(logService.findRecent(anyOrNull(), anyOrNull(), any())).thenReturn(emptyList())

        mockMvc.perform(get("/api/logs").param("level", "FATAL")).andExpect(status().isOk)

        verify(logService).findRecent(eq(null), eq(null), eq(200))
    }

    @Test
    @DisplayName("DELETE でログを消去し204を返す")
    fun clearsLogs() {
        mockMvc
            .perform(delete("/api/logs"))
            .andExpect(status().isNoContent)

        verify(logService).clear()
    }
}
