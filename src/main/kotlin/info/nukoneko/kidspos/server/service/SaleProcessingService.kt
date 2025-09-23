package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import info.nukoneko.kidspos.common.Constants
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.entity.SaleDetailEntity

/**
 * Main sale processing service that orchestrates the sale creation process
 *
 * This service handles the complete sale transaction workflow by coordinating
 * validation, calculation, and persistence services. It was refactored from
 * the original SaleService to follow Single Responsibility Principle and
 * improve maintainability.
 *
 * @constructor Creates SaleProcessingService with required dependencies
 * @param saleCalculationService Service for sales amount calculations
 * @param saleValidationService Service for sales validation logic
 * @param salePersistenceService Service for sales data persistence
 */
@Service
@Transactional
class SaleProcessingService(
    private val saleCalculationService: SaleCalculationService,
    private val saleValidationService: SaleValidationService,
    private val salePersistenceService: SalePersistenceService
) {
    private val logger = LoggerFactory.getLogger(SaleProcessingService::class.java)

    /**
     * Process a complete sale transaction
     *
     * Orchestrates the entire sale processing workflow including validation,
     * persistence of sale record and sale details. Logs the processing progress
     * and handles transaction boundaries.
     *
     * @param saleBean Sale request data containing store ID, staff info, and deposit
     * @param items List of items being purchased with their details
     * @return Persisted sale entity with generated ID and calculated total
     * @throws IllegalArgumentException if validation fails for sale request or items
     */
    fun processSale(saleBean: SaleBean, items: List<ItemBean>): SaleEntity {
        logger.info("Processing sale for store: {}, items: {}", saleBean.storeId, items.size)

        // Step 1: Validate the sale request
        saleValidationService.validateSaleRequest(saleBean, items)

        // Step 2: Save the sale
        val savedSale = salePersistenceService.saveSale(saleBean, items)

        // Step 3: Save sale details
        salePersistenceService.saveSaleDetails(savedSale.id, items)

        logger.info("Sale processed successfully: ID={}, total={}",
                   savedSale.id, savedSale.amount)

        return savedSale
    }

    /**
     * Extract staff ID from staff barcode
     *
     * Parses the staff barcode to extract the numeric staff ID from the suffix.
     * Returns 0 if the barcode is invalid or too short.
     *
     * @param staffBarcode The barcode string containing staff information
     * @return Extracted staff ID as integer, or 0 if extraction fails
     */
    fun extractStaffId(staffBarcode: String): Int {
        return if (staffBarcode.length > Constants.Barcode.MIN_LENGTH) {
            staffBarcode.substring(staffBarcode.length - Constants.Barcode.SUFFIX_LENGTH)
                .toIntOrNull() ?: 0
        } else {
            0
        }
    }

    /**
     * Calculate sale summary
     *
     * Calculates comprehensive sale summary including total amount, change,
     * item counts, and quantity distributions for reporting purposes.
     *
     * @param items List of items in the sale
     * @param deposit Customer deposit amount
     * @return SaleSummary containing calculated totals and statistics
     */
    fun calculateSaleSummary(items: List<ItemBean>, deposit: Int): SaleSummary {
        val totalAmount = saleCalculationService.calculateSaleAmount(items)
        val change = saleCalculationService.calculateChange(totalAmount, deposit)
        val itemQuantities = saleCalculationService.calculateItemQuantities(items)

        return SaleSummary(
            totalAmount = totalAmount,
            deposit = deposit,
            change = change,
            itemCount = items.size,
            uniqueItems = itemQuantities.size,
            itemQuantities = itemQuantities
        )
    }

    /**
     * Validate and process sale with enhanced error handling
     *
     * Processes a sale with comprehensive error handling and returns structured
     * result indicating success, validation error, or processing error.
     *
     * @param saleBean Sale request data
     * @param items List of items being purchased
     * @return SaleResult indicating success with data or specific error type
     */
    fun processSaleWithValidation(saleBean: SaleBean, items: List<ItemBean>): SaleResult {
        return try {
            val sale = processSale(saleBean, items)
            val summary = calculateSaleSummary(items, saleBean.deposit)

            SaleResult.Success(sale, summary)
        } catch (e: IllegalArgumentException) {
            logger.warn("Sale validation failed: {}", e.message)
            SaleResult.ValidationError(e.message ?: "Validation error")
        } catch (e: Exception) {
            logger.error("Sale processing failed", e)
            SaleResult.ProcessingError("Failed to process sale: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Find sale by ID
     *
     * Retrieves a specific sale record by its unique identifier.
     *
     * @param id Unique sale identifier
     * @return SaleEntity if found, null otherwise
     */
    fun findSaleById(id: Int): SaleEntity? {
        return salePersistenceService.findSaleById(id)
    }

    /**
     * Find all sales
     *
     * Retrieves all sale records from the database.
     *
     * @return List of all SaleEntity records
     */
    fun findAllSales(): List<SaleEntity> {
        return salePersistenceService.findAllSales()
    }

    /**
     * Find sale details by sale ID
     *
     * Retrieves all sale detail records for a specific sale.
     *
     * @param saleId Sale identifier
     * @return List of SaleDetailEntity records
     */
    fun findSaleDetailsBySaleId(saleId: Int): List<SaleDetailEntity> {
        return salePersistenceService.findSaleDetailsBySaleId(saleId)
    }
}

/**
 * Data class for sale summary information
 */
data class SaleSummary(
    val totalAmount: Int,
    val deposit: Int,
    val change: Int,
    val itemCount: Int,
    val uniqueItems: Int,
    val itemQuantities: Map<Int, Int>
)

/**
 * Sealed class for sale processing results
 */
sealed class SaleResult {
    data class Success(val sale: SaleEntity, val summary: SaleSummary) : SaleResult()
    data class Error(val message: String) : SaleResult()
    data class ValidationError(val message: String) : SaleResult()
    data class ProcessingError(val message: String) : SaleResult()
}