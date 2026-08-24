package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.response.SystemTimeResponse
import info.nukoneko.kidspos.server.controller.dto.response.SystemTimeSyncResponse
import info.nukoneko.kidspos.server.service.SystemTimeService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(SpringExtension::class)
@WebMvcTest(SystemTimeApiController::class)
@DisplayName("SystemTimeApiController")
class SystemTimeApiControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var systemTimeService: SystemTimeService

    private val time =
        SystemTimeResponse(
            epochMillis = 1_735_689_600_000L,
            iso = "2025-01-01T09:00:00+09:00",
            display = "2025-01-01 09:00:00",
            timeZone = "Asia/Tokyo",
        )

    @Test
    fun `サーバー時刻を取得できる`() {
        whenever(systemTimeService.currentTime()).thenReturn(time)

        mockMvc
            .perform(get("/api/system/time"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.epochMillis").value(1_735_689_600_000L))
            .andExpect(jsonPath("$.display").value("2025-01-01 09:00:00"))
            .andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"))
    }

    @Test
    fun `同期に成功すると200を返す`() {
        whenever(systemTimeService.sync(eq(1_735_689_600_000L)))
            .thenReturn(
                SystemTimeSyncResponse(
                    success = true,
                    message = "システム時刻を同期しました",
                    driftMillis = 12_000,
                    requested = time,
                    current = time,
                ),
            )

        mockMvc
            .perform(
                post("/api/system/time")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"epochMillis":1735689600000}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.driftMillis").value(12_000))
    }

    @Test
    fun `同期に失敗すると500と理由を返す`() {
        whenever(systemTimeService.sync(eq(1_735_689_600_000L)))
            .thenReturn(
                SystemTimeSyncResponse(
                    success = false,
                    message = "システム時刻の変更に失敗しました",
                    driftMillis = 0,
                    requested = time,
                    current = time,
                ),
            )

        mockMvc
            .perform(
                post("/api/system/time")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"epochMillis":1735689600000}"""),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("システム時刻の変更に失敗しました"))
    }

    @Test
    fun `範囲外の時刻は400を返しサービスを呼ばない`() {
        mockMvc
            .perform(
                post("/api/system/time")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"epochMillis":1}"""),
            ).andExpect(status().isBadRequest)

        verify(systemTimeService, never()).sync(any())
    }
}
