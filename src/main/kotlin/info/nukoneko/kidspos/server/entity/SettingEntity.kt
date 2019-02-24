package info.nukoneko.kidspos.server.entity

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "setting")
data class SettingEntity(@Id val key: String, val value: String)