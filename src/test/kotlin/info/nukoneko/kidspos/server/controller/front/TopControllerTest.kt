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
@WebMvcTest(TopController::class)
@TestPropertySource(properties = ["local.server.port=8080"])
@DisplayName("TopController")
class TopControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var networkHostService: NetworkHostService

    @Test
    @DisplayName("ダッシュボードにホスト一覧を表示する")
    fun showsDashboard() {
        val hosts = listOf(NetworkHostService.HostBean(name = "eth0", address = "192.168.1.5"))
        whenever(networkHostService.findHosts()).thenReturn(hosts)

        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("index"))
            .andExpect(model().attribute("title", "ダッシュボード"))
            .andExpect(model().attribute("hosts", hosts))
            .andExpect(model().attribute("port", "8080"))
    }

    @Test
    @DisplayName("ホストが見つからなくても画面を表示する")
    fun showsDashboardWithoutHosts() {
        whenever(networkHostService.findHosts()).thenReturn(emptyList())

        mockMvc
            .perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(view().name("index"))
            .andExpect(model().attribute("hosts", emptyList<NetworkHostService.HostBean>()))
    }
}
