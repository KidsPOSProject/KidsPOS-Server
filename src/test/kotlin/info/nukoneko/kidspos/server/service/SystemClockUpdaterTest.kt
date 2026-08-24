package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SystemClockUpdaterTest {
    private val updater = SystemClockUpdater()

    @Test
    fun `エポックミリ秒を秒とミリ秒に分けたdateコマンドを組み立てる`() {
        assertEquals(listOf("date", "-s", "@1735689600.000"), updater.buildCommand(1_735_689_600_000L))
    }

    @Test
    fun `ミリ秒はゼロ埋め三桁で表現する`() {
        assertEquals(listOf("date", "-s", "@1735689600.007"), updater.buildCommand(1_735_689_600_007L))
        assertEquals(listOf("date", "-s", "@1735689600.070"), updater.buildCommand(1_735_689_600_070L))
    }

    @Test
    fun `コマンドが正常終了すると成功として扱う`() {
        val result = updater.runCommand(listOf("sh", "-c", "exit 0"))

        assertTrue(result.succeeded)
    }

    @Test
    fun `コマンドが異常終了すると標準エラー出力を含めて失敗として扱う`() {
        val result = updater.runCommand(listOf("sh", "-c", "echo 'Operation not permitted' >&2; exit 1"))

        assertFalse(result.succeeded)
        assertEquals("Operation not permitted", result.output)
    }

    @Test
    fun `コマンドが存在しない場合も失敗として扱う`() {
        val result = updater.runCommand(listOf("kidspos-no-such-command"))

        assertFalse(result.succeeded)
        assertTrue(result.output.isNotBlank())
    }
}
