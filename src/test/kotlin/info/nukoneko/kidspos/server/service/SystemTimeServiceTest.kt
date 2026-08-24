package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.time.ZoneId
import kotlin.math.abs

@ExtendWith(MockitoExtension::class)
class SystemTimeServiceTest {
    @Mock
    private lateinit var systemClockUpdater: SystemClockUpdater

    private lateinit var service: SystemTimeService

    @BeforeEach
    fun setUp() {
        service = SystemTimeService(systemClockUpdater)
    }

    @Test
    fun `現在時刻を取得できる`() {
        val before = System.currentTimeMillis()
        val response = service.currentTime()
        val after = System.currentTimeMillis()

        assertTrue(response.epochMillis in before..after)
        assertEquals(ZoneId.systemDefault().id, response.timeZone)
        assertTrue(response.display.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")))
        assertTrue(response.iso.isNotBlank())
    }

    @Test
    fun `時刻同期に成功すると成功レスポンスを返す`() {
        val requested = System.currentTimeMillis() + 60_000
        whenever(systemClockUpdater.setTime(eq(requested)))
            .thenReturn(SystemClockUpdater.Result(succeeded = true, output = ""))

        val response = service.sync(requested)

        assertTrue(response.success)
        assertEquals("システム時刻を同期しました", response.message)
        assertEquals(requested, response.requested.epochMillis)
        assertTrue(abs(response.driftMillis - 60_000) < 5_000)
        verify(systemClockUpdater).setTime(eq(requested))
    }

    @Test
    fun `時刻変更コマンドが失敗すると理由付きで失敗レスポンスを返す`() {
        val requested = System.currentTimeMillis()
        whenever(systemClockUpdater.setTime(eq(requested)))
            .thenReturn(SystemClockUpdater.Result(succeeded = false, output = "date: cannot set date: Operation not permitted"))

        val response = service.sync(requested)

        assertFalse(response.success)
        assertTrue(response.message.contains("Operation not permitted"))
    }

    @Test
    fun `時刻変更コマンドの出力が空でも失敗理由を返す`() {
        val requested = System.currentTimeMillis()
        whenever(systemClockUpdater.setTime(eq(requested)))
            .thenReturn(SystemClockUpdater.Result(succeeded = false, output = "  "))

        val response = service.sync(requested)

        assertFalse(response.success)
        assertTrue(response.message.contains("詳細不明"))
    }

    @Test
    fun `古すぎる時刻では時刻変更を実行しない`() {
        val response = service.sync(SystemTimeService.MIN_EPOCH_MILLIS - 1)

        assertFalse(response.success)
        assertEquals("指定された時刻が有効な範囲外です", response.message)
        verify(systemClockUpdater, never()).setTime(any())
    }

    @Test
    fun `未来すぎる時刻では時刻変更を実行しない`() {
        val response = service.sync(SystemTimeService.MAX_EPOCH_MILLIS + 1)

        assertFalse(response.success)
        assertEquals("指定された時刻が有効な範囲外です", response.message)
        verify(systemClockUpdater, never()).setTime(any())
    }

    @Test
    fun `境界値の時刻は受け付ける`() {
        whenever(systemClockUpdater.setTime(eq(SystemTimeService.MIN_EPOCH_MILLIS)))
            .thenReturn(SystemClockUpdater.Result(succeeded = true, output = ""))

        val response = service.sync(SystemTimeService.MIN_EPOCH_MILLIS)

        assertTrue(response.success)
    }
}
