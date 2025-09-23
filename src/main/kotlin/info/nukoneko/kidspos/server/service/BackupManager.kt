package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import java.security.MessageDigest

/**
 * Handles backup operations with proper resource management
 * Extracted from ResourceManager to follow Single Responsibility Principle
 */
@Service
class BackupManager {
    private val logger = LoggerFactory.getLogger(BackupManager::class.java)

    /**
     * Create backup of a file
     */
    fun createBackup(filePath: String): String {
        val sourceFile = File(filePath)
        if (!sourceFile.exists()) {
            throw RuntimeException("Source file does not exist: $filePath")
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))
        val backupFileName = "${sourceFile.nameWithoutExtension}.backup.$timestamp.${sourceFile.extension}"
        val backupPath = File(sourceFile.parent, backupFileName).absolutePath

        try {
            Files.copy(Paths.get(filePath), Paths.get(backupPath), StandardCopyOption.COPY_ATTRIBUTES)
            logger.debug("Created backup: {} -> {}", filePath, backupPath)
            return backupPath
        } catch (e: Exception) {
            logger.error("Failed to create backup for: {}", filePath, e)
            throw RuntimeException("Unable to create backup: $filePath", e)
        }
    }

    /**
     * Restore file from backup
     */
    fun restoreFromBackup(backupPath: String, targetPath: String) {
        val backupFile = File(backupPath)
        if (!backupFile.exists()) {
            throw RuntimeException("Backup file does not exist: $backupPath")
        }

        try {
            Files.copy(Paths.get(backupPath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING)
            logger.info("Restored file from backup: {} -> {}", backupPath, targetPath)
        } catch (e: Exception) {
            logger.error("Failed to restore from backup: {} -> {}", backupPath, targetPath, e)
            throw RuntimeException("Unable to restore from backup", e)
        }
    }

    /**
     * Cleanup old backups
     */
    fun cleanupOldBackups(directory: String, maxAge: Long, maxCount: Int): Int {
        val dir = File(directory)
        if (!dir.exists() || !dir.isDirectory()) {
            return 0
        }

        val backupFiles = dir.listFiles { _, name -> name.contains(".backup.") }
            ?.sortedByDescending { it.lastModified() }
            ?: return 0

        var deletedCount = 0
        val currentTime = System.currentTimeMillis()
        val maxAgeMs = maxAge * 24 * 60 * 60 * 1000 // Convert days to milliseconds

        // Delete files older than maxAge or beyond maxCount
        for (i in backupFiles.indices) {
            val file = backupFiles[i]
            val shouldDelete = i >= maxCount || (currentTime - file.lastModified()) > maxAgeMs

            if (shouldDelete && file.delete()) {
                deletedCount++
                logger.debug("Deleted old backup: {}", file.name)
            }
        }

        logger.info("Cleaned up {} old backup files in directory: {}", deletedCount, directory)
        return deletedCount
    }

    /**
     * Validate backup integrity
     */
    fun validateBackupIntegrity(originalPath: String, backupPath: String): Boolean {
        return try {
            val originalHash = calculateFileHash(originalPath)
            val backupHash = calculateFileHash(backupPath)
            val isValid = originalHash == backupHash

            if (isValid) {
                logger.debug("Backup integrity validated: {}", backupPath)
            } else {
                logger.warn("Backup integrity check failed: {}", backupPath)
            }

            isValid
        } catch (e: Exception) {
            logger.error("Failed to validate backup integrity", e)
            false
        }
    }

    private fun calculateFileHash(filePath: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val file = File(filePath)

        return file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}