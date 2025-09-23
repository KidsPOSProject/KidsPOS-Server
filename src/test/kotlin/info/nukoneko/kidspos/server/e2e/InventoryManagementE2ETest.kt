package info.nukoneko.kidspos.server.e2e

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.*
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.controller.dto.request.CreateItemRequest

/**
 * End-to-end test for inventory management workflow
 * Part of Task 10.1: E2E test implementation
 */
@DisplayName("Inventory Management E2E Tests")
class InventoryManagementE2ETest : EndToEndTestBase() {

    @BeforeEach
    fun setupInitialInventory() {
        // Create initial inventory items
        itemRepository.saveAll(listOf(
            ItemEntity(id = 1, barcode = "1234567890123", name = "Item A", price = 100),
            ItemEntity(id = 2, barcode = "2234567890123", name = "Item B", price = 200),
            ItemEntity(id = 3, barcode = "3234567890123", name = "Item C", price = 300)
        ))
    }

    @Test
    @DisplayName("Should complete full inventory management workflow")
    fun shouldCompleteFullInventoryManagementWorkflow() {
        // Step 1: List all items
        mockMvc.perform(get("/api/items"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("Item A"))
            .andExpect(jsonPath("$[1].name").value("Item B"))
            .andExpect(jsonPath("$[2].name").value("Item C"))

        // Step 2: Add new item
        val newItemRequest = CreateItemRequest(
            barcode = "4234567890123",
            name = "New Item D",
            price = 400
        )

        val createResult = mockMvc.perform(
            post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(newItemRequest))
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("New Item D"))
            .andExpect(jsonPath("$.price").value(400))
            .andExpect(jsonPath("$.barcode").value("4234567890123"))
            .andReturn()

        val newItemResponse = fromJson(createResult.response.contentAsString, Map::class.java) as Map<String, Any>
        val newItemId = newItemResponse["id"] as Int

        // Step 3: Search item by barcode
        mockMvc.perform(get("/api/items/barcode/4234567890123"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(newItemId))
            .andExpect(jsonPath("$.name").value("New Item D"))

        // Step 4: Update item price
        val updateRequest = mapOf(
            "barcode" to "4234567890123",
            "name" to "New Item D",
            "price" to 450
        )

        mockMvc.perform(
            put("/api/items/$newItemId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(updateRequest))
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.price").value(450))

        // Step 5: Verify updated item
        mockMvc.perform(get("/api/items/$newItemId"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.price").value(450))

        // Step 6: Verify total item count
        mockMvc.perform(get("/api/items"))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(4))
    }

    @Test
    @DisplayName("Should handle duplicate barcode validation")
    fun shouldHandleDuplicateBarcodeValidation() {
        // Try to create item with existing barcode
        val duplicateRequest = CreateItemRequest(
            barcode = "1234567890123", // Already exists
            name = "Duplicate Item",
            price = 500
        )

        mockMvc.perform(
            post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(duplicateRequest))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)

        // Verify original item is unchanged
        mockMvc.perform(get("/api/items/barcode/1234567890123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Item A"))
            .andExpect(jsonPath("$.price").value(100))
    }

    @Test
    @DisplayName("Should handle batch item operations")
    fun shouldHandleBatchItemOperations() {
        // Create multiple items in batch
        val batchItems = (1..5).map { i ->
            CreateItemRequest(
                barcode = "999000000000$i",
                name = "Batch Item $i",
                price = i * 100
            )
        }

        // Add all items
        batchItems.forEach { item ->
            mockMvc.perform(
                post("/api/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(item))
            )
                .andExpect(status().isCreated)
        }

        // Verify total count
        mockMvc.perform(get("/api/items"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(8)) // 3 initial + 5 batch

        // Search for batch items
        batchItems.forEach { item ->
            mockMvc.perform(get("/api/items/barcode/${item.barcode}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value(item.name))
                .andExpect(jsonPath("$.price").value(item.price))
        }
    }

    @Test
    @DisplayName("Should validate item price constraints")
    fun shouldValidateItemPriceConstraints() {
        // Test with negative price
        val negativePrice = CreateItemRequest(
            barcode = "9990000000001",
            name = "Negative Price Item",
            price = -100
        )

        mockMvc.perform(
            post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(negativePrice))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)

        // Test with zero price
        val zeroPrice = CreateItemRequest(
            barcode = "9990000000002",
            name = "Zero Price Item",
            price = 0
        )

        mockMvc.perform(
            post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(zeroPrice))
        )
            .andDo(print())
            .andExpect(status().isBadRequest)

        // Test with valid price
        val validPrice = CreateItemRequest(
            barcode = "9990000000003",
            name = "Valid Price Item",
            price = 1
        )

        mockMvc.perform(
            post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(validPrice))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.price").value(1))
    }
}