package info.nukoneko.kidspos.server.controller.advice

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import info.nukoneko.kidspos.server.controller.dto.response.ErrorResponse
import info.nukoneko.kidspos.server.domain.exception.*
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ItemNotFoundException::class)
    fun handleItemNotFound(
        ex: ItemNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Item not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "ITEM_NOT_FOUND",
                    message = ex.message ?: "Item not found",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(SaleNotFoundException::class)
    fun handleSaleNotFound(
        ex: SaleNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Sale not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "SALE_NOT_FOUND",
                    message = ex.message ?: "Sale not found",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(StoreNotFoundException::class)
    fun handleStoreNotFound(
        ex: StoreNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Store not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "STORE_NOT_FOUND",
                    message = ex.message ?: "Store not found",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(StaffNotFoundException::class)
    fun handleStaffNotFound(
        ex: StaffNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Staff not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "STAFF_NOT_FOUND",
                    message = ex.message ?: "Staff not found",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(InvalidBarcodeException::class)
    fun handleInvalidBarcode(
        ex: InvalidBarcodeException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid barcode: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "INVALID_BARCODE",
                    message = ex.message ?: "Invalid barcode format",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(
        ex: ValidationException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation failed: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "VALIDATION_ERROR",
                    message = ex.message ?: "Validation failed",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        ex: ResourceNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.debug("Resource not found: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(
                ErrorResponse(
                    code = "RESOURCE_NOT_FOUND",
                    message = ex.message ?: "Resource not found",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicateResource(
        ex: DuplicateResourceException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.debug("Duplicate resource: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(
                ErrorResponse(
                    code = "DUPLICATE_RESOURCE",
                    message = ex.message ?: "Duplicate resource",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.debug("Validation error: ${ex.message}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "VALIDATION_ERROR",
                    message = ex.message ?: "Invalid request",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors =
            ex.bindingResult.fieldErrors.map { error ->
                val fieldMessage =
                    when (error.field) {
                        "itemIds" ->
                            when {
                                error.defaultMessage?.contains("blank") == true || error.rejectedValue == "" ->
                                    "商品IDが空です。売上作成には少なくとも1つの商品が必要です"
                                else -> error.defaultMessage ?: "商品IDの形式が不正です"
                            }
                        "storeId" -> "店舗IDが無効です。存在する店舗のIDを指定してください"
                        "deposit" -> "預かり金額が不正です。0以上の金額を指定してください"
                        else -> "${error.field}: ${error.defaultMessage}"
                    }
                mapOf(
                    "field" to error.field,
                    "message" to fieldMessage,
                    "value" to error.rejectedValue,
                )
            }

        val message =
            if (fieldErrors.size == 1) {
                fieldErrors.first()["message"] as String
            } else {
                "複数の入力エラーがあります。詳細は 'details' を確認してください"
            }

        logger.warn("Validation failed: ${fieldErrors.map { it["field"] to it["message"] }}")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "VALIDATION_FAILED",
                    message = message,
                    path = request.getDescription(false),
                    details = mapOf("fieldErrors" to fieldErrors),
                ),
            )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errors =
            ex.constraintViolations
                .map { "${it.propertyPath}: ${it.message}" }
                .joinToString(", ")
        logger.debug("Constraint violation: $errors")
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "VALIDATION_ERROR",
                    message = "Validation failed: $errors",
                    path = request.getDescription(false),
                ),
            )
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: org.springframework.http.converter.HttpMessageNotReadableException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid JSON format: ${ex.message}")

        val detailedMessage =
            when {
                ex.message?.contains("JSON parse error") == true -> {
                    when {
                        ex.message?.contains("storeId") == true ->
                            "リクエストの形式が不正です。'storeId'フィールドは必須です"
                        ex.message?.contains("Required request body is missing") == true ->
                            "リクエストボディが空です。JSON形式のデータを送信してください"
                        else ->
                            "JSONの形式が不正です。正しいJSON形式でリクエストを送信してください"
                    }
                }
                ex.message?.contains("Required request body is missing") == true ->
                    "リクエストボディが必要です。JSON形式のデータを送信してください"
                else ->
                    "リクエストの形式が不正です。JSON形式を確認してください"
            }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "INVALID_REQUEST_FORMAT",
                    message = detailedMessage,
                    path = request.getDescription(false),
                    details = if (logger.isDebugEnabled) mapOf("debug" to ex.message) else null,
                ),
            )
    }

    @ExceptionHandler(MissingKotlinParameterException::class)
    fun handleMissingKotlinParameter(
        ex: MissingKotlinParameterException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val parameterName = ex.parameter.name ?: "unknown"
        val parameterType = "String"
        logger.warn("Missing required parameter: $parameterName (type: $parameterType)")

        val detailedMessage =
            when (parameterName) {
                "itemIds" -> "売上作成には商品IDが必要です。'itemIds'フィールドにカンマ区切りの商品IDを指定してください（例: '1,2,3'）"
                "storeId" -> "店舗IDが必要です。'storeId'フィールドに店舗IDを指定してください"
                "deposit" -> "預かり金額が必要です。'deposit'フィールドに金額を指定してください"
                else -> "必須パラメータ '$parameterName' (型: $parameterType) が不足しています。リクエストボディに含めてください"
            }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "MISSING_REQUIRED_FIELD",
                    message = detailedMessage,
                    path = request.getDescription(false),
                    details =
                        mapOf(
                            "field" to parameterName,
                            "type" to parameterType,
                            "required" to true,
                        ),
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred: ${ex.message}", ex)

        val userMessage =
            when {
                ex.message?.contains("connection", ignoreCase = true) == true ->
                    "データベース接続エラーが発生しました。しばらく待ってから再試行してください"
                ex.message?.contains("timeout", ignoreCase = true) == true ->
                    "処理がタイムアウトしました。しばらく待ってから再試行してください"
                else ->
                    "サーバーエラーが発生しました。問題が続く場合は管理者に連絡してください"
            }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ErrorResponse(
                    code = "INTERNAL_SERVER_ERROR",
                    message = userMessage,
                    path = request.getDescription(false),
                    details = if (logger.isDebugEnabled) mapOf("debug" to ex.message) else null,
                ),
            )
    }
}
