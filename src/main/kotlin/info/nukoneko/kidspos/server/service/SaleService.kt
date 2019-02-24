package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.api.model.SaleBean
import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import info.nukoneko.kidspos.server.entity.SaleEntity
import info.nukoneko.kidspos.server.repository.SaleDetailRepository
import info.nukoneko.kidspos.server.repository.SaleRepository
import info.nukoneko.kidspos.server.repository.StaffRepository
import info.nukoneko.kidspos.server.repository.StoreRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SaleService {
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
    fun save(saleBean: SaleBean): SaleEntity {
        val id = try {
            saleRepository.getLastId() + 1
        } catch (e: EmptyResultDataAccessException) {
            1
        }

        // 売り上げを保存
        val sale = SaleEntity(id, saleBean.store_id, saleBean.staff_id,
                saleBean.items.size, saleBean.items.sumBy { it.price })

        val savedSale = saleRepository.save(sale)

        // 売り上げの詳細を保存
        saleBean.items
                .toSet()
                .map { it.id }.filter { it != null }.distinct()
                .forEach { itemId ->
                    val saleDetailId = try {
                        saleDetailRepository.getLastId() + 1
                    } catch (e: EmptyResultDataAccessException) {
                        1
                    }
                    val items = saleBean.items.filter { it.id == itemId!! }
                    saleDetailRepository.save(SaleDetailEntity(saleDetailId, id, itemId!!, items[0].price, items.size))
                }

        return savedSale
    }
}