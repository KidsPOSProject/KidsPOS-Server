package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.LogService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@ExtendWith(SpringExtension::class)
@WebMvcTest(LogsController::class)
@DisplayName("LogsController")
class LogsControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var logService: LogService

    @Test
    @DisplayName("ログ画面を表示する")
    fun showsLogPage() {
        whenever(logService.capacity()).thenReturn(500)

        mockMvc
            .perform(get("/logs"))
            .andExpect(status().isOk)
            .andExpect(view().name("logs/index"))
            .andExpect(model().attribute("title", "ログ"))
            .andExpect(model().attribute("capacity", 500))
            .andExpect(model().attribute("levels", listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR")))
    }
}
