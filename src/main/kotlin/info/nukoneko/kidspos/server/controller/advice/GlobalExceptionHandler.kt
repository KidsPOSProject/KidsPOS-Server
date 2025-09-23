package info.nukoneko.kidspos.server.controller.advice

import info.nukoneko.kidspos.server.controller.dto.response.ErrorResponse
import info.nukoneko.kidspos.server.domain.exception.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ItemNotFoundException::class)
    fun handleItemNotFound(
        ex: ItemNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Item not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                code = "ITEM_NOT_FOUND",
                message = ex.message ?: "Item not found",
                path = request.getDescription(false)
            ))
    }

    @ExceptionHandler(SaleNotFoundException::class)
    fun handleSaleNotFound(
        ex: SaleNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Sale not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                code = "SALE_NOT_FOUND",
                message = ex.message ?: "Sale not found",
                path = request.getDescription(false)
            ))
    }

    @ExceptionHandler(StoreNotFoundException::class)
    fun handleStoreNotFound(
        ex: StoreNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Store not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                code = "STORE_NOT_FOUND",
                message = ex.message ?: "Store not found",
                path = request.getDescription(false)
            ))
    }

    @ExceptionHandler(StaffNotFoundException::class)
    fun handleStaffNotFound(
        ex: StaffNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Staff not found: ${ex.message}")
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                code = "STAFF_NOT_FOUND",
                message = ex.message ?: "Staff not found",
                path = request.getDescription(false)
            ))
    }

    @ExceptionHandler(InvalidBarcodeException::class)
    fun handleInvalidBarcode(
        ex: InvalidBarcodeException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid barcode: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                code = "INVALID_BARCODE",
                message = ex.message ?: "Invalid barcode format",
                path = request.getDescription(false)
            ))
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(
        ex: ValidationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation failed: ${ex.message}")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                code = "VALIDATION_ERROR",
                message = ex.message ?: "Validation failed",
                path = request.getDescription(false)
            ))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }
            .joinToString(", ")

        logger.warn("Validation failed: $errors")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                code = "VALIDATION_ERROR",
                message = "Validation failed: $errors",
                path = request.getDescription(false)
            ))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                code = "INTERNAL_ERROR",
                message = "An unexpected error occurred",
                path = request.getDescription(false)
            ))
    }
}