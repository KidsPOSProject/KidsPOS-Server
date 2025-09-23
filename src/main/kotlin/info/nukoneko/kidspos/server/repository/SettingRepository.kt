package info.nukoneko.kidspos.server.repository

import info.nukoneko.kidspos.server.entity.SettingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 設定エンティティのリポジトリインターフェース
 *
 * アプリケーション設定情報の永続化操作を提供
 */
@Repository
interface SettingRepository : JpaRepository<SettingEntity, String>