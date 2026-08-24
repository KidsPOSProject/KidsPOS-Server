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
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ApkVersionServiceTest {
    @Mock
    private lateinit var apkVersionRepository: ApkVersionRepository

    private lateinit var apkVersionService: ApkVersionService

    private val testUploadDir = "./test-uploads/apk"
    private val maxFileSize = 104857600L

    @BeforeEach
    fun setUp() {
        apkVersionService = createService(maxFileSize)

        // テスト用ディレクトリをクリーンアップ
        val testDir = Paths.get(testUploadDir)
        if (Files.exists(testDir)) {
            Files
                .walk(testDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }

        apkVersionService.init()
    }

    private fun createService(fileSizeLimit: Long) =
        ApkVersionService(apkVersionRepository, ApkManifestParser(), testUploadDir, fileSizeLimit)

    private fun apkFixture(name: String): MultipartFile {
        val bytes =
            checkNotNull(javaClass.getResourceAsStream("/apk/$name")) {
                "fixture not found: $name"
            }.use { it.readBytes() }
        return MockMultipartFile("file", name, "application/vnd.android.package-archive", bytes)
    }

    @Test
    fun `analyzeApk should read version information from APK`() {
        val result = apkVersionService.analyzeApk(apkFixture("valid-utf16.apk"))

        assertEquals("1.2.3", result.versionName)
        assertEquals(10203, result.versionCode)
    }

    @Test
    fun `analyzeApk should throw exception when file is not an APK`() {
        assertThrows<InvalidFileException> {
            apkVersionService.analyzeApk(apkFixture("not-a-zip.apk"))
        }
    }

    @Test
    fun `uploadApk should read version from APK and save the file`() {
        val releaseNotes = "Initial release"
        val file = apkFixture("valid-utf16.apk")

        whenever(apkVersionRepository.existsByVersion("1.2.3")).thenReturn(false)
        whenever(apkVersionRepository.existsByVersionCode(10203)).thenReturn(false)
        whenever(apkVersionRepository.findMaxId()).thenReturn(null)
        whenever(apkVersionRepository.save(any<ApkVersionEntity>())).thenAnswer { it.arguments[0] as ApkVersionEntity }

        val result = apkVersionService.uploadApk(file, releaseNotes)

        assertEquals("1.2.3", result.version)
        assertEquals(10203, result.versionCode)
        assertEquals("kidspos-v1.2.3.apk", result.fileName)
        assertEquals(file.size, result.fileSize)
        assertEquals(releaseNotes, result.releaseNotes)
        assertTrue(Files.exists(Paths.get(testUploadDir, "kidspos-v1.2.3.apk")))
        assertArrayEquals(file.bytes, Files.readAllBytes(Paths.get(result.filePath)))
    }

    @Test
    fun `uploadApk should sanitize version names that contain path separators`() {
        val file = apkFixture("path-traversal-version.apk")

        whenever(apkVersionRepository.existsByVersion(any())).thenReturn(false)
        whenever(apkVersionRepository.existsByVersionCode(any())).thenReturn(false)
        whenever(apkVersionRepository.findMaxId()).thenReturn(null)
        whenever(apkVersionRepository.save(any<ApkVersionEntity>())).thenAnswer { it.arguments[0] as ApkVersionEntity }

        val result = apkVersionService.uploadApk(file, null)

        assertEquals("../../../etc/evil", result.version)
        assertFalse(result.fileName.contains("/"))
        assertFalse(result.fileName.contains(".."))
        assertEquals(Paths.get(testUploadDir, result.fileName).toString(), result.filePath)
        assertTrue(Files.exists(Paths.get(result.filePath)))
    }

    @Test
    fun `uploadApk should reject version names that sanitize to an unusable file name`() {
        whenever(apkVersionRepository.existsByVersion(any())).thenReturn(false)
        whenever(apkVersionRepository.existsByVersionCode(any())).thenReturn(false)

        assertThrows<InvalidFileException> {
            apkVersionService.uploadApk(apkFixture("only-dots-version.apk"), null)
        }
    }

    @Test
    fun `uploadApk should throw exception when file is empty`() {
        val emptyFile = MockMultipartFile("file", "empty.apk", "application/vnd.android.package-archive", ByteArray(0))

        assertThrows<InvalidFileException> {
            apkVersionService.uploadApk(emptyFile, null)
        }
    }

    @Test
    fun `uploadApk should throw exception when file size exceeds limit`() {
        val service = createService(100L)

        assertThrows<InvalidFileException> {
            service.uploadApk(apkFixture("valid-utf16.apk"), null)
        }
    }

    @Test
    fun `uploadApk should throw exception when file is not an APK`() {
        assertThrows<InvalidFileException> {
            apkVersionService.uploadApk(apkFixture("no-manifest.apk"), null)
        }
    }

    @Test
    fun `uploadApk should throw exception when version already exists`() {
        whenever(apkVersionRepository.existsByVersion("1.2.3")).thenReturn(true)

        assertThrows<DuplicateResourceException> {
            apkVersionService.uploadApk(apkFixture("valid-utf16.apk"), null)
        }
    }

    @Test
    fun `uploadApk should throw exception when version code already exists`() {
        whenever(apkVersionRepository.existsByVersion("1.2.3")).thenReturn(false)
        whenever(apkVersionRepository.existsByVersionCode(10203)).thenReturn(true)

        assertThrows<DuplicateResourceException> {
            apkVersionService.uploadApk(apkFixture("valid-utf16.apk"), null)
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
    fun `detectVersionOrderConflicts should report a version code that contradicts the version name`() {
        val mistyped = versionOf(id = 1L, version = "1.0.10", versionCode = 100)
        val newest = versionOf(id = 2L, version = "1.0.11", versionCode = 12)

        val conflicts = apkVersionService.detectVersionOrderConflicts(listOf(mistyped, newest))

        assertEquals(1, conflicts.size)
        assertEquals(newest, conflicts.first().newerName)
        assertEquals(mistyped, conflicts.first().higherCode)
    }

    @Test
    fun `detectVersionOrderConflicts should return empty when version codes follow version names`() {
        val versions =
            listOf(
                versionOf(id = 3L, version = "1.0.11", versionCode = 12),
                versionOf(id = 2L, version = "1.0.10", versionCode = 11),
                versionOf(id = 1L, version = "1.0.9", versionCode = 10),
            )

        assertTrue(apkVersionService.detectVersionOrderConflicts(versions).isEmpty())
    }

    @Test
    fun `detectVersionOrderConflicts should compare version name segments numerically`() {
        val versions =
            listOf(
                versionOf(id = 1L, version = "1.0.9", versionCode = 9),
                versionOf(id = 2L, version = "1.0.10", versionCode = 10),
            )

        assertTrue(apkVersionService.detectVersionOrderConflicts(versions).isEmpty())
    }

    @Test
    fun `detectVersionOrderConflicts should report every conflicting pair`() {
        val mistyped = versionOf(id = 1L, version = "1.0.10", versionCode = 100)
        val versions =
            listOf(
                mistyped,
                versionOf(id = 2L, version = "1.0.11", versionCode = 12),
                versionOf(id = 3L, version = "1.1.0", versionCode = 13),
            )

        val conflicts = apkVersionService.detectVersionOrderConflicts(versions)

        assertEquals(2, conflicts.size)
        assertTrue(conflicts.all { it.higherCode == mistyped })
        assertEquals(listOf("1.0.11", "1.1.0"), conflicts.map { it.newerName.version })
    }

    @Test
    fun `detectVersionOrderConflicts should ignore versions that share a version name`() {
        val versions =
            listOf(
                versionOf(id = 1L, version = "1.0.0", versionCode = 1),
                versionOf(id = 2L, version = "1.0.0", versionCode = 2),
            )

        assertTrue(apkVersionService.detectVersionOrderConflicts(versions).isEmpty())
    }

    private fun versionOf(
        id: Long,
        version: String,
        versionCode: Int,
    ) = ApkVersionEntity(
        id = id,
        version = version,
        versionCode = versionCode,
        fileName = "kidspos-v$version.apk",
        fileSize = 1000L,
        filePath = "$testUploadDir/kidspos-v$version.apk",
        isActive = true,
        uploadedAt = LocalDateTime.now(),
    )

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
