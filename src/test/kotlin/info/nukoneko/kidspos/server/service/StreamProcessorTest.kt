package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test for StreamProcessor - handles streaming operations
 * Part of ResourceManager refactoring into smaller, focused classes
 */
class StreamProcessorTest {

    private lateinit var streamProcessor: StreamProcessor

    @BeforeEach
    fun setup() {
        streamProcessor = StreamProcessor()
    }

    @Test
    fun `should process lines from stream efficiently`() {
        // Given
        val inputData = "line1\nline2\nline3\nline4\nline5"
        val inputStream = ByteArrayInputStream(inputData.toByteArray())
        val processedLines = mutableListOf<String>()

        // When
        streamProcessor.processLinesFromStream(inputStream) { line: String ->
            processedLines.add(line.uppercase())
        }

        // Then
        assertEquals(5, processedLines.size)
        assertEquals("LINE1", processedLines[0])
        assertEquals("LINE5", processedLines[4])
    }

    @Test
    fun `should copy stream with progress tracking`() {
        // Given
        val inputData = "This is test data for stream copying"
        val inputStream = ByteArrayInputStream(inputData.toByteArray())
        val outputStream = ByteArrayOutputStream()
        var progressCalled = false

        // When
        streamProcessor.copyStreamWithProgress(inputStream, outputStream) { bytes: Long ->
            progressCalled = true
            assertTrue(bytes > 0)
        }

        // Then
        assertTrue(progressCalled)
        assertEquals(inputData, outputStream.toString())
    }

    @Test
    fun `should process large data stream in chunks`() {
        // Given
        val largeData = "x".repeat(10000)
        val inputStream = ByteArrayInputStream(largeData.toByteArray())
        val chunkCount = AtomicInteger(0)

        // When
        streamProcessor.processStreamInChunks(inputStream, 1024) { chunk: ByteArray ->
            chunkCount.incrementAndGet()
            assertTrue(chunk.isNotEmpty())
        }

        // Then
        assertTrue(chunkCount.get() > 1) // Should process in multiple chunks
    }

    @Test
    fun `should handle empty stream gracefully`() {
        // Given
        val emptyStream = ByteArrayInputStream(byteArrayOf())
        val processedLines = mutableListOf<String>()

        // When
        streamProcessor.processLinesFromStream(emptyStream) { line: String ->
            processedLines.add(line)
        }

        // Then
        assertEquals(0, processedLines.size)
    }

    @Test
    fun `should process with timeout`() {
        // Given
        val inputData = "test data"
        val inputStream = ByteArrayInputStream(inputData.toByteArray())
        val outputStream = ByteArrayOutputStream()

        // When
        val result = streamProcessor.copyStreamWithTimeout(inputStream, outputStream, 5000)

        // Then
        assertTrue(result)
        assertEquals(inputData, outputStream.toString())
    }
}