package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.entity.ItemEntity
import info.nukoneko.kidspos.server.service.ItemService
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
@WebMvcTest(ItemsController::class)
@DisplayName("ItemsController")
class ItemsControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var itemService: ItemService

    @Test
    @DisplayName("商品一覧を表示する")
    fun showsItemList() {
        val items = listOf(ItemEntity(id = 1, barcode = "A01000001A", name = "りんご", price = 100))
        whenever(itemService.findAll()).thenReturn(items)

        mockMvc
            .perform(get("/items"))
            .andExpect(status().isOk)
            .andExpect(view().name("items/index"))
            .andExpect(model().attribute("title", "商品管理"))
            .andExpect(model().attribute("data", items))
    }

    @Test
    @DisplayName("商品作成画面を表示する")
    fun showsNewItemForm() {
        mockMvc
            .perform(get("/items/new"))
            .andExpect(status().isOk)
            .andExpect(view().name("items/new"))
            .andExpect(model().attribute("title", "商品作成"))
    }

    @Test
    @DisplayName("商品編集画面を表示する")
    fun showsEditForm() {
        val item = ItemEntity(id = 1, barcode = "A01000001A", name = "りんご", price = 100)
        whenever(itemService.findItem(eq(1))).thenReturn(item)

        mockMvc
            .perform(get("/items/1/edit"))
            .andExpect(status().isOk)
            .andExpect(view().name("items/edit"))
            .andExpect(model().attribute("title", "商品編集"))
            .andExpect(model().attribute("item", item))
    }

    @Test
    @DisplayName("存在しない商品の編集は一覧へ戻す")
    fun redirectsWhenItemIsMissing() {
        whenever(itemService.findItem(eq(999))).thenReturn(null)

        mockMvc
            .perform(get("/items/999/edit"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/items"))
    }

    @Test
    @DisplayName("商品を削除して一覧へ戻す")
    fun deletesItem() {
        mockMvc
            .perform(post("/items/1/delete"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/items"))

        verify(itemService).delete(eq(1))
    }
}
