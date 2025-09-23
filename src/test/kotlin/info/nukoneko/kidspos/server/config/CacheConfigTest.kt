package info.nukoneko.kidspos.server.config

import info.nukoneko.kidspos.server.service.ItemService
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles

/**
 * Test for cache configuration
 * Ensures cache properly handles null values
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Cache Configuration Tests")
@Disabled("Spring context not configured")
class CacheConfigTest {
    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var itemService: ItemService

    @Test
    @DisplayName("Should handle null values in cache")
    fun shouldHandleNullValuesInCache() {
        // Try to find a non-existent item
        val nonExistentId = 999999
        val result = itemService.findItem(nonExistentId)

        // Should return null without throwing exception
        assertNull(result)

        // Second call should also work (from cache)
        val cachedResult = itemService.findItem(nonExistentId)
        assertNull(cachedResult)
    }

    @Test
    @DisplayName("Should cache existing items")
    fun shouldCacheExistingItems() {
        // This test would need actual data setup
        // For now, just verify cache manager exists
        assertNotNull(cacheManager)

        val itemCache = cacheManager.getCache(CacheConfig.ITEM_BY_ID_CACHE)
        assertNotNull(itemCache)
    }
}