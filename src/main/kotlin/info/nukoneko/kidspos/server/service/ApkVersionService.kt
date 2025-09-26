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

@Service
@Transactional
class ApkVersionService(
    private val apkVersionRepository: ApkVersionRepository,
    @Value("\${app.apk.upload-dir:./uploads/apk}")
    private val uploadDir: String = "./uploads/apk",
    @Value("\${app.apk.max-file-size:104857600}")
    private val maxFileSize: Long = 104857600,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        createUploadDirectory()
    }

    private fun createUploadDirectory() {
        val directory = Paths.get(uploadDir)
        if (!Files.exists(directory)) {
            Files.createDirectories(directory)
            logger.info("APKアップロードディレクトリを作成しました: $uploadDir")
        }
    }

    fun uploadApk(
        file: MultipartFile,
        version: String,
        versionCode: Int,
        releaseNotes: String?,
    ): ApkVersionEntity {
        validateApkFile(file)

        if (apkVersionRepository.existsByVersion(version)) {
            throw DuplicateResourceException("バージョン $version は既に存在します")
        }

        if (apkVersionRepository.existsByVersionCode(versionCode)) {
            throw DuplicateResourceException("バージョンコード $versionCode は既に存在します")
        }

        val fileName = "kidspos-v$version.apk"
        val filePath = saveApkFile(file, fileName)

        val apkVersion =
            ApkVersionEntity(
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
    fun getAllVersions(): List<ApkVersionEntity> = apkVersionRepository.findByIsActiveTrueOrderByVersionCodeDesc()

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

    fun deleteVersion(id: Long) {
        val apkVersion = getVersionById(id)

        val file = File(apkVersion.filePath)
        if (file.exists()) {
            file.delete()
            logger.info("APKファイルを削除しました: ${apkVersion.filePath}")
        }

        apkVersionRepository.deleteById(id)
    }
}
