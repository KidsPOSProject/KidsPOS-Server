package info.nukoneko.kidspos.server.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "sale_detail")
data class SaleDetailEntity(
    @Id var id: Int = 0,
    val saleId: Int, // 売り上げID
    val itemId: Int, // 商品ID
    val price: Int, // 単価
    val quantity: Int // 数量
)
