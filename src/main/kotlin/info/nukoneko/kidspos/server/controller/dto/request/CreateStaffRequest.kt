package info.nukoneko.kidspos.server.controller.dto.request

import info.nukoneko.kidspos.common.Constants
import jakarta.validation.constraints.*

/**
 * スタッフ作成リクエストDTO
 *
 * 新規スタッフ作成時のリクエストデータを表現
 */
data class CreateStaffRequest(
    @field:NotBlank(message = "Staff name is required")
    @field:Size(max = Constants.Validation.NAME_MAX_LENGTH, message = "Staff name is too long")
    val name: String,
    @field:NotBlank(message = "Barcode is required")
    @field:Pattern(regexp = Constants.Validation.BARCODE_PATTERN, message = "Invalid barcode format")
    val barcode: String,
    @field:NotNull(message = "Store ID is required")
    @field:Min(value = 1, message = "Store ID must be positive")
    val storeId: Int,
)
