package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.common.service.IdGenerationService
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.repository.SaleDetailRepository
import info.nukoneko.kidspos.server.repository.SaleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service responsible for sale persistence operations
 * Separated from SaleService to follow Single Responsibility Principle
 */
@Service
@Transactional
class SalePersistenceService(
    private val saleRepository: SaleRepository,
    private val saleDetailRepository: SaleDetailRepository,
    private val idGenerationService: IdGenerationService,
    private val saleCalculationService: SaleCalculationService,
) {
    private val logger = LoggerFactory.getLogger(SalePersistenceService::class.java)

    /**
     * Save sale entity
     */
    fun saveSale(
        saleBean: SaleBean,
        items: List<ItemBean>,
    ): SaleEntity {
        val saleId = idGenerationService.generateNextId(saleRepository)
        val totalAmount = saleCalculationService.calculateSaleAmount(items)
        val quantity = saleCalculationService.calculateQuantity(items)
        val changeAmount = saleCalculationService.calculateChange(saleBean.deposit, totalAmount)

        val sale =
            SaleEntity(
                id = saleId,
                storeId = saleBean.storeId,
                quantity = quantity,
                amount = totalAmount,
                deposit = saleBean.deposit,
                changeAmount = changeAmount,
                createdAt = Date(),
            )

        val savedSale = saleRepository.save(sale)
        logger.info(
            "Sale saved successfully: ID={}, amount={}, items={}",
            savedSale.id,
            savedSale.amount,
            savedSale.quantity,
        )

        return savedSale
    }

    /**
     * Save sale detail entities
     */
    fun saveSaleDetails(
        saleId: Int,
        items: List<ItemBean>,
    ): List<SaleDetailEntity> {
        val groupedItems = saleCalculationService.groupItemsByType(items)
        val savedDetails = mutableListOf<SaleDetailEntity>()

        groupedItems.forEach { (itemId, itemList) ->
            val detailId = idGenerationService.generateNextId(saleDetailRepository)
            val quantity = itemList.size
            val unitPrice = itemList.first().price

            val saleDetail =
                SaleDetailEntity(
                    id = detailId,
                    saleId = saleId,
                    itemId = itemId,
                    price = unitPrice,
                    quantity = quantity,
                )

            val savedDetail = saleDetailRepository.save(saleDetail)
            savedDetails.add(savedDetail)

            logger.debug(
                "Sale detail saved: item={}, quantity={}, price={}",
                itemId,
                quantity,
                unitPrice,
            )
        }

        logger.info("Saved {} sale detail records for sale ID: {}", savedDetails.size, saleId)
        return savedDetails
    }

    /**
     * Find sale by ID
     */
    fun findSaleById(id: Int): SaleEntity? = saleRepository.findById(id).orElse(null)

    /**
     * Find all sales
     */
    fun findAllSales(): List<SaleEntity> = saleRepository.findAll()

    /**
     * Find sale details by sale ID
     */
    fun findSaleDetailsBySaleId(saleId: Int): List<SaleDetailEntity> = saleDetailRepository.findBySaleId(saleId)

    /**
     * Find all sale details
     */
    fun findAllSaleDetails(): List<SaleDetailEntity> = saleDetailRepository.findAll()
}
