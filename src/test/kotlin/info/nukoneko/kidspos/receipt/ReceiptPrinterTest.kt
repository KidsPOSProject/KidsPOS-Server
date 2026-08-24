package info.nukoneko.kidspos.receipt

import info.nukoneko.kidspos.server.entity.ItemEntity
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ServerSocket
import java.util.Date
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class ReceiptPrinterTest {
    private fun detail() =
        ReceiptDetail(
            items = listOf(ItemEntity(id = 1, barcode = "0123456789", name = "あめ", price = 100)),
            storeName = "テスト店",
            deposit = 500,
            transactionId = "0123456789",
            createdAt = Date(),
        )

    @Test
    fun `プリンターへ印字データを送信する`() {
        val received = ArrayBlockingQueue<ByteArray>(1)
        ServerSocket(0).use { server ->
            Thread {
                server.accept().use { socket ->
                    received.put(socket.getInputStream().readBytes())
                }
            }.apply { isDaemon = true }.start()

            ReceiptPrinter("127.0.0.1", server.localPort, detail()).print()

            val bytes = received.poll(WAIT_LIMIT_MILLIS, TimeUnit.MILLISECONDS)
            assertTrue(bytes != null && bytes.isNotEmpty(), "印字データが送信される")
        }
    }

    @Test
    fun `接続を拒否されたら待たずに失敗する`() {
        val closedPort = ServerSocket(0).use { it.localPort }

        val elapsedMillis =
            measureMillis {
                assertThrows(IOException::class.java) {
                    ReceiptPrinter("127.0.0.1", closedPort, detail()).print(TIMEOUT_MILLIS)
                }
            }

        assertTrue(elapsedMillis < WAIT_LIMIT_MILLIS, "すぐに失敗する: ${elapsedMillis}ms")
    }

    @Test
    fun `到達できないプリンターは指定したタイムアウトで打ち切る`() {
        val elapsedMillis =
            measureMillis {
                assertThrows(IOException::class.java) {
                    ReceiptPrinter(UNROUTABLE_HOST, 9100, detail()).print(TIMEOUT_MILLIS)
                }
            }

        assertTrue(elapsedMillis < WAIT_LIMIT_MILLIS, "指定したタイムアウトで打ち切られる: ${elapsedMillis}ms")
    }

    private fun measureMillis(block: () -> Unit): Long {
        val startedAt = System.nanoTime()
        block()
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
    }

    companion object {
        // RFC 5737 のドキュメント用アドレス。経路が無いため接続タイムアウトの経路を通る
        private const val UNROUTABLE_HOST = "192.0.2.1"
        private const val TIMEOUT_MILLIS = 300
        private const val WAIT_LIMIT_MILLIS = 5_000L
    }
}
