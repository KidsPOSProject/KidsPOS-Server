package info.nukoneko.kidspos.server.config

import info.nukoneko.kidspos.server.service.ClientClockSynchronizer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

/**
 * クライアントが申告した時刻をサーバー時刻の同期に回す
 *
 * 管理画面やレジアプリが通信するだけでサーバーの時刻が合うようにするため、
 * 専用の同期操作ではなく通常のリクエストヘッダーから拾う。
 */
class ClientTimeSyncFilter(
    private val clientClockSynchronizer: ClientClockSynchronizer,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        clientClockSynchronizer.onClientTime(request.getHeader(CLIENT_TIME_HEADER))
        filterChain.doFilter(request, response)
    }

    companion object {
        const val CLIENT_TIME_HEADER = "X-Client-Time"
    }
}
