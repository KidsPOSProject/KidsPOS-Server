package info.nukoneko.kidspos.server.e2e

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.repository.*
import info.nukoneko.kidspos.server.service.*

/**
 * Base class for end-to-end integration tests
 * Part of Task 10.1: E2E test implementation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
abstract class EndToEndTestBase {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var itemRepository: ItemRepository

    @Autowired
    protected lateinit var saleRepository: SaleRepository

    @Autowired
    protected lateinit var saleDetailRepository: SaleDetailRepository

    @Autowired
    protected lateinit var staffRepository: StaffRepository

    @Autowired
    protected lateinit var storeRepository: StoreRepository

    @Autowired
    protected lateinit var settingRepository: SettingRepository

    @Autowired
    protected lateinit var itemService: ItemService

    @Autowired
    protected lateinit var saleService: SaleService

    @Autowired
    protected lateinit var staffService: StaffService

    @Autowired
    protected lateinit var storeService: StoreService

    @BeforeEach
    fun clearDatabase() {
        // Clear all data before each test for isolation
        saleDetailRepository.deleteAll()
        saleRepository.deleteAll()
        itemRepository.deleteAll()
        staffRepository.deleteAll()
        storeRepository.deleteAll()
        settingRepository.deleteAll()
    }

    /**
     * Helper method to convert objects to JSON
     */
    protected fun toJson(obj: Any): String {
        return objectMapper.writeValueAsString(obj)
    }

    /**
     * Helper method to parse JSON response
     */
    protected fun <T> fromJson(json: String, clazz: Class<T>): T {
        return objectMapper.readValue(json, clazz)
    }
}