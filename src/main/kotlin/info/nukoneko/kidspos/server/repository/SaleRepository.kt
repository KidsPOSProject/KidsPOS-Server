package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.SaleEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    @Query(value = "SELECT max(sale.id) FROM SaleEntity as sale")
    fun getLastId(): Int

    /**
     * Find sale with details to avoid N+1 problem
     */
    @Query("SELECT DISTINCT s FROM SaleEntity s WHERE s.id = :id")
    fun findByIdWithDetails(id: Int): SaleEntity?

    /**
     * Find sales by store with pagination
     */
    fun findByStoreId(storeId: Int, pageable: Pageable): Page<SaleEntity>

    /**
     * Find sales by date range for reporting
     */
    @Query("SELECT s FROM SaleEntity s WHERE s.createdAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    fun findByDateRange(startDate: Date, endDate: Date): List<SaleEntity>

    /**
     * Sales summary projection for dashboard
     */
    @Query(
        """
        SELECT s.storeId as storeId,
               COUNT(s.id) as totalSales,
               SUM(s.amount) as totalAmount,
               AVG(s.amount) as averageAmount
        FROM SaleEntity s
        WHERE s.createdAt >= :fromDate
        GROUP BY s.storeId
    """
    )
    fun findSalesSummaryByStore(fromDate: Date): List<SalesSummary>

    /**
     * Count sales by store
     */
    fun countByStoreId(storeId: Int): Long
}

/**
 * Projection for sales summary reports
 */
interface SalesSummary {
    val storeId: Int
    val totalSales: Long
    val totalAmount: Long
    val averageAmount: Double
}