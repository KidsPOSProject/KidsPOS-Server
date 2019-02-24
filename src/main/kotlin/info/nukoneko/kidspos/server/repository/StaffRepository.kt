package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.StaffEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface StaffRepository : JpaRepository<StaffEntity, Int> {
    @Query(value = "SELECT max(staff.id) FROM StaffEntity as staff")
    fun getLastId(): Int
}