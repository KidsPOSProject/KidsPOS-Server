package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.common.service.IdGenerationService
import info.nukoneko.kidspos.server.config.CacheConfig
import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing product item operations
 *
 * Handles CRUD operations for product items with comprehensive caching
 * support and automatic ID generation. Provides multiple lookup strategies
 * including by ID and barcode for efficient POS operations. Implements
 * multi-level caching to optimize performance for frequently accessed
 * product data.
 *
 * Key responsibilities:
 * - Managing product item data storage and retrieval
 * - Providing cached access with multiple cache levels
 * - Supporting both ID-based and barcode-based lookups
 * - Automatic ID generation for new items
 * - Cache invalidation management for data consistency
 *
 * Caching strategy:
 * - All items cache for bulk operations
 * - Individual item cache by ID
 * - Individual item cache by barcode
 * - Coordinated cache eviction on updates
 *
 * @constructor Creates ItemService with required dependencies
 * @param repository Repository for item data access
 * @param idGenerationService Service for generating unique item IDs
 */
@Service
@Transactional
class ItemService(
    private val repository: ItemRepository,
    private val idGenerationService: IdGenerationService,
) {
    private val logger = LoggerFactory.getLogger(ItemService::class.java)

    @Cacheable(value = [CacheConfig.ITEMS_CACHE])
    fun findAll(): List<ItemEntity> {
        logger.debug("Fetching all items from database")
        return repository.findAll()
    }

    @Cacheable(value = [CacheConfig.ITEM_BY_ID_CACHE], key = "#id")
    fun findItem(id: Int): ItemEntity? {
        logger.debug("Fetching item by ID: {} from database", id)
        return repository.findByIdOrNull(id)
    }

    @Cacheable(value = [CacheConfig.ITEM_BY_BARCODE_CACHE], key = "#barcode")
    fun findItem(barcode: String): ItemEntity? {
        logger.debug("Fetching item by barcode: {} from database", barcode)
        return repository.findByBarcode(barcode)
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.ITEMS_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.ITEM_BY_ID_CACHE], key = "#result.id"),
            CacheEvict(value = [CacheConfig.ITEM_BY_BARCODE_CACHE], key = "#result.barcode"),
        ],
    )
    fun save(itemBean: ItemBean): ItemEntity {
        logger.info("Creating item with barcode: {}, name: {}", itemBean.barcode, itemBean.name)
        val itemId = itemBean.id
        val generatedId =
            if (itemId != null && itemId > 0) {
                itemId
            } else {
                idGenerationService.generateNextId(repository)
            }
        val item = ItemEntity(generatedId, itemBean.barcode, itemBean.name, itemBean.price)
        val savedItem = repository.save(item)
        logger.info("Item created successfully with ID: {}", savedItem.id)
        return savedItem
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.ITEMS_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.ITEM_BY_ID_CACHE], key = "#id"),
        ],
    )
    fun delete(id: Int) {
        logger.info("Deleting item with ID: {}", id)
        val item = repository.findByIdOrNull(id)
        if (item != null) {
            repository.delete(item)
            logger.info("Item deleted successfully with ID: {}", id)
        }
    }
}
