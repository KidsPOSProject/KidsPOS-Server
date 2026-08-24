package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DangerZoneVerifyRateLimiter")
class DangerZoneVerifyRateLimiterTest {
    private lateinit var limiter: DangerZoneVerifyRateLimiter
    private var now = 1_700_000_000_000L

    @BeforeEach
    fun setUp() {
        limiter = DangerZoneVerifyRateLimiter(maxFailures = 3, blockSeconds = 60)
        limiter.timeSource = { now }
    }

    @Test
    fun `失敗が上限未満ならブロックしない`() {
        repeat(2) { limiter.recordFailure(CLIENT) }

        assertEquals(0, limiter.retryAfterSeconds(CLIENT))
    }

    @Test
    fun `失敗が上限に達したらブロックする`() {
        repeat(3) { limiter.recordFailure(CLIENT) }

        assertEquals(60, limiter.retryAfterSeconds(CLIENT))
    }

    @Test
    fun `ブロックは時間経過で解除される`() {
        repeat(3) { limiter.recordFailure(CLIENT) }

        now += 59_000
        assertEquals(1, limiter.retryAfterSeconds(CLIENT))

        now += 1_000
        assertEquals(0, limiter.retryAfterSeconds(CLIENT))
    }

    @Test
    fun `解除後は改めて上限まで試行できる`() {
        repeat(3) { limiter.recordFailure(CLIENT) }
        now += 60_000

        repeat(2) { limiter.recordFailure(CLIENT) }
        assertEquals(0, limiter.retryAfterSeconds(CLIENT))

        limiter.recordFailure(CLIENT)
        assertEquals(60, limiter.retryAfterSeconds(CLIENT))
    }

    @Test
    fun `成功すると失敗回数が消える`() {
        repeat(2) { limiter.recordFailure(CLIENT) }
        limiter.recordSuccess(CLIENT)

        repeat(2) { limiter.recordFailure(CLIENT) }
        assertEquals(0, limiter.retryAfterSeconds(CLIENT))
    }

    @Test
    fun `間隔が空いた失敗は数え直す`() {
        repeat(2) { limiter.recordFailure(CLIENT) }

        now += 60_001
        limiter.recordFailure(CLIENT)

        assertEquals(0, limiter.retryAfterSeconds(CLIENT))
    }

    @Test
    fun `クライアントごとに別々に数える`() {
        repeat(3) { limiter.recordFailure(CLIENT) }

        assertEquals(60, limiter.retryAfterSeconds(CLIENT))
        assertEquals(0, limiter.retryAfterSeconds("192.168.0.99"))
    }

    @Test
    fun `未知のクライアントはブロックされない`() {
        assertEquals(0, limiter.retryAfterSeconds("192.168.0.50"))
    }

    @Test
    fun `古いエントリは掃除される`() {
        repeat(1_000) { index -> limiter.recordFailure("10.0.0.$index") }
        now += 60_001

        limiter.recordFailure(CLIENT)

        assertEquals(1, limiter.trackedClientCount())
    }

    private companion object {
        const val CLIENT = "192.168.0.10"
    }
}
