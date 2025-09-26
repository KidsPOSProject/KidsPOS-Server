package info.nukoneko.kidspos.server.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

/**
 * スタッフエンティティ
 *
 * 店舗スタッフ情報を表現するデータベースエンティティ
 * @property barcode スタッフのバーコードID
 * @property name スタッフ名
 */
@Entity
@Table(name = "staff")
data class StaffEntity(
    @Id
    @field:NotBlank(message = "Barcode is required")
    var barcode: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
)
