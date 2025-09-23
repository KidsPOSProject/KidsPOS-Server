package info.nukoneko.kidspos.server.performance

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultHandler
import kotlin.system.measureTimeMillis
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * Base class for performance testing
 * Provides utilities for measuring response times and load testing
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class PerformanceTestBase {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    protected val responseTimesMs = CopyOnWriteArrayList<Long>()

    @BeforeEach
    fun clearMetrics() {
        responseTimesMs.clear()
    }

    /**
     * Measures the execution time of a request
     */
    protected fun measureRequestTime(request: () -> MvcResult): Long {
        val time = measureTimeMillis {
            request()
        }
        responseTimesMs.add(time)
        return time
    }

    /**
     * Calculates average response time
     */
    protected fun calculateAverageResponseTime(): Double {
        return if (responseTimesMs.isNotEmpty()) {
            responseTimesMs.average()
        } else {
            0.0
        }
    }

    /**
     * Calculates percentile response time
     */
    protected fun calculatePercentile(percentile: Double): Long {
        if (responseTimesMs.isEmpty()) return 0

        val sortedTimes = responseTimesMs.sorted()
        val index = (percentile / 100.0 * sortedTimes.size).toInt()
        return sortedTimes[index.coerceIn(0, sortedTimes.size - 1)]
    }

    /**
     * Performs concurrent load testing
     */
    protected fun performLoadTest(
        concurrentUsers: Int,
        requestsPerUser: Int,
        request: () -> MvcResult
    ): LoadTestResult {
        val executor = Executors.newFixedThreadPool(concurrentUsers)
        val latch = CountDownLatch(concurrentUsers * requestsPerUser)
        val startTime = System.currentTimeMillis()

        repeat(concurrentUsers) {
            repeat(requestsPerUser) {
                executor.submit {
                    try {
                        measureRequestTime(request)
                    } finally {
                        latch.countDown()
                    }
                }
            }
        }

        latch.await()
        executor.shutdown()

        val totalTime = System.currentTimeMillis() - startTime
        val throughput = (concurrentUsers * requestsPerUser * 1000.0) / totalTime

        return LoadTestResult(
            totalRequests = concurrentUsers * requestsPerUser,
            totalTimeMs = totalTime,
            averageResponseTimeMs = calculateAverageResponseTime(),
            minResponseTimeMs = responseTimesMs.minOrNull() ?: 0,
            maxResponseTimeMs = responseTimesMs.maxOrNull() ?: 0,
            p50ResponseTimeMs = calculatePercentile(50.0),
            p95ResponseTimeMs = calculatePercentile(95.0),
            p99ResponseTimeMs = calculatePercentile(99.0),
            throughputPerSecond = throughput
        )
    }

    /**
     * Custom result handler for performance monitoring
     */
    protected inner class PerformanceResultHandler : ResultHandler {
        override fun handle(result: MvcResult) {
            val responseTime = result.response.getHeaderValue("X-Response-Time")?.toString()?.toLongOrNull()
            responseTime?.let { responseTimesMs.add(it) }
        }
    }
}

/**
 * Data class for load test results
 */
data class LoadTestResult(
    val totalRequests: Int,
    val totalTimeMs: Long,
    val averageResponseTimeMs: Double,
    val minResponseTimeMs: Long,
    val maxResponseTimeMs: Long,
    val p50ResponseTimeMs: Long,
    val p95ResponseTimeMs: Long,
    val p99ResponseTimeMs: Long,
    val throughputPerSecond: Double
) {
    fun printSummary() {
        println("""
            |Load Test Results:
            |==================
            |Total Requests: $totalRequests
            |Total Time: ${totalTimeMs}ms
            |Throughput: ${"%.2f".format(throughputPerSecond)} req/s
            |
            |Response Times:
            |  Average: ${"%.2f".format(averageResponseTimeMs)}ms
            |  Min: ${minResponseTimeMs}ms
            |  Max: ${maxResponseTimeMs}ms
            |  P50: ${p50ResponseTimeMs}ms
            |  P95: ${p95ResponseTimeMs}ms
            |  P99: ${p99ResponseTimeMs}ms
        """.trimMargin())
    }
}