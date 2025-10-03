package info.nukoneko.kidspos.server.monitor

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.NetworkInterface
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * サーバーステータスモニター
 *
 * サーバー起動後、定期的に以下の情報を出力します:
 * - 現在時刻
 * - IPアドレス
 * - サーバー状態
 */
@Component
class ServerStatusMonitor {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scheduler = Executors.newScheduledThreadPool(1)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @EventListener(ApplicationReadyEvent::class)
    fun startMonitoring() {
        logger.info("サーバーステータスモニターを起動しました")

        // 初回即座に出力
        printStatus()

        // 10秒ごとに定期実行
        scheduler.scheduleAtFixedRate(
            { printStatus() },
            10,
            10,
            TimeUnit.SECONDS,
        )
    }

    private fun printStatus() {
        val currentTime = LocalDateTime.now().format(dateFormatter)
        val ipAddresses = getIpAddresses()
        val status = "接続可能"

        val statusMessage =
            buildString {
                appendLine("=" * 60)
                appendLine("🖥️  サーバーステータス")
                appendLine("=" * 60)
                appendLine("⏰ 時刻: $currentTime")
                appendLine("🌐 IPアドレス:")
                ipAddresses.forEach { ip ->
                    appendLine("   - $ip")
                }
                appendLine("✅ 状態: $status")
                appendLine("=" * 60)
            }

        logger.info("\n$statusMessage")
    }

    private fun getIpAddresses(): List<String> {
        val addresses = mutableListOf<String>()

        try {
            NetworkInterface
                .getNetworkInterfaces()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filter { !it.isLoopbackAddress && it.hostAddress.contains(".") }
                .forEach { addresses.add("${it.hostAddress}") }
        } catch (e: Exception) {
            logger.warn("IPアドレスの取得に失敗しました: ${e.message}")
            // フォールバック
            try {
                addresses.add(InetAddress.getLocalHost().hostAddress)
            } catch (ex: Exception) {
                addresses.add("取得失敗")
            }
        }

        return addresses.ifEmpty { listOf("IPアドレスが見つかりませんでした") }
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}
