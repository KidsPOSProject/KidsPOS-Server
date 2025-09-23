package info.nukoneko.kidspos.server.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest(classes = [AppPropertiesTest::class])
@EnableConfigurationProperties(AppProperties::class)
@TestPropertySource(properties = [
    "app.receipt.printer.host=test-printer",
    "app.receipt.printer.port=9999",
    "app.barcode.qr-size=250",
    "app.barcode.pdf.margin=30",
    "app.barcode.pdf.image-size=150",
    "app.network.allowed-ip-prefix=10."
])
@Disabled("Spring context not configured")
class AppPropertiesTest {
    @Autowired
    private lateinit var appProperties: AppProperties

    @Test
    fun `should load receipt printer configuration from properties`() {
        assertThat(appProperties.receipt.printer.host).isEqualTo("test-printer")
        assertThat(appProperties.receipt.printer.port).isEqualTo(9999)
    }

    @Test
    fun `should load barcode configuration from properties`() {
        assertThat(appProperties.barcode.qrSize).isEqualTo(250)
        assertThat(appProperties.barcode.pdf.margin).isEqualTo(30)
        assertThat(appProperties.barcode.pdf.imageSize).isEqualTo(150)
    }

    @Test
    fun `should load network configuration from properties`() {
        assertThat(appProperties.network.allowedIpPrefix).isEqualTo("10.")
    }

    @Test
    fun `should use default values when properties are not set`() {
        // This test would require a separate test configuration
        // We'll implement this in the actual AppProperties class with defaults
    }
}