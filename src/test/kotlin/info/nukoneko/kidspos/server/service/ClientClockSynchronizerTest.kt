package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.response.SystemTimeResponse
import info.nukoneko.kidspos.server.controller.dto.response.SystemTimeSyncResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.Executor

@ExtendWith(MockitoExtension::class)
class ClientClockSynchronizerTest {
    @Mock
    private lateinit var systemTimeService: SystemTimeService

    private var currentMillis = BASE_MILLIS
    private var currentNanos = 0L

    private fun synchronizer(
        enabled: Boolean = true,
        thresholdMillis: Long = 30_000,
        cooldownMillis: Long = 60_000,
    ): ClientClockSynchronizer =
        ClientClockSynchronizer(systemTimeService, enabled, thresholdMillis, cooldownMillis).apply {
            clock = { currentMillis }
            monotonicNanos = { currentNanos }
            executor = Executor { it.run() }
        }

    private fun syncResult(success: Boolean) =
        SystemTimeSyncResponse(
            success = success,
            message = if (success) "システム時刻を同期しました" else "システム時刻の変更に失敗しました",
            driftMillis = 0,
            requested = response(BASE_MILLIS),
            current = response(BASE_MILLIS),
        )

    private fun response(epochMillis: Long) =
        SystemTimeResponse(
            epochMillis = epochMillis,
            iso = "",
            display = "",
            timeZone = "UTC",
        )

    @BeforeEach
    fun setUp() {
        currentMillis = BASE_MILLIS
        currentNanos = 0L
    }

    @Test
    fun `閾値を超えてずれていると同期する`() {
        val requested = BASE_MILLIS + 60_000
        whenever(systemTimeService.sync(eq(requested))).thenReturn(syncResult(true))

        synchronizer().onClientTime(requested.toString())

        verify(systemTimeService).sync(eq(requested))
    }

    @Test
    fun `サーバーが進んでいる場合も巻き戻して同期する`() {
        val requested = BASE_MILLIS - 60_000
        whenever(systemTimeService.sync(eq(requested))).thenReturn(syncResult(true))

        synchronizer().onClientTime(requested.toString())

        verify(systemTimeService).sync(eq(requested))
    }

    @Test
    fun `閾値未満のずれでは同期しない`() {
        synchronizer().onClientTime((BASE_MILLIS + 29_999).toString())

        verify(systemTimeService, never()).sync(any())
    }

    @Test
    fun `ヘッダーが無い場合は同期しない`() {
        synchronizer().onClientTime(null)

        verify(systemTimeService, never()).sync(any())
    }

    @Test
    fun `数値でないヘッダーは無視する`() {
        synchronizer().onClientTime("not-a-number")

        verify(systemTimeService, never()).sync(any())
    }

    @Test
    fun `前後の空白を除いて解釈する`() {
        val requested = BASE_MILLIS + 60_000
        whenever(systemTimeService.sync(eq(requested))).thenReturn(syncResult(true))

        synchronizer().onClientTime("  $requested  ")

        verify(systemTimeService).sync(eq(requested))
    }

    @Test
    fun `範囲外に古い時刻は無視する`() {
        synchronizer().onClientTime((SystemTimeService.MIN_EPOCH_MILLIS - 1).toString())

        verify(systemTimeService, never()).sync(any())
    }

    @Test
    fun `範囲外に新しい時刻は無視する`() {
        synchronizer().onClientTime((SystemTimeService.MAX_EPOCH_MILLIS + 1).toString())

        verify(systemTimeService, never()).sync(any())
    }

    @Test
    fun `無効化されていると同期しない`() {
        synchronizer(enabled = false).onClientTime((BASE_MILLIS + 60_000).toString())

        verify(systemTimeService, never()).sync(any())
    }

    @Test
    fun `クールダウン中は再同期しない`() {
        whenever(systemTimeService.sync(any())).thenReturn(syncResult(true))
        val target = synchronizer()

        target.onClientTime((BASE_MILLIS + 60_000).toString())
        currentNanos += 59_999 * NANOS_PER_MILLI
        target.onClientTime((BASE_MILLIS + 120_000).toString())

        verify(systemTimeService).sync(eq(BASE_MILLIS + 60_000))
        verify(systemTimeService, never()).sync(eq(BASE_MILLIS + 120_000))
    }

    @Test
    fun `クールダウンを過ぎると再同期する`() {
        whenever(systemTimeService.sync(any())).thenReturn(syncResult(true))
        val target = synchronizer()

        target.onClientTime((BASE_MILLIS + 60_000).toString())
        currentNanos += 60_000 * NANOS_PER_MILLI
        target.onClientTime((BASE_MILLIS + 120_000).toString())

        verify(systemTimeService).sync(eq(BASE_MILLIS + 60_000))
        verify(systemTimeService).sync(eq(BASE_MILLIS + 120_000))
    }

    @Test
    fun `同期に失敗してもクールダウンは働く`() {
        whenever(systemTimeService.sync(any())).thenReturn(syncResult(false))
        val target = synchronizer()

        target.onClientTime((BASE_MILLIS + 60_000).toString())
        target.onClientTime((BASE_MILLIS + 120_000).toString())

        verify(systemTimeService).sync(eq(BASE_MILLIS + 60_000))
        verify(systemTimeService, never()).sync(eq(BASE_MILLIS + 120_000))
    }

    private companion object {
        const val BASE_MILLIS = 1_700_000_000_000L
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
