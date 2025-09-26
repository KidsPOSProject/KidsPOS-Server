package info.nukoneko.kidspos.server.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 販売明細エンティティ
 *
 * 販売取引の明細情報を表現するデータベースエンティティ
 * @property id 明細ID
 * @property saleId 売上げID
 * @property itemId 商品ID
 * @property price 単価
 * @property quantity 数量
 */
@Entity
@Table(name = "sale_detail")
data class SaleDetailEntity(
    @Id var id: Int = 0,
    val saleId: Int, // 売り上げID
    val itemId: Int, // 商品ID
    val price: Int, // 単価
    val quantity: Int, // 数量
)
