package info.nukoneko.kidspos.receipt

import info.nukoneko.kidspos.common.PrintCommand
import info.nukoneko.kidspos.common.toAllEm
import info.nukoneko.kidspos.common.toEm
import java.io.IOException
import java.net.Socket
import java.nio.charset.Charset
import java.text.SimpleDateFormat

class ReceiptPrinter(
    private val ipOrHost: String,
    private val port: Int,
    detail: ReceiptDetail,
) {
    private val command = PrintCommand(Charset.forName("SJIS"))

    init {
        // タイトル
        command.setGravity(PrintCommand.Direction.CENTER)
        command.writeBytes(0x1C, 0x70, 0x01, 0x00)
        command.writeTextLine(dateFormat.format(detail.createdAt))
        command.newLine()

        // 店舗名
        command.setGravity(PrintCommand.Direction.LEFT)
        if (!detail.storeName.isNullOrEmpty()) {
            command.writeTextLine("店舗名：${detail.storeName.toAllEm()}")
        }
        command.drawLine()

        command.setGravity(PrintCommand.Direction.CENTER)

        // 商品
        detail.items.forEach {
            writeKV(it.name, it.price)
        }
        command.drawLine()

        // 小計
        val total = detail.items.sumOf { it.price }
        writeKV("ごうけい", total)
        writeKV("あずかり", detail.deposit)
        writeKV("おつり", detail.deposit - total)
        command.drawLine()

        // / Footer

        // 注釈
        command.newLine()
        command.writeTextLine("印字保護のため、こちらの面を")
        command.writeTextLine("内側に折って保管してください")
        command.newLine()

        // バーコード
        if (detail.transactionId != null) {
            command.drawBarcode(detail.transactionId)
        }

        command.cut()
    }

    /**
     * 商品1行を印字する
     * safe** は 古いプリンタのための対応 JISだと 半角文字が干渉しておかしくなる
     */
    private fun writeKV(
        key: String,
        value: Int,
    ) {
        val safeKey = key.toAllEm()
        val safePrefix = "リバー"
        val safeValue = value.toEm()

        val keyLength = safeKey.length
        val valueLength = safeValue.length
        val prefixLength = safePrefix.length

        val needSpaceNum =
            MAX_ROW_TEXT_NUM - keyLength - valueLength - prefixLength
        val safeSpace = "　".repeat(needSpaceNum)

//        command.writeBytes(0x1B, 0x24, 0x18, 0x00) // 先頭スペース
        command.writeText(safeKey + safeSpace + safeValue + safePrefix)
        command.newLine()
    }

    @Throws(IOException::class)
    fun print() {
        Socket().apply {
            soTimeout = 5000 // 5秒でタイムアウト
            connect(java.net.InetSocketAddress(ipOrHost, port), 5000) // 接続タイムアウト5秒
        }.use { socket ->
            socket.getOutputStream().use {
                it.write(command.build())
            }
        }
    }

    companion object {
        private const val MAX_ROW_TEXT_NUM = 20
        private val dateFormat =
            SimpleDateFormat("yyyy年MM月dd日(E) HH時mm分ss秒")
    }
}
