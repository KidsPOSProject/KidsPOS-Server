package info.nukoneko.kidspos.server.controller.dto.request

import info.nukoneko.kidspos.server.service.DangerZonePasswordService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Danger Zone パスワードの設定・変更リクエストDTO
 *
 * 未設定の状態から初めて設定する場合、currentPassword は不要
 */
data class ChangeDangerZonePasswordRequest(
    val currentPassword: String? = null,
    @field:Size(
        min = DangerZonePasswordService.MIN_LENGTH,
        max = DangerZonePasswordService.MAX_LENGTH,
        message = "パスワードは4文字以上128文字以内で設定してください",
    )
    val newPassword: String,
)

data class ClearDangerZonePasswordRequest(
    @field:NotBlank(message = "現在のパスワードを入力してください")
    val currentPassword: String,
)

data class VerifyDangerZonePasswordRequest(
    @field:NotBlank(message = "パスワードを入力してください")
    val password: String,
)
