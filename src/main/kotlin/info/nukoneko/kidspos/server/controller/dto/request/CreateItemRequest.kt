package info.nukoneko.kidspos.server.controller.dto.request

import info.nukoneko.kidspos.common.Constants
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 商品作成リクエストDTO
 *
 * 新規商品作成時のリクエストデータを表現
 */
data class CreateItemRequest(
    @field:NotBlank(message = "Item name is required")
    @field:Size(max = Constants.Validation.NAME_MAX_LENGTH)
    val name: String,
    @field:NotBlank(message = "Barcode is required")
    @field:Pattern(regexp = Constants.Validation.BARCODE_PATTERN, message = "Invalid barcode format")
    val barcode: String,
    @field:Min(value = 0, message = "Price must be non-negative")
    val price: Int,
)
