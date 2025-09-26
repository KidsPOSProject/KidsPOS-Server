package info.nukoneko.kidspos.server.controller.dto.response

import java.time.Instant

/**
 * Standard error response format
 */
data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
    val path: String? = null,
    val details: Map<String, String>? = null,
)
