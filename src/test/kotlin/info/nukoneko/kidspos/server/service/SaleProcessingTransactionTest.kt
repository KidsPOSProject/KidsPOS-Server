package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.dto.request.ItemBean
import info.nukoneko.kidspos.server.controller.dto.request.SaleBean
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.repository.SaleDetailRepository
import info.nukoneko.kidspos.server.repository.SaleRepository
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles

/**
 * 明細の保存が失敗したときにトランザクション境界が破綻していないことを確かめる
 *
 * サービスをプロキシ経由で受け取らないとトランザクションが張られず、この不具合は再現しない。
 */
@SpringBootTest
@ActiveProfiles("test")
class SaleProcessingTransactionTest {
    @Autowired
    private lateinit var saleProcessingService: SaleProcessingService

    @Autowired
    private lateinit var saleRepository: SaleRepository

    @MockBean
    private lateinit var saleDetailRepository: SaleDetailRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    private fun existingStoreId(): Int = storeRepository.save(StoreEntity(name = "検証店", printerUri = "")).id

    @Test
    fun `明細の保存に失敗したら処理エラーを返す`() {
        whenever(saleDetailRepository.save(any<SaleDetailEntity>()))
            .thenThrow(RuntimeException("明細の保存に失敗"))

        val saleBean = SaleBean(storeId = existingStoreId(), itemIds = "1", deposit = 500)
        val items = listOf(ItemBean(id = 1, barcode = "A01000001A", name = "テスト商品", price = 100))

        val result = saleProcessingService.processSaleWithValidation(saleBean, items)

        assertThat(result).isInstanceOf(SaleResult.ProcessingError::class.java)
    }

    @Test
    fun `明細の保存に失敗したら売上も残さない`() {
        whenever(saleDetailRepository.save(any<SaleDetailEntity>()))
            .thenThrow(RuntimeException("明細の保存に失敗"))

        val storeId = existingStoreId()
        val before = saleRepository.count()
        val saleBean = SaleBean(storeId = storeId, itemIds = "1", deposit = 500)
        val items = listOf(ItemBean(id = 1, barcode = "A01000001A", name = "テスト商品", price = 100))

        saleProcessingService.processSaleWithValidation(saleBean, items)

        assertThat(saleRepository.count()).isEqualTo(before)
    }
}
