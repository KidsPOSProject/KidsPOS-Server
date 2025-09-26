package info.nukoneko.kidspos.server.controller.dto.response

import java.time.LocalDateTime

/**
 * スタッフレスポンスDTO
 *
 * スタッフ情報のAPIレスポンスデータを表現
 */
data class StaffResponse(
    val id: String,
    val name: String,
    val barcode: String,
    val storeId: Int,
    val storeName: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
) {
    val displayName: String
        get() = "$name (ID: $id)"
}
