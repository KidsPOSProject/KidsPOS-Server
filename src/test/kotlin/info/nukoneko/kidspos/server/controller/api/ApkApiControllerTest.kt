package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.response.ApkVersionResponse
import info.nukoneko.kidspos.server.controller.dto.response.UpdateCheckResponse
import info.nukoneko.kidspos.server.entity.ApkVersionEntity
import info.nukoneko.kidspos.server.service.ApkVersionService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@WebMvcTest(ApkApiController::class)
class ApkApiControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var apkVersionService: ApkVersionService

    @Test
    fun `GET version latest should return latest version when exists`() {
        // Given
        val latestVersion = ApkVersionEntity(
            id = 1L,
            version = "1.0.0",
            versionCode = 100,
            fileName = "kidspos-v1.0.0.apk",
            fileSize = 1000000L,
            filePath = "/uploads/apk/kidspos-v1.0.0.apk",
            releaseNotes = "Initial release",
            isActive = true,
            uploadedAt = LocalDateTime.now(),
        )
        whenever(apkVersionService.getLatestVersion()).thenReturn(latestVersion)

        // When & Then
        mockMvc.perform(get("/api/apk/version/latest"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value("1.0.0"))
            .andExpect(jsonPath("$.versionCode").value(100))
            .andExpect(jsonPath("$.fileName").value("kidspos-v1.0.0.apk"))
            .andExpect(jsonPath("$.downloadUrl").value("/api/apk/download/1"))
    }

    @Test
    fun `GET version latest should return 404 when no version exists`() {
        // Given
        whenever(apkVersionService.getLatestVersion()).thenReturn(null)

        // When & Then
        mockMvc.perform(get("/api/apk/version/latest"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET version check should return update available when newer version exists`() {
        // Given
        val newerVersion = ApkVersionEntity(
            id = 2L,
            version = "2.0.0",
            versionCode = 200,
            fileName = "kidspos-v2.0.0.apk",
            fileSize = 2000000L,
            filePath = "/uploads/apk/kidspos-v2.0.0.apk",
            releaseNotes = "Major update",
            isActive = true,
            uploadedAt = LocalDateTime.now(),
        )
        whenever(apkVersionService.checkForUpdate(100)).thenReturn(newerVersion)

        // When & Then
        mockMvc.perform(get("/api/apk/version/check")
                .param("currentVersionCode", "100"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasUpdate").value(true))
            .andExpect(jsonPath("$.latestVersion.version").value("2.0.0"))
            .andExpect(jsonPath("$.latestVersion.versionCode").value(200))
    }

    @Test
    fun `GET version check should return no update when no newer version exists`() {
        // Given
        whenever(apkVersionService.checkForUpdate(200)).thenReturn(null)

        // When & Then
        mockMvc.perform(get("/api/apk/version/check")
                .param("currentVersionCode", "200"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasUpdate").value(false))
            .andExpect(jsonPath("$.latestVersion").doesNotExist())
    }

    @Test
    fun `GET version all should return all versions`() {
        // Given
        val versions = listOf(
            ApkVersionEntity(
                id = 2L,
                version = "2.0.0",
                versionCode = 200,
                fileName = "kidspos-v2.0.0.apk",
                fileSize = 2000000L,
                filePath = "/uploads/apk/kidspos-v2.0.0.apk",
                releaseNotes = "Major update",
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            ),
            ApkVersionEntity(
                id = 1L,
                version = "1.0.0",
                versionCode = 100,
                fileName = "kidspos-v1.0.0.apk",
                fileSize = 1000000L,
                filePath = "/uploads/apk/kidspos-v1.0.0.apk",
                releaseNotes = "Initial release",
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            ),
        )
        whenever(apkVersionService.getAllVersions()).thenReturn(versions)

        // When & Then
        mockMvc.perform(get("/api/apk/version/all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].version").value("2.0.0"))
            .andExpect(jsonPath("$[0].versionCode").value(200))
            .andExpect(jsonPath("$[1].version").value("1.0.0"))
            .andExpect(jsonPath("$[1].versionCode").value(100))
    }

    @Test
    fun `POST upload should upload APK successfully`() {
        // Given
        val mockFile = MockMultipartFile(
            "file",
            "test.apk",
            "application/vnd.android.package-archive",
            ByteArray(1000),
        )
        val uploadedVersion = ApkVersionEntity(
            id = 1L,
            version = "1.0.0",
            versionCode = 100,
            fileName = "kidspos-v1.0.0.apk",
            fileSize = 1000L,
            filePath = "/uploads/apk/kidspos-v1.0.0.apk",
            releaseNotes = "Initial release",
            isActive = true,
            uploadedAt = LocalDateTime.now(),
        )

        whenever(apkVersionService.uploadApk(any(), eq("1.0.0"), eq(100), eq("Initial release")))
            .thenReturn(uploadedVersion)

        // When & Then
        mockMvc.perform(
            multipart("/api/apk/upload")
                .file(mockFile)
                .param("version", "1.0.0")
                .param("versionCode", "100")
                .param("releaseNotes", "Initial release"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.version").value("1.0.0"))
            .andExpect(jsonPath("$.versionCode").value(100))
    }

    @Test
    fun `DELETE version should delete version successfully`() {
        // Given
        doNothing().whenever(apkVersionService).deleteVersion(1L)

        // When & Then
        mockMvc.perform(delete("/api/apk/version/1"))
            .andExpect(status().isNoContent)

        verify(apkVersionService).deleteVersion(1L)
    }

    @Test
    fun `PUT version deactivate should deactivate version successfully`() {
        // Given
        val deactivatedVersion = ApkVersionEntity(
            id = 1L,
            version = "1.0.0",
            versionCode = 100,
            fileName = "kidspos-v1.0.0.apk",
            fileSize = 1000000L,
            filePath = "/uploads/apk/kidspos-v1.0.0.apk",
            releaseNotes = "Initial release",
            isActive = false,
            uploadedAt = LocalDateTime.now(),
        )
        whenever(apkVersionService.deactivateVersion(1L)).thenReturn(deactivatedVersion)

        // When & Then
        mockMvc.perform(put("/api/apk/version/1/deactivate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value("1.0.0"))
            .andExpect(jsonPath("$.isActive").value(false))
    }
}