package info.nukoneko.kidspos.server.controller.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * 売上作成リクエストDTO
 *
 * 新規売上作成時のリクエストデータを表現
 */
data class CreateSaleRequest(
    @field:NotNull(message = "Store ID is required")
    val storeId: Int,
    @field:NotBlank(message = "Staff barcode is required")
    val staffBarcode: String,
    @field:NotBlank(message = "Item IDs are required")
    val itemIds: String,
    @field:Min(value = 0, message = "Deposit must be non-negative")
    val deposit: Int,
)
