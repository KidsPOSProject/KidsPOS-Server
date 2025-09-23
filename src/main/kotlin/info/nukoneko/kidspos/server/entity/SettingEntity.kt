package info.nukoneko.kidspos.server.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 設定エンティティ
 *
 * アプリケーション設定情報を表現するデータベースエンティティ
 * @property key 設定キー
 * @property value 設定値
 */
@Entity
@Table(name = "setting")
data class SettingEntity(@Id val key: String, val value: String)