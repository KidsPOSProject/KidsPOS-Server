package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.domain.exception.InvalidFileException
import info.nukoneko.kidspos.server.entity.ApkVersionEntity
import info.nukoneko.kidspos.server.service.ApkManifestInfo
import info.nukoneko.kidspos.server.service.ApkVersionService
import info.nukoneko.kidspos.server.service.VersionOrderConflict
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@WebMvcTest(ApkController::class)
class ApkControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var apkVersionService: ApkVersionService

    private fun apkFile(
        name: String = "test.apk",
        contentType: String = "application/vnd.android.package-archive",
    ) = MockMultipartFile("file", name, contentType, ByteArray(100))

    @Test
    fun `GET index should expose detected version order conflicts`() {
        val mistyped = apkVersion(id = 1L, version = "1.0.10", versionCode = 100)
        val newest = apkVersion(id = 2L, version = "1.0.11", versionCode = 12)
        val conflicts = listOf(VersionOrderConflict(newerName = newest, higherCode = mistyped))
        val versions = listOf(mistyped, newest)

        whenever(apkVersionService.getAllVersions()).thenReturn(versions)
        whenever(apkVersionService.getLatestVersion()).thenReturn(mistyped)
        whenever(apkVersionService.detectVersionOrderConflicts(versions)).thenReturn(conflicts)

        mockMvc
            .perform(get("/apk"))
            .andExpect(status().isOk)
            .andExpect(view().name("apk/index"))
            .andExpect(model().attribute("versionOrderConflicts", conflicts))
            .andExpect(content().string(containsString("バージョン番号の並びが逆転しています")))
            .andExpect(content().string(containsString("1.0.11")))
    }

    @Test
    fun `GET index should expose an empty conflict list when versions are consistent`() {
        val versions = listOf(apkVersion(id = 1L, version = "1.0.11", versionCode = 12))

        whenever(apkVersionService.getAllVersions()).thenReturn(versions)
        whenever(apkVersionService.getLatestVersion()).thenReturn(versions.first())
        whenever(apkVersionService.detectVersionOrderConflicts(versions)).thenReturn(emptyList())

        mockMvc
            .perform(get("/apk"))
            .andExpect(status().isOk)
            .andExpect(model().attribute("versionOrderConflicts", emptyList<VersionOrderConflict>()))
            .andExpect(content().string(not(containsString("バージョン番号の並びが逆転しています"))))
    }

    private fun apkVersion(
        id: Long,
        version: String,
        versionCode: Int,
    ) = ApkVersionEntity(
        id = id,
        version = version,
        versionCode = versionCode,
        fileName = "kidspos-v$version.apk",
        fileSize = 1000L,
        filePath = "/uploads/apk/kidspos-v$version.apk",
        isActive = true,
        uploadedAt = LocalDateTime.now(),
    )

    @Test
    fun `POST analyze should return version information as JSON`() {
        whenever(apkVersionService.analyzeApk(any())).thenReturn(ApkManifestInfo("1.2.3", 10203))

        mockMvc
            .perform(multipart("/apk/analyze").file(apkFile()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value("1.2.3"))
            .andExpect(jsonPath("$.versionCode").value(10203))
    }

    @Test
    fun `POST analyze should return 400 with an error message when parsing fails`() {
        whenever(apkVersionService.analyzeApk(any()))
            .thenThrow(InvalidFileException("AndroidManifest.xml が見つかりません"))

        mockMvc
            .perform(multipart("/apk/analyze").file(apkFile("broken.apk")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("APK_ANALYZE_FAILED"))
            .andExpect(jsonPath("$.message").value("AndroidManifest.xml が見つかりません"))
    }

    @Test
    fun `POST upload should redirect to the list with a success message`() {
        val uploaded =
            ApkVersionEntity(
                id = 1L,
                version = "1.2.3",
                versionCode = 10203,
                fileName = "kidspos-v1.2.3.apk",
                fileSize = 100L,
                filePath = "/uploads/apk/kidspos-v1.2.3.apk",
                releaseNotes = "初回リリース",
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )
        whenever(apkVersionService.uploadApk(any(), eq("初回リリース"))).thenReturn(uploaded)

        mockMvc
            .perform(
                multipart("/apk/upload")
                    .file(apkFile())
                    .param("releaseNotes", "初回リリース"),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/apk"))
            .andExpect(flash().attributeExists("successMessage"))
    }

    @Test
    fun `POST upload should redirect back to the form with an error message on failure`() {
        whenever(apkVersionService.uploadApk(any(), eq(null)))
            .thenThrow(InvalidFileException("APKファイルのみアップロード可能です"))

        mockMvc
            .perform(multipart("/apk/upload").file(apkFile("test.txt", "text/plain")))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/apk/upload"))
            .andExpect(flash().attributeExists("errorMessage"))
    }

    @Test
    fun `POST upload should not require version parameters`() {
        val uploaded =
            ApkVersionEntity(
                id = 1L,
                version = "1.2.3",
                versionCode = 10203,
                fileName = "kidspos-v1.2.3.apk",
                fileSize = 100L,
                filePath = "/uploads/apk/kidspos-v1.2.3.apk",
                releaseNotes = null,
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )
        whenever(apkVersionService.uploadApk(any(), eq(null))).thenReturn(uploaded)

        mockMvc
            .perform(multipart("/apk/upload").file(apkFile()))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/apk"))

        verify(apkVersionService).uploadApk(any(), eq(null))
    }
}
