package info.nukoneko.kidspos.server.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "item")
data class ItemEntity(
    @Id var id: Int = 0,
    val barcode: String,
    val name: String = "",
    val price: Int = 0
)
