package info.nukoneko.kidspos.server

import info.nukoneko.kidspos.server.repository.*
import info.nukoneko.kidspos.server.service.SaleService
import info.nukoneko.kidspos.server.service.SettingService
import info.nukoneko.kidspos.server.service.StoreService
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Test configuration for isolated controller tests
 *
 * Provides mock beans for all dependencies to prevent Spring context loading issues
 */
@TestConfiguration
class TestConfiguration {
    // Mock all repositories
    @Bean
    @Primary
    fun mockItemRepository(): ItemRepository = mock(ItemRepository::class.java)

    @Bean
    @Primary
    fun mockSaleRepository(): SaleRepository = mock(SaleRepository::class.java)

    @Bean
    @Primary
    fun mockSaleDetailRepository(): SaleDetailRepository = mock(SaleDetailRepository::class.java)

    @Bean
    @Primary
    fun mockStoreRepository(): StoreRepository = mock(StoreRepository::class.java)

    @Bean
    @Primary
    fun mockSettingRepository(): SettingRepository = mock(SettingRepository::class.java)

    // Mock all services that might be autowired
    @Bean
    @Primary
    fun mockSaleService(): SaleService = mock(SaleService::class.java)

    @Bean
    @Primary
    fun mockStoreService(): StoreService = mock(StoreService::class.java)

    @Bean
    @Primary
    fun mockSettingService(): SettingService = mock(SettingService::class.java)
}
