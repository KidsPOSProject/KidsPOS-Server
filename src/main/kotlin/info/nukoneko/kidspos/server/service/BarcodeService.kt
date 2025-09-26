package info.nukoneko.kidspos.server.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.oned.Code39Writer
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import info.nukoneko.kidspos.server.entity.ItemEntity
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*

@Service
class BarcodeService {
    companion object {
        // レイアウト仕様に基づく定数
        private const val LABEL_COLUMNS = 4
        private const val LABEL_ROWS = 11
        private const val PAGE_MARGIN_TOP = 8.0f
        private const val PAGE_MARGIN_LEFT = 8.4f
        private const val CELL_WIDTH = 48.3f
        private const val CELL_HEIGHT = 25.4f
    }

    /**
     * 選択された商品のバーコードPDFを生成（1商品につき1ページ44枚）
     * @param items 商品リスト
     * @param showBorders 罫線を表示するかどうか（デフォルト: false）
     */
    fun generateBarcodePdf(
        items: List<ItemEntity>,
        showBorders: Boolean = false,
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writer = PdfWriter(outputStream)
        val pdf = PdfDocument(writer)
        val document = Document(pdf, PageSize.A4)

        // 日本語フォントの設定
        val font =
            try {
                // まず、クラスパスからフォントを探す（JARに含まれている場合）
                val fontInputStream =
                    this.javaClass.getResourceAsStream("/fonts/japanese.ttf")
                        ?: this.javaClass.getResourceAsStream("/fonts/ipag.ttf")
                        ?: this.javaClass.getResourceAsStream("/fonts/NotoSansCJKjp-Regular.otf")

                if (fontInputStream != null) {
                    // クラスパスからフォントを読み込み
                    val fontBytes = fontInputStream.use { it.readBytes() }
                    PdfFontFactory.createFont(fontBytes, "Identity-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED)
                } else {
                    // クラスパスにフォントがない場合、システムフォントを探す
                    val fontPath =
                        when {
                            // macOSのヒラギノフォント
                            java.io.File("/System/Library/Fonts/Hiragino Sans GB.ttc").exists() ->
                                "/System/Library/Fonts/Hiragino Sans GB.ttc"
                            // macOSのApple SDゴシック
                            java.io.File("/System/Library/Fonts/AppleSDGothicNeo.ttc").exists() ->
                                "/System/Library/Fonts/AppleSDGothicNeo.ttc"
                            // Windowsのメイリオ
                            java.io.File("C:/Windows/Fonts/meiryo.ttc").exists() ->
                                "C:/Windows/Fonts/meiryo.ttc"
                            // LinuxのIPAフォント
                            java.io.File("/usr/share/fonts/opentype/ipafont-gothic/ipag.ttf").exists() ->
                                "/usr/share/fonts/opentype/ipafont-gothic/ipag.ttf"

                            else -> null
                        }

                    if (fontPath != null) {
                        PdfFontFactory.createFont(fontPath, "Identity-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED)
                    } else {
                        // フォントが見つからない場合は標準フォントを使用（日本語は表示されません）
                        println("警告: 日本語フォントが見つかりません。src/main/resources/fonts/にフォントファイルを配置してください。")
                        PdfFontFactory.createFont(StandardFonts.HELVETICA)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // エラー時のフォールバック
                PdfFontFactory.createFont(StandardFonts.HELVETICA)
            }

        // マージンをミリメートル単位で設定（仕様書準拠・変更不可）
        document.setMargins(
            PAGE_MARGIN_TOP * 2.835f, // 上: 8mm
            20f, // 右: 固定値
            20f, // 下: 固定値
            PAGE_MARGIN_LEFT * 2.835f, // 左: 8.4mm
        )

        // 各商品ごとに1ページ作成
        items.forEachIndexed { itemIndex, item ->
            if (itemIndex > 0) {
                document.add(
                    com.itextpdf.layout.element
                        .AreaBreak(),
                )
            }

            // グリッドレイアウトでバーコードを配置（同じ商品を44枚）
            // タイトルは削除してスペースを最大限活用
            // テーブル全体の幅を正確に設定（4列 × 48.3mm = 193.2mm）
            val tableWidth = LABEL_COLUMNS * CELL_WIDTH * 2.835f // ポイント単位
            val table =
                Table(LABEL_COLUMNS)
                    .setWidth(tableWidth)
                    .setFixedLayout()

            // 4列×11行 = 44枚のラベルを同じ商品で埋める
            for (row in 0 until LABEL_ROWS) {
                for (col in 0 until LABEL_COLUMNS) {
                    table.addCell(createBarcodeCell(item, showBorders, font))
                }
            }

            document.add(table)
        }

        document.close()
        return outputStream.toByteArray()
    }

    /**
     * バーコードセルを作成（仕様書準拠）
     * @param item 商品エンティティ
     * @param showBorders 罫線を表示するかどうか
     */
    private fun createBarcodeCell(
        item: ItemEntity,
        showBorders: Boolean = false,
        font: PdfFont,
    ): com.itextpdf.layout.element.Cell {
        val cellWidthPt = CELL_WIDTH * 2.835f // 48.3mm → ポイント
        val cellHeightPt = CELL_HEIGHT * 2.835f // 25.4mm → ポイント

        val cell =
            com.itextpdf.layout.element
                .Cell()
                .setPadding(0f) // パディングを0に
                .setWidth(cellWidthPt) // セル幅を正確に設定
                .setHeight(cellHeightPt) // セル高さを正確に設定
                .setBorder(
                    if (showBorders) {
                        com.itextpdf.layout.borders
                            .SolidBorder(0.5f)
                    } else {
                        com.itextpdf.layout.borders.Border.NO_BORDER
                    },
                )

        // セル内のレイアウト用テーブル（単純化）
        val innerTable =
            Table(1)
                .setWidth(UnitValue.createPercentValue(100f))
                .setPadding(0f)

        // 商品名（中央配置）
        innerTable.addCell(
            com.itextpdf.layout.element
                .Cell()
                .add(
                    Paragraph(item.name)
                        .setFont(font)
                        .setFontSize(10f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginTop(3.0f * 2.835f),
                ) // 上部に適度な余白
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setPadding(0f),
        )

        // バーコード画像（中央配置）
        val barcodeImage = generateCode39(item.barcode)
        if (barcodeImage != null) {
            // 元の仕様書の比率を適用
            val originalWidth = 300f
            val originalHeight = 100f
            val image =
                Image(ImageDataFactory.create(barcodeImage))
                    .setWidth(originalWidth * 0.4f) // 横幅を拡大
                    .setHeight(originalHeight * 0.25f) // 縦幅を拡大
                    .setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER)
                    .setMarginTop(2.0f * 2.835f)

            innerTable.addCell(
                com.itextpdf.layout.element
                    .Cell()
                    .add(image)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(0f),
            )

            // バーコード番号を表示
            innerTable.addCell(
                com.itextpdf.layout.element
                    .Cell()
                    .add(
                        Paragraph(item.barcode)
                            .setFont(font)
                            .setFontSize(8f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginTop(1.0f * 2.835f),
                    ).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(0f),
            )
        } else {
            innerTable.addCell(
                com.itextpdf.layout.element
                    .Cell()
                    .add(
                        Paragraph("バーコード生成エラー")
                            .setFontSize(7f),
                    ).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .setPadding(0f),
            )
        }

        cell.add(innerTable)
        return cell
    }

    private fun generateCode39(content: String): ByteArray? =
        try {
            val code39Writer = Code39Writer()
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.MARGIN] = 1

            // CODE39は数字、大文字英字、いくつかの特殊文字のみ対応
            // 小文字は自動的に大文字に変換される
            val bitMatrix = code39Writer.encode(content.uppercase(), BarcodeFormat.CODE_39, 300, 100, hints)
            val outputStream = ByteArrayOutputStream()
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
            outputStream.toByteArray()
        } catch (e: WriterException) {
            null
        }
}
