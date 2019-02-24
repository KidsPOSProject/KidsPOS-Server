package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.SettingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SettingRepository : JpaRepository<SettingEntity, String>