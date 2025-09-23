package info.nukoneko.kidspos.server.controller.dto.response

import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.*

/**
 * 商品レスポンスDTO
 *
 * 商品情報のAPIレスポンスデータを表現
 */
data class ItemResponse(
    val id: Int,
    val barcode: String,
    val name: String,
    val price: Int,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    val formattedPrice: String
        get() {
            val formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
            return formatter.format(price)
        }

    val displayName: String
        get() = "$name ($barcode)"
}