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
        const val TOTAL_LENGTH = 10
        const val ID_LENGTH = 6

        // バーコード種別コード
        const val TYPE_STAFF = "00"
        const val TYPE_ITEM = "01"
        const val TYPE_SALE = "02"

        // バーコードの開始・終了記号
        const val PREFIX = "A"
        const val SUFFIX = "A"
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

        // 独自バーコードフォーマット: A + 種別(2桁) + ID(6桁) + A
        // 種別: 00(STAFF), 01(ITEM), 02(SALE)
        // 例: A01000001A (商品ID:1)
        const val BARCODE_PATTERN = "^A(00|01|02)\\d{6}A$"
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
