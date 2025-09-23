package info.nukoneko.kidspos.server.performance

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.http.MediaType
import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class SystemLoadTest : PerformanceTestBase() {

    @Autowired
    private lateinit var saleRepository: SaleRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var staffRepository: StaffRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        clearMetrics()
        saleRepository.deleteAll()
        itemRepository.deleteAll()
        staffRepository.deleteAll()
        storeRepository.deleteAll()
    }

    @Test
    fun `should handle mixed workload under target response times`() {
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val executor = Executors.newFixedThreadPool(20)
        val latch = CountDownLatch(100)

        val totalTime = measureTimeMillis {
            // Simulate mixed workload
            repeat(100) { i ->
                executor.submit {
                    try {
                        when (i % 5) {
                            0 -> {
                                // Item lookup (most frequent)
                                val result = mockMvc.perform(get("/api/items/TEST001")
                                    .contentType(MediaType.APPLICATION_JSON))
                                    .andReturn()

                                if (result.response.status in 200..299) {
                                    successCount.incrementAndGet()
                                } else {
                                    failureCount.incrementAndGet()
                                }
                            }
                            1 -> {
                                // Sale transaction
                                val saleRequest = mapOf(
                                    "storeId" to 1,
                                    "staffId" to 1,
                                    "itemIds" to listOf("TEST001"),
                                    "payment" to 3000
                                )

                                val result = mockMvc.perform(post("/api/sales")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(saleRequest)))
                                    .andReturn()

                                if (result.response.status in 200..299) {
                                    successCount.incrementAndGet()
                                } else {
                                    failureCount.incrementAndGet()
                                }
                            }
                            2 -> {
                                // Store list
                                val result = mockMvc.perform(get("/api/stores")
                                    .contentType(MediaType.APPLICATION_JSON))
                                    .andReturn()

                                if (result.response.status in 200..299) {
                                    successCount.incrementAndGet()
                                } else {
                                    failureCount.incrementAndGet()
                                }
                            }
                            3 -> {
                                // Sales report
                                val result = mockMvc.perform(get("/api/sales")
                                    .contentType(MediaType.APPLICATION_JSON))
                                    .andReturn()

                                if (result.response.status in 200..299) {
                                    successCount.incrementAndGet()
                                } else {
                                    failureCount.incrementAndGet()
                                }
                            }
                            else -> {
                                // Item search
                                val result = mockMvc.perform(get("/api/items/search")
                                    .param("query", "Test")
                                    .contentType(MediaType.APPLICATION_JSON))
                                    .andReturn()

                                if (result.response.status in 200..299) {
                                    successCount.incrementAndGet()
                                } else {
                                    failureCount.incrementAndGet()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        failureCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()
        }

        val averageResponseTime = if (responseTimesMs.isNotEmpty()) {
            calculateAverageResponseTime()
        } else {
            0.0
        }

        println("""
            |System Load Test Results:
            |========================
            |Total Requests: 100
            |Successful: ${successCount.get()}
            |Failed: ${failureCount.get()}
            |Total Time: ${totalTime}ms
            |Average Response Time: ${"%.2f".format(averageResponseTime)}ms
            |P50: ${calculatePercentile(50.0)}ms
            |P95: ${calculatePercentile(95.0)}ms
            |P99: ${calculatePercentile(99.0)}ms
            |Throughput: ${"%.2f".format((100 * 1000.0) / totalTime)} req/s
        """.trimMargin())

        // Assertions - Initially these will fail (RED phase)
        assertTrue(successCount.get() >= 95, "Success rate below 95%")
        assertTrue(averageResponseTime < 200, "Average response time exceeded 200ms")
    }

    @Test
    fun `should handle peak load scenario`() {
        // Simulate Black Friday scenario - high concurrent sales
        val result = performLoadTest(
            concurrentUsers = 20,
            requestsPerUser = 5
        ) {
            val saleRequest = mapOf(
                "storeId" to 1,
                "staffId" to 1,
                "itemIds" to listOf("TEST001", "TEST002"),
                "payment" to 5000
            )

            mockMvc.perform(post("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleRequest)))
                .andReturn()
        }

        result.printSummary()

        // Peak load should still maintain reasonable performance
        assertTrue(result.averageResponseTimeMs < 500,
            "Peak load average response time ${result.averageResponseTimeMs}ms exceeded 500ms")
        assertTrue(result.p99ResponseTimeMs < 1000,
            "Peak load P99 response time ${result.p99ResponseTimeMs}ms exceeded 1000ms")
    }

    @Test
    fun `should maintain performance during sustained load`() {
        val sustainedDurationMs = 5000L
        val startTime = System.currentTimeMillis()
        val requestCount = AtomicInteger(0)
        val errors = AtomicInteger(0)

        while (System.currentTimeMillis() - startTime < sustainedDurationMs) {
            try {
                val time = measureRequestTime {
                    mockMvc.perform(get("/api/items")
                        .contentType(MediaType.APPLICATION_JSON))
                        .andReturn()
                }
                requestCount.incrementAndGet()

                // Check if individual request exceeds threshold
                if (time > 500) {
                    errors.incrementAndGet()
                }
            } catch (e: Exception) {
                errors.incrementAndGet()
            }
        }

        val averageResponseTime = calculateAverageResponseTime()
        val errorRate = (errors.get().toDouble() / requestCount.get()) * 100

        println("""
            |Sustained Load Test Results:
            |===========================
            |Duration: ${sustainedDurationMs}ms
            |Total Requests: ${requestCount.get()}
            |Average Response Time: ${"%.2f".format(averageResponseTime)}ms
            |Error Rate: ${"%.2f".format(errorRate)}%
            |Requests/Second: ${"%.2f".format((requestCount.get() * 1000.0) / sustainedDurationMs)}
        """.trimMargin())

        // Assertions for sustained load
        assertTrue(averageResponseTime < 200,
            "Sustained load average response time ${averageResponseTime}ms exceeded 200ms")
        assertTrue(errorRate < 5.0,
            "Error rate ${errorRate}% exceeded 5%")
    }
}