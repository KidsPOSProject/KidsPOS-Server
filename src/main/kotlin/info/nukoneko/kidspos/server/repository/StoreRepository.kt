package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.StoreEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 店舗エンティティのリポジトリインターフェース
 *
 * 店舗情報の永続化操作を提供
 */
@Repository
interface StoreRepository : JpaRepository<StoreEntity, Int>
