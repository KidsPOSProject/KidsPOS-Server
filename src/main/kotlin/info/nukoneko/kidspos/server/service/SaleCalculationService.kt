package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for sale calculations
 *
 * Handles all calculation logic related to sales including total amounts,
 * change calculations, item quantities, and subtotals. Separated from
 * SaleService to follow Single Responsibility Principle and improve testability.
 */
@Service
class SaleCalculationService {
    private val logger = LoggerFactory.getLogger(SaleCalculationService::class.java)

    /**
     * Calculate total amount for sale
     *
     * Sums up the prices of all items in the sale to get the total amount.
     *
     * @param items List of items with their prices
     * @return Total amount as integer
     */
    fun calculateSaleAmount(items: List<ItemBean>): Int {
        val total = items.sumOf { it.price }
        logger.debug("Calculated sale amount: {} for {} items", total, items.size)
        return total
    }

    /**
     * Calculate change for sale
     *
     * Calculates the change to be returned to customer by subtracting
     * total amount from the deposit amount.
     *
     * @param amount Total sale amount
     * @param deposit Customer deposit amount
     * @return Change amount (may be negative if insufficient funds)
     */
    fun calculateChange(
        amount: Int,
        deposit: Int,
    ): Int {
        val change = deposit - amount
        logger.debug("Calculated change: {} (deposit: {}, amount: {})", change, deposit, amount)
        return change
    }

    /**
     * Calculate quantity of items in sale
     *
     * Returns the total number of items in the sale (including duplicates).
     *
     * @param items List of items in the sale
     * @return Total quantity count
     */
    fun calculateQuantity(items: List<ItemBean>): Int = items.size

    /**
     * Group items by their ID to handle duplicates
     *
     * Groups items by their unique ID to facilitate quantity and subtotal
     * calculations for each distinct item type.
     *
     * @param items List of items to group
     * @return Map with item ID as key and list of matching items as value
     */
    fun groupItemsByType(items: List<ItemBean>): Map<Int, List<ItemBean>> {
        val grouped = items.groupBy { it.id!! }
        logger.debug("Grouped {} items into {} distinct types", items.size, grouped.size)
        return grouped
    }

    /**
     * Calculate quantity for each unique item
     *
     * Creates a map showing how many of each unique item are in the sale.
     *
     * @param items List of items in the sale
     * @return Map with item ID as key and quantity as value
     */
    fun calculateItemQuantities(items: List<ItemBean>): Map<Int, Int> = groupItemsByType(items).mapValues { it.value.size }

    /**
     * Calculate subtotal for each unique item
     *
     * Calculates the subtotal price for each distinct item type,
     * accounting for multiple quantities of the same item.
     *
     * @param items List of items in the sale
     * @return Map with item ID as key and subtotal amount as value
     */
    fun calculateItemSubtotals(items: List<ItemBean>): Map<Int, Int> =
        groupItemsByType(items).mapValues { (_, itemList) ->
            itemList.sumOf { it.price }
        }

    /**
     * Validate that deposit covers the total amount
     *
     * Checks if the customer's deposit is sufficient to cover the sale amount.
     *
     * @param amount Total sale amount
     * @param deposit Customer deposit amount
     * @return True if deposit is sufficient, false otherwise
     */
    fun isDepositSufficient(
        amount: Int,
        deposit: Int,
    ): Boolean = deposit >= amount
}
