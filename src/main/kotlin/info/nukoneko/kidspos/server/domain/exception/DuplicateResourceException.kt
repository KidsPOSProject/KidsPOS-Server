package info.nukoneko.kidspos.server.domain.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Exception thrown when attempting to create a duplicate resource
 */
@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateResourceException(
    message: String,
) : RuntimeException(message)
