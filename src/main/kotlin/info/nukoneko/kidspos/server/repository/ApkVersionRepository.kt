package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.ApkVersionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ApkVersionRepository : JpaRepository<ApkVersionEntity, Long> {
    fun findByIsActiveTrueOrderByVersionCodeDesc(): List<ApkVersionEntity>

    fun findAllByOrderByVersionCodeDesc(): List<ApkVersionEntity>

    fun findTopByIsActiveTrueOrderByVersionCodeDesc(): Optional<ApkVersionEntity>

    fun findByVersion(version: String): Optional<ApkVersionEntity>

    @Query("SELECT a FROM ApkVersionEntity a WHERE a.isActive = true AND a.versionCode > :currentVersionCode ORDER BY a.versionCode DESC")
    fun findNewerVersions(currentVersionCode: Int): List<ApkVersionEntity>

    fun existsByVersionCode(versionCode: Int): Boolean

    @Query("SELECT MAX(a.id) FROM ApkVersionEntity a")
    fun findMaxId(): Long?

    fun existsByVersion(version: String): Boolean
}
