package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.SettingEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("DangerZonePasswordService")
class DangerZonePasswordServiceTest {
    @Mock
    private lateinit var settingService: SettingService

    @Mock
    private lateinit var passwordHasher: PasswordHasher

    private lateinit var service: DangerZonePasswordService

    private val storedSetting = SettingEntity(SettingService.KEY_DANGER_ZONE_PASSWORD, "stored-hash")

    @BeforeEach
    fun setUp() {
        service = DangerZonePasswordService(settingService, passwordHasher)
    }

    @Test
    fun `パスワード未設定なら未設定として扱う`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(null)

        assertFalse(service.isConfigured())
    }

    @Test
    fun `パスワード設定済みなら設定済みとして扱う`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(storedSetting)

        assertTrue(service.isConfigured())
    }

    @Test
    fun `未設定なら現在のパスワードなしで設定できる`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(null)
        whenever(passwordHasher.hash("newpass")).thenReturn("new-hash")

        val result = service.changePassword(null, "newpass")

        assertTrue(result.succeeded)
        assertTrue(result.configured)
        val captor = argumentCaptor<SettingEntity>()
        verify(settingService).saveSetting(captor.capture())
        assertEquals(SettingService.KEY_DANGER_ZONE_PASSWORD, captor.firstValue.key)
        assertEquals("new-hash", captor.firstValue.value)
    }

    @Test
    fun `設定済みなら現在のパスワードが一致したときだけ変更できる`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(storedSetting)
        whenever(passwordHasher.matches("oldpass", "stored-hash")).thenReturn(true)
        whenever(passwordHasher.hash("newpass")).thenReturn("new-hash")

        val result = service.changePassword("oldpass", "newpass")

        assertTrue(result.succeeded)
        verify(settingService).saveSetting(any())
    }

    @Test
    fun `現在のパスワードが違うと変更できない`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(storedSetting)
        whenever(passwordHasher.matches("wrong", "stored-hash")).thenReturn(false)

        val result = service.changePassword("wrong", "newpass")

        assertFalse(result.succeeded)
        assertTrue(result.configured)
        assertEquals("現在のパスワードが違います", result.message)
        verify(settingService, never()).saveSetting(any())
    }

    @Test
    fun `設定済みで現在のパスワードが空だと変更できない`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(storedSetting)
        whenever(passwordHasher.matches("", "stored-hash")).thenReturn(false)

        val result = service.changePassword(null, "newpass")

        assertFalse(result.succeeded)
        verify(settingService, never()).saveSetting(any())
    }

    @Test
    fun `短すぎるパスワードは保存しない`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(null)

        val result = service.changePassword(null, "abc")

        assertFalse(result.succeeded)
        assertFalse(result.configured)
        verify(settingService, never()).saveSetting(any())
        verify(passwordHasher, never()).hash(any())
    }

    @Test
    fun `長すぎるパスワードは保存しない`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(null)

        val result = service.changePassword(null, "a".repeat(DangerZonePasswordService.MAX_LENGTH + 1))

        assertFalse(result.succeeded)
        verify(settingService, never()).saveSetting(any())
    }

    @Test
    fun `境界の長さは保存できる`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(null)
        whenever(passwordHasher.hash(any())).thenReturn("new-hash")

        assertTrue(service.changePassword(null, "a".repeat(DangerZonePasswordService.MIN_LENGTH)).succeeded)
        assertTrue(service.changePassword(null, "a".repeat(DangerZonePasswordService.MAX_LENGTH)).succeeded)
    }

    @Test
    fun `現在のパスワードが一致すれば解除できる`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(storedSetting)
        whenever(passwordHasher.matches("oldpass", "stored-hash")).thenReturn(true)

        val result = service.clearPassword("oldpass")

        assertTrue(result.succeeded)
        assertFalse(result.configured)
        verify(settingService).deleteSetting(eq(SettingService.KEY_DANGER_ZONE_PASSWORD))
    }

    @Test
    fun `現在のパスワードが違うと解除できない`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(storedSetting)
        whenever(passwordHasher.matches("wrong", "stored-hash")).thenReturn(false)

        val result = service.clearPassword("wrong")

        assertFalse(result.succeeded)
        verify(settingService, never()).deleteSetting(any())
    }

    @Test
    fun `未設定なら解除は失敗する`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(null)

        val result = service.clearPassword("oldpass")

        assertFalse(result.succeeded)
        assertFalse(result.configured)
        assertEquals("パスワードは設定されていません", result.message)
        verify(settingService, never()).deleteSetting(any())
    }

    @Test
    fun `正しいパスワードなら照合に成功する`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(storedSetting)
        whenever(passwordHasher.matches("oldpass", "stored-hash")).thenReturn(true)

        val result = service.verify("oldpass")

        assertTrue(result.valid)
        assertTrue(result.configured)
    }

    @Test
    fun `誤ったパスワードなら照合に失敗する`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(storedSetting)
        whenever(passwordHasher.matches("wrong", "stored-hash")).thenReturn(false)

        val result = service.verify("wrong")

        assertFalse(result.valid)
        assertTrue(result.configured)
        assertEquals("パスワードが違います", result.message)
    }

    @Test
    fun `未設定なら照合は未設定として失敗する`() {
        whenever(settingService.findSetting(SettingService.KEY_DANGER_ZONE_PASSWORD)).thenReturn(null)

        val result = service.verify("anything")

        assertFalse(result.valid)
        assertFalse(result.configured)
        assertEquals("サーバーにパスワードが設定されていません", result.message)
    }
}
