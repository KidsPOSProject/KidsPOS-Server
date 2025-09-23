package info.nukoneko.kidspos.server.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
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
 * Service for generating barcodes and QR codes for items
 *
 * Provides functionality to generate printable PDF documents containing
 * QR codes for product barcodes. Uses iText PDF library for document generation
 * and ZXing library for QR code generation. The service creates formatted
 * PDF reports suitable for printing barcode labels.
 *
 * Key responsibilities:
 * - Generating QR codes from barcode strings
 * - Creating PDF documents with barcode grids
 * - Formatting product information alongside barcodes
 * - Handling QR code generation errors gracefully
 *
 * Technical implementation:
 * - Uses ZXing QRCodeWriter for QR code matrix generation
 * - Uses iText PDF library for document layout and formatting
 * - Generates A4-sized PDF documents with proper margins
 * - Creates table-based layout for organized barcode presentation
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
            val barcodeImage = generateQRCode(item.barcode)

            table.addCell(
                Paragraph(item.id.toString())
                    .setTextAlignment(TextAlignment.CENTER)
            )

            if (barcodeImage != null) {
                table.addCell(
                    Image(ImageDataFactory.create(barcodeImage))
                        .setWidth(100f)
                        .setHeight(100f)
                )
            } else {
                table.addCell(Paragraph("QRコード生成エラー"))
            }

            table.addCell(
                Paragraph("${item.name}\n¥${item.price}")
                    .setTextAlignment(TextAlignment.LEFT)
            )
        }

        document.add(table)
        document.close()

        return outputStream.toByteArray()
    }

    private fun generateQRCode(content: String): ByteArray? {
        return try {
            val qrCodeWriter = QRCodeWriter()
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 2

            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200, hints)
            val outputStream = ByteArrayOutputStream()
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
            outputStream.toByteArray()
        } catch (e: WriterException) {
            null
        }
    }
}