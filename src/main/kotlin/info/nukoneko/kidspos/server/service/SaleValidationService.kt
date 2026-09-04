package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for sale validation
 *
 * Handles all validation logic for sale requests including store ID, staff barcode,
 * items, and deposit validation.
 */
@Service
class SaleValidationService(
    private val storeRepository: StoreRepository,
) {
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
    fun validateSaleRequest(
        saleBean: SaleBean,
        items: List<ItemBean>,
    ) {
        logger.debug("Validating sale request for store: {}", saleBean.storeId)

        validateStoreId(saleBean.storeId)
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
        // 外部キーはSQLiteの既定で効かないため、ここで実在を確かめる
        if (!storeRepository.existsById(storeId)) {
            throw IllegalArgumentException("Store with ID $storeId does not exist")
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
        val itemId = item.id
        if (itemId == null || itemId <= 0) {
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
    private fun validateDeposit(
        saleBean: SaleBean,
        items: List<ItemBean>,
    ) {
        val totalAmount = items.sumOf { it.price }

        if (saleBean.deposit < 0) {
            throw IllegalArgumentException("Deposit cannot be negative")
        }

        if (saleBean.deposit < totalAmount) {
            throw IllegalArgumentException(
                "Insufficient deposit: required $totalAmount, provided ${saleBean.deposit}",
            )
        }
    }
}
