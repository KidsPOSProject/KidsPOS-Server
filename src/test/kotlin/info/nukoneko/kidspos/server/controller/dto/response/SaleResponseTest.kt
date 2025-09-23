package info.nukoneko.kidspos.server.controller.dto.response

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class SaleResponseTest {

    @Test
    fun `should create SaleResponse with all fields`() {
        // Given
        val id = 1
        val storeId = 10
        val storeName = "Main Store"
        val staffId = "STAFF001"
        val staffName = "John Doe"
        val totalAmount = 5000
        val deposit = 10000
        val change = 5000
        val saleTime = LocalDateTime.now()
        val items = listOf(
            SaleItemResponse(
                itemId = 1,
                itemName = "Item 1",
                barcode = "1234",
                quantity = 2,
                unitPrice = 1000,
                subtotal = 2000
            ),
            SaleItemResponse(
                itemId = 2,
                itemName = "Item 2",
                barcode = "5678",
                quantity = 3,
                unitPrice = 1000,
                subtotal = 3000
            )
        )

        // When
        val response = SaleResponse(
            id = id,
            storeId = storeId,
            storeName = storeName,
            staffId = staffId,
            staffName = staffName,
            totalAmount = totalAmount,
            deposit = deposit,
            change = change,
            saleTime = saleTime,
            items = items
        )

        // Then
        assertEquals(id, response.id)
        assertEquals(storeId, response.storeId)
        assertEquals(storeName, response.storeName)
        assertEquals(staffId, response.staffId)
        assertEquals(staffName, response.staffName)
        assertEquals(totalAmount, response.totalAmount)
        assertEquals(deposit, response.deposit)
        assertEquals(change, response.change)
        assertEquals(saleTime, response.saleTime)
        assertEquals(2, response.items.size)
        assertEquals(5, response.totalItems)
    }

    @Test
    fun `should calculate total items correctly`() {
        // Given
        val items = listOf(
            SaleItemResponse(1, "Item 1", "1234", 2, 100, 200),
            SaleItemResponse(2, "Item 2", "5678", 3, 200, 600),
            SaleItemResponse(3, "Item 3", "9012", 1, 300, 300)
        )

        val response = SaleResponse(
            id = 1,
            storeId = 1,
            storeName = "Store",
            staffId = "S1",
            staffName = "Staff",
            totalAmount = 1100,
            deposit = 2000,
            change = 900,
            saleTime = LocalDateTime.now(),
            items = items
        )

        // When
        val totalItems = response.totalItems

        // Then
        assertEquals(6, totalItems)
    }

    @Test
    fun `should format amounts correctly`() {
        // Given
        val response = SaleResponse(
            id = 1,
            storeId = 1,
            storeName = "Store",
            staffId = "S1",
            staffName = "Staff",
            totalAmount = 15500,
            deposit = 20000,
            change = 4500,
            saleTime = LocalDateTime.now(),
            items = emptyList()
        )

        // When & Then
        assertEquals("¥15,500", response.formattedTotalAmount)
        assertEquals("¥20,000", response.formattedDeposit)
        assertEquals("¥4,500", response.formattedChange)
    }
}

class SaleItemResponseTest {

    @Test
    fun `should create SaleItemResponse with all fields`() {
        // Given
        val itemId = 1
        val itemName = "Test Item"
        val barcode = "1234567890"
        val quantity = 3
        val unitPrice = 500
        val subtotal = 1500

        // When
        val response = SaleItemResponse(
            itemId = itemId,
            itemName = itemName,
            barcode = barcode,
            quantity = quantity,
            unitPrice = unitPrice,
            subtotal = subtotal
        )

        // Then
        assertEquals(itemId, response.itemId)
        assertEquals(itemName, response.itemName)
        assertEquals(barcode, response.barcode)
        assertEquals(quantity, response.quantity)
        assertEquals(unitPrice, response.unitPrice)
        assertEquals(subtotal, response.subtotal)
    }

    @Test
    fun `should format prices correctly`() {
        // Given
        val response = SaleItemResponse(
            itemId = 1,
            itemName = "Item",
            barcode = "1234",
            quantity = 2,
            unitPrice = 1250,
            subtotal = 2500
        )

        // When & Then
        assertEquals("¥1,250", response.formattedUnitPrice)
        assertEquals("¥2,500", response.formattedSubtotal)
    }
}