package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.controller.api.model.ItemBean
import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

@Service
@Transactional
class ItemService {

    @Autowired
    private lateinit var repository: ItemRepository
    
    // Simple in-memory cache to reduce database access
    private val idCache = ConcurrentHashMap<Int, ItemEntity>(100)
    private val barcodeCache = ConcurrentHashMap<String, ItemEntity>(100)
    
    fun findAll(): List<ItemEntity> {
        return repository.findAll()
    }

    fun findItem(id: Int): ItemEntity? {
        // Try to get from cache first
        return idCache[id] ?: run {
            val item = repository.findByIdOrNull(id)
            if (item != null) {
                idCache[id] = item
                barcodeCache[item.barcode] = item
            }
            item
        }
    }

    fun findItem(barcode: String): ItemEntity? {
        // Try to get from cache first
        return barcodeCache[barcode] ?: run {
            val item = repository.findByBarcode(barcode)
            if (item != null) {
                idCache[item.id] = item
                barcodeCache[barcode] = item
            }
            item
        }
    }

    fun save(itemBean: ItemBean): ItemEntity {
        val id = try {
            repository.getLastId() + 1
        } catch (e: Throwable) {
            1
        }
        val item = ItemEntity(id, itemBean.barcode, itemBean.name, itemBean.price)
        val savedItem = repository.save(item)
        
        // Update cache with saved item
        idCache[savedItem.id] = savedItem
        barcodeCache[savedItem.barcode] = savedItem
        
        return savedItem
    }
    
    // Method to clear cache if needed (e.g., for admin operations)
    fun clearCache() {
        idCache.clear()
        barcodeCache.clear()
    }
}