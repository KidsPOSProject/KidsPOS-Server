package info.nukoneko.kidspos.server.service

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * パスワードのハッシュ化と照合を行うコンポーネント
 *
 * 保存形式は pbkdf2-sha256:反復回数:ソルト:ハッシュ（ソルトとハッシュはBase64）
 */
@Component
class PasswordHasher {
    fun hash(rawPassword: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        val derived = derive(rawPassword, salt, ITERATIONS)
        return listOf(
            ALGORITHM_TAG,
            ITERATIONS.toString(),
            encoder.encodeToString(salt),
            encoder.encodeToString(derived),
        ).joinToString(":")
    }

    fun matches(
        rawPassword: String,
        storedHash: String,
    ): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != PARTS_COUNT || parts[0] != ALGORITHM_TAG) {
            return false
        }
        val iterations = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return false
        val salt = decodeOrNull(parts[2]) ?: return false
        val expected = decodeOrNull(parts[3]) ?: return false
        return MessageDigest.isEqual(expected, derive(rawPassword, salt, iterations))
    }

    private fun decodeOrNull(value: String): ByteArray? =
        try {
            decoder.decode(value)
        } catch (e: IllegalArgumentException) {
            null
        }

    private fun derive(
        rawPassword: String,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val spec = PBEKeySpec(rawPassword.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ALGORITHM_TAG = "pbkdf2-sha256"
        const val ITERATIONS = 100_000
        const val SALT_LENGTH_BYTES = 16
        const val KEY_LENGTH_BITS = 256
        const val PARTS_COUNT = 4

        val secureRandom = SecureRandom()
        val encoder: Base64.Encoder = Base64.getEncoder()
        val decoder: Base64.Decoder = Base64.getDecoder()
    }
}
