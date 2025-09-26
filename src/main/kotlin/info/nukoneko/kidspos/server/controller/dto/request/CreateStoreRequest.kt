package info.nukoneko.kidspos.server.controller.dto.request

import info.nukoneko.kidspos.common.Constants
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 店舗作成リクエストDTO
 *
 * 新規店舗作成時のリクエストデータを表現
 */
data class CreateStoreRequest(
    @field:NotBlank(message = "Store name is required")
    @field:Size(max = Constants.Validation.NAME_MAX_LENGTH, message = "Store name is too long")
    val name: String,
    @field:NotBlank(message = "Barcode is required")
    @field:Pattern(regexp = Constants.Validation.BARCODE_PATTERN, message = "Invalid barcode format")
    val barcode: String,
    @field:Size(max = Constants.Validation.NAME_MAX_LENGTH, message = "Kana name is too long")
    val kana: String? = null,
)
