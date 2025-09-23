package info.nukoneko.kidspos.server.performance

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.http.MediaType
import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.repository.StoreRepository
import info.nukoneko.kidspos.server.repository.StaffRepository
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.BeforeEach

class StoreApiPerformanceTest : PerformanceTestBase() {

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var staffRepository: StaffRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        clearMetrics()
        staffRepository.deleteAll()
        storeRepository.deleteAll()
    }

    @Test
    fun `should retrieve all stores under 100ms`() {
        val time = measureRequestTime {
            mockMvc.perform(get("/api/stores")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 100, "Store list retrieval time ${time}ms exceeded 100ms limit")
    }

    @Test
    fun `should create store under 50ms`() {
        val newStore = mapOf(
            "name" to "New Performance Store"
        )

        val time = measureRequestTime {
            mockMvc.perform(post("/api/stores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newStore)))
                .andExpect(status().isCreated)
                .andReturn()
        }

        assertTrue(time < 50, "Store creation time ${time}ms exceeded 50ms limit")
    }

    @Test
    fun `should retrieve store with staff under 100ms`() {
        val storeId = 1

        val time = measureRequestTime {
            mockMvc.perform(get("/api/stores/$storeId")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 100, "Store details retrieval time ${time}ms exceeded 100ms limit")
    }

    @Test
    fun `should handle concurrent store operations`() {
        val result = performLoadTest(
            concurrentUsers = 10,
            requestsPerUser = 5
        ) {
            mockMvc.perform(get("/api/stores")
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn()
        }

        result.printSummary()

        // Assertions
        assertTrue(result.averageResponseTimeMs < 150,
            "Average response time ${result.averageResponseTimeMs}ms exceeded 150ms")
        assertTrue(result.p99ResponseTimeMs < 300,
            "P99 response time ${result.p99ResponseTimeMs}ms exceeded 300ms")
    }

    @Test
    fun `should update store under 50ms`() {
        val storeId = 1
        val updateRequest = mapOf(
            "name" to "Updated Store Name"
        )

        val time = measureRequestTime {
            mockMvc.perform(put("/api/stores/$storeId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 50, "Store update time ${time}ms exceeded 50ms limit")
    }
}