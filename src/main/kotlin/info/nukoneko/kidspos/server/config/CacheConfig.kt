package info.nukoneko.kidspos.server.config

import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurerSupport
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Cache configuration for improving performance
 * Uses in-memory caching with ConcurrentHashMap
 */
@Configuration
@EnableCaching
class CacheConfig : CachingConfigurerSupport() {
    private val logger = LoggerFactory.getLogger(CacheConfig::class.java)

    companion object {
        // Cache names
        const val ITEMS_CACHE = "items"
        const val ITEM_BY_ID_CACHE = "itemById"
        const val ITEM_BY_BARCODE_CACHE = "itemByBarcode"
        const val STORES_CACHE = "stores"
        const val STORE_BY_ID_CACHE = "storeById"
        const val STAFF_CACHE = "staff"
        const val STAFF_BY_ID_CACHE = "staffById"
        const val SETTINGS_CACHE = "settings"
        const val NETWORK_HOSTS_CACHE = "networkHosts"
    }

    @Bean
    override fun cacheManager(): CacheManager {
        logger.info("Initializing cache manager with predefined caches")

        return ConcurrentMapCacheManager(
            ITEMS_CACHE,
            ITEM_BY_ID_CACHE,
            ITEM_BY_BARCODE_CACHE,
            STORES_CACHE,
            STORE_BY_ID_CACHE,
            STAFF_CACHE,
            STAFF_BY_ID_CACHE,
            SETTINGS_CACHE,
            NETWORK_HOSTS_CACHE,
        ).apply {
            // Allow null values to handle non-existent items
            isAllowNullValues = true
            logger.info("Cache manager initialized with {} caches", cacheNames.size)
        }
    }

    @Bean
    fun cacheEventLogger(): CacheEventLogger = CacheEventLogger()
}

/**
 * Logger for cache events (for debugging and monitoring)
 */
class CacheEventLogger {
    private val logger = LoggerFactory.getLogger(CacheEventLogger::class.java)

    fun logCacheHit(
        cacheName: String,
        key: Any,
    ) {
        logger.debug("Cache HIT - Cache: {}, Key: {}", cacheName, key)
    }

    fun logCacheMiss(
        cacheName: String,
        key: Any,
    ) {
        logger.debug("Cache MISS - Cache: {}, Key: {}", cacheName, key)
    }

    fun logCachePut(
        cacheName: String,
        key: Any,
    ) {
        logger.debug("Cache PUT - Cache: {}, Key: {}", cacheName, key)
    }

    fun logCacheEvict(
        cacheName: String,
        key: Any,
    ) {
        logger.debug("Cache EVICT - Cache: {}, Key: {}", cacheName, key)
    }
}
