package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.entity.StoreEntity
import info.nukoneko.kidspos.server.service.StoreService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@ExtendWith(SpringExtension::class)
@WebMvcTest(StoresController::class)
@DisplayName("StoresController")
class StoresControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var storeService: StoreService

    @Test
    @DisplayName("店舗一覧を表示する")
    fun showsStoreList() {
        val stores = listOf(StoreEntity(id = 1, name = "本店", printerUri = "192.168.1.10"))
        whenever(storeService.findAll()).thenReturn(stores)

        mockMvc
            .perform(get("/stores"))
            .andExpect(status().isOk)
            .andExpect(view().name("stores/index"))
            .andExpect(model().attribute("title", "店舗管理"))
            .andExpect(model().attribute("data", stores))
    }

    @Test
    @DisplayName("店舗作成画面を表示する")
    fun showsNewStoreForm() {
        mockMvc
            .perform(get("/stores/new"))
            .andExpect(status().isOk)
            .andExpect(view().name("stores/new"))
            .andExpect(model().attribute("title", "店舗作成"))
    }

    @Test
    @DisplayName("店舗編集画面を表示する")
    fun showsEditForm() {
        val store = StoreEntity(id = 1, name = "本店", printerUri = "192.168.1.10")
        whenever(storeService.findStore(eq(1))).thenReturn(store)

        mockMvc
            .perform(get("/stores/1/edit"))
            .andExpect(status().isOk)
            .andExpect(view().name("stores/edit"))
            .andExpect(model().attribute("title", "店舗編集"))
            .andExpect(model().attribute("store", store))
    }

    @Test
    @DisplayName("存在しない店舗の編集は一覧へ戻す")
    fun redirectsWhenStoreIsMissing() {
        whenever(storeService.findStore(eq(999))).thenReturn(null)

        mockMvc
            .perform(get("/stores/999/edit"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/stores"))
    }

    @Test
    @DisplayName("店舗を削除して一覧へ戻す")
    fun deletesStore() {
        mockMvc
            .perform(post("/stores/1/delete"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/stores"))

        verify(storeService).delete(eq(1))
    }
}
