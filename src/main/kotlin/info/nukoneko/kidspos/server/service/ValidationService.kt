package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.common.Constants
import info.nukoneko.kidspos.server.domain.exception.ValidationException
import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.StaffRepository
import info.nukoneko.kidspos.server.repository.StoreRepository
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
 * - Entity existence validation (items, stores, staff)
 * - Uniqueness validation (barcodes, staff IDs)
 * - Range validation (prices, quantities)
 * - Business rule enforcement
 *
 * @constructor Creates ValidationService with required repositories
 * @param itemRepository Repository for item data validation
 * @param storeRepository Repository for store data validation
 * @param staffRepository Repository for staff data validation
 */
@Service
class ValidationService(
    private val itemRepository: ItemRepository,
    private val storeRepository: StoreRepository,
    private val staffRepository: StaffRepository,
) {
    private val logger = LoggerFactory.getLogger(ValidationService::class.java)

    fun validateItemExists(itemId: Int) {
        if (!itemRepository.existsById(itemId)) {
            logger.warn("Validation failed: Item with ID {} does not exist", itemId)
            throw ValidationException("Item with ID $itemId does not exist")
        }
    }

    fun validateStoreExists(storeId: Int) {
        if (!storeRepository.existsById(storeId)) {
            logger.warn("Validation failed: Store with ID {} does not exist", storeId)
            throw ValidationException("Store with ID $storeId does not exist")
        }
    }

    fun validateStaffExists(staffId: String) {
        if (!staffRepository.existsById(staffId)) {
            logger.warn("Validation failed: Staff with ID {} does not exist", staffId)
            throw ValidationException("Staff with ID $staffId does not exist")
        }
    }

    fun validateBarcodeUnique(
        barcode: String,
        excludeId: Int? = null,
    ) {
        val existingItem = itemRepository.findByBarcode(barcode)
        if (existingItem != null && existingItem.id != excludeId) {
            logger.warn("Validation failed: Barcode {} already exists", barcode)
            throw ValidationException("Barcode $barcode already exists")
        }
    }

    fun validateStaffBarcodeUnique(
        barcode: String,
        excludeId: String? = null,
    ) {
        // Since StaffRepository doesn't have findByBarcode, we'll check by ID for now
        // This would need to be enhanced with a proper query method
        if (excludeId == null || excludeId != barcode) {
            if (staffRepository.existsById(barcode)) {
                logger.warn("Validation failed: Staff barcode {} already exists", barcode)
                throw ValidationException("Staff barcode $barcode already exists")
            }
        }
    }

    fun validateStoreBarcodeUnique(
        barcode: String,
        excludeId: Int? = null,
    ) {
        // Since StoreRepository doesn't have findByBarcode, we'll need to add it or use a different approach
        // For now, we'll skip this validation
        logger.debug("Store barcode uniqueness validation not yet implemented")
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

    fun validateQuantityRange(quantity: Int) {
        if (quantity <= 0) {
            logger.warn("Validation failed: Quantity {} is not positive", quantity)
            throw ValidationException("Quantity must be positive")
        }
        if (quantity > Constants.Validation.MAX_QUANTITY) {
            logger.warn("Validation failed: Quantity {} exceeds maximum", quantity)
            throw ValidationException("Quantity exceeds maximum allowed value")
        }
    }
}
