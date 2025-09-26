package info.nukoneko.kidspos.server.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.*

/**
 * 販売エンティティ
 *
 * 販売取引情報を表現するデータベースエンティティ
 * @property id 売上げID
 * @property storeId 店舗ID
 * @property staffId スタッフID
 * @property quantity 数量
 * @property amount 売上げ金額
 * @property deposit 預かり金
 * @property createdAt 作成日時
 */
@Entity
@Table(name = "sale")
data class SaleEntity(
    @Id var id: Int = 0, // 売り上げID
    val storeId: Int, // 店舗ID
    val staffId: Int, // スタッフID
    val quantity: Int, // 数量
    val amount: Int, // 売り上げ
    val deposit: Int,
    val createdAt: Date,
)
