package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * クライアントから届いた時刻でサーバーの時刻を合わせる
 *
 * サーバーはイントラネットに閉じており NTP に到達できない。さらに Raspberry Pi には RTC が無く、
 * 電源を入れるたびに時刻が巻き戻る。RTC を持つタブレットやブラウザの時刻の方が信頼できるため、
 * 通信してきたクライアントの時刻を正として OS の時刻を追従させる。
 */
@Component
class ClientClockSynchronizer(
    private val systemTimeService: SystemTimeService,
    @Value("\${app.system-time.auto-sync-enabled:true}")
    private val autoSyncEnabled: Boolean,
    @Value("\${app.system-time.auto-sync-threshold-millis:30000}")
    private val thresholdMillis: Long,
    @Value("\${app.system-time.auto-sync-cooldown-millis:60000}")
    private val cooldownMillis: Long,
) {
    private val logger = LoggerFactory.getLogger(ClientClockSynchronizer::class.java)

    private val lastAttemptNanos = AtomicReference<Long?>(null)

    @Volatile
    internal var clock: () -> Long = System::currentTimeMillis

    // クールダウンの計測に currentTimeMillis を使うと、同期で時刻が巻き戻ったときに
    // 経過時間が負になって連続実行を止められなくなる
    @Volatile
    internal var monotonicNanos: () -> Long = System::nanoTime

    // date -s は外部プロセスの起動を伴い最大 10 秒かかる。会計中のリクエストを待たせないよう
    // 別スレッドへ逃がす
    @Volatile
    internal var executor: Executor =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "client-clock-sync").apply { isDaemon = true }
        }

    fun onClientTime(headerValue: String?) {
        if (!autoSyncEnabled) return

        val clientMillis = headerValue?.trim()?.toLongOrNull() ?: return
        if (clientMillis < SystemTimeService.MIN_EPOCH_MILLIS || clientMillis > SystemTimeService.MAX_EPOCH_MILLIS) {
            return
        }

        val driftMillis = clientMillis - clock()
        if (abs(driftMillis) < thresholdMillis) return
        if (!tryAcquire()) return

        executor.execute {
            val response = systemTimeService.sync(clientMillis)
            if (response.success) {
                logger.info("クライアント時刻でシステム時刻を同期しました 補正={}ms", response.driftMillis)
            } else {
                logger.warn("クライアント時刻での同期に失敗しました: {}", response.message)
            }
        }
    }

    private fun tryAcquire(): Boolean {
        val now = monotonicNanos()
        val cooldownNanos = cooldownMillis * NANOS_PER_MILLI
        while (true) {
            val last = lastAttemptNanos.get()
            if (last != null && now - last < cooldownNanos) return false
            if (lastAttemptNanos.compareAndSet(last, now)) return true
        }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
