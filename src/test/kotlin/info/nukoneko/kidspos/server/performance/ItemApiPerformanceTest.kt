package info.nukoneko.kidspos.server.performance

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.http.MediaType
import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.repository.ItemRepository
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.BeforeEach

class ItemApiPerformanceTest : PerformanceTestBase() {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        clearMetrics()
        itemRepository.deleteAll()
        // Test data will be created as needed for each test
    }

    @Test
    fun `should retrieve all items under 200ms`() {
        // RED: Test that API responds within 200ms
        val time = measureRequestTime {
            mockMvc.perform(get("/api/items")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 200, "Response time ${time}ms exceeded 200ms limit")
    }

    @Test
    fun `should retrieve single item under 50ms`() {
        val testBarcode = "TEST0000001"

        val time = measureRequestTime {
            mockMvc.perform(get("/api/items/$testBarcode")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 50, "Response time ${time}ms exceeded 50ms limit")
    }

    @Test
    fun `should create item under 100ms`() {
        val newItem = mapOf(
            "barcode" to "NEW0000001",
            "name" to "New Test Item",
            "price" to 500,
            "stock" to 50
        )

        val time = measureRequestTime {
            mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated)
                .andReturn()
        }

        assertTrue(time < 100, "Response time ${time}ms exceeded 100ms limit")
    }

    @Test
    fun `should handle concurrent item retrievals`() {
        val result = performLoadTest(
            concurrentUsers = 10,
            requestsPerUser = 10
        ) {
            mockMvc.perform(get("/api/items")
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()
        }

        result.printSummary()

        // Assertions for concurrent load
        assertTrue(result.averageResponseTimeMs < 200,
            "Average response time ${result.averageResponseTimeMs}ms exceeded 200ms")
        assertTrue(result.p95ResponseTimeMs < 300,
            "P95 response time ${result.p95ResponseTimeMs}ms exceeded 300ms")
        assertTrue(result.throughputPerSecond > 50,
            "Throughput ${result.throughputPerSecond} req/s is below 50 req/s")
    }

    @Test
    fun `should handle item search performance`() {
        val time = measureRequestTime {
            mockMvc.perform(get("/api/items/search")
                .param("query", "Test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 150, "Search response time ${time}ms exceeded 150ms limit")
    }

    @Test
    fun `should handle bulk update performance`() {
        val updates = (1..10).map { i ->
            mapOf(
                "barcode" to "TEST${String.format("%07d", i)}",
                "stock" to 200
            )
        }

        val time = measureRequestTime {
            mockMvc.perform(put("/api/items/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 500, "Bulk update time ${time}ms exceeded 500ms limit")
    }
}