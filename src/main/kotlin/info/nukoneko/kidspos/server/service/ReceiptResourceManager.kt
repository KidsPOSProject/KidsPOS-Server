package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.*
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/**
 * Resource manager specifically for receipt printing operations
 * Implements proper resource management for network connections and file operations
 */
@Service
class ReceiptResourceManager {
    private val logger = LoggerFactory.getLogger(ReceiptResourceManager::class.java)
    private val connectionPool = ConcurrentHashMap<String, SocketConnection>()

    /**
     * Send receipt data to printer using proper resource management
     */
    fun sendToThermalPrinter(printerHost: String, printerPort: Int, receiptData: String): Boolean {
        val connectionKey = "$printerHost:$printerPort"

        return try {
            getOrCreateConnection(connectionKey, printerHost, printerPort).use { connection ->
                connection.outputStream.use { output ->
                    val writer = OutputStreamWriter(output, StandardCharsets.UTF_8)
                    writer.use { w ->
                        w.write(receiptData)
                        w.flush()
                        logger.debug("Successfully sent receipt data to printer {}:{}", printerHost, printerPort)
                        true
                    }
                }
            }
        } catch (e: IOException) {
            logger.error("Failed to send receipt to printer {}:{}", printerHost, printerPort, e)
            // Clean up failed connection
            cleanupConnection(connectionKey)
            false
        }
    }

    /**
     * Save receipt to file using try-with-resources
     */
    fun saveReceiptToFile(receiptData: String, filePath: String): Boolean {
        return try {
            FileOutputStream(filePath).use { fileOut ->
                OutputStreamWriter(fileOut, StandardCharsets.UTF_8).use { writer ->
                    BufferedWriter(writer).use { bufferedWriter ->
                        bufferedWriter.write(receiptData)
                        bufferedWriter.flush()
                        logger.debug("Receipt saved to file: {}", filePath)
                        true
                    }
                }
            }
        } catch (e: IOException) {
            logger.error("Failed to save receipt to file: {}", filePath, e)
            false
        }
    }

    /**
     * Process receipt template with resource management
     */
    fun processReceiptTemplate(templatePath: String, data: Map<String, String>): String {
        return try {
            FileInputStream(templatePath).use { fileInput ->
                InputStreamReader(fileInput, StandardCharsets.UTF_8).use { reader ->
                    BufferedReader(reader).use { bufferedReader ->
                        val template = bufferedReader.readText()
                        var processedTemplate = template

                        // Replace placeholders with actual data
                        data.forEach { (key, value) ->
                            processedTemplate = processedTemplate.replace("{{$key}}", value)
                        }

                        logger.debug("Processed receipt template with {} placeholders", data.size)
                        processedTemplate
                    }
                }
            }
        } catch (e: IOException) {
            logger.error("Failed to process receipt template: {}", templatePath, e)
            throw RuntimeException("Unable to process receipt template", e)
        }
    }

    /**
     * Batch print multiple receipts efficiently
     */
    fun batchPrintReceipts(
        printerHost: String,
        printerPort: Int,
        receipts: List<String>,
        batchSize: Int = 10
    ): BatchPrintResult {
        logger.info("Starting batch print of {} receipts", receipts.size)

        var successCount = 0
        var failureCount = 0
        val errors = mutableListOf<String>()

        receipts.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            logger.debug("Processing batch {} of {}", batchIndex + 1, (receipts.size + batchSize - 1) / batchSize)

            batch.forEach { receiptData ->
                if (sendToThermalPrinter(printerHost, printerPort, receiptData)) {
                    successCount++
                } else {
                    failureCount++
                    errors.add("Failed to print receipt in batch ${batchIndex + 1}")
                }
            }

            // Small delay between batches to avoid overwhelming the printer
            if (batchIndex < receipts.size / batchSize) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return@forEachIndexed
                }
            }
        }

        val result = BatchPrintResult(successCount, failureCount, errors)
        logger.info("Batch print completed: {} successful, {} failed", successCount, failureCount)
        return result
    }

    /**
     * Get or create a socket connection with proper resource management
     */
    private fun getOrCreateConnection(key: String, host: String, port: Int): SocketConnection {
        return connectionPool.compute(key) { _, existing ->
            if (existing?.isValid() == true) {
                existing
            } else {
                try {
                    val socket = Socket(host, port)
                    socket.soTimeout = 5000 // 5 second timeout
                    SocketConnection(socket)
                } catch (e: IOException) {
                    logger.error("Failed to create socket connection to {}:{}", host, port, e)
                    throw e
                }
            }
        }!!
    }

    /**
     * Clean up a failed connection
     */
    private fun cleanupConnection(key: String) {
        connectionPool.remove(key)?.close()
    }

    /**
     * Clean up all connections (for shutdown)
     */
    fun shutdown() {
        logger.info("Shutting down receipt resource manager")
        connectionPool.values.forEach { it.close() }
        connectionPool.clear()
    }
}

/**
 * Wrapper for socket connection with proper resource management
 */
class SocketConnection(private val socket: Socket) : Closeable {
    private val logger = LoggerFactory.getLogger(SocketConnection::class.java)

    val outputStream: OutputStream
        get() = socket.getOutputStream()

    val inputStream: InputStream
        get() = socket.getInputStream()

    fun isValid(): Boolean {
        return !socket.isClosed && socket.isConnected
    }

    override fun close() {
        try {
            if (!socket.isClosed) {
                socket.close()
                logger.debug("Socket connection closed")
            }
        } catch (e: IOException) {
            logger.warn("Error closing socket connection", e)
        }
    }
}

/**
 * Result of batch printing operation
 */
data class BatchPrintResult(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<String>
) {
    val totalProcessed: Int
        get() = successCount + failureCount

    val successRate: Double
        get() = if (totalProcessed > 0) successCount.toDouble() / totalProcessed else 0.0
}