package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.domain.exception.DuplicateResourceException
import info.nukoneko.kidspos.server.domain.exception.InvalidFileException
import info.nukoneko.kidspos.server.domain.exception.ResourceNotFoundException
import info.nukoneko.kidspos.server.entity.ApkVersionEntity
import info.nukoneko.kidspos.server.repository.ApkVersionRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime

/**
 * バージョン名の新しさとバージョンコードの大小が食い違う組み合わせ
 */
data class VersionOrderConflict(
    val newerName: ApkVersionEntity,
    val higherCode: ApkVersionEntity,
)

@Service
@Transactional
class ApkVersionService(
    private val apkVersionRepository: ApkVersionRepository,
    private val apkManifestParser: ApkManifestParser,
    @Value("\${app.apk.upload-dir:./uploads/apk}")
    private val uploadDir: String = "./uploads/apk",
    @Value("\${app.apk.max-file-size:104857600}")
    private val maxFileSize: Long = 104857600,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        try {
            createUploadDirectory()
            logger.info("APKVersionService が正常に初期化されました")
        } catch (e: Exception) {
            logger.error("APKVersionService の初期化中にエラーが発生しました: ${e.message}", e)
            throw e
        }
    }

    private fun createUploadDirectory() {
        val directory = Paths.get(uploadDir)
        if (!Files.exists(directory)) {
            Files.createDirectories(directory)
            logger.info("APKアップロードディレクトリを作成しました: $uploadDir")
        }
    }

    @Transactional(readOnly = true)
    fun analyzeApk(file: MultipartFile): ApkManifestInfo {
        validateApkFile(file)
        return file.inputStream.use { apkManifestParser.parse(it) }
    }

    fun uploadApk(
        file: MultipartFile,
        releaseNotes: String?,
    ): ApkVersionEntity {
        val manifest = analyzeApk(file)
        val version = manifest.versionName
        val versionCode = manifest.versionCode

        if (apkVersionRepository.existsByVersion(version)) {
            throw DuplicateResourceException("バージョン $version は既に存在します")
        }

        if (apkVersionRepository.existsByVersionCode(versionCode)) {
            throw DuplicateResourceException("バージョンコード $versionCode は既に存在します")
        }

        val fileName = buildFileName(version)
        val filePath = saveApkFile(file, fileName)

        // 新しいIDを生成（最大ID + 1）
        val nextId = (apkVersionRepository.findMaxId() ?: 0) + 1

        val apkVersion =
            ApkVersionEntity(
                id = nextId,
                version = version,
                versionCode = versionCode,
                fileName = fileName,
                fileSize = file.size,
                filePath = filePath,
                releaseNotes = releaseNotes,
                isActive = true,
                uploadedAt = LocalDateTime.now(),
            )

        return apkVersionRepository.save(apkVersion)
    }

    private fun validateApkFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw InvalidFileException("ファイルが選択されていません")
        }

        if (file.size > maxFileSize) {
            throw InvalidFileException("ファイルサイズが上限（${maxFileSize / 1024 / 1024}MB）を超えています")
        }

        val contentType = file.contentType ?: ""
        if (!contentType.contains("android") && file.originalFilename?.endsWith(".apk", true) != true) {
            throw InvalidFileException("APKファイルのみアップロード可能です")
        }
    }

    // バージョン名はAPKの中身に由来する信頼できない入力のため、ファイル名に使う前に無害化する
    private fun buildFileName(version: String): String {
        val sanitized =
            version
                .replace(UNSAFE_FILE_NAME_CHARS, "_")
                .replace(CONSECUTIVE_DOTS, ".")
                .take(MAX_VERSION_NAME_LENGTH)
        if (sanitized.isBlank() || sanitized.all { it == '.' || it == '_' }) {
            throw InvalidFileException("APKのバージョン名がファイル名として利用できません: $version")
        }
        return "kidspos-v$sanitized.apk"
    }

    private fun saveApkFile(
        file: MultipartFile,
        fileName: String,
    ): String {
        val targetPath = Paths.get(uploadDir, fileName)

        Files.deleteIfExists(targetPath)

        file.inputStream.use { input ->
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        logger.info("APKファイルを保存しました: $targetPath")
        return targetPath.toString()
    }

    @Transactional(readOnly = true)
    fun getLatestVersion(): ApkVersionEntity? = apkVersionRepository.findTopByIsActiveTrueOrderByVersionCodeDesc().orElse(null)

    @Transactional(readOnly = true)
    fun getAllVersions(): List<ApkVersionEntity> = apkVersionRepository.findAllByOrderByVersionCodeDesc()

    /**
     * バージョン名の新しさとバージョンコードの大小が食い違う組み合わせを洗い出す
     *
     * 最新バージョンはバージョンコードの降順で決まるため、
     * 手入力ミスで大きすぎるコードが登録されると古いAPKが配信され続ける。
     */
    fun detectVersionOrderConflicts(versions: List<ApkVersionEntity>): List<VersionOrderConflict> =
        versions.indices.flatMap { left ->
            (left + 1 until versions.size).mapNotNull { right ->
                conflictBetween(versions[left], versions[right])
            }
        }

    private fun conflictBetween(
        left: ApkVersionEntity,
        right: ApkVersionEntity,
    ): VersionOrderConflict? {
        val nameOrder = compareVersionNames(left.version, right.version)
        val codeOrder = left.versionCode.compareTo(right.versionCode)
        if (nameOrder == 0 || codeOrder == 0 || (nameOrder > 0) == (codeOrder > 0)) {
            return null
        }
        return if (nameOrder > 0) {
            VersionOrderConflict(newerName = left, higherCode = right)
        } else {
            VersionOrderConflict(newerName = right, higherCode = left)
        }
    }

    private fun compareVersionNames(
        left: String,
        right: String,
    ): Int {
        val leftParts = left.split('.')
        val rightParts = right.split('.')
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val leftPart = leftParts.getOrElse(index) { "" }
            val rightPart = rightParts.getOrElse(index) { "" }
            val leftNumber = leftPart.toIntOrNull()
            val rightNumber = rightPart.toIntOrNull()
            val order =
                if (leftNumber != null && rightNumber != null) {
                    leftNumber.compareTo(rightNumber)
                } else {
                    leftPart.compareTo(rightPart)
                }
            if (order != 0) {
                return order
            }
        }
        return 0
    }

    @Transactional(readOnly = true)
    fun getVersionById(id: Long): ApkVersionEntity =
        apkVersionRepository
            .findById(id)
            .orElseThrow { ResourceNotFoundException("APKバージョンが見つかりません: ID=$id") }

    @Transactional(readOnly = true)
    fun checkForUpdate(currentVersionCode: Int): ApkVersionEntity? {
        val newerVersions = apkVersionRepository.findNewerVersions(currentVersionCode)
        return newerVersions.firstOrNull()
    }

    fun getApkFile(id: Long): File {
        val apkVersion = getVersionById(id)
        val file = File(apkVersion.filePath)

        if (!file.exists()) {
            throw ResourceNotFoundException("APKファイルが見つかりません: ${apkVersion.filePath}")
        }

        return file
    }

    fun deactivateVersion(id: Long): ApkVersionEntity {
        val apkVersion = getVersionById(id)
        val updated = apkVersion.copy(isActive = false)
        return apkVersionRepository.save(updated)
    }

    fun activateVersion(id: Long): ApkVersionEntity {
        val apkVersion = getVersionById(id)
        val updated = apkVersion.copy(isActive = true)
        return apkVersionRepository.save(updated)
    }

    fun deleteVersion(id: Long) {
        val apkVersion = getVersionById(id)

        val file = File(apkVersion.filePath)
        if (file.exists()) {
            file.delete()
            logger.info("APKファイルを削除しました: ${apkVersion.filePath}")
        }

        apkVersionRepository.deleteById(id)
    }

    companion object {
        private val UNSAFE_FILE_NAME_CHARS = Regex("[^A-Za-z0-9._-]")
        private val CONSECUTIVE_DOTS = Regex("\\.{2,}")
        private const val MAX_VERSION_NAME_LENGTH = 64
    }
}
