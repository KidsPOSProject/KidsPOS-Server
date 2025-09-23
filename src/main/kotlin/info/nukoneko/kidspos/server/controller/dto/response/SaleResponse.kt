package info.nukoneko.kidspos.server.controller.dto.response

import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.*

/**
 * 売上レスポンスDTO
 *
 * 売上情報のAPIレスポンスデータを表現
 */
data class SaleResponse(
    val id: Int,
    val storeId: Int,
    val storeName: String,
    val staffId: String,
    val staffName: String,
    val totalAmount: Int,
    val deposit: Int,
    val change: Int,
    val saleTime: LocalDateTime,
    val items: List<SaleItemResponse> = emptyList()
) {
    val totalItems: Int
        get() = items.sumOf { it.quantity }

    val formattedTotalAmount: String
        get() = formatCurrency(totalAmount)

    val formattedDeposit: String
        get() = formatCurrency(deposit)

    val formattedChange: String
        get() = formatCurrency(change)

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
        return formatter.format(amount)
    }
}

/**
 * 売上商品レスポンスDTO
 *
 * 売上に含まれる商品情報を表現
 */
data class SaleItemResponse(
    val itemId: Int,
    val itemName: String,
    val barcode: String,
    val quantity: Int,
    val unitPrice: Int,
    val subtotal: Int
) {
    val formattedUnitPrice: String
        get() = formatCurrency(unitPrice)

    val formattedSubtotal: String
        get() = formatCurrency(subtotal)

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.JAPAN)
        return formatter.format(amount)
    }
}