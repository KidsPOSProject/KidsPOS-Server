package info.nukoneko.kidspos.server.controller.dto.response

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

class ItemResponseTest {

    @Test
    fun `should create ItemResponse with all fields`() {
        // Given
        val id = 1
        val barcode = "1234567890"
        val name = "Test Item"
        val price = 1000
        val createdAt = LocalDateTime.now()
        val updatedAt = LocalDateTime.now()

        // When
        val response = ItemResponse(
            id = id,
            barcode = barcode,
            name = name,
            price = price,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // Then
        assertEquals(id, response.id)
        assertEquals(barcode, response.barcode)
        assertEquals(name, response.name)
        assertEquals(price, response.price)
        assertEquals(createdAt, response.createdAt)
        assertEquals(updatedAt, response.updatedAt)
    }

    @Test
    fun `should format price for display`() {
        // Given
        val response = ItemResponse(
            id = 1,
            barcode = "1234",
            name = "Item",
            price = 1500,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // When
        val formattedPrice = response.formattedPrice

        // Then
        assertEquals("¥1,500", formattedPrice)
    }

    @Test
    fun `should return display name with barcode`() {
        // Given
        val response = ItemResponse(
            id = 1,
            barcode = "1234567890",
            name = "Test Item",
            price = 100,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // When
        val displayName = response.displayName

        // Then
        assertEquals("Test Item (1234567890)", displayName)
    }
}