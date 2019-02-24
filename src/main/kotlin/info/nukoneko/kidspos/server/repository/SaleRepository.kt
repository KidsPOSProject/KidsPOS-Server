package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.SaleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SaleRepository : JpaRepository<SaleEntity, Int> {
    @Query(value = "SELECT max(sale.id) FROM SaleEntity as sale")
    fun getLastId(): Int
}