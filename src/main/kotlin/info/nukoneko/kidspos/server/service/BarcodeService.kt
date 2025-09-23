package info.nukoneko.kidspos.server.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.oned.Code39Writer
import com.itextpdf.io.image.ImageDataFactory
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

/**
 * Service for generating barcodes for items
 *
 * Provides functionality to generate printable PDF documents containing
 * CODE39 barcodes for product identification. Uses iText PDF library for document generation
 * and ZXing library for CODE39 barcode generation. The service creates formatted
 * PDF reports suitable for printing barcode labels.
 *
 * Key responsibilities:
 * - Generating CODE39 barcodes from product barcode strings
 * - Creating PDF documents with barcode grids
 * - Formatting product information alongside barcodes
 * - Handling barcode generation errors gracefully
 *
 * Technical implementation:
 * - Uses ZXing Code39Writer for CODE39 barcode matrix generation
 * - Uses iText PDF library for document layout and formatting
 * - Generates A4-sized PDF documents with proper margins
 * - Creates table-based layout for organized barcode presentation
 * - CODE39 supports numbers, uppercase letters, and some special characters
 */
@Service
class BarcodeService {

    fun generateBarcodePdf(items: List<ItemEntity>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writer = PdfWriter(outputStream)
        val pdf = PdfDocument(writer)
        val document = Document(pdf, PageSize.A4)

        document.setMargins(20f, 20f, 20f, 20f)

        val title = Paragraph("商品バーコード一覧")
            .setFontSize(18f)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        document.add(title)

        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))

        for (item in items) {
            val barcodeImage = generateCode39(item.barcode)

            table.addCell(
                Paragraph(item.id.toString())
                    .setTextAlignment(TextAlignment.CENTER)
            )

            if (barcodeImage != null) {
                table.addCell(
                    Image(ImageDataFactory.create(barcodeImage))
                        .setWidth(150f)
                        .setHeight(50f)
                )
            } else {
                table.addCell(Paragraph("バーコード生成エラー"))
            }

            table.addCell(
                Paragraph("${item.name}\n${item.price}リバー")
                    .setTextAlignment(TextAlignment.LEFT)
            )
        }

        document.add(table)
        document.close()

        return outputStream.toByteArray()
    }

    private fun generateCode39(content: String): ByteArray? {
        return try {
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
}