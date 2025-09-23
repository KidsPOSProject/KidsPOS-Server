package info.nukoneko.kidspos.server.performance

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.http.MediaType
import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.SaleRepository
import info.nukoneko.kidspos.server.repository.StoreRepository
import info.nukoneko.kidspos.server.repository.StaffRepository
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.BeforeEach

class SaleApiPerformanceTest : PerformanceTestBase() {

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
        storeRepository.deleteAll()
        staffRepository.deleteAll()
    }

    @Test
    fun `should create sale transaction under 200ms`() {
        val saleRequest = mapOf(
            "storeId" to 1,
            "staffId" to 1,
            "itemIds" to listOf("ITEM001", "ITEM002"),
            "payment" to 5000
        )

        val time = measureRequestTime {
            mockMvc.perform(post("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleRequest)))
                .andExpect(status().isCreated)
                .andReturn()
        }

        assertTrue(time < 200, "Sale creation time ${time}ms exceeded 200ms limit")
    }

    @Test
    fun `should retrieve sales list under 200ms`() {
        val time = measureRequestTime {
            mockMvc.perform(get("/api/sales")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 200, "Sales list retrieval time ${time}ms exceeded 200ms limit")
    }

    @Test
    fun `should handle concurrent sale transactions`() {
        val result = performLoadTest(
            concurrentUsers = 5,
            requestsPerUser = 10
        ) {
            val saleRequest = mapOf(
                "storeId" to 1,
                "staffId" to 1,
                "itemIds" to listOf("ITEM001"),
                "payment" to 3000
            )

            mockMvc.perform(post("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saleRequest)))
                .andReturn()
        }

        result.printSummary()

        // Assertions for concurrent sales
        assertTrue(result.averageResponseTimeMs < 300,
            "Average sale time ${result.averageResponseTimeMs}ms exceeded 300ms")
        assertTrue(result.p95ResponseTimeMs < 500,
            "P95 sale time ${result.p95ResponseTimeMs}ms exceeded 500ms")
        assertTrue(result.throughputPerSecond > 20,
            "Sales throughput ${result.throughputPerSecond} req/s is below 20 req/s")
    }

    @Test
    fun `should retrieve sale details under 50ms`() {
        val saleId = 1

        val time = measureRequestTime {
            mockMvc.perform(get("/api/sales/$saleId")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 50, "Sale details retrieval time ${time}ms exceeded 50ms limit")
    }

    @Test
    fun `should handle daily report generation under 500ms`() {
        val time = measureRequestTime {
            mockMvc.perform(get("/api/sales/report/daily")
                .param("date", "2025-09-17")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 500, "Daily report generation time ${time}ms exceeded 500ms limit")
    }

    @Test
    fun `should handle sales search with filters under 200ms`() {
        val time = measureRequestTime {
            mockMvc.perform(get("/api/sales/search")
                .param("storeId", "1")
                .param("fromDate", "2025-09-01")
                .param("toDate", "2025-09-30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk)
                .andReturn()
        }

        assertTrue(time < 200, "Sales search time ${time}ms exceeded 200ms limit")
    }
}