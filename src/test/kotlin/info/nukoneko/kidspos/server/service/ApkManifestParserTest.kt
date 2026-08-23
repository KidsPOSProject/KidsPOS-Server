package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.domain.exception.InvalidFileException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.InputStream

class ApkManifestParserTest {
    private val parser = ApkManifestParser()

    private fun fixture(name: String): InputStream =
        checkNotNull(javaClass.getResourceAsStream("/apk/$name")) { "fixture not found: $name" }

    @Test
    fun `UTF-16文字列プールのAPKからバージョン情報を読み取る`() {
        val info = fixture("valid-utf16.apk").use { parser.parse(it) }

        assertThat(info.versionName).isEqualTo("1.2.3")
        assertThat(info.versionCode).isEqualTo(10203)
    }

    @Test
    fun `UTF-8文字列プールのAPKからバージョン情報を読み取る`() {
        val info = fixture("valid-utf8.apk").use { parser.parse(it) }

        assertThat(info.versionName).isEqualTo("2.0.0-beta")
        assertThat(info.versionCode).isEqualTo(20000)
    }

    @Test
    fun `UTF-8プールのマルチバイト文字を含むバージョン名を読み取る`() {
        val info = fixture("valid-utf8-multibyte.apk").use { parser.parse(it) }

        assertThat(info.versionName).isEqualTo("1.0.0-テスト")
        assertThat(info.versionCode).isEqualTo(100)
    }

    @Test
    fun `文字列として格納されたバージョンコードを数値として読み取る`() {
        val info = fixture("version-code-as-string.apk").use { parser.parse(it) }

        assertThat(info.versionName).isEqualTo("3.1.4")
        assertThat(info.versionCode).isEqualTo(30104)
    }

    @Test
    fun `バージョン名がリソース参照の場合は例外を投げる`() {
        assertThatThrownBy { fixture("version-name-reference.apk").use { parser.parse(it) } }
            .isInstanceOf(InvalidFileException::class.java)
    }

    @Test
    fun `バージョンコードが欠落している場合は例外を投げる`() {
        assertThatThrownBy { fixture("missing-version-code.apk").use { parser.parse(it) } }
            .isInstanceOf(InvalidFileException::class.java)
    }

    @Test
    fun `manifest要素が存在しない場合は例外を投げる`() {
        assertThatThrownBy { fixture("no-manifest-element.apk").use { parser.parse(it) } }
            .isInstanceOf(InvalidFileException::class.java)
    }

    @Test
    fun `AndroidManifest_xmlを含まないzipの場合は例外を投げる`() {
        assertThatThrownBy { fixture("no-manifest.apk").use { parser.parse(it) } }
            .isInstanceOf(InvalidFileException::class.java)
    }

    @Test
    fun `zipではないファイルの場合は例外を投げる`() {
        assertThatThrownBy { fixture("not-a-zip.apk").use { parser.parse(it) } }
            .isInstanceOf(InvalidFileException::class.java)
    }

    @Test
    fun `チャンクヘッダが壊れている場合は例外を投げる`() {
        assertThatThrownBy { fixture("broken-manifest.apk").use { parser.parse(it) } }
            .isInstanceOf(InvalidFileException::class.java)
    }

    @Test
    fun `空のファイルの場合は例外を投げる`() {
        assertThatThrownBy { ByteArray(0).inputStream().use { parser.parse(it) } }
            .isInstanceOf(InvalidFileException::class.java)
    }
}
