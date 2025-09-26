package info.nukoneko.kidspos.server.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Service for encrypting and decrypting sensitive data
 */
@Service
class DataEncryptionService {
    private val logger = LoggerFactory.getLogger(DataEncryptionService::class.java)

    @Value("\${app.security.encryption-key:DefaultKidsPOSKey123!@#}")
    private lateinit var encryptionKey: String

    private val algorithm = "AES/CBC/PKCS5Padding"
    private val keyAlgorithm = "AES"

    /**
     * Encrypt sensitive data
     */
    fun encrypt(data: String): String {
        try {
            val key = generateKey()
            val cipher = Cipher.getInstance(algorithm)
            val iv = IvParameterSpec(ByteArray(16))
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)

            val encrypted = cipher.doFinal(data.toByteArray())
            val encoded = Base64.getEncoder().encodeToString(encrypted)

            logger.debug("Data encrypted successfully")
            return encoded
        } catch (e: Exception) {
            logger.error("Encryption failed", e)
            throw SecurityException("Failed to encrypt data", e)
        }
    }

    /**
     * Decrypt sensitive data
     */
    fun decrypt(encryptedData: String): String {
        try {
            val key = generateKey()
            val cipher = Cipher.getInstance(algorithm)
            val iv = IvParameterSpec(ByteArray(16))
            cipher.init(Cipher.DECRYPT_MODE, key, iv)

            val decoded = Base64.getDecoder().decode(encryptedData)
            val decrypted = cipher.doFinal(decoded)

            logger.debug("Data decrypted successfully")
            return String(decrypted)
        } catch (e: Exception) {
            logger.error("Decryption failed", e)
            throw SecurityException("Failed to decrypt data", e)
        }
    }

    /**
     * Hash sensitive data (one-way)
     */
    fun hash(data: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashedBytes = digest.digest(data.toByteArray())
            val encoded = Base64.getEncoder().encodeToString(hashedBytes)

            logger.debug("Data hashed successfully")
            return encoded
        } catch (e: Exception) {
            logger.error("Hashing failed", e)
            throw SecurityException("Failed to hash data", e)
        }
    }

    /**
     * Verify if data matches a hash
     */
    fun verifyHash(
        data: String,
        hash: String,
    ): Boolean =
        try {
            val computedHash = hash(data)
            computedHash == hash
        } catch (e: Exception) {
            logger.error("Hash verification failed", e)
            false
        }

    /**
     * Mask sensitive data for display (e.g., credit card numbers)
     */
    fun mask(
        data: String,
        visibleChars: Int = 4,
    ): String {
        if (data.length <= visibleChars) {
            return "*".repeat(data.length)
        }

        val masked = "*".repeat(data.length - visibleChars) + data.takeLast(visibleChars)
        logger.debug("Data masked: {} characters hidden", data.length - visibleChars)
        return masked
    }

    /**
     * Generate encryption key from the configured key string
     */
    private fun generateKey(): SecretKeySpec {
        val keyBytes = encryptionKey.toByteArray().take(16).toByteArray()
        val paddedKey =
            if (keyBytes.size < 16) {
                keyBytes + ByteArray(16 - keyBytes.size)
            } else {
                keyBytes
            }
        return SecretKeySpec(paddedKey, keyAlgorithm)
    }

    /**
     * Sanitize data before encryption to prevent injection
     */
    fun sanitizeAndEncrypt(data: String): String {
        val sanitized =
            data
                .replace("\u0000", "") // Remove null characters
                .replace("\r", "") // Remove carriage returns
                .trim() // Remove leading/trailing whitespace

        return encrypt(sanitized)
    }
}
