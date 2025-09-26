package info.nukoneko.kidspos.server.controller.dto.response

import java.time.LocalDateTime

/**
 * 店舗レスポンスDTO
 *
 * 店舗情報のAPIレスポンスデータを表現
 */
data class StoreResponse(
    val id: Int,
    val name: String,
    val barcode: String,
    val kana: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
) {
    val displayName: String
        get() = kana?.let { "$name ($it)" } ?: name
}
