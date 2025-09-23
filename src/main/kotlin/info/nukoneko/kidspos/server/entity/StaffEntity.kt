package info.nukoneko.kidspos.server.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * スタッフエンティティ
 *
 * 店舗スタッフ情報を表現するデータベースエンティティ
 * @property barcode スタッフのバーコードID
 * @property name スタッフ名
 */
@Entity
@Table(name = "staff")
data class StaffEntity(@Id var barcode: String, val name: String)