package info.nukoneko.kidspos.receipt

import info.nukoneko.kidspos.common.PrintCommand
import java.io.IOException
import java.net.Socket
import java.nio.charset.Charset
import java.text.SimpleDateFormat

class ReceiptPrinter(private val ipOrHost: String,
                     private val port: Int,
                     private val detail: ReceiptDetail) {
    private val command = PrintCommand(Charset.forName("SJIS"))

    init {
        // タイトル
        command.setGravity(PrintCommand.Direction.CENTER)
        command.writeBytes(0x1C, 0x70, 0x01, 0x00)
        command.writeTextLine(dateFormat.format(detail.createdAt))
        command.newLine()

        // 店舗名・担当者
        command.setGravity(PrintCommand.Direction.LEFT)
        if (!detail.storeName.isNullOrEmpty()) {
            command.writeTextLine("店舗名: ${detail.storeName}")
        }
        if (!detail.staffName.isNullOrEmpty()) {
            command.writeTextLine(" 担当 : ${detail.staffName}")
        }
        command.drawLine()

        // 商品
        detail.items.forEach {
            writeKV(it.name, it.price)
        }
        command.drawLine()

        // 小計
        val total = detail.items.sumBy { it.price }
        writeKV("ごうけい", total)
        writeKV("あずかり", detail.deposit)
        writeKV(" おつり ", total - detail.deposit)
        command.drawLine()

        /// Footer
        command.setGravity(PrintCommand.Direction.CENTER)

        // 注釈
        command.newLine()
        command.writeTextLine("印字保護のため、こちらの面を")
        command.writeTextLine("内側に折って保管してください")
        command.newLine()

        // バーコード
        if (detail.transactionId != null) {
            command.drawBarcode(detail.transactionId)
        }
    }

    /**
     * 商品1行を印字する
     */
    private fun writeKV(key: String, value: Int, valuePrefix: String = "リバー") {
        val order = if (value == 0) {
            1
        } else {
            Math.log(value.toDouble()).toInt() + 1
        }

        command.writeBytes(0x1B, 0x24, 0x18, 0x00) // 先頭スペース
        command.writeText(key) // 商品名
        command.writeBytes(0x1B, 0x24, (226 - order * 12).toByte(), 0x01) // 値段とのスペース
        command.writeText("$value$valuePrefix")
        command.newLine()
    }

    @Throws(IOException::class)
    fun print() {
        Socket(ipOrHost, port).use { socket ->
            socket.getOutputStream().use {
                it.write(command.build())
            }
        }
    }

    companion object {
        private val dateFormat =
                SimpleDateFormat("yyyy年MM月dd日(E) HH時mm分ss秒")
    }
}