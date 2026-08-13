package info.nukoneko.kidspos.server.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * レスポンスにセキュリティヘッダーを付与する
 *
 * Spring Security を導入していないため、標準のセキュリティヘッダーは自前で付与する。
 */
@Component
class SecurityHeadersFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("X-Frame-Options", "DENY")
        response.setHeader("Referrer-Policy", "no-referrer")

        // TRACE はサーブレットコンテナが既定でリクエストをそのまま返すため、
        // Cross-Site Tracing の踏み台にならないようフィルタ層で遮断する
        if (request.method.equals("TRACE", ignoreCase = true)) {
            response.status = HttpServletResponse.SC_METHOD_NOT_ALLOWED
            return
        }

        filterChain.doFilter(request, response)
    }
}
