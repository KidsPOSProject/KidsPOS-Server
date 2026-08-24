package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.NetworkHostService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@ExtendWith(SpringExtension::class)
@WebMvcTest(IpController::class)
@TestPropertySource(properties = ["local.server.port=8080"])
@DisplayName("IpController")
class IpControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var networkHostService: NetworkHostService

    @Test
    @DisplayName("ネットワーク情報を表示する")
    fun showsNetworkInformation() {
        val hosts = listOf(NetworkHostService.HostBean(name = "eth0", address = "192.168.1.5"))
        whenever(networkHostService.findHosts()).thenReturn(hosts)

        mockMvc
            .perform(get("/ip"))
            .andExpect(status().isOk)
            .andExpect(view().name("ip/index"))
            .andExpect(model().attribute("title", "ネットワーク情報"))
            .andExpect(model().attribute("hosts", hosts))
            .andExpect(model().attribute("port", "8080"))
    }
}
