package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

internal const val DEFAULT_VERIFY_MAX_FAILURES = 5
internal const val DEFAULT_VERIFY_BLOCK_SECONDS = 60L

/**
 * Danger Zone パスワード照合の連打を抑えるレートリミッター
 *
 * 照合は PBKDF2 を10万回反復するため、総当たりは推測される危険に加えて
 * Raspberry Pi の CPU を占有し他の会計処理まで巻き添えにする。
 * クライアントごとに失敗回数を数え、上限に達した間は照合そのものを行わせない。
 */
@Component
class DangerZoneVerifyRateLimiter(
    @Value("\${app.danger-zone.verify.max-failures:$DEFAULT_VERIFY_MAX_FAILURES}")
    private val maxFailures: Int = DEFAULT_VERIFY_MAX_FAILURES,
    @Value("\${app.danger-zone.verify.block-seconds:$DEFAULT_VERIFY_BLOCK_SECONDS}")
    private val blockSeconds: Long = DEFAULT_VERIFY_BLOCK_SECONDS,
) {
    private val logger = LoggerFactory.getLogger(DangerZoneVerifyRateLimiter::class.java)
    private val attempts = ConcurrentHashMap<String, Attempt>()

    @Volatile
    internal var timeSource: () -> Long = System::currentTimeMillis

    /**
     * ブロック中なら解除までの残り秒数を、そうでなければ0を返す。
     */
    fun retryAfterSeconds(clientKey: String): Long {
        val attempt = attempts[clientKey] ?: return 0
        val remaining = attempt.blockedUntil - timeSource()
        if (remaining <= 0) {
            return 0
        }
        return (remaining + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
    }

    fun recordFailure(clientKey: String) {
        val now = timeSource()
        val blockMillis = blockSeconds * MILLIS_PER_SECOND
        purgeExpired(now, blockMillis)

        val updated =
            attempts.compute(clientKey) { _, current ->
                val continued = current != null && now - current.lastFailureAt <= blockMillis
                val failures = if (continued) current.failures + 1 else 1
                if (failures >= maxFailures) {
                    Attempt(failures = 0, blockedUntil = now + blockMillis, lastFailureAt = now)
                } else {
                    Attempt(failures = failures, blockedUntil = 0, lastFailureAt = now)
                }
            }

        if (updated != null && updated.blockedUntil > now) {
            logger.warn("Danger zone verification blocked for {} seconds (client={})", blockSeconds, clientKey)
        }
    }

    fun recordSuccess(clientKey: String) {
        attempts.remove(clientKey)
    }

    internal fun trackedClientCount(): Int = attempts.size

    /**
     * 失敗した端末の分だけエントリが残るため、上限を超えたら期限切れの分を捨てる。
     */
    private fun purgeExpired(
        now: Long,
        blockMillis: Long,
    ) {
        if (attempts.size < MAX_TRACKED_CLIENTS) {
            return
        }
        attempts.entries.removeIf { (_, attempt) ->
            attempt.blockedUntil <= now && now - attempt.lastFailureAt > blockMillis
        }
    }

    private data class Attempt(
        val failures: Int,
        val blockedUntil: Long,
        val lastFailureAt: Long,
    )

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val MAX_TRACKED_CLIENTS = 1_000
    }
}
