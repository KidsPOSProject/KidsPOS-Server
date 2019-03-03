package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.api.model.ItemBean
import info.nukoneko.kidspos.server.controller.api.model.SaleBean
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.repository.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class SaleService {
    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var saleRepository: SaleRepository

    @Autowired
    private lateinit var saleDetailRepository: SaleDetailRepository

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var staffRepository: StaffRepository

    fun findAllSale(): List<SaleEntity> {
        return saleRepository.findAll()
    }

    fun findAllSaleDetail(): List<SaleDetailEntity> {
        return saleDetailRepository.findAll()
    }

    fun findSale(id: Int): SaleEntity {
        return saleRepository.findById(id).get()
    }

    fun findSale(barcode: String): SaleEntity {
        val id = barcode.substring(barcode.length - 3).toInt()
        return saleRepository.findById(id).get()
    }

    /**
     * 綺麗にしたい
     */
    fun save(saleBean: SaleBean, items: List<ItemBean>): SaleEntity {
        val id = try {
            saleRepository.getLastId() + 1
        } catch (e: EmptyResultDataAccessException) {
            1
        }

        // 売り上げを保存
        val staffId = if (saleBean.staffBarcode.length > 4) {
            saleBean.staffBarcode.substring(saleBean.staffBarcode.length - 3).toIntOrNull() ?: 0
        } else {
            0
        }
        items.forEach {
            println(it.id)
            println(it.name)
            println(it.price)
        }
        val sale = SaleEntity(id, saleBean.storeId, staffId,
                items.size, items.sumBy { it.price }, saleBean.deposit, Date())

        val savedSale = saleRepository.save(sale)

        // 売り上げの詳細を保存
        items
                .toSet()
                .map { it.id }.filter { it != null }.distinct()
                .forEach { itemId ->
                    val saleDetailId = try {
                        saleDetailRepository.getLastId() + 1
                    } catch (e: EmptyResultDataAccessException) {
                        1
                    }
                    val items = items.filter { it.id == itemId!! }
                    saleDetailRepository.save(SaleDetailEntity(saleDetailId, id, itemId!!, items[0].price, items.size))
                }

        return savedSale
    }
}