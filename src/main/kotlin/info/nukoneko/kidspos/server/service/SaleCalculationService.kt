package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for sale calculations
 *
 * Handles calculation of sale totals, quantities, and item grouping.
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
}
