package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.ItemEntity
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
@DisplayName("BarcodePdfService")
class BarcodePdfServiceTest {
    @Mock
    private lateinit var itemService: ItemService

    @Mock
    private lateinit var barcodeService: BarcodeService

    private lateinit var service: BarcodePdfService

    private val items =
        listOf(
            ItemEntity(1, "A01000001A", "りんご", 100),
            ItemEntity(2, "A01000002A", "みかん", 150),
        )

    @BeforeEach
    fun setUp() {
        service = BarcodePdfService(itemService, barcodeService)
    }

    @Test
    @DisplayName("商品一覧が変わらなければPDFは一度しか生成されない")
    fun reusesCachedPdf() {
        whenever(itemService.findAll()).thenReturn(items)
        whenever(barcodeService.generateBarcodePdf(any(), eq(false))).thenReturn(byteArrayOf(1, 2, 3))

        val first = service.getAllItemsPdf(false)
        val second = service.getAllItemsPdf(false)

        assertArrayEquals(byteArrayOf(1, 2, 3), first)
        assertArrayEquals(first, second)
        verify(barcodeService, times(1)).generateBarcodePdf(any(), eq(false))
    }

    @Test
    @DisplayName("商品が追加されたらPDFを作り直す")
    fun regeneratesWhenItemAdded() {
        whenever(itemService.findAll())
            .thenReturn(items)
            .thenReturn(items + ItemEntity(3, "A01000003A", "ぶどう", 200))
        whenever(barcodeService.generateBarcodePdf(any(), eq(false)))
            .thenReturn(byteArrayOf(1))
            .thenReturn(byteArrayOf(2))

        val first = service.getAllItemsPdf(false)
        val second = service.getAllItemsPdf(false)

        assertArrayEquals(byteArrayOf(1), first)
        assertArrayEquals(byteArrayOf(2), second)
        verify(barcodeService, times(2)).generateBarcodePdf(any(), eq(false))
    }

    @Test
    @DisplayName("商品名や価格が変わったらPDFを作り直す")
    fun regeneratesWhenItemUpdated() {
        whenever(itemService.findAll())
            .thenReturn(items)
            .thenReturn(listOf(ItemEntity(1, "A01000001A", "りんご", 120), items[1]))
        whenever(barcodeService.generateBarcodePdf(any(), eq(false)))
            .thenReturn(byteArrayOf(1))
            .thenReturn(byteArrayOf(2))

        service.getAllItemsPdf(false)
        service.getAllItemsPdf(false)

        verify(barcodeService, times(2)).generateBarcodePdf(any(), eq(false))
    }

    @Test
    @DisplayName("罫線ありとなしは別々に保持される")
    fun cachesPerShowBordersFlag() {
        whenever(itemService.findAll()).thenReturn(items)
        whenever(barcodeService.generateBarcodePdf(any(), eq(false))).thenReturn(byteArrayOf(1))
        whenever(barcodeService.generateBarcodePdf(any(), eq(true))).thenReturn(byteArrayOf(2))

        assertArrayEquals(byteArrayOf(1), service.getAllItemsPdf(false))
        assertArrayEquals(byteArrayOf(2), service.getAllItemsPdf(true))
        assertArrayEquals(byteArrayOf(1), service.getAllItemsPdf(false))
        assertArrayEquals(byteArrayOf(2), service.getAllItemsPdf(true))

        verify(barcodeService, times(1)).generateBarcodePdf(any(), eq(false))
        verify(barcodeService, times(1)).generateBarcodePdf(any(), eq(true))
    }

    @Test
    @DisplayName("同時に要求されても生成は一度だけ行われる")
    fun generatesOnceUnderConcurrentAccess() {
        val threadCount = 8
        val start = CountDownLatch(1)
        val done = CountDownLatch(threadCount)

        whenever(itemService.findAll()).thenReturn(items)
        whenever(barcodeService.generateBarcodePdf(any(), eq(false))).thenAnswer {
            Thread.sleep(20)
            byteArrayOf(1)
        }

        repeat(threadCount) {
            Thread {
                start.await()
                service.getAllItemsPdf(false)
                done.countDown()
            }.also { it.isDaemon = true }.start()
        }

        start.countDown()
        assertTrue(done.await(10, TimeUnit.SECONDS))
        verify(barcodeService, times(1)).generateBarcodePdf(any(), eq(false))
    }

    @Test
    @DisplayName("ウォームアップ前後でキャッシュの有無が変わる")
    fun warmUpFillsCache() {
        whenever(itemService.findAll()).thenReturn(items)
        whenever(barcodeService.generateBarcodePdf(any(), eq(false))).thenReturn(byteArrayOf(1))

        assertFalse(service.isCached(false))
        service.warmUp()
        assertTrue(service.isCached(false))
    }

    @Test
    @DisplayName("ウォームアップが失敗しても例外を投げない")
    fun warmUpSwallowsFailure() {
        whenever(itemService.findAll()).thenThrow(IllegalStateException("db down"))

        service.warmUp()

        assertFalse(service.isCached(false))
    }

    @Test
    @DisplayName("設定で無効にすると起動時ウォームアップを行わない")
    fun skipsStartupWarmUpWhenDisabled() {
        val disabled = BarcodePdfService(itemService, barcodeService, warmUpOnStartup = false)

        disabled.warmUpInBackground()

        assertFalse(disabled.isCached(false))
        verifyNoInteractions(itemService, barcodeService)
    }

    @Test
    @DisplayName("設定が有効なら起動時にキャッシュが作られる")
    fun warmsUpOnStartupWhenEnabled() {
        whenever(itemService.findAll()).thenReturn(items)
        whenever(barcodeService.generateBarcodePdf(any(), eq(false))).thenReturn(byteArrayOf(1))

        service.warmUpInBackground()

        val deadline = System.currentTimeMillis() + 5_000
        while (!service.isCached(false) && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        assertTrue(service.isCached(false))
    }
}
