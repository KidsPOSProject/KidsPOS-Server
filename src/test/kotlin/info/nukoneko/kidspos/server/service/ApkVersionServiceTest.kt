package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.domain.exception.DuplicateResourceException
import info.nukoneko.kidspos.server.domain.exception.InvalidFileException
import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.ApkVersionEntity
import info.nukoneko.kidspos.server.repository.ApkVersionRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ApkVersionServiceTest {
    @Mock
    private lateinit var apkVersionRepository: ApkVersionRepository

    @Mock
    private lateinit var multipartFile: MultipartFile

    @InjectMocks
    private lateinit var apkVersionService: ApkVersionService

    private val testUploadDir = "./test-uploads/apk"
    private val maxFileSize = 104857600L

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(apkVersionService, "uploadDir", testUploadDir)
        ReflectionTestUtils.setField(apkVersionService, "maxFileSize", maxFileSize)

        // テスト用ディレクトリをクリーンアップ
        val testDir = Paths.get(testUploadDir)
        if (Files.exists(testDir)) {
            Files
                .walk(testDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `uploadApk should successfully upload valid APK file`() {
        // Given
        val version = "1.0.0"
        val versionCode = 100
        val releaseNotes = "Initial release"
        val fileContent = ByteArray(1000)

        whenever(multipartFile.isEmpty).thenReturn(false)
        whenever(multipartFile.size).thenReturn(1000L)
        whenever(multipartFile.contentType).thenReturn("application/vnd.android.package-archive")
        whenever(multipartFile.originalFilename).thenReturn("test.apk")
        whenever(multipartFile.inputStream).thenReturn(fileContent.inputStream())

        whenever(apkVersionRepository.existsByVersion(version)).thenReturn(false)
        whenever(apkVersionRepository.existsByVersionCode(versionCode)).thenReturn(false)

        val savedEntity =
            ApkVersionEntity(
                id = 1L,
                version = version,
                versionCode = versionCode,
                fileName = "kidspos-v$version.apk",
                fileSize = 1000L,
                filePath = "$testUploadDir/kidspos-v$version.apk",
                releaseNotes = releaseNotes,
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )
        whenever(apkVersionRepository.save(any<ApkVersionEntity>())).thenReturn(savedEntity)

        // When
        val result = apkVersionService.uploadApk(multipartFile, version, versionCode, releaseNotes)

        // Then
        assertNotNull(result)
        assertEquals(version, result.version)
        assertEquals(versionCode, result.versionCode)
        assertEquals(releaseNotes, result.releaseNotes)
        verify(apkVersionRepository).save(any())
    }

    @Test
    fun `uploadApk should throw exception when file is empty`() {
        // Given
        whenever(multipartFile.isEmpty).thenReturn(true)

        // When & Then
        assertThrows<InvalidFileException> {
            apkVersionService.uploadApk(multipartFile, "1.0.0", 100, null)
        }
    }

    @Test
    fun `uploadApk should throw exception when file size exceeds limit`() {
        // Given
        whenever(multipartFile.isEmpty).thenReturn(false)
        whenever(multipartFile.size).thenReturn(maxFileSize + 1)

        // When & Then
        assertThrows<InvalidFileException> {
            apkVersionService.uploadApk(multipartFile, "1.0.0", 100, null)
        }
    }

    @Test
    fun `uploadApk should throw exception when version already exists`() {
        // Given
        val version = "1.0.0"
        whenever(multipartFile.isEmpty).thenReturn(false)
        whenever(multipartFile.size).thenReturn(1000L)
        whenever(multipartFile.contentType).thenReturn("application/vnd.android.package-archive")
        whenever(apkVersionRepository.existsByVersion(version)).thenReturn(true)

        // When & Then
        assertThrows<DuplicateResourceException> {
            apkVersionService.uploadApk(multipartFile, version, 100, null)
        }
    }

    @Test
    fun `getLatestVersion should return latest version when exists`() {
        // Given
        val latestVersion =
            ApkVersionEntity(
                id = 1L,
                version = "1.0.0",
                versionCode = 100,
                fileName = "kidspos-v1.0.0.apk",
                fileSize = 1000L,
                filePath = "$testUploadDir/kidspos-v1.0.0.apk",
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )
        whenever(apkVersionRepository.findTopByIsActiveTrueOrderByVersionCodeDesc())
            .thenReturn(Optional.of(latestVersion))

        // When
        val result = apkVersionService.getLatestVersion()

        // Then
        assertNotNull(result)
        assertEquals(latestVersion.version, result?.version)
        assertEquals(latestVersion.versionCode, result?.versionCode)
    }

    @Test
    fun `getLatestVersion should return null when no version exists`() {
        // Given
        whenever(apkVersionRepository.findTopByIsActiveTrueOrderByVersionCodeDesc())
            .thenReturn(Optional.empty())

        // When
        val result = apkVersionService.getLatestVersion()

        // Then
        assertNull(result)
    }

    @Test
    fun `checkForUpdate should return newer version when available`() {
        // Given
        val currentVersionCode = 100
        val newerVersion =
            ApkVersionEntity(
                id = 2L,
                version = "2.0.0",
                versionCode = 200,
                fileName = "kidspos-v2.0.0.apk",
                fileSize = 2000L,
                filePath = "$testUploadDir/kidspos-v2.0.0.apk",
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )
        whenever(apkVersionRepository.findNewerVersions(currentVersionCode))
            .thenReturn(listOf(newerVersion))

        // When
        val result = apkVersionService.checkForUpdate(currentVersionCode)

        // Then
        assertNotNull(result)
        assertEquals(newerVersion.version, result?.version)
        assertEquals(newerVersion.versionCode, result?.versionCode)
    }

    @Test
    fun `checkForUpdate should return null when no newer version available`() {
        // Given
        val currentVersionCode = 200
        whenever(apkVersionRepository.findNewerVersions(currentVersionCode))
            .thenReturn(emptyList())

        // When
        val result = apkVersionService.checkForUpdate(currentVersionCode)

        // Then
        assertNull(result)
    }

    @Test
    fun `getVersionById should return version when exists`() {
        // Given
        val id = 1L
        val apkVersion =
            ApkVersionEntity(
                id = id,
                version = "1.0.0",
                versionCode = 100,
                fileName = "kidspos-v1.0.0.apk",
                fileSize = 1000L,
                filePath = "$testUploadDir/kidspos-v1.0.0.apk",
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )
        whenever(apkVersionRepository.findById(id))
            .thenReturn(Optional.of(apkVersion))

        // When
        val result = apkVersionService.getVersionById(id)

        // Then
        assertNotNull(result)
        assertEquals(apkVersion.version, result.version)
        assertEquals(apkVersion.versionCode, result.versionCode)
    }

    @Test
    fun `getVersionById should throw exception when version not found`() {
        // Given
        val id = 999L
        whenever(apkVersionRepository.findById(id))
            .thenReturn(Optional.empty())

        // When & Then
        assertThrows<ResourceNotFoundException> {
            apkVersionService.getVersionById(id)
        }
    }

    @Test
    fun `deactivateVersion should deactivate existing version`() {
        // Given
        val id = 1L
        val apkVersion =
            ApkVersionEntity(
                id = id,
                version = "1.0.0",
                versionCode = 100,
                fileName = "kidspos-v1.0.0.apk",
                fileSize = 1000L,
                filePath = "$testUploadDir/kidspos-v1.0.0.apk",
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )
        val deactivatedVersion = apkVersion.copy(isActive = false)

        whenever(apkVersionRepository.findById(id))
            .thenReturn(Optional.of(apkVersion))
        whenever(apkVersionRepository.save(any<ApkVersionEntity>()))
            .thenReturn(deactivatedVersion)

        // When
        val result = apkVersionService.deactivateVersion(id)

        // Then
        assertNotNull(result)
        assertFalse(result.isActive)
        verify(apkVersionRepository).save(any())
    }

    @Test
    fun `deleteVersion should delete existing version and file`() {
        // Given
        val id = 1L
        val apkVersion =
            ApkVersionEntity(
                id = id,
                version = "1.0.0",
                versionCode = 100,
                fileName = "kidspos-v1.0.0.apk",
                fileSize = 1000L,
                filePath = "$testUploadDir/kidspos-v1.0.0.apk",
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )

        whenever(apkVersionRepository.findById(id))
            .thenReturn(Optional.of(apkVersion))
        doNothing().whenever(apkVersionRepository).deleteById(id)

        // When
        apkVersionService.deleteVersion(id)

        // Then
        verify(apkVersionRepository).deleteById(id)
    }

    @Test
    fun `getApkFile should throw exception when file does not exist`() {
        // Given
        val id = 1L
        val apkVersion =
            ApkVersionEntity(
                id = id,
                version = "1.0.0",
                versionCode = 100,
                fileName = "kidspos-v1.0.0.apk",
                fileSize = 1000L,
                filePath = "/non/existent/path/kidspos-v1.0.0.apk",
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )

        whenever(apkVersionRepository.findById(id))
            .thenReturn(Optional.of(apkVersion))

        // When & Then
        assertThrows<ResourceNotFoundException> {
            apkVersionService.getApkFile(id)
        }
    }
}
