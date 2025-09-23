package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.system.measureTimeMillis

/**
 * Service for managing resources efficiently
 * Delegates to specialized services following Single Responsibility Principle
 * @deprecated Use FileManager, StreamProcessor, or BackupManager directly
 */
@Service
@Deprecated("Use specialized services: FileManager, StreamProcessor, BackupManager")
class ResourceManager(
    private val fileManager: FileManager,
    private val streamProcessor: StreamProcessor,
    private val backupManager: BackupManager
) {
    private val logger = LoggerFactory.getLogger(ResourceManager::class.java)

    /**
     * Read file content using try-with-resources pattern
     * @deprecated Use FileManager.readFileContent() instead
     */
    @Deprecated("Use FileManager.readFileContent() instead")
    fun readFileContent(filePath: String, onClose: (() -> Unit)? = null): String {
        logger.warn("ResourceManager.readFileContent is deprecated. Use FileManager instead.")
        return fileManager.readFileContent(filePath, onClose)
    }

    /**
     * Copy file using proper resource management
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
            throw RuntimeException("Unable to copy file", e)
        }
    }

    /**
     * Process large dataset using streaming to optimize memory usage
     */
    fun processLargeDataset(data: List<String>): ProcessingResult {
        logger.debug("Processing large dataset with {} items", data.size)

        val totalItems = data.size
        var totalLength = 0L
        var processedCount = 0

        // Use streaming to process data efficiently
        data.stream()
            .parallel()
            .forEach { item ->
                totalLength += item.length
                processedCount++

                // Simulate some processing
                if (processedCount % 1000 == 0) {
                    logger.debug("Processed {} items", processedCount)
                }
            }

        val averageLength = if (totalItems > 0) totalLength.toDouble() / totalItems else 0.0
        val summary = "Processed $totalItems items with average length ${"%.2f".format(averageLength)}"

        logger.info("Completed processing: {}", summary)
        return ProcessingResult(totalItems, averageLength, summary)
    }

    /**
     * Process data with timeout management
     */
    fun processWithTimeout(taskName: String, timeoutMs: Long): String {
        val executor = Executors.newSingleThreadExecutor()

        return try {
            val future = executor.submit<String> {
                // Simulate processing work
                Thread.sleep(100) // Simulate some work
                "$taskName processed successfully"
            }

            val result = future.get(timeoutMs, TimeUnit.MILLISECONDS)
            logger.debug("Task {} completed within timeout", taskName)
            result
        } catch (e: Exception) {
            logger.error("Task {} failed or timed out", taskName, e)
            throw RuntimeException("Task $taskName failed", e)
        } finally {
            executor.shutdown()
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executor.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
    }

    /**
     * Read file and throw exception to test resource cleanup
     */
    fun readFileWithException(filePath: String): String {
        return Files.newBufferedReader(Paths.get(filePath)).use { _ ->
            // Throw exception to test cleanup
            throw RuntimeException("Simulated exception during file reading")
        }
    }

    /**
     * Process data in batches to optimize memory usage
     */
    fun processBatchedData(totalItems: Int, batchSize: Int): BatchResult {
        logger.debug("Processing {} items in batches of {}", totalItems, batchSize)

        val runtime = Runtime.getRuntime()
        var maxMemoryUsed = 0L
        var processedCount = 0
        var batchCount = 0

        for (startIndex in 0 until totalItems step batchSize) {
            val endIndex = minOf(startIndex + batchSize, totalItems)

            // Create batch data
            val batch = (startIndex until endIndex).map { "Item $it" }

            // Process batch
            batch.forEach { _ ->
                // Simulate processing
                processedCount++
            }

            batchCount++

            // Monitor memory usage
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            if (usedMemory > maxMemoryUsed) {
                maxMemoryUsed = usedMemory
            }

            // Log progress
            if (batchCount % 5 == 0) {
                logger.debug("Processed {} batches, {} items total", batchCount, processedCount)
            }

            // Optional: suggest GC between batches for large datasets
            if (batchCount % 10 == 0) {
                System.gc()
            }
        }

        logger.info("Batch processing completed: {} items in {} batches, max memory: {} bytes",
                   processedCount, batchCount, maxMemoryUsed)

        return BatchResult(processedCount, batchCount, maxMemoryUsed)
    }

    /**
     * Create a simple resource pool for demonstration
     */
    fun createResourcePool(poolSize: Int): ResourcePool {
        logger.debug("Creating resource pool with size {}", poolSize)
        return ResourcePool(poolSize)
    }

    /**
     * Process streaming data efficiently
     */
    fun processStreamingData(dataStream: Stream<String>): String {
        return try {
            dataStream.use { stream ->
                val items = stream
                    .filter { it.isNotEmpty() }
                    .map { it.trim() }
                    .limit(1000) // Limit to prevent memory issues
                    .toList()

                val result = items.joinToString(",")
                logger.debug("Processed streaming data, result length: {}", result.length)
                result
            }
        } catch (e: Exception) {
            logger.error("Error processing streaming data", e)
            throw RuntimeException("Failed to process streaming data", e)
        }
    }
}

/**
 * Result classes for processing operations
 */
data class ProcessingResult(
    val totalItems: Int,
    val averageLength: Double,
    val summary: String
)

data class BatchResult(
    val processedCount: Int,
    val batchCount: Int,
    val maxMemoryUsed: Long
)

/**
 * Mock resource for pooling demonstration
 */
class MockResource(val id: Int) {
    fun process(data: String): String = "Resource $id processed: $data"
}

/**
 * Simple resource pool implementation
 */
class ResourcePool(private val maxSize: Int) {
    private val resources = mutableListOf<MockResource>()
    private val inUse = mutableSetOf<MockResource>()
    var totalAcquired = 0
        private set

    init {
        repeat(maxSize) { index ->
            resources.add(MockResource(index))
        }
    }

    @Synchronized
    fun acquire(): MockResource {
        if (resources.isEmpty()) {
            throw RuntimeException("No resources available")
        }
        val resource = resources.removeAt(0)
        inUse.add(resource)
        totalAcquired++
        return resource
    }

    @Synchronized
    fun release(resource: MockResource) {
        if (inUse.remove(resource)) {
            resources.add(resource)
        }
    }

    val currentPoolSize: Int
        @Synchronized get() = resources.size + inUse.size
}