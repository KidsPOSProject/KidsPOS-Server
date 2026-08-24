package info.nukoneko.kidspos.server.controller.dto.response

data class DangerZoneStatusResponse(
    val configured: Boolean,
)

data class DangerZonePasswordResponse(
    val success: Boolean,
    val message: String,
    val configured: Boolean,
)

data class DangerZoneVerifyResponse(
    val valid: Boolean,
    val configured: Boolean,
    val message: String,
)
