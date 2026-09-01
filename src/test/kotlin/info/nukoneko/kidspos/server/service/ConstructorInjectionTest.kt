package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.repository.ItemRepository
import info.nukoneko.kidspos.server.repository.SaleDetailRepository
import info.nukoneko.kidspos.server.repository.SaleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher

@SpringBootTest
class ConstructorInjectionTest {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun `services should use constructor injection`() {
        // Given - Create mocks
        val itemRepository = mock(ItemRepository::class.java)
        val eventPublisher = mock(ApplicationEventPublisher::class.java)

        // When - Create service with constructor injection
        val itemService = ItemService(itemRepository, eventPublisher)

        // Then - Service should be properly initialized
        assertThat(itemService).isNotNull
    }

    @Test
    fun `sale service should use constructor injection`() {
        // Given - Create mocks
        val itemRepository = mock(ItemRepository::class.java)
        val saleRepository = mock(SaleRepository::class.java)
        val saleDetailRepository = mock(SaleDetailRepository::class.java)

        // When - Create service with constructor injection
        val saleService =
            SaleService(
                itemRepository,
                saleRepository,
                saleDetailRepository,
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
