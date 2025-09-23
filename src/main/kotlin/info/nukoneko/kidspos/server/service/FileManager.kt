package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Handles file operations with proper resource management
 *
 * Provides file I/O operations using proper resource management patterns
 * with try-with-resources and comprehensive error handling. Extracted from
 * ResourceManager to follow Single Responsibility Principle.
 */
@Service
class FileManager {
    private val logger = LoggerFactory.getLogger(FileManager::class.java)

    /**
     * Read file content using try-with-resources pattern
     *
     * Reads the entire content of a file as a string with proper resource management.
     * Optionally executes a cleanup callback after reading is complete.
     *
     * @param filePath Path to the file to read
     * @param onClose Optional callback to execute after reading
     * @return File content as string
     * @throws RuntimeException if file cannot be read
     */
    fun readFileContent(filePath: String, onClose: (() -> Unit)? = null): String {
        return try {
            Files.newBufferedReader(Paths.get(filePath)).use { reader ->
                val content = reader.readText()
                logger.debug("Successfully read {} characters from file: {}", content.length, filePath)
                content
            }
        } catch (e: IOException) {
            logger.error("Failed to read file: {}", filePath, e)
            throw RuntimeException("Unable to read file: $filePath", e)
        } finally {
            onClose?.invoke()
        }
    }

    /**
     * Copy file using proper resource management
     *
     * Copies a file from source to destination using streams with
     * proper resource management to prevent resource leaks.
     *
     * @param sourcePath Path to source file
     * @param destinationPath Path to destination file
     * @throws RuntimeException if file cannot be copied
     */
    fun copyFile(sourcePath: String, destinationPath: String) {
        try {
            Files.newInputStream(Paths.get(sourcePath)).use { input ->
                Files.newOutputStream(Paths.get(destinationPath)).use { output ->
                    input.copyTo(output)
                    logger.debug("Successfully copied file from {} to {}", sourcePath, destinationPath)
                }
            }
        } catch (e: IOException) {
            logger.error("Failed to copy file from {} to {}", sourcePath, destinationPath, e)
            throw RuntimeException("Unable to copy file: $sourcePath -> $destinationPath", e)
        }
    }

    /**
     * Check if file exists
     *
     * Checks whether a file exists at the specified path.
     *
     * @param filePath Path to check for file existence
     * @return True if file exists, false otherwise
     */
    fun fileExists(filePath: String): Boolean {
        return Files.exists(Paths.get(filePath))
    }

    /**
     * Create directory if it doesn't exist
     *
     * Creates the specified directory and any necessary parent directories
     * if they don't already exist.
     *
     * @param dirPath Path of directory to create
     * @throws RuntimeException if directory cannot be created
     */
    fun createDirectoryIfNotExists(dirPath: String) {
        val path = Paths.get(dirPath)
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path)
                logger.debug("Created directory: {}", dirPath)
            } catch (e: IOException) {
                logger.error("Failed to create directory: {}", dirPath, e)
                throw RuntimeException("Unable to create directory: $dirPath", e)
            }
        }
    }
}