package info.nukoneko.kidspos.server.config

import info.nukoneko.kidspos.server.service.ClientClockSynchronizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@ExtendWith(MockitoExtension::class)
class ClientTimeSyncFilterTest {
    @Mock
    private lateinit var clientClockSynchronizer: ClientClockSynchronizer

    @Test
    fun `ヘッダーの値を同期処理に渡す`() {
        val request = MockHttpServletRequest("GET", "/api/item")
        request.addHeader(ClientTimeSyncFilter.CLIENT_TIME_HEADER, "1700000000000")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        ClientTimeSyncFilter(clientClockSynchronizer).doFilter(request, response, chain)

        verify(clientClockSynchronizer).onClientTime("1700000000000")
        assertEquals(request, chain.request)
    }

    @Test
    fun `ヘッダーが無くてもリクエストは継続する`() {
        val request = MockHttpServletRequest("GET", "/api/item")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        ClientTimeSyncFilter(clientClockSynchronizer).doFilter(request, response, chain)

        verify(clientClockSynchronizer).onClientTime(null)
        assertEquals(request, chain.request)
    }
}
