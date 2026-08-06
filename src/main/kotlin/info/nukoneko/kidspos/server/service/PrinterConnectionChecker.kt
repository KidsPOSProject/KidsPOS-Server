package info.nukoneko.kidspos.server.service

import org.springframework.stereotype.Component
import java.net.InetSocketAddress
import java.net.Socket

/**
 * プリンターへのTCP到達性確認コンポーネント
 */
@Component
class PrinterConnectionChecker {
    fun isReachable(
        host: String,
        port: Int,
        timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
    ): Boolean =
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMillis)
                true
            }
        } catch (e: Exception) {
            false
        }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 1_000
    }
}
