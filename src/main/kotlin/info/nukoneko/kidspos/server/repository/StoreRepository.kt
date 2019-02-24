package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.StoreEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface StoreRepository : JpaRepository<StoreEntity, Int> {
    @Query(value = "SELECT max(store.id) FROM StoreEntity as store")
    fun getLastId(): Int
}