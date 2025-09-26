package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Handles streaming operations with proper resource management
 * Extracted from ResourceManager to follow Single Responsibility Principle
 */
@Service
class StreamProcessor {
    private val logger = LoggerFactory.getLogger(StreamProcessor::class.java)

    /**
     * Process lines from stream efficiently
     */
    fun processLinesFromStream(
        inputStream: InputStream,
        processor: (String) -> Unit,
    ) {
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    processor(line)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process lines from stream", e)
            throw RuntimeException("Unable to process stream lines", e)
        }
    }

    /**
     * Copy stream with progress tracking
     */
    fun copyStreamWithProgress(
        inputStream: InputStream,
        outputStream: OutputStream,
        progressCallback: (Long) -> Unit,
    ) {
        try {
            val buffer = ByteArray(8192)
            var totalBytes = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
                progressCallback(totalBytes)
            }
            outputStream.flush()
        } catch (e: Exception) {
            logger.error("Failed to copy stream with progress", e)
            throw RuntimeException("Unable to copy stream", e)
        }
    }

    /**
     * Process stream in chunks
     */
    fun processStreamInChunks(
        inputStream: InputStream,
        chunkSize: Int,
        chunkProcessor: (ByteArray) -> Unit,
    ) {
        try {
            val buffer = ByteArray(chunkSize)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                val chunk = if (bytesRead == chunkSize) buffer else buffer.copyOf(bytesRead)
                chunkProcessor(chunk)
            }
        } catch (e: Exception) {
            logger.error("Failed to process stream in chunks", e)
            throw RuntimeException("Unable to process stream chunks", e)
        }
    }

    /**
     * Copy stream with timeout
     */
    fun copyStreamWithTimeout(
        inputStream: InputStream,
        outputStream: OutputStream,
        timeoutMs: Long,
    ): Boolean {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            val future =
                executor.submit {
                    copyStreamWithProgress(inputStream, outputStream) { _ -> }
                }
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
            true
        } catch (e: Exception) {
            logger.warn("Stream copy timed out or failed", e)
            false
        } finally {
            executor.shutdown()
        }
    }
}
