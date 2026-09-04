package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.common.Constants
import info.nukoneko.kidspos.server.domain.exception.ValidationException
import info.nukoneko.kidspos.server.repository.ItemRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for performing business rule validations across the application
 *
 * Provides centralized validation logic for business rules and data integrity
 * constraints. This service ensures data consistency and enforces business
 * rules before data persistence operations. Validates entities existence,
 * uniqueness constraints, and value ranges according to business requirements.
 *
 * Key responsibilities:
 * - Validating entity existence across different repositories
 * - Enforcing uniqueness constraints for barcodes and identifiers
 * - Validating value ranges for prices and quantities
 * - Providing consistent error messaging through ValidationException
 * - Centralizing business rule logic for maintainability
 *
 * Validation categories:
 * - Entity existence validation (items, stores)
 * - Uniqueness validation (barcodes)
 * - Range validation (prices, quantities)
 * - Business rule enforcement
 *
 * @constructor Creates ValidationService with required repositories
 * @param itemRepository Repository for item data validation
 */
@Service
class ValidationService(
    private val itemRepository: ItemRepository,
) {
    private val logger = LoggerFactory.getLogger(ValidationService::class.java)

    fun validateItemExists(itemId: Int) {
        if (!itemRepository.existsById(itemId)) {
            logger.warn("Validation failed: Item with ID {} does not exist", itemId)
            throw ValidationException("Item with ID $itemId does not exist")
        }
    }

    fun validateBarcodeUnique(
        barcode: String?,
        excludeId: Int? = null,
    ) {
        // nullの場合は自動生成されるためバリデーションスキップ
        if (barcode.isNullOrBlank()) {
            return
        }

        val existingItem = itemRepository.findByBarcode(barcode)
        if (existingItem != null && existingItem.id != excludeId) {
            logger.warn("Validation failed: Barcode {} already exists", barcode)
            throw ValidationException("Barcode $barcode already exists")
        }
    }

    fun validatePriceRange(price: Int) {
        if (price < 0) {
            logger.warn("Validation failed: Price {} is negative", price)
            throw ValidationException("Price cannot be negative")
        }
        if (price > Constants.Validation.MAX_PRICE) {
            logger.warn("Validation failed: Price {} exceeds maximum", price)
            throw ValidationException("Price exceeds maximum allowed value")
        }
    }
}
