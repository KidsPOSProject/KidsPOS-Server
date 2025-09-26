package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Refactored ResourceManager that delegates to specialized services
 * Demonstrates composition over inheritance principle
 */
@Service
class ResourceManagerRefactored(
    private val fileManager: FileManager,
    private val streamProcessor: StreamProcessor,
    private val backupManager: BackupManager,
) {
    private val logger = LoggerFactory.getLogger(ResourceManagerRefactored::class.java)

    // File operations delegation
    fun readFileContent(
        filePath: String,
        onClose: (() -> Unit)? = null,
    ): String = fileManager.readFileContent(filePath, onClose)

    fun copyFile(
        sourcePath: String,
        destinationPath: String,
    ) = fileManager.copyFile(sourcePath, destinationPath)

    fun fileExists(filePath: String): Boolean = fileManager.fileExists(filePath)

    fun createDirectoryIfNotExists(dirPath: String) = fileManager.createDirectoryIfNotExists(dirPath)

    // Stream processing delegation
    fun processLinesFromStream(
        inputStream: java.io.InputStream,
        processor: (String) -> Unit,
    ) = streamProcessor.processLinesFromStream(inputStream, processor)

    fun copyStreamWithProgress(
        inputStream: java.io.InputStream,
        outputStream: java.io.OutputStream,
        progressCallback: (Long) -> Unit,
    ) = streamProcessor.copyStreamWithProgress(inputStream, outputStream, progressCallback)

    // Backup operations delegation
    fun createBackup(filePath: String): String = backupManager.createBackup(filePath)

    fun restoreFromBackup(
        backupPath: String,
        targetPath: String,
    ) = backupManager.restoreFromBackup(backupPath, targetPath)

    fun cleanupOldBackups(
        directory: String,
        maxAge: Long,
        maxCount: Int,
    ): Int = backupManager.cleanupOldBackups(directory, maxAge, maxCount)

    // High-level operations combining multiple services
    fun safeFileOperation(
        sourcePath: String,
        destinationPath: String,
    ): Boolean =
        try {
            // Create backup before operation
            val backupPath = backupManager.createBackup(sourcePath)
            logger.info("Created backup: {}", backupPath)

            // Perform file operation
            fileManager.copyFile(sourcePath, destinationPath)
            logger.info("File operation completed successfully")

            true
        } catch (e: Exception) {
            logger.error("Safe file operation failed: ${e.message}", e)
            false
        }
}
