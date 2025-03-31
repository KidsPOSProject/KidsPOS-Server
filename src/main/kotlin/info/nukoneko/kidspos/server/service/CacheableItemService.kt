package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class CacheableItemService(
    private val repository: ItemRepository
) {
    // シンプルなインメモリキャッシュ
    private val barcodeCache = ConcurrentHashMap<String, ItemEntity>(100)
    private val idCache = ConcurrentHashMap<Int, ItemEntity>(100)

    @Cacheable("items")
    fun findAll(): List<ItemEntity> {
        return repository.findAll().toList()
    }

    fun findItem(barcode: String): ItemEntity? {
        return barcodeCache[barcode] ?: run {
            val item = repository.findByBarcode(barcode).firstOrNull()
            if (item != null) {
                barcodeCache[barcode] = item
                idCache[item.id] = item
            }
            item
        }
    }

    fun findItem(id: Int): ItemEntity? {
        return idCache[id] ?: run {
            val item = repository.findById(id).orElse(null)
            if (item != null) {
                idCache[id] = item
                barcodeCache[item.barcode] = item
            }
            item
        }
    }
}
