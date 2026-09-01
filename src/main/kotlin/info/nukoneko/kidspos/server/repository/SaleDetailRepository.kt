package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.SaleDetailEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 販売明細エンティティのリポジトリインターフェース
 *
 * 販売取引の明細情報の永続化操作を提供
 */
@Repository
interface SaleDetailRepository : JpaRepository<SaleDetailEntity, Int> {
    fun findBySaleId(saleId: Int): List<SaleDetailEntity>

    fun findBySaleIdIn(saleIds: Collection<Int>): List<SaleDetailEntity>
}
