package info.nukoneko.kidspos.server.config

import info.nukoneko.kidspos.common.service.IdGenerationService
import info.nukoneko.kidspos.server.service.*
import info.nukoneko.kidspos.server.service.mapper.ItemMapper
import info.nukoneko.kidspos.server.service.mapper.SaleMapper
import info.nukoneko.kidspos.server.service.mapper.StaffMapper
import info.nukoneko.kidspos.server.service.mapper.StoreMapper
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Test configuration for OpenAPI integration tests
 *
 * Provides mock beans for all required services to enable
 * Spring context initialization during OpenAPI testing.
 */
@TestConfiguration
class OpenApiTestConfiguration {

    @Bean
    @Primary
    fun idGenerationService(): IdGenerationService = mock(IdGenerationService::class.java)

    @Bean
    @Primary
    fun itemService(): ItemService = mock(ItemService::class.java)

    @Bean
    @Primary
    fun storeService(): StoreService = mock(StoreService::class.java)

    @Bean
    @Primary
    fun saleService(): SaleService = mock(SaleService::class.java)

    @Bean
    @Primary
    fun staffService(): StaffService = mock(StaffService::class.java)

    @Bean
    @Primary
    fun itemMapper(): ItemMapper = mock(ItemMapper::class.java)

    @Bean
    @Primary
    fun storeMapper(): StoreMapper = mock(StoreMapper::class.java)

    @Bean
    @Primary
    fun staffMapper(): StaffMapper = mock(StaffMapper::class.java)

    @Bean
    @Primary
    fun saleMapper(): SaleMapper = mock(SaleMapper::class.java)

    @Bean
    @Primary
    fun validationService(): ValidationService = mock(ValidationService::class.java)


    @Bean
    @Primary
    fun receiptService(): ReceiptService = mock(ReceiptService::class.java)


    @Bean
    @Primary
    fun saleProcessingService(): SaleProcessingService = mock(SaleProcessingService::class.java)


}