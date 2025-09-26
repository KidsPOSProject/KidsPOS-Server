package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.common.service.IdGenerationService
import info.nukoneko.kidspos.server.config.CacheConfig
import info.nukoneko.kidspos.server.controller.dto.request.StoreBean
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing store operations and configurations
 *
 * Handles CRUD operations for store entities with caching support and
 * automatic ID generation. Stores represent physical locations where
 * POS systems operate, each with their own configuration including
 * printer settings and operational parameters.
 *
 * Key responsibilities:
 * - Managing store entity data storage and retrieval
 * - Providing cached access to store information
 * - Automatic ID generation for new stores
 * - Supporting store-specific configuration management
 * - Cache invalidation coordination for data consistency
 *
 * Store lifecycle:
 * - New stores get automatically generated unique IDs
 * - Store configurations include printer URI settings
 * - Cached data ensures fast access for POS operations
 * - Updates trigger appropriate cache eviction
 *
 * @constructor Creates StoreService with required dependencies
 * @param repository Repository for store data access
 * @param idGenerationService Service for generating unique store IDs
 */
@Service
@Transactional
class StoreService(
    private val repository: StoreRepository,
    private val idGenerationService: IdGenerationService,
) {
    private val logger = LoggerFactory.getLogger(StoreService::class.java)

    @Cacheable(value = [CacheConfig.STORES_CACHE])
    fun findAll(): List<StoreEntity> {
        logger.debug("Fetching all stores from database")
        return repository.findAll()
    }

    @Cacheable(value = [CacheConfig.STORE_BY_ID_CACHE], key = "#id")
    fun findStore(id: Int): StoreEntity? {
        logger.debug("Fetching store by ID: {} from database", id)
        return repository.findByIdOrNull(id)
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.STORES_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.STORE_BY_ID_CACHE], key = "#result.id"),
        ],
    )
    fun save(storeBean: StoreBean): StoreEntity {
        logger.info("Saving store with name: {}", storeBean.name)
        val storeId = storeBean.id
        val generatedId =
            if (storeId != null && storeId > 0) {
                storeId
            } else {
                idGenerationService.generateNextId(repository)
            }
        val store = StoreEntity(generatedId, storeBean.name, storeBean.printerUri)
        val savedStore = repository.save(store)
        logger.info("Store saved successfully with ID: {}", savedStore.id)
        return savedStore
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.STORES_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.STORE_BY_ID_CACHE], key = "#result.id"),
        ],
    )
    fun save(store: StoreEntity): StoreEntity {
        logger.info("Saving store: {}", store.name)
        val savedStore =
            if (store.id == 0) {
                val id = idGenerationService.generateNextId(repository)
                repository.save(store.copy(id = id))
            } else {
                repository.save(store)
            }
        logger.info("Store saved successfully with ID: {}", savedStore.id)
        return savedStore
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.STORES_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.STORE_BY_ID_CACHE], key = "#id"),
        ],
    )
    fun delete(id: Int) {
        logger.info("Deleting store with ID: {}", id)
        repository.deleteById(id)
        logger.info("Store deleted successfully with ID: {}", id)
    }
}
