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
    @Value("\${app.system-time.backward-sync-window-millis:300000}")
    private val backwardSyncWindowMillis: Long,
) {
    private val logger = LoggerFactory.getLogger(ClientClockSynchronizer::class.java)

    private val lastAttemptNanos = AtomicReference<Long?>(null)

    private val firstAttemptNanos = AtomicReference<Long?>(null)

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

        val nowNanos = monotonicNanos()
        firstAttemptNanos.compareAndSet(null, nowNanos)

        val clientMillis = headerValue?.trim()?.toLongOrNull() ?: return
        if (clientMillis < SystemTimeService.MIN_EPOCH_MILLIS || clientMillis > SystemTimeService.MAX_EPOCH_MILLIS) {
            return
        }

        val driftMillis = clientMillis - clock()
        if (abs(driftMillis) < thresholdMillis) return
        if (driftMillis < 0 && !withinBackwardWindow(nowNanos)) {
            logger.debug("巻き戻しの猶予を過ぎているため同期しません 補正={}ms", driftMillis)
            return
        }
        if (!tryAcquire(nowNanos)) return

        executor.execute {
            val response = systemTimeService.sync(clientMillis)
            if (response.success) {
                logger.info("クライアント時刻でシステム時刻を同期しました 補正={}ms", response.driftMillis)
            } else {
                logger.warn("クライアント時刻での同期に失敗しました: {}", response.message)
            }
        }
    }

    /**
     * 起動直後は fake-hwclock から復元した時刻がずれているため巻き戻しを許す。
     * それを過ぎたら過去へ戻さず、売上やログの時系列が前後しないようにする。
     * 猶予の後にサーバー時刻が未来へ飛んだ場合は時刻同期APIから明示的に直す。
     */
    private fun withinBackwardWindow(nowNanos: Long): Boolean {
        val startedAt = firstAttemptNanos.get() ?: nowNanos
        return nowNanos - startedAt < backwardSyncWindowMillis * NANOS_PER_MILLI
    }

    private fun tryAcquire(nowNanos: Long): Boolean {
        val cooldownNanos = cooldownMillis * NANOS_PER_MILLI
        while (true) {
            val last = lastAttemptNanos.get()
            if (last != null && nowNanos - last < cooldownNanos) return false
            if (lastAttemptNanos.compareAndSet(last, nowNanos)) return true
        }
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
