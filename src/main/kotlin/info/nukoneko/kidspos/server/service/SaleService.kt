package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.common.Constants
import info.nukoneko.kidspos.common.service.IdGenerationService
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.repository.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service for handling sale operations
 *
 * Manages sale transactions including creation, validation, and persistence
 * of sales data. This is the legacy service that is being gradually replaced
 * by more specialized services following Single Responsibility Principle.
 *
 * @constructor Creates SaleService with required repositories and services
 * @param itemRepository Repository for item data access
 * @param saleRepository Repository for sale data access
 * @param saleDetailRepository Repository for sale detail data access
 * @param idGenerationService Service for generating unique IDs
 */
@Service
@Transactional
class SaleService(
    private val itemRepository: ItemRepository,
    private val saleRepository: SaleRepository,
    private val saleDetailRepository: SaleDetailRepository,
    private val idGenerationService: IdGenerationService
) {
    private val logger = LoggerFactory.getLogger(SaleService::class.java)

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var staffRepository: StaffRepository

    fun findAllSale(): List<SaleEntity> {
        return saleRepository.findAll()
    }

    fun findAllSaleDetail(): List<SaleDetailEntity> {
        return saleDetailRepository.findAll()
    }

    fun findSale(id: Int): SaleEntity {
        return saleRepository.findById(id).get()
    }

    fun findSale(barcode: String): SaleEntity {
        val id = barcode.substring(barcode.length - Constants.Barcode.SUFFIX_LENGTH).toInt()
        return saleRepository.findById(id).get()
    }

    /**
     * 綺麗にしたい
     */
    fun save(saleBean: SaleBean, items: List<ItemBean>): SaleEntity {
        logger.info("Creating sale for store: {}, staff barcode: {}", saleBean.storeId, "***")
        val id = idGenerationService.generateNextId(saleRepository)

        // 売り上げを保存
        val staffId = if (saleBean.staffBarcode.length > Constants.Barcode.MIN_LENGTH) {
            saleBean.staffBarcode.substring(saleBean.staffBarcode.length - Constants.Barcode.SUFFIX_LENGTH)
                .toIntOrNull() ?: 0
        } else {
            0
        }
        items.forEach {
            logger.debug("Item - ID: {}, Name: {}, Price: {}", it.id, it.name, it.price)
        }
        val sale = SaleEntity(
            id, saleBean.storeId, staffId,
            items.size, items.sumOf { it.price }, saleBean.deposit, Date()
        )

        val savedSale = saleRepository.save(sale)
        logger.info("Sale created successfully with ID: {}, total amount: {}", savedSale.id, savedSale.amount)

        // 売り上げの詳細を保存
        items
            .toSet().mapNotNull { it.id }.distinct()
            .forEach { itemId ->
                val saleDetailId = idGenerationService.generateNextId(saleDetailRepository)
                val filteredItems = items.filter { it.id == itemId }
                saleDetailRepository.save(
                    SaleDetailEntity(
                        saleDetailId,
                        id,
                        itemId,
                        filteredItems[0].price,
                        filteredItems.size
                    )
                )
            }

        return savedSale
    }
}
