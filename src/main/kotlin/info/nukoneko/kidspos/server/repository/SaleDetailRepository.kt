package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SaleDetailRepository : JpaRepository<SaleDetailEntity, Int> {
    @Query(value = "SELECT max(sale_detail.id) FROM SaleDetailEntity as sale_detail")
    fun getLastId(): Int
}