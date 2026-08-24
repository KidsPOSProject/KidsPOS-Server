package info.nukoneko.kidspos.server.service

import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import org.slf4j.LoggerFactory
import java.io.File

/**
 * PDF に日本語を出力するためのフォントを提供する。
 *
 * iText の PdfFont は PdfDocument をまたいで再利用できないため、
 * 読み込んだフォントデータだけを保持して都度 PdfFont を生成する。
 */
object JapanesePdfFont {
    private val logger = LoggerFactory.getLogger(JapanesePdfFont::class.java)

    private val CLASSPATH_CANDIDATES =
        listOf(
            "/fonts/japanese.ttf",
            "/fonts/ipag.ttf",
            "/fonts/NotoSansCJKjp-Regular.otf",
        )

    private val SYSTEM_CANDIDATES =
        listOf(
            "/System/Library/Fonts/Hiragino Sans GB.ttc",
            "/System/Library/Fonts/AppleSDGothicNeo.ttc",
            "C:/Windows/Fonts/meiryo.ttc",
            "/usr/share/fonts/opentype/ipafont-gothic/ipag.ttf",
            "/usr/share/fonts/truetype/fonts-japanese-gothic.ttf",
        )

    private val fontBytes: ByteArray? by lazy { loadFontBytes() }

    fun create(): PdfFont =
        try {
            val bytes = fontBytes
            if (bytes != null) {
                PdfFontFactory.createFont(bytes, "Identity-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED)
            } else {
                logger.warn("日本語フォントが見つからないため標準フォントを使用します。日本語は表示されません")
                PdfFontFactory.createFont(StandardFonts.HELVETICA)
            }
        } catch (e: Exception) {
            logger.error("日本語フォントの読み込みに失敗しました", e)
            PdfFontFactory.createFont(StandardFonts.HELVETICA)
        }

    private fun loadFontBytes(): ByteArray? {
        CLASSPATH_CANDIDATES.forEach { path ->
            javaClass.getResourceAsStream(path)?.use { stream ->
                logger.info("日本語フォントを読み込みました: {}", path)
                return stream.readBytes()
            }
        }

        SYSTEM_CANDIDATES.forEach { path ->
            val file = File(path)
            if (file.exists()) {
                logger.info("日本語フォントを読み込みました: {}", path)
                return file.readBytes()
            }
        }

        return null
    }
}
