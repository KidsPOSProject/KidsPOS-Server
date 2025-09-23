package info.nukoneko.kidspos.server.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 商品エンティティ
 *
 * 商品情報を表現するデータベースエンティティ
 * @property id 商品ID
 * @property barcode バーコード
 * @property name 商品名
 * @property price 価格
 */
@Entity
@Table(name = "item")
data class ItemEntity(
    @Id var id: Int = 0,
    val barcode: String,
    val name: String = "",
    val price: Int = 0
)
