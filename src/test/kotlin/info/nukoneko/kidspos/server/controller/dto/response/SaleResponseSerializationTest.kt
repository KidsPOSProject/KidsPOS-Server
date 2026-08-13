package info.nukoneko.kidspos.server.controller.dto.response

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * api.yaml が saleTime を format: date-time (RFC3339) と宣言しているため、
 * 生成クライアントは ISO_OFFSET_DATE_TIME でパースする。
 * オフセット無しの表現に戻すと生成クライアント側が実行時に失敗するので、
 * ここで出力形式を固定する。
 */
@JsonTest
class SaleResponseSerializationTest {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val saleTime: OffsetDateTime =
        OffsetDateTime.of(2026, 8, 13, 14, 43, 22, 947_000_000, ZoneOffset.ofHours(9))

    private fun sampleResponse() =
        SaleResponse(
            id = 1,
            storeId = 10,
            storeName = "Store 10",
            totalAmount = 300,
            deposit = 500,
            change = 200,
            saleTime = saleTime,
            items =
                listOf(
                    SaleItemResponse(
                        itemId = 100,
                        itemName = "Item 100",
                        barcode = "A01000100A",
                        quantity = 2,
                        unitPrice = 150,
                        subtotal = 300,
                    ),
                ),
        )

    private fun serializedSaleTime(): String =
        objectMapper.readTree(objectMapper.writeValueAsString(sampleResponse())).get("saleTime").let {
            assertTrue(it.isTextual, "saleTime should be serialized as a string, not a timestamp")
            it.asText()
        }

    @Test
    fun `saleTime should be parseable by the generated client parser`() {
        val parsed = OffsetDateTime.parse(serializedSaleTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        assertEquals(saleTime.toInstant(), parsed.toInstant())
    }

    @Test
    fun `saleTime should carry an explicit offset`() {
        assertEquals(ZoneOffset.ofHours(9), OffsetDateTime.parse(serializedSaleTime()).offset)
    }

    @Test
    fun `derived fields declared in api yaml should be present`() {
        val json = objectMapper.readTree(objectMapper.writeValueAsString(sampleResponse()))

        assertEquals(2, json.get("totalItems").asInt())
        assertTrue(json.has("formattedTotalAmount"))
        assertTrue(json.has("formattedDeposit"))
        assertTrue(json.has("formattedChange"))
        assertTrue(json.get("items")[0].has("formattedUnitPrice"))
        assertTrue(json.get("items")[0].has("formattedSubtotal"))
    }
}
