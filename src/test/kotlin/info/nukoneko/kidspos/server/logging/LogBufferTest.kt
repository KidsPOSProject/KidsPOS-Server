package info.nukoneko.kidspos.server.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("LogBuffer")
class LogBufferTest {
    @BeforeEach
    fun setUp() {
        LogBuffer.configureCapacity(LogBuffer.DEFAULT_CAPACITY)
        LogBuffer.clear()
    }

    private fun entry(message: String) =
        LogEntry(
            timestamp = LocalDateTime.of(2026, 8, 24, 12, 0, 0),
            level = "INFO",
            logger = "test",
            thread = "main",
            message = message,
            stackTrace = null,
        )

    @Test
    @DisplayName("追加した順に保持する")
    fun keepsInsertionOrder() {
        LogBuffer.add(entry("1"))
        LogBuffer.add(entry("2"))

        assertEquals(listOf("1", "2"), LogBuffer.snapshot().map { it.message })
    }

    @Test
    @DisplayName("容量を超えると古いものから捨てる")
    fun dropsOldestWhenOverCapacity() {
        LogBuffer.configureCapacity(3)

        repeat(5) { LogBuffer.add(entry(it.toString())) }

        assertEquals(listOf("2", "3", "4"), LogBuffer.snapshot().map { it.message })
        assertEquals(3, LogBuffer.size())
    }

    @Test
    @DisplayName("容量を縮めると既存の古いログも切り詰める")
    fun trimsExistingEntriesWhenCapacityShrinks() {
        repeat(10) { LogBuffer.add(entry(it.toString())) }

        LogBuffer.configureCapacity(2)

        assertEquals(listOf("8", "9"), LogBuffer.snapshot().map { it.message })
    }

    @Test
    @DisplayName("容量は下限と上限に丸められる")
    fun clampsCapacity() {
        LogBuffer.configureCapacity(0)
        assertEquals(1, LogBuffer.capacity())

        LogBuffer.configureCapacity(999999)
        assertEquals(10000, LogBuffer.capacity())
    }

    @Test
    @DisplayName("clear で空になる")
    fun clearsEntries() {
        LogBuffer.add(entry("1"))

        LogBuffer.clear()

        assertEquals(0, LogBuffer.size())
        assertTrue(LogBuffer.snapshot().isEmpty())
    }

    @Test
    @DisplayName("snapshot は元のバッファから独立している")
    fun snapshotIsIndependent() {
        LogBuffer.add(entry("1"))
        val snapshot = LogBuffer.snapshot()

        LogBuffer.clear()

        assertEquals(1, snapshot.size)
    }

    @Test
    @DisplayName("複数スレッドから追加しても容量を超えない")
    fun staysWithinCapacityUnderConcurrentWrites() {
        LogBuffer.configureCapacity(50)
        val threads = 8
        val perThread = 200
        val executor = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)

        repeat(threads) { threadIndex ->
            executor.submit {
                start.await()
                repeat(perThread) { LogBuffer.add(entry("$threadIndex-$it")) }
                done.countDown()
            }
        }

        start.countDown()
        assertTrue(done.await(10, TimeUnit.SECONDS))
        executor.shutdownNow()

        assertEquals(50, LogBuffer.size())
    }
}
