package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PasswordHasher")
class PasswordHasherTest {
    private val hasher = PasswordHasher()

    @Test
    fun `ハッシュ化したパスワードは元のパスワードと一致する`() {
        val hash = hasher.hash("kidspos1234")

        assertTrue(hasher.matches("kidspos1234", hash))
    }

    @Test
    fun `異なるパスワードは一致しない`() {
        val hash = hasher.hash("kidspos1234")

        assertFalse(hasher.matches("kidspos1235", hash))
    }

    @Test
    fun `同じパスワードでもソルトが異なるためハッシュは毎回変わる`() {
        val first = hasher.hash("kidspos1234")
        val second = hasher.hash("kidspos1234")

        assertNotEquals(first, second)
        assertTrue(hasher.matches("kidspos1234", first))
        assertTrue(hasher.matches("kidspos1234", second))
    }

    @Test
    fun `保存形式はアルゴリズムと反復回数とソルトとハッシュの4要素`() {
        val parts = hasher.hash("kidspos1234").split(":")

        assertEquals(4, parts.size)
        assertEquals("pbkdf2-sha256", parts[0])
        assertEquals(100_000, parts[1].toInt())
    }

    @Test
    fun `保存値が壊れている場合は一致しない`() {
        assertFalse(hasher.matches("kidspos1234", ""))
        assertFalse(hasher.matches("kidspos1234", "kidspos1234"))
        assertFalse(hasher.matches("kidspos1234", "pbkdf2-sha256:100000:not-base64!!:x"))
        assertFalse(hasher.matches("kidspos1234", "md5:100000:c2FsdA==:aGFzaA=="))
        assertFalse(hasher.matches("kidspos1234", "pbkdf2-sha256:0:c2FsdA==:aGFzaA=="))
    }

    @Test
    fun `空文字や日本語を含むパスワードも扱える`() {
        val emptyHash = hasher.hash("")
        val japaneseHash = hasher.hash("かぎ1234")

        assertTrue(hasher.matches("", emptyHash))
        assertFalse(hasher.matches("a", emptyHash))
        assertTrue(hasher.matches("かぎ1234", japaneseHash))
    }
}
