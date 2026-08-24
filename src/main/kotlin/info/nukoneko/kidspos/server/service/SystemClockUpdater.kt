package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.TimeUnit

internal const val DEFAULT_FAKE_HWCLOCK_PATHS = "/sbin/fake-hwclock,/usr/sbin/fake-hwclock,/usr/bin/fake-hwclock"

/**
 * OSのシステム時刻を変更するコンポーネント
 */
@Component
class SystemClockUpdater(
    @Value("\${app.system-time.fake-hwclock-paths:$DEFAULT_FAKE_HWCLOCK_PATHS}")
    private val fakeHwclockPaths: List<String>,
) {
    private val logger = LoggerFactory.getLogger(SystemClockUpdater::class.java)

    fun setTime(epochMillis: Long): Result {
        val result = runCommand(buildCommand(epochMillis))
        if (result.succeeded) {
            persistToFakeHwclock()
        }
        return result
    }

    internal fun buildCommand(epochMillis: Long): List<String> {
        val seconds = epochMillis / MILLIS_PER_SECOND
        val millis = epochMillis % MILLIS_PER_SECOND
        return listOf("date", "-s", "@$seconds.${"%03d".format(millis)}")
    }

    internal fun findFakeHwclock(): String? =
        fakeHwclockPaths
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && File(it).canExecute() }

    /**
     * Raspberry Pi には RTC がなく、起動直後の時刻は /etc/fake-hwclock.data から復元される。
     * このファイルは毎時の cron と正常シャットダウン時にしか保存されないため、同期した直後に
     * 電源を落とすと最大 1 時間巻き戻る。同期のたびに保存して巻き戻りを防ぐ。
     * 保存に失敗してもシステム時刻そのものの変更結果には影響させない。
     */
    internal fun persistToFakeHwclock(): Result {
        val executable =
            findFakeHwclock()
                ?: return Result(succeeded = false, output = "fake-hwclock が見つかりません")

        val result = runCommand(listOf(executable, "save"))
        if (result.succeeded) {
            logger.info("fake-hwclock に現在時刻を保存しました")
        } else {
            logger.warn("fake-hwclock の保存に失敗しました: {}", result.output.ifBlank { "詳細不明" })
        }
        return result
    }

    internal fun runCommand(command: List<String>): Result {
        var process: Process? = null
        return try {
            process = ProcessBuilder(command).redirectErrorStream(true).start()
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return Result(succeeded = false, output = "時刻設定コマンドが応答しませんでした")
            }
            val output =
                process.inputStream
                    .bufferedReader()
                    .use { it.readText() }
                    .trim()
            Result(succeeded = process.exitValue() == 0, output = output)
        } catch (e: InterruptedException) {
            process?.destroyForcibly()
            Thread.currentThread().interrupt()
            Result(succeeded = false, output = e.message ?: e.javaClass.simpleName)
        } catch (e: Exception) {
            process?.destroyForcibly()
            Result(succeeded = false, output = e.message ?: e.javaClass.simpleName)
        }
    }

    data class Result(
        val succeeded: Boolean,
        val output: String,
    )

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val TIMEOUT_SECONDS = 10L
    }
}
