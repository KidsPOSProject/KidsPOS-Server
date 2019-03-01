package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.ItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ItemRepository : JpaRepository<ItemEntity, Int> {
    @Query(value = "SELECT max(item.id) FROM ItemEntity as item")
    fun getLastId(): Int

    fun findByBarcode(barcode: String): ItemEntity?
}