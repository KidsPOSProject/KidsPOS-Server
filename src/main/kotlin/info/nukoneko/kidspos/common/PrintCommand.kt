package info.nukoneko.kidspos.common

import java.nio.charset.Charset

class PrintCommand(private val textEncoding: Charset) : Commander() {

    init {
        // 初期化
        writeBytes(0x1B, 0x40)

        /// スタイル調整
        // 改行量
        writeBytes(0x1B, 0x33, 0x28)
    }

    /***
     * 線を印字
     */
    fun drawLine() {
        writeTextLine("────────────────────────") // 24文字
    }

    /**
     *
     */

    /***
     * CODE39バーコード印字
     * 最後に改行もする
     */
    fun drawBarcode(code: String) {
        writeBytes(0x1D, 0x68, 0x50) // 高さ設定 80(0x50) * 1dot(0.125mm) = 80dot(10mm)
        writeBytes(0x1D, 0x67, 0x02) // モジュール幅設定 3(0x03) * 1dot(0.125mm) = 3dot ＜2から6＞
        writeBytes(0x1D, 0x48, 0x00) // 解説文字印字（印字しない）
        writeBytes(0x1D, 0x6B, 0x45) // CODE39指定
        val codeByteArray = code.toByteArray(textEncoding)
        writeBytes(codeByteArray.size.toByte())
        writeByteArray(codeByteArray)
        writeTextLine(code.substring(1, code.length - 1))
        newLine()
    }

    /***
     * QRコードを印字する
     * 対応している機種のみ
     */
    fun drawQR(code: String) {
        writeBytes(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x05)
        writeBytes(0x1D, 0x28, 0x6B, (code.length + 3).toByte(), 0, 0x31, 0x50, 0x30)
        writeByteArray(code.toByteArray(textEncoding))
        writeBytes(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30)
    }

    /***
     * テキストを印字する
     */
    fun writeText(text: String) {
        writeByteArray(text.toByteArray(textEncoding))
    }

    /***
     * テキストを印字し改行する
     */
    fun writeTextLine(text: String) {
        writeByteArray(text.toByteArray(textEncoding))
        newLine()
    }

    fun writeTextBold(text: String) {
        writeBytes(0x1B, 0x45, 0x01)
        writeByteArray(text.toByteArray(textEncoding))
        writeBytes(0x1B, 0x45, 0x00)
    }

    /***
     * 改行する
     */
    fun newLine() {
        writeBytes(0x0A)
    }

    /***
     * 印字文字の位置揃え
     */
    fun setGravity(direction: Direction) {
        when (direction) {
            Direction.LEFT -> writeBytes(0x1B, 0x61, 0x00)
            Direction.CENTER -> writeBytes(0x1B, 0x61, 0x01)
            Direction.RIGHT -> writeBytes(0x1B, 0x61, 0x02)
        }
    }

    /***
     * 裁断する
     */
    fun cut() {
        // 裁断
        writeBytes(0x1B, 0x64, 0x04) //下部余白の調整は ここの3バイト目
        writeBytes(0x1D, 0x56, 0x30, 0x0)
    }

    enum class Direction {
        LEFT, CENTER, RIGHT
    }
}