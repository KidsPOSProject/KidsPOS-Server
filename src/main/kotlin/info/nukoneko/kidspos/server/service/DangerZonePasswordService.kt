package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.SettingEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 危険操作（Danger Zone）を保護するパスワードを管理するサービス
 *
 * パスワードはハッシュ化して設定テーブルに保存し、平文では保持しない。
 */
@Service
class DangerZonePasswordService(
    private val settingService: SettingService,
    private val passwordHasher: PasswordHasher,
) {
    private val logger = LoggerFactory.getLogger(DangerZonePasswordService::class.java)

    fun isConfigured(): Boolean = settingService.findSetting(KEY_PASSWORD) != null

    fun changePassword(
        currentPassword: String?,
        newPassword: String,
    ): ChangeResult {
        val stored = settingService.findSetting(KEY_PASSWORD)
        if (stored != null && !passwordHasher.matches(currentPassword.orEmpty(), stored.value)) {
            logger.warn("Danger zone password change rejected: current password mismatch")
            return ChangeResult(false, "現在のパスワードが違います", configured = true)
        }
        if (newPassword.length !in MIN_LENGTH..MAX_LENGTH) {
            return ChangeResult(
                false,
                "パスワードは${MIN_LENGTH}文字以上${MAX_LENGTH}文字以内で設定してください",
                configured = stored != null,
            )
        }
        settingService.saveSetting(SettingEntity(KEY_PASSWORD, passwordHasher.hash(newPassword)))
        logger.info("Danger zone password saved")
        return ChangeResult(true, "パスワードを保存しました", configured = true)
    }

    fun clearPassword(currentPassword: String): ChangeResult {
        val stored =
            settingService.findSetting(KEY_PASSWORD)
                ?: return ChangeResult(false, "パスワードは設定されていません", configured = false)
        if (!passwordHasher.matches(currentPassword, stored.value)) {
            logger.warn("Danger zone password clear rejected: current password mismatch")
            return ChangeResult(false, "現在のパスワードが違います", configured = true)
        }
        settingService.deleteSetting(KEY_PASSWORD)
        logger.info("Danger zone password cleared")
        return ChangeResult(true, "パスワードを解除しました", configured = false)
    }

    fun verify(password: String): VerifyResult {
        val stored =
            settingService.findSetting(KEY_PASSWORD)
                ?: return VerifyResult(false, configured = false, message = "サーバーにパスワードが設定されていません")
        return if (passwordHasher.matches(password, stored.value)) {
            VerifyResult(true, configured = true, message = "認証しました")
        } else {
            logger.warn("Danger zone password verification failed")
            VerifyResult(false, configured = true, message = "パスワードが違います")
        }
    }

    data class ChangeResult(
        val succeeded: Boolean,
        val message: String,
        val configured: Boolean,
    )

    data class VerifyResult(
        val valid: Boolean,
        val configured: Boolean,
        val message: String,
    )

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 128

        private const val KEY_PASSWORD = SettingService.KEY_DANGER_ZONE_PASSWORD
    }
}
