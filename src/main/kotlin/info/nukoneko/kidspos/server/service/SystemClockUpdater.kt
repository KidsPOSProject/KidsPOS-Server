package info.nukoneko.kidspos.server.service

import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * OSのシステム時刻を変更するコンポーネント
 */
@Component
class SystemClockUpdater {
    fun setTime(epochMillis: Long): Result = runCommand(buildCommand(epochMillis))

    internal fun buildCommand(epochMillis: Long): List<String> {
        val seconds = epochMillis / MILLIS_PER_SECOND
        val millis = epochMillis % MILLIS_PER_SECOND
        return listOf("date", "-s", "@$seconds.${"%03d".format(millis)}")
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
