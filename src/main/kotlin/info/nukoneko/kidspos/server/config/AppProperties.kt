package info.nukoneko.kidspos.server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val receipt: ReceiptProperties = ReceiptProperties(),
    val barcode: BarcodeProperties = BarcodeProperties(),
    val network: NetworkProperties = NetworkProperties()
) {
    data class ReceiptProperties(
        val printer: PrinterProperties = PrinterProperties()
    ) {
        data class PrinterProperties(
            val host: String = "localhost",
            val port: Int = 9100
        )
    }

    data class BarcodeProperties(
        val qrSize: Int = 200,
        val pdf: PdfProperties = PdfProperties()
    ) {
        data class PdfProperties(
            val margin: Float = 20f,
            val imageSize: Float = 100f
        )
    }

    data class NetworkProperties(
        val allowedIpPrefix: String = "192."
    )
}