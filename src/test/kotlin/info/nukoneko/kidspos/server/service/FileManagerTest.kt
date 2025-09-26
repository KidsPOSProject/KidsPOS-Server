package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * Test for FileManager - handles file operations
 * Part of ResourceManager refactoring into smaller, focused classes
 */
class FileManagerTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var fileManager: FileManager

    @BeforeEach
    fun setup() {
        fileManager = FileManager()
    }

    @Test
    fun `should read file content successfully`() {
        // Given
        val testFile = tempDir.resolve("test.txt").toFile()
        val expectedContent = "Hello, World!"
        testFile.writeText(expectedContent)

        // When
        val result = fileManager.readFileContent(testFile.absolutePath)

        // Then
        assertEquals(expectedContent, result)
    }

    @Test
    fun `should throw exception when file does not exist`() {
        // Given
        val nonExistentFile = tempDir.resolve("nonexistent.txt").toFile()

        // When & Then
        assertThrows(RuntimeException::class.java) {
            fileManager.readFileContent(nonExistentFile.absolutePath)
        }
    }

    @Test
    fun `should copy file successfully`() {
        // Given
        val sourceFile = tempDir.resolve("source.txt").toFile()
        val destFile = tempDir.resolve("dest.txt").toFile()
        val content = "Test content for copying"
        sourceFile.writeText(content)

        // When
        fileManager.copyFile(sourceFile.absolutePath, destFile.absolutePath)

        // Then
        assertTrue(destFile.exists())
        assertEquals(content, destFile.readText())
    }

    @Test
    fun `should check if file exists`() {
        // Given
        val existingFile = tempDir.resolve("existing.txt").toFile()
        existingFile.writeText("exists")
        val nonExistentFile = tempDir.resolve("missing.txt").toFile()

        // When & Then
        assertTrue(fileManager.fileExists(existingFile.absolutePath))
        assertFalse(fileManager.fileExists(nonExistentFile.absolutePath))
    }

    @Test
    fun `should create directory if not exists`() {
        // Given
        val newDir = tempDir.resolve("newdir").toFile()
        assertFalse(newDir.exists())

        // When
        fileManager.createDirectoryIfNotExists(newDir.absolutePath)

        // Then
        assertTrue(newDir.exists())
        assertTrue(newDir.isDirectory())
    }

    @Test
    fun `should handle callback on file close`() {
        // Given
        val testFile = tempDir.resolve("callback.txt").toFile()
        testFile.writeText("callback test")
        var callbackExecuted = false

        // When
        fileManager.readFileContent(testFile.absolutePath) {
            callbackExecuted = true
        }

        // Then
        assertTrue(callbackExecuted)
    }
}
