package info.nukoneko.kidspos.server.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "store")
data class StoreEntity(@Id var id: Int = 0, val name: String, val printerUri: String)