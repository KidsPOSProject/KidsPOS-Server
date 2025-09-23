package info.nukoneko.kidspos.server.domain.exception

/**
 * Base class for all business exceptions
 */
sealed class BusinessException(message: String) : RuntimeException(message)

/**
 * Thrown when an item is not found
 */
class ItemNotFoundException(id: Int? = null, barcode: String? = null) :
    BusinessException(
        when {
            id != null -> "Item with ID $id not found"
            barcode != null -> "Item with barcode $barcode not found"
            else -> "Item not found"
        }
    )

/**
 * Thrown when a sale is not found
 */
class SaleNotFoundException(id: Int) :
    BusinessException("Sale with ID $id not found")

/**
 * Thrown when a store is not found
 */
class StoreNotFoundException(id: Int) :
    BusinessException("Store with ID $id not found")

/**
 * Thrown when a staff member is not found
 */
class StaffNotFoundException(id: Int) :
    BusinessException("Staff with ID $id not found")

/**
 * Thrown when barcode format is invalid
 */
class InvalidBarcodeException(barcode: String) :
    BusinessException("Invalid barcode format: $barcode")

/**
 * Thrown when a validation fails
 */
class ValidationException(message: String) :
    BusinessException(message)