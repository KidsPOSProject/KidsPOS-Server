package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.receipt.ReceiptDetail
import info.nukoneko.kidspos.receipt.ReceiptPrinter
import info.nukoneko.kidspos.server.config.CacheConfig
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.entity.SettingEntity
import info.nukoneko.kidspos.server.repository.SettingRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date

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
    private val repository: SettingRepository,
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
    fun saveSetting(setting: SettingEntity): SettingEntity {
        logger.info("Saving setting with key: {}", setting.key)
        return repository.save(setting)
    }

    @CacheEvict(value = [CacheConfig.SETTINGS_CACHE], allEntries = true)
    fun savePrinterHostPort(
        storeId: Int,
        host: String,
        port: Int,
    ) {
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
            return if (matchResult.groupValues.size != 3) {
                null
            } else {
                Pair(
                    matchResult.groupValues[1],
                    matchResult.groupValues[2].toInt(),
                )
            }
        }
    }

    @CacheEvict(value = [CacheConfig.SETTINGS_CACHE], allEntries = true)
    fun deleteSetting(key: String) {
        logger.info("Deleting setting with key: {}", key)
        repository.deleteById(key)
    }

    @CacheEvict(value = [CacheConfig.SETTINGS_CACHE], allEntries = true)
    fun saveApplicationSetting(applicationSetting: ApplicationSetting) {
        logger.info(
            "Saving application settings: host={}, port={}",
            applicationSetting.serverHost,
            applicationSetting.serverPort,
        )
        repository.save(SettingEntity("${KEY_APP}_host", applicationSetting.serverHost))
        repository.save(SettingEntity("${KEY_APP}_port", applicationSetting.serverPort.toString()))
    }

    @Cacheable(value = [CacheConfig.SETTINGS_CACHE], key = "'application'")
    fun getApplicationSetting(): ApplicationSetting? {
        val host = repository.findByIdOrNull("${KEY_APP}_host")?.value
        val port = repository.findByIdOrNull("${KEY_APP}_port")?.value?.toIntOrNull()

        return if (host != null && port != null) {
            ApplicationSetting(host, port)
        } else {
            null
        }
    }

    /**
     * レシートのテスト印刷を実行
     *
     * @param storeId 店舗ID
     * @throws IllegalStateException プリンタ設定が見つからない場合
     * @throws Exception 印刷に失敗した場合
     */
    fun testPrintReceipt(storeId: Int) {
        logger.info("Test print receipt for store ID: {}", storeId)

        // プリンタ設定を取得
        val printerSettings =
            findPrinterHostPortById(storeId)
                ?: throw IllegalStateException("プリンタ設定が見つかりません（店舗ID: $storeId）")

        val (host, port) = printerSettings

        // テスト印刷用のレシート詳細を作成
        val testDetail =
            ReceiptDetail(
                storeName = "テスト店舗",
                items =
                    listOf(
                        ItemEntity(1, "TEST-001", "テスト商品A", 100),
                        ItemEntity(2, "TEST-002", "テスト商品B", 200),
                        ItemEntity(3, "TEST-003", "テスト商品C", 300),
                    ),
                deposit = 1000,
                createdAt = Date(),
                transactionId = "TEST-${System.currentTimeMillis()}",
            )

        // レシートを印刷
        val printer = ReceiptPrinter(host, port, testDetail)
        printer.print()

        logger.info("Test print completed successfully for store ID: {}", storeId)
    }

    private companion object {
        private const val KEY_PRINTER = "printer"
        private const val KEY_APP = "application"

        private val REGEX_HOST_PORT = "(.*):([0-9]{2,5})".toRegex()
    }

    data class ApplicationSetting(
        val serverHost: String,
        val serverPort: Int,
    )
}
