package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.domain.exception.DuplicateResourceException
import info.nukoneko.kidspos.server.domain.exception.InvalidFileException
import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.ApkVersionEntity
import info.nukoneko.kidspos.server.service.ApkManifestInfo
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
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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
        val latestVersion =
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
            )
        whenever(apkVersionService.getLatestVersion()).thenReturn(latestVersion)

        // When & Then
        mockMvc
            .perform(get("/api/apk/version/latest"))
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
        mockMvc
            .perform(get("/api/apk/version/latest"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET version check should return update available when newer version exists`() {
        // Given
        val newerVersion =
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
            )
        whenever(apkVersionService.checkForUpdate(100)).thenReturn(newerVersion)

        // When & Then
        mockMvc
            .perform(
                get("/api/apk/version/check")
                    .param("currentVersionCode", "100"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.hasUpdate").value(true))
            .andExpect(jsonPath("$.latestVersion.version").value("2.0.0"))
            .andExpect(jsonPath("$.latestVersion.versionCode").value(200))
    }

    @Test
    fun `GET version check should return no update when no newer version exists`() {
        // Given
        whenever(apkVersionService.checkForUpdate(200)).thenReturn(null)

        // When & Then
        mockMvc
            .perform(
                get("/api/apk/version/check")
                    .param("currentVersionCode", "200"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.hasUpdate").value(false))
            .andExpect(jsonPath("$.latestVersion").doesNotExist())
    }

    @Test
    fun `GET version all should return all versions`() {
        // Given
        val versions =
            listOf(
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
        mockMvc
            .perform(get("/api/apk/version/all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].version").value("2.0.0"))
            .andExpect(jsonPath("$[0].versionCode").value(200))
            .andExpect(jsonPath("$[1].version").value("1.0.0"))
            .andExpect(jsonPath("$[1].versionCode").value(100))
    }

    @Test
    fun `POST upload should upload APK successfully`() {
        // Given
        val mockFile =
            MockMultipartFile(
                "file",
                "test.apk",
                "application/vnd.android.package-archive",
                ByteArray(1000),
            )
        val uploadedVersion =
            ApkVersionEntity(
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

        whenever(apkVersionService.uploadApk(any(), eq("Initial release")))
            .thenReturn(uploadedVersion)

        // When & Then
        mockMvc
            .perform(
                multipart("/api/apk/upload")
                    .file(mockFile)
                    .param("releaseNotes", "Initial release"),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.version").value("1.0.0"))
            .andExpect(jsonPath("$.versionCode").value(100))

        verify(apkVersionService, never()).analyzeApk(any())
    }

    @Test
    fun `POST upload should accept version parameters that match the APK`() {
        // Given
        val mockFile =
            MockMultipartFile(
                "file",
                "test.apk",
                "application/vnd.android.package-archive",
                ByteArray(1000),
            )
        val uploadedVersion =
            ApkVersionEntity(
                id = 1L,
                version = "1.0.0",
                versionCode = 100,
                fileName = "kidspos-v1.0.0.apk",
                fileSize = 1000L,
                filePath = "/uploads/apk/kidspos-v1.0.0.apk",
                releaseNotes = null,
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )

        whenever(apkVersionService.analyzeApk(any())).thenReturn(ApkManifestInfo("1.0.0", 100))
        whenever(apkVersionService.uploadApk(any(), eq(null))).thenReturn(uploadedVersion)

        // When & Then
        mockMvc
            .perform(
                multipart("/api/apk/upload")
                    .file(mockFile)
                    .param("version", "1.0.0")
                    .param("versionCode", "100"),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.version").value("1.0.0"))
    }

    @Test
    fun `POST upload should return 400 when version parameter differs from the APK`() {
        // Given
        val mockFile =
            MockMultipartFile(
                "file",
                "test.apk",
                "application/vnd.android.package-archive",
                ByteArray(1000),
            )
        whenever(apkVersionService.analyzeApk(any())).thenReturn(ApkManifestInfo("1.0.0", 100))

        // When & Then
        mockMvc
            .perform(
                multipart("/api/apk/upload")
                    .file(mockFile)
                    .param("version", "9.9.9"),
            ).andExpect(status().isBadRequest)

        verify(apkVersionService, never()).uploadApk(any(), any())
    }

    @Test
    fun `POST upload should return 400 when version code parameter differs from the APK`() {
        // Given
        val mockFile =
            MockMultipartFile(
                "file",
                "test.apk",
                "application/vnd.android.package-archive",
                ByteArray(1000),
            )
        whenever(apkVersionService.analyzeApk(any())).thenReturn(ApkManifestInfo("1.0.0", 100))

        // When & Then
        mockMvc
            .perform(
                multipart("/api/apk/upload")
                    .file(mockFile)
                    .param("versionCode", "999"),
            ).andExpect(status().isBadRequest)

        verify(apkVersionService, never()).uploadApk(any(), any())
    }

    @Test
    fun `POST analyze should return version information from the APK`() {
        // Given
        val mockFile =
            MockMultipartFile(
                "file",
                "test.apk",
                "application/vnd.android.package-archive",
                ByteArray(1000),
            )
        whenever(apkVersionService.analyzeApk(any())).thenReturn(ApkManifestInfo("1.2.3", 10203))

        // When & Then
        mockMvc
            .perform(multipart("/api/apk/analyze").file(mockFile))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value("1.2.3"))
            .andExpect(jsonPath("$.versionCode").value(10203))
    }

    @Test
    fun `POST analyze should return 400 when the APK cannot be parsed`() {
        // Given
        val mockFile =
            MockMultipartFile(
                "file",
                "broken.apk",
                "application/vnd.android.package-archive",
                ByteArray(10),
            )
        whenever(apkVersionService.analyzeApk(any()))
            .thenThrow(InvalidFileException("AndroidManifest.xml が見つかりません"))

        // When & Then
        mockMvc
            .perform(multipart("/api/apk/analyze").file(mockFile))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_FILE"))
    }

    @Test
    fun `DELETE version should delete version successfully`() {
        // Given
        doNothing().whenever(apkVersionService).deleteVersion(1L)

        // When & Then
        mockMvc
            .perform(delete("/api/apk/version/1"))
            .andExpect(status().isNoContent)

        verify(apkVersionService).deleteVersion(1L)
    }

    @Test
    fun `PUT version deactivate should deactivate version successfully`() {
        // Given
        val deactivatedVersion =
            ApkVersionEntity(
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
        mockMvc
            .perform(put("/api/apk/version/1/deactivate"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value("1.0.0"))
            .andExpect(jsonPath("$.isActive").value(false))
    }

    @Test
    fun `GET download should return APK with safely encoded filename`() {
        // Given
        val apkFile = File.createTempFile("kidspos-test", ".apk").apply { deleteOnExit() }
        Files.write(apkFile.toPath(), byteArrayOf(0x50, 0x4B, 0x03, 0x04))
        val trickyFileName = "kidspos-v1.0\"; evil=攻撃.apk"
        val apkVersion =
            ApkVersionEntity(
                id = 1L,
                version = "1.0.0",
                versionCode = 100,
                fileName = trickyFileName,
                fileSize = apkFile.length(),
                filePath = apkFile.absolutePath,
                releaseNotes = null,
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )
        whenever(apkVersionService.getApkFile(1L)).thenReturn(apkFile)
        whenever(apkVersionService.getVersionById(1L)).thenReturn(apkVersion)

        val expectedDisposition =
            ContentDisposition
                .attachment()
                .filename(trickyFileName, StandardCharsets.UTF_8)
                .build()
                .toString()

        // When & Then
        mockMvc
            .perform(get("/api/apk/download/1"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, expectedDisposition))
            .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, apkFile.length().toString()))
    }

    @Test
    fun `GET download should return 404 when version does not exist`() {
        // Given
        whenever(apkVersionService.getApkFile(99L))
            .thenThrow(ResourceNotFoundException("APKバージョンが見つかりません: ID=99"))

        // When & Then
        mockMvc
            .perform(get("/api/apk/download/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    }

    @Test
    fun `GET download should return 500 when file read fails unexpectedly`() {
        // Given
        whenever(apkVersionService.getApkFile(1L))
            .thenThrow(RuntimeException("disk read error"))

        // When & Then
        mockMvc
            .perform(get("/api/apk/download/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
    }

    @Test
    fun `POST upload should return 409 when version already exists`() {
        // Given
        val mockFile =
            MockMultipartFile(
                "file",
                "test.apk",
                "application/vnd.android.package-archive",
                ByteArray(1000),
            )
        whenever(apkVersionService.uploadApk(any(), eq(null)))
            .thenThrow(DuplicateResourceException("バージョン 1.0.0 は既に存在します"))

        // When & Then
        mockMvc
            .perform(
                multipart("/api/apk/upload")
                    .file(mockFile),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
    }

    @Test
    fun `POST upload should return 400 when file is invalid`() {
        // Given
        val mockFile =
            MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                ByteArray(10),
            )
        whenever(apkVersionService.uploadApk(any(), eq(null)))
            .thenThrow(InvalidFileException("APKファイルのみアップロード可能です"))

        // When & Then
        mockMvc
            .perform(
                multipart("/api/apk/upload")
                    .file(mockFile),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_FILE"))
    }

    @Test
    fun `DELETE version should return 404 when version does not exist`() {
        // Given
        whenever(apkVersionService.deleteVersion(99L))
            .thenThrow(ResourceNotFoundException("APKバージョンが見つかりません: ID=99"))

        // When & Then
        mockMvc
            .perform(delete("/api/apk/version/99"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    }

    @Test
    fun `PUT version deactivate should return 404 when version does not exist`() {
        // Given
        whenever(apkVersionService.deactivateVersion(99L))
            .thenThrow(ResourceNotFoundException("APKバージョンが見つかりません: ID=99"))

        // When & Then
        mockMvc
            .perform(put("/api/apk/version/99/deactivate"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
    }
}
