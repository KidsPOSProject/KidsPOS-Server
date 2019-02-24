package info.nukoneko.kidspos.server.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "sale")
data class SaleEntity(
        @Id var id: Int = 0, // 売り上げID
        val storeId: Int, // 店舗ID
        val staffId: Int, // スタッフID
        val quantity: Int, // 数量
        val amount: Int // 売り上げ
)