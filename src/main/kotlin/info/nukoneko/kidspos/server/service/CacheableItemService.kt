package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.repository.ItemRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * 商品情報にキャッシュ機構を追加したサービスクラス
 * 頻繁にアクセスされる商品データをメモリ上にキャッシュしておくことで
 * データベースアクセスを削減し、パフォーマンスとメモリ効率を向上させる
 */
@Service
class CacheableItemService {

    @Autowired
    private lateinit var repository: ItemRepository
    
    // 高頻度で使用される商品のメモリキャッシュ（最大100件に制限）
    private val cache = ConcurrentHashMap<String, ItemEntity>(100)
    
    // キャッシュヒット数と総リクエスト数を記録
    private var cacheHits = 0
    private var totalRequests = 0
    
    fun findItem(barcode: String): ItemEntity? {
        totalRequests++
        
        // キャッシュをチェック
        return cache[barcode]?.also {
            cacheHits++
        } ?: run {
            // キャッシュにない場合はリポジトリから取得
            val item = repository.findByBarcode(barcode)
            
            // 結果をキャッシュに保存（キャッシュが大きすぎる場合は追加しない）
            if (item != null && cache.size < 100) {
                cache[barcode] = item
            }
            
            item
        }
    }
    
    fun findAll(): List<ItemEntity> {
        return repository.findAll()
    }
    
    // キャッシュの統計情報を取得するメソッド
    fun getCacheStats(): Map<String, Any> {
        val hitRate = if (totalRequests > 0) cacheHits.toDouble() / totalRequests else 0.0
        return mapOf(
            "cacheSize" to cache.size,
            "cacheHits" to cacheHits,
            "totalRequests" to totalRequests,
            "hitRate" to hitRate
        )
    }
}