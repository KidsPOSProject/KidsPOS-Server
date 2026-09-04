package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.entity.SaleEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Main sale processing service that orchestrates the sale creation process
 *
 * This service handles the complete sale transaction workflow by coordinating
 * validation and persistence services.
 *
 * @constructor Creates SaleProcessingService with required dependencies
 * @param saleValidationService Service for sales validation logic
 * @param salePersistenceService Service for sales data persistence
 */
@Service
class SaleProcessingService(
    private val saleValidationService: SaleValidationService,
    private val salePersistenceService: SalePersistenceService,
) {
    private val logger = LoggerFactory.getLogger(SaleProcessingService::class.java)

    /**
     * Process a complete sale transaction
     *
     * Orchestrates the entire sale processing workflow including validation and
     * persistence of the sale record and its details. The transaction boundary
     * belongs to the persistence service.
     *
     * @param saleBean Sale request data containing store ID, staff info, and deposit
     * @param items List of items being purchased with their details
     * @return Persisted sale entity with generated ID and calculated total
     * @throws IllegalArgumentException if validation fails for sale request or items
     */
    fun processSale(
        saleBean: SaleBean,
        items: List<ItemBean>,
    ): SaleEntity {
        logger.info("Processing sale for store: {}, items: {}", saleBean.storeId, items.size)

        saleValidationService.validateSaleRequest(saleBean, items)

        // 保存はひとつのトランザクションにまとめる。ここで境界を張ると、下位が付けたロールバック指定と
        // 呼び出し元の例外処理が衝突して結果を返せなくなる
        val savedSale = salePersistenceService.saveSaleWithDetails(saleBean, items)

        logger.info(
            "Sale processed successfully: ID={}, total={}",
            savedSale.id,
            savedSale.amount,
        )

        return savedSale
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
    fun processSaleWithValidation(
        saleBean: SaleBean,
        items: List<ItemBean>,
    ): SaleResult =
        try {
            SaleResult.Success(processSale(saleBean, items))
        } catch (e: IllegalArgumentException) {
            logger.warn("Sale validation failed: {}", e.message)
            SaleResult.ValidationError(e.message ?: "Validation error")
        } catch (e: Exception) {
            logger.error("Sale processing failed", e)
            SaleResult.ProcessingError("Failed to process sale: ${e.message ?: "Unknown error"}")
        }

    /**
     * Find sale by ID
     *
     * Retrieves a specific sale record by its unique identifier.
     *
     * @param id Unique sale identifier
     * @return SaleEntity if found, null otherwise
     */
    fun findSaleById(id: Int): SaleEntity? = salePersistenceService.findSaleById(id)

    /**
     * Find all sales
     *
     * Retrieves all sale records from the database.
     *
     * @return List of all SaleEntity records
     */
    fun findAllSales(): List<SaleEntity> = salePersistenceService.findAllSales()
}

/**
 * Sealed class for sale processing results
 */
sealed class SaleResult {
    data class Success(
        val sale: SaleEntity,
    ) : SaleResult()

    data class ValidationError(
        val message: String,
    ) : SaleResult()

    data class ProcessingError(
        val message: String,
    ) : SaleResult()
}
