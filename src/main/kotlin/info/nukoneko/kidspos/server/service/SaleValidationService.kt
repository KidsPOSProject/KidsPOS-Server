package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for sale validation
 *
 * Handles all validation logic for sale requests including store ID, staff barcode,
 * items, and deposit validation. Separated from SaleService to follow Single
 * Responsibility Principle and improve maintainability.
 */
@Service
class SaleValidationService {
    private val logger = LoggerFactory.getLogger(SaleValidationService::class.java)

    /**
     * Validate sale request
     *
     * Performs comprehensive validation of the sale request including all
     * required fields and business rules.
     *
     * @param saleBean Sale request data containing store, staff, and deposit info
     * @param items List of items being purchased
     * @throws IllegalArgumentException if any validation rule fails
     */
    fun validateSaleRequest(saleBean: SaleBean, items: List<ItemBean>) {
        logger.debug("Validating sale request for store: {}", saleBean.storeId)

        validateStoreId(saleBean.storeId)
        validateStaffBarcode(saleBean.staffBarcode)
        validateItems(items)
        validateDeposit(saleBean, items)

        logger.debug("Sale request validation passed")
    }

    /**
     * Validate store ID
     */
    private fun validateStoreId(storeId: Int) {
        if (storeId <= 0) {
            throw IllegalArgumentException("Store ID must be positive")
        }
    }

    /**
     * Validate staff barcode
     */
    private fun validateStaffBarcode(staffBarcode: String) {
        if (staffBarcode.isBlank()) {
            throw IllegalArgumentException("Staff barcode cannot be empty")
        }
    }

    /**
     * Validate items list
     */
    private fun validateItems(items: List<ItemBean>) {
        if (items.isEmpty()) {
            throw IllegalArgumentException("Items list cannot be empty")
        }

        items.forEach { item ->
            validateItem(item)
        }
    }

    /**
     * Validate individual item
     */
    private fun validateItem(item: ItemBean) {
        if (item.id == null || item.id <= 0) {
            throw IllegalArgumentException("Item ID must be positive")
        }

        if (item.name.isBlank()) {
            throw IllegalArgumentException("Item name cannot be empty")
        }

        if (item.price < 0) {
            throw IllegalArgumentException("Item price cannot be negative")
        }
    }

    /**
     * Validate deposit amount
     */
    private fun validateDeposit(saleBean: SaleBean, items: List<ItemBean>) {
        val totalAmount = items.sumOf { it.price }

        if (saleBean.deposit < 0) {
            throw IllegalArgumentException("Deposit cannot be negative")
        }

        if (saleBean.deposit < totalAmount) {
            throw IllegalArgumentException(
                "Insufficient deposit: required $totalAmount, provided ${saleBean.deposit}"
            )
        }
    }

    /**
     * Validate barcode format
     *
     * Validates that barcode contains only digits and has minimum length of 4.
     *
     * @param barcode Barcode string to validate
     * @return True if barcode format is valid, false otherwise
     */
    fun validateBarcodeFormat(barcode: String): Boolean {
        return barcode.matches(Regex("^[0-9]{4,}$"))
    }

    /**
     * Validate price range
     *
     * Checks if the given price falls within the acceptable range.
     *
     * @param price Price to validate
     * @param minPrice Minimum allowed price (default: 0)
     * @param maxPrice Maximum allowed price (default: 1,000,000)
     * @return True if price is within range, false otherwise
     */
    fun validatePriceRange(price: Int, minPrice: Int = 0, maxPrice: Int = 1000000): Boolean {
        return price in minPrice..maxPrice
    }
}