package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.ItemEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * 商品エンティティのリポジトリインターフェース
 *
 * 商品の永続化操作とクエリメソッドを提供
 */
@Repository
interface ItemRepository : JpaRepository<ItemEntity, Int> {
    @Query(value = "SELECT max(item.id) FROM ItemEntity as item")
    fun getLastId(): Int

    fun findByBarcode(barcode: String): ItemEntity?

    /**
     * Optimized pagination support
     */
    override fun findAll(pageable: Pageable): Page<ItemEntity>

    /**
     * Optimized count query for items with price filter
     */
    fun countByPriceGreaterThan(price: Int): Long

    /**
     * Projection query for read-only operations
     */
    @Query("SELECT i.id as id, i.name as name, i.price as price FROM ItemEntity i")
    fun findAllItemSummaries(): List<ItemSummary>

    /**
     * Batch fetch multiple items by IDs
     */
    @Query("SELECT i FROM ItemEntity i WHERE i.id IN :ids ORDER BY i.id")
    fun findAllByIdsBatch(ids: List<Int>): List<ItemEntity>

    /**
     * Find items with price range (uses index)
     */
    @Query("SELECT i FROM ItemEntity i WHERE i.price BETWEEN :minPrice AND :maxPrice ORDER BY i.price")
    fun findByPriceRange(minPrice: Int, maxPrice: Int): List<ItemEntity>
}

/**
 * 商品サマリー用のProjectionインターフェース
 *
 * 読み取り専用操作のための軽量なデータ構造
 */
interface ItemSummary {
    val id: Int
    val name: String
    val price: Int
}