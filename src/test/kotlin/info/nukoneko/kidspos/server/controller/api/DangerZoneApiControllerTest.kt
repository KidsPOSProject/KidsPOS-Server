package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.service.DangerZonePasswordService
import info.nukoneko.kidspos.server.service.DangerZoneVerifyRateLimiter
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(SpringExtension::class)
@WebMvcTest(DangerZoneApiController::class)
@DisplayName("DangerZoneApiController")
class DangerZoneApiControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var dangerZonePasswordService: DangerZonePasswordService

    @MockBean
    private lateinit var verifyRateLimiter: DangerZoneVerifyRateLimiter

    @Test
    fun `未設定なら設定状態はfalseを返す`() {
        whenever(dangerZonePasswordService.isConfigured()).thenReturn(false)

        mockMvc
            .perform(get("/api/setting/danger-zone/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.configured").value(false))
    }

    @Test
    fun `設定済みなら設定状態はtrueを返す`() {
        whenever(dangerZonePasswordService.isConfigured()).thenReturn(true)

        mockMvc
            .perform(get("/api/setting/danger-zone/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.configured").value(true))
    }

    @Test
    fun `パスワードを設定できる`() {
        whenever(dangerZonePasswordService.changePassword(eq(null), eq("kidspos1234")))
            .thenReturn(DangerZonePasswordService.ChangeResult(true, "パスワードを保存しました", configured = true))

        mockMvc
            .perform(
                post("/api/setting/danger-zone/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"newPassword":"kidspos1234"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.message").value("パスワードを保存しました"))
    }

    @Test
    fun `現在のパスワードが違うと設定は400を返す`() {
        whenever(dangerZonePasswordService.changePassword(eq("wrong"), eq("kidspos1234")))
            .thenReturn(DangerZonePasswordService.ChangeResult(false, "現在のパスワードが違います", configured = true))

        mockMvc
            .perform(
                post("/api/setting/danger-zone/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currentPassword":"wrong","newPassword":"kidspos1234"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("現在のパスワードが違います"))
    }

    @Test
    fun `短すぎる新パスワードは400を返しサービスを呼ばない`() {
        mockMvc
            .perform(
                post("/api/setting/danger-zone/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"newPassword":"abc"}"""),
            ).andExpect(status().isBadRequest)

        verify(dangerZonePasswordService, never()).changePassword(any(), any())
    }

    @Test
    fun `パスワードを解除できる`() {
        whenever(dangerZonePasswordService.clearPassword(eq("kidspos1234")))
            .thenReturn(DangerZonePasswordService.ChangeResult(true, "パスワードを解除しました", configured = false))

        mockMvc
            .perform(
                post("/api/setting/danger-zone/password/clear")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currentPassword":"kidspos1234"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.configured").value(false))
    }

    @Test
    fun `解除に失敗すると400を返す`() {
        whenever(dangerZonePasswordService.clearPassword(eq("wrong")))
            .thenReturn(DangerZonePasswordService.ChangeResult(false, "現在のパスワードが違います", configured = true))

        mockMvc
            .perform(
                post("/api/setting/danger-zone/password/clear")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currentPassword":"wrong"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `解除で現在のパスワードが空だと400を返しサービスを呼ばない`() {
        mockMvc
            .perform(
                post("/api/setting/danger-zone/password/clear")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currentPassword":""}"""),
            ).andExpect(status().isBadRequest)

        verify(dangerZonePasswordService, never()).clearPassword(any())
    }

    @Test
    fun `正しいパスワードなら照合は200でvalidを返す`() {
        whenever(dangerZonePasswordService.verify(eq("kidspos1234")))
            .thenReturn(DangerZonePasswordService.VerifyResult(true, configured = true, message = "認証しました"))

        mockMvc
            .perform(
                post("/api/setting/danger-zone/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"password":"kidspos1234"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.configured").value(true))
    }

    @Test
    fun `誤ったパスワードでも照合は200でvalidがfalseになる`() {
        whenever(dangerZonePasswordService.verify(eq("wrong")))
            .thenReturn(DangerZonePasswordService.VerifyResult(false, configured = true, message = "パスワードが違います"))

        mockMvc
            .perform(
                post("/api/setting/danger-zone/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"password":"wrong"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.message").value("パスワードが違います"))
    }

    @Test
    fun `未設定なら照合はconfiguredがfalseになる`() {
        whenever(dangerZonePasswordService.verify(eq("kidspos1234")))
            .thenReturn(
                DangerZonePasswordService.VerifyResult(
                    false,
                    configured = false,
                    message = "サーバーにパスワードが設定されていません",
                ),
            )

        mockMvc
            .perform(
                post("/api/setting/danger-zone/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"password":"kidspos1234"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.configured").value(false))
    }

    @Test
    fun `照合でパスワードが空だと400を返しサービスを呼ばない`() {
        mockMvc
            .perform(
                post("/api/setting/danger-zone/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"password":""}"""),
            ).andExpect(status().isBadRequest)

        verify(dangerZonePasswordService, never()).verify(any())
    }

    @Test
    fun `連打が上限を超えると429を返し照合しない`() {
        whenever(verifyRateLimiter.retryAfterSeconds(any())).thenReturn(30)
        whenever(dangerZonePasswordService.isConfigured()).thenReturn(true)

        mockMvc
            .perform(
                post("/api/setting/danger-zone/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"password":"wrong"}"""),
            ).andExpect(status().isTooManyRequests)
            .andExpect(header().string("Retry-After", "30"))
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.message").value("試行回数が多すぎます。30秒後にもう一度お試しください"))

        verify(dangerZonePasswordService, never()).verify(any())
    }

    @Test
    fun `照合に失敗したら失敗として記録する`() {
        whenever(dangerZonePasswordService.verify(eq("wrong")))
            .thenReturn(DangerZonePasswordService.VerifyResult(false, configured = true, message = "パスワードが違います"))

        mockMvc
            .perform(
                post("/api/setting/danger-zone/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"password":"wrong"}"""),
            ).andExpect(status().isOk)

        verify(verifyRateLimiter).recordFailure(any())
        verify(verifyRateLimiter, never()).recordSuccess(any())
    }

    @Test
    fun `照合に成功したら記録を消す`() {
        whenever(dangerZonePasswordService.verify(eq("kidspos1234")))
            .thenReturn(DangerZonePasswordService.VerifyResult(true, configured = true, message = "認証しました"))

        mockMvc
            .perform(
                post("/api/setting/danger-zone/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"password":"kidspos1234"}"""),
            ).andExpect(status().isOk)

        verify(verifyRateLimiter).recordSuccess(any())
        verify(verifyRateLimiter, never()).recordFailure(any())
    }

    @Test
    fun `パスワード未設定の照合は失敗として数えない`() {
        whenever(dangerZonePasswordService.verify(eq("kidspos1234")))
            .thenReturn(
                DangerZonePasswordService.VerifyResult(
                    false,
                    configured = false,
                    message = "サーバーにパスワードが設定されていません",
                ),
            )

        mockMvc
            .perform(
                post("/api/setting/danger-zone/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"password":"kidspos1234"}"""),
            ).andExpect(status().isOk)

        verify(verifyRateLimiter, never()).recordFailure(any())
    }
}
