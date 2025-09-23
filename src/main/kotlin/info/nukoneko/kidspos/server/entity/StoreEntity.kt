package info.nukoneko.kidspos.server.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

/**
 * 店舗エンティティ
 *
 * 店舗情報を表現するデータベースエンティティ
 * @property id 店舗ID
 * @property name 店舗名
 * @property printerUri プリンターURI
 */
@Entity
@Table(name = "store")
data class StoreEntity(
    @Id var id: Int = 0,

    @field:NotBlank(message = "Store name is required")
    val name: String,

    @field:NotBlank(message = "Printer URI is required")
    val printerUri: String
)
