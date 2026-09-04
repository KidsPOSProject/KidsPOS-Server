package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.ItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 商品エンティティのリポジトリインターフェース
 *
 * 商品の永続化操作とクエリメソッドを提供
 */
@Repository
interface ItemRepository : JpaRepository<ItemEntity, Int> {
    fun findByBarcode(barcode: String): ItemEntity?
}
