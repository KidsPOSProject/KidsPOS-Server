package info.nukoneko.kidspos.common

/**
 * Application-wide constants
 */
object Constants {

    /**
     * Barcode-related constants
     */
    object Barcode {
        const val SUFFIX_LENGTH = 3
        const val MIN_LENGTH = 4
    }

    /**
     * Database-related constants
     */
    object Database {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 100
    }

    /**
     * Validation patterns and limits
     */
    object Validation {
        const val NAME_MAX_LENGTH = 255
        const val BARCODE_PATTERN = "^[0-9]{4,}$"
        const val MAX_PRICE = 1000000
        const val MAX_QUANTITY = 9999
    }

    /**
     * Print command constants
     */
    object PrintCommand {
        const val ESC_CODE = 0x1B.toChar()
        const val GS_CODE = 0x1D.toChar()
        const val RESET = "${ESC_CODE}@"
        const val ALIGN_CENTER = "${ESC_CODE}a1"
        const val ALIGN_LEFT = "${ESC_CODE}a0"
        const val CUT_PAPER = "${GS_CODE}V0"
    }
}