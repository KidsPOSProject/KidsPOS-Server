package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SystemClockUpdaterTest {
    private val updater = SystemClockUpdater(DEFAULT_FAKE_HWCLOCK_PATHS.split(","))

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

    @Test
    fun `実行可能なfake-hwclockが無ければnullを返す`(
        @TempDir tempDir: File,
    ) {
        val missing = File(tempDir, "fake-hwclock")
        val updaterWithoutHwclock = SystemClockUpdater(listOf(missing.absolutePath, " "))

        assertNull(updaterWithoutHwclock.findFakeHwclock())
    }

    @Test
    fun `候補のうち実行可能な最初のパスを選ぶ`(
        @TempDir tempDir: File,
    ) {
        val missing = File(tempDir, "missing")
        val executable = writeExecutable(tempDir, "fake-hwclock", "exit 0")

        val updaterWithHwclock = SystemClockUpdater(listOf(missing.absolutePath, executable.absolutePath))

        assertEquals(executable.absolutePath, updaterWithHwclock.findFakeHwclock())
    }

    @Test
    fun `fake-hwclockが無い場合は保存に失敗した結果を返す`(
        @TempDir tempDir: File,
    ) {
        val updaterWithoutHwclock = SystemClockUpdater(listOf(File(tempDir, "missing").absolutePath))

        val result = updaterWithoutHwclock.persistToFakeHwclock()

        assertFalse(result.succeeded)
        assertTrue(result.output.contains("fake-hwclock"))
    }

    @Test
    fun `fake-hwclockにsaveを渡して実行する`(
        @TempDir tempDir: File,
    ) {
        val marker = File(tempDir, "marker")
        val executable = writeExecutable(tempDir, "fake-hwclock", "echo \"\$1\" > ${marker.absolutePath}")

        val result = SystemClockUpdater(listOf(executable.absolutePath)).persistToFakeHwclock()

        assertTrue(result.succeeded)
        assertEquals("save", marker.readText().trim())
    }

    @Test
    fun `fake-hwclockの保存に失敗しても失敗理由を返す`(
        @TempDir tempDir: File,
    ) {
        val executable = writeExecutable(tempDir, "fake-hwclock", "echo 'permission denied' >&2; exit 1")

        val result = SystemClockUpdater(listOf(executable.absolutePath)).persistToFakeHwclock()

        assertFalse(result.succeeded)
        assertEquals("permission denied", result.output)
    }

    private fun writeExecutable(
        directory: File,
        name: String,
        body: String,
    ): File =
        File(directory, name).apply {
            writeText("#!/bin/sh\n$body\n")
            setExecutable(true)
        }
}
