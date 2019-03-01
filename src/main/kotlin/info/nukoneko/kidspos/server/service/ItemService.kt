package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.api.model.ItemBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ItemService {

    @Autowired
    private lateinit var repository: ItemRepository

    fun findAll(): List<ItemEntity> {
        return repository.findAll()
    }

    fun findItem(id: Int): ItemEntity? {
        return repository.findByIdOrNull(id)
    }

    fun findItem(barcode: String): ItemEntity? {
        return repository.findByBarcode(barcode)
    }

    fun save(itemBean: ItemBean): ItemEntity {
        val id = try {
            repository.getLastId() + 1
        } catch (e: Throwable) {
            1
        }
        val item = ItemEntity(id, itemBean.barcode, itemBean.name, itemBean.price)
        return repository.save(item)
    }
}