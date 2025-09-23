package info.nukoneko.kidspos.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConstantsTest {

    @Test
    fun `should have barcode constants defined`() {
        assertThat(Constants.Barcode.SUFFIX_LENGTH).isEqualTo(3)
        assertThat(Constants.Barcode.MIN_LENGTH).isEqualTo(4)
    }

    @Test
    fun `should have database constants defined`() {
        assertThat(Constants.Database.DEFAULT_PAGE_SIZE).isEqualTo(20)
        assertThat(Constants.Database.MAX_PAGE_SIZE).isEqualTo(100)
    }

    @Test
    fun `should have validation constants defined`() {
        assertThat(Constants.Validation.NAME_MAX_LENGTH).isEqualTo(255)
        assertThat(Constants.Validation.BARCODE_PATTERN).isEqualTo("^[0-9]{4,}$")
    }

    @Test
    fun `should have print command constants defined`() {
        assertThat(Constants.PrintCommand.ESC_CODE).isEqualTo(0x1B.toChar())
        assertThat(Constants.PrintCommand.GS_CODE).isEqualTo(0x1D.toChar())
        assertThat(Constants.PrintCommand.RESET).isEqualTo("${0x1B.toChar()}@")
    }
}