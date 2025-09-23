package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.nio.file.Path

/**
 * Test for BackupManager - handles backup operations
 * Part of ResourceManager refactoring into smaller, focused classes
 */
class BackupManagerTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var backupManager: BackupManager

    @BeforeEach
    fun setup() {
        backupManager = BackupManager()
    }

    @Test
    fun `should create backup successfully`() {
        // Given
        val sourceFile = tempDir.resolve("original.txt").toFile()
        sourceFile.writeText("Original content")

        // When
        val backupPath = backupManager.createBackup(sourceFile.absolutePath)

        // Then
        assertNotNull(backupPath)
        val backupFile = File(backupPath!!)
        assertTrue(backupFile.exists())
        assertEquals("Original content", backupFile.readText())
        assertTrue(backupFile.name.contains(".backup."))
    }

    @Test
    fun `should restore from backup`() {
        // Given
        val originalFile = tempDir.resolve("original.txt").toFile()
        originalFile.writeText("Original content")
        val backupPath = backupManager.createBackup(originalFile.absolutePath)

        // Modify the original
        originalFile.writeText("Modified content")

        // When
        backupManager.restoreFromBackup(backupPath, originalFile.absolutePath)

        // Then
        assertEquals("Original content", originalFile.readText())
    }

    @Test
    fun `should cleanup old backups`() {
        // Given
        val sourceFile = tempDir.resolve("test.txt").toFile()
        sourceFile.writeText("test")

        // Create multiple backups
        val backup1 = backupManager.createBackup(sourceFile.absolutePath)
        Thread.sleep(10) // Ensure different timestamps
        val backup2 = backupManager.createBackup(sourceFile.absolutePath)
        val backup3 = backupManager.createBackup(sourceFile.absolutePath)

        // When
        val deletedCount = backupManager.cleanupOldBackups(tempDir.toString(), maxAge = 1, maxCount = 2)

        // Then
        assertTrue(deletedCount >= 0)
        // At least some backups should remain
        val backupFiles = tempDir.toFile().listFiles { _, name -> name.contains(".backup.") }
        assertTrue(backupFiles?.size ?: 0 <= 2)
    }

    @Test
    fun `should validate backup integrity`() {
        // Given
        val sourceFile = tempDir.resolve("integrity_test.txt").toFile()
        val originalContent = "Test content for integrity validation"
        sourceFile.writeText(originalContent)
        val backupPath = backupManager.createBackup(sourceFile.absolutePath)

        // When
        val isValid = backupManager.validateBackupIntegrity(sourceFile.absolutePath, backupPath)

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `should detect corrupted backup`() {
        // Given
        val sourceFile = tempDir.resolve("corruption_test.txt").toFile()
        sourceFile.writeText("Original content")
        val backupPath = backupManager.createBackup(sourceFile.absolutePath)

        // Corrupt the backup
        File(backupPath!!).writeText("Corrupted content")

        // When
        val isValid = backupManager.validateBackupIntegrity(sourceFile.absolutePath, backupPath)

        // Then
        assertFalse(isValid)
    }
}