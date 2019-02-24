package info.nukoneko.kidspos.server.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "staff")
data class StaffEntity(@Id var id: Int = 0, var barcode: String, val name: String)