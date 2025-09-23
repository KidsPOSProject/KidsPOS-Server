package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.CacheConfig
import info.nukoneko.kidspos.server.entity.SettingEntity
import info.nukoneko.kidspos.server.repository.SettingRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing application settings and configurations
 *
 * Handles CRUD operations for system settings including caching management
 * and specialized printer configuration management. Settings are cached to
 * improve performance and reduce database load for frequently accessed
 * configuration values.
 *
 * Key responsibilities:
 * - Managing application-wide settings storage and retrieval
 * - Handling printer host/port configuration for stores
 * - Providing cached access to settings for improved performance
 * - Maintaining settings data integrity with proper cache eviction
 *
 * @constructor Creates SettingService with required repository
 * @param repository Repository for settings data access
 */
@Service
@Transactional
class SettingService(
    private val repository: SettingRepository
) {
    private val logger = LoggerFactory.getLogger(SettingService::class.java)

    @Cacheable(value = [CacheConfig.SETTINGS_CACHE])
    fun findAllSetting(): List<SettingEntity> {
        logger.debug("Fetching all settings from database")
        return repository.findAll()
    }

    @Cacheable(value = [CacheConfig.SETTINGS_CACHE], key = "#key")
    fun findSetting(key: String): SettingEntity? {
        logger.debug("Fetching setting by key: {} from database", key)
        return repository.findByIdOrNull(key)
    }

    @CacheEvict(value = [CacheConfig.SETTINGS_CACHE], allEntries = true)
    fun savePrinterHostPort(storeId: Int, host: String, port: Int) {
        logger.info("Saving printer settings for store ID: {}", storeId)
        repository.save(SettingEntity("${KEY_PRINTER}_$storeId", "$host:$port"))
    }

    @Cacheable(value = [CacheConfig.SETTINGS_CACHE], key = "'printer_' + #storeId")
    fun findPrinterHostPortById(storeId: Int): Pair<String, Int>? {
        val setting: SettingEntity? =
            repository.findByIdOrNull("${KEY_PRINTER}_$storeId")
        return setting?.let {
            val matchResult =
                REGEX_HOST_PORT.matchEntire(it.value) ?: return null
            return if (matchResult.groupValues.size != 2) {
                null
            } else {
                Pair(
                    matchResult.groupValues[0],
                    matchResult.groupValues[1].toInt()
                )
            }
        }
    }

    private companion object {
        private const val KEY_PRINTER = "printer"

        private val REGEX_HOST_PORT = "(.*):([0-9]{2,5})".toRegex()
    }

    data class ApplicationSetting(
        val serverHost: String,
        val serverPort: Int
    )
}
