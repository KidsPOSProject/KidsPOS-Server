package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.StaffEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * スタッフエンティティのリポジトリインターフェース
 *
 * スタッフ情報のCRUD操作を提供
 */
@Repository
interface StaffRepository : JpaRepository<StaffEntity, String>