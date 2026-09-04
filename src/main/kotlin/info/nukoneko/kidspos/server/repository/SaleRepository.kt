package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.SaleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

/**
 * 販売エンティティのリポジトリインターフェース
 *
 * 販売取引データの永続化操作と最適化されたクエリを提供
 */
@Repository
interface SaleRepository : JpaRepository<SaleEntity, Int> {
    /**
     * Find sales by date range for reporting
     */
    @Query("SELECT s FROM SaleEntity s WHERE s.createdAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    fun findByDateRange(
        startDate: Date,
        endDate: Date,
    ): List<SaleEntity>
}
