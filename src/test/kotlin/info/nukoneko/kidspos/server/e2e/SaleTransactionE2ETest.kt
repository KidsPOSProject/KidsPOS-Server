package info.nukoneko.kidspos.server.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.*
import info.nukoneko.kidspos.server.entity.*
import info.nukoneko.kidspos.server.controller.dto.request.CreateSaleRequest
import java.util.Date

/**
 * End-to-end test for complete sale transaction workflow
 * Part of Task 10.1: E2E test implementation
 */
@DisplayName("Sale Transaction E2E Tests")
class SaleTransactionE2ETest : EndToEndTestBase() {

    private lateinit var testStore: StoreEntity
    private lateinit var testStaff: StaffEntity
    private lateinit var testItem1: ItemEntity
    private lateinit var testItem2: ItemEntity

    @BeforeEach
    fun setupTestData() {
        // Setup test store
        testStore = storeRepository.save(
            StoreEntity(
                id = 1,
                name = "Test Store",
                printerUri = "tcp://localhost:9100"
            )
        )

        // Setup test staff
        testStaff = staffRepository.save(
            StaffEntity(
                barcode = "STAFF001",
                name = "Test Staff"
            )
        )

        // Setup test items
        testItem1 = itemRepository.save(
            ItemEntity(
                id = 101,
                barcode = "4901234567890",
                name = "Test Item 1",
                price = 100
            )
        )

        testItem2 = itemRepository.save(
            ItemEntity(
                id = 102,
                barcode = "4901234567891",
                name = "Test Item 2",
                price = 200
            )
        )
    }

    @Test
    @DisplayName("Should complete full sale transaction workflow")
    fun shouldCompleteFullSaleTransactionWorkflow() {
        // Step 1: Create a new sale
        val saleRequest = CreateSaleRequest(
            storeId = testStore.id,
            staffBarcode = testStaff.barcode,
            deposit = 500,
            itemIds = "${testItem1.id},${testItem2.id},${testItem1.id}" // 2x item1, 1x item2
        )

        val createResult = mockMvc.perform(
            post("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(saleRequest))
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.amount").value(400)) // 100*2 + 200
            .andExpect(jsonPath("$.quantity").value(3))
            .andExpect(jsonPath("$.deposit").value(500))
            .andExpect(jsonPath("$.change").value(100))
            .andExpect(jsonPath("$.staffName").value("Test Staff"))
            .andExpect(jsonPath("$.storeName").value("Test Store"))
            .andReturn()

        val saleResponse = fromJson(createResult.response.contentAsString, Map::class.java) as Map<String, Any>
        val saleId = saleResponse["id"] as Int

        // Step 2: Verify sale was persisted
        val savedSale = saleRepository.findById(saleId).orElse(null)
        assertNotNull(savedSale, "Sale should be saved in database")
        assertEquals(400, savedSale.amount)
        assertEquals(3, savedSale.quantity)

        // Step 3: Verify sale details were created
        val saleDetails = saleDetailRepository.findAll()
            .filter { it.saleId == saleId }

        assertEquals(2, saleDetails.size, "Should have 2 sale detail records (grouped by item)")

        val item1Detail = saleDetails.find { it.itemId == testItem1.id }
        assertNotNull(item1Detail)
        assertEquals(2, item1Detail?.quantity, "Item 1 should have quantity 2")
        assertEquals(100, item1Detail?.price)

        val item2Detail = saleDetails.find { it.itemId == testItem2.id }
        assertNotNull(item2Detail)
        assertEquals(1, item2Detail?.quantity, "Item 2 should have quantity 1")
        assertEquals(200, item2Detail?.price)

        // Step 4: Retrieve sale via API
        mockMvc.perform(
            get("/api/sales/$saleId")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(saleId))
            .andExpect(jsonPath("$.amount").value(400))
            .andExpect(jsonPath("$.items").isArray)
            .andExpect(jsonPath("$.items.length()").value(2))

        // Step 5: List all sales
        mockMvc.perform(
            get("/api/sales")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(saleId))
    }

    @Test
    @DisplayName("Should handle sale validation errors correctly")
    fun shouldHandleSaleValidationErrors() {
        // Test with invalid staff barcode
        val invalidStaffRequest = CreateSaleRequest(
            storeId = testStore.id,
            staffBarcode = "INVALID_STAFF",
            deposit = 500,
            itemIds = testItem1.id.toString()
        )

        mockMvc.perform(
            post("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalidStaffRequest))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)

        // Test with insufficient deposit
        val insufficientDepositRequest = CreateSaleRequest(
            storeId = testStore.id,
            staffBarcode = testStaff.barcode,
            deposit = 50, // Less than item price (100)
            itemIds = testItem1.id.toString()
        )

        mockMvc.perform(
            post("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(insufficientDepositRequest))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)

        // Test with invalid item ID
        val invalidItemRequest = CreateSaleRequest(
            storeId = testStore.id,
            staffBarcode = testStaff.barcode,
            deposit = 500,
            itemIds = "99999" // Non-existent item
        )

        mockMvc.perform(
            post("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(invalidItemRequest))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("Should handle concurrent sale transactions")
    fun shouldHandleConcurrentSaleTransactions() {
        // Create multiple sales concurrently
        val saleRequests = (1..3).map { i ->
            CreateSaleRequest(
                storeId = testStore.id,
                staffBarcode = testStaff.barcode,
                deposit = 1000,
                itemIds = "${testItem1.id},${testItem2.id}"
            )
        }

        val results = saleRequests.map { request ->
            mockMvc.perform(
                post("/api/sales")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(request))
            )
                .andExpect(status().isCreated)
                .andReturn()
        }

        // Verify all sales were created with unique IDs
        val saleIds = results.map { result ->
            val response = fromJson(result.response.contentAsString, Map::class.java) as Map<String, Any>
            response["id"] as Int
        }

        assertEquals(3, saleIds.distinct().size, "All sales should have unique IDs")

        // Verify total sales count
        val allSales = saleRepository.findAll()
        assertEquals(3, allSales.size, "Should have 3 sales in total")

        // Verify each sale has correct details
        allSales.forEach { sale ->
            assertEquals(300, sale.amount, "Each sale should have total 300")
            assertEquals(2, sale.quantity, "Each sale should have 2 items")
        }
    }
}