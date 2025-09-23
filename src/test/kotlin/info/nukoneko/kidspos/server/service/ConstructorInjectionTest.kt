package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.common.service.IdGenerationService
import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.SaleRepository
import info.nukoneko.kidspos.server.repository.SaleDetailRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@SpringBootTest
@Disabled("Spring context not configured")
class ConstructorInjectionTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `services should use constructor injection`() {
        // Given - Create mocks
        val itemRepository = mock(ItemRepository::class.java)
        val idGenerationService = mock(IdGenerationService::class.java)

        // When - Create service with constructor injection
        val itemService = ItemService(itemRepository, idGenerationService)

        // Then - Service should be properly initialized
        assertThat(itemService).isNotNull
    }

    @Test
    fun `sale service should use constructor injection`() {
        // Given - Create mocks
        val itemRepository = mock(ItemRepository::class.java)
        val saleRepository = mock(SaleRepository::class.java)
        val saleDetailRepository = mock(SaleDetailRepository::class.java)
        val idGenerationService = mock(IdGenerationService::class.java)

        // When - Create service with constructor injection
        val saleService = SaleService(
            itemRepository,
            saleRepository,
            saleDetailRepository,
            idGenerationService
        )

        // Then - Service should be properly initialized
        assertThat(saleService).isNotNull
    }

    @Test
    fun `spring context should properly wire dependencies with constructor injection`() {
        // When - Get beans from context
        val itemService = applicationContext.getBean(ItemService::class.java)
        val saleService = applicationContext.getBean(SaleService::class.java)

        // Then - Beans should be properly initialized
        assertThat(itemService).isNotNull
        assertThat(saleService).isNotNull
    }
}