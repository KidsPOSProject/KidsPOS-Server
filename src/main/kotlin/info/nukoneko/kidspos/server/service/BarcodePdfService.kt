package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.ItemEntity
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * バーコードPDFを保持するサービス
 *
 * 商品一覧が変わらない限り生成済みのバイト列を返す。
 * 起動直後と商品変更後にバックグラウンドで生成しておくことで、
 * 端末側のリクエストがタイムアウトする事態を避ける。
 */
@Service
class BarcodePdfService(
    private val itemService: ItemService,
    private val barcodeService: BarcodeService,
    @Value("\${app.barcode.pdf.warmup-on-startup:true}")
    private val warmUpOnStartup: Boolean = true,
) {
    private val logger = LoggerFactory.getLogger(BarcodePdfService::class.java)
    private val cache = ConcurrentHashMap<Boolean, CachedPdf>()
    private val locks = ConcurrentHashMap<Boolean, Any>()
    private val selectedLock = Any()

    private val selectedCache: MutableMap<String, ByteArray> =
        Collections.synchronizedMap(
            object : LinkedHashMap<String, ByteArray>(SELECTED_CACHE_CAPACITY, LOAD_FACTOR, true) {
                override fun removeEldestEntry(eldest: Map.Entry<String, ByteArray>): Boolean = size > SELECTED_CACHE_CAPACITY
            },
        )

    private val warmUpExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "barcode-pdf-warmup").apply { isDaemon = true }
        }
    }

    fun getAllItemsPdf(showBorders: Boolean = false): ByteArray {
        val items = itemService.findAll()
        val signature = signatureOf(items, showBorders)

        cache[showBorders]?.let { cached ->
            if (cached.signature == signature) {
                logger.debug("Reusing cached barcode PDF (showBorders={})", showBorders)
                return cached.bytes
            }
        }

        return synchronized(locks.computeIfAbsent(showBorders) { Any() }) {
            cache[showBorders]?.let { cached ->
                if (cached.signature == signature) {
                    return@synchronized cached.bytes
                }
            }

            val startedAt = System.currentTimeMillis()
            val bytes = barcodeService.generateBarcodePdf(items, showBorders)
            cache[showBorders] = CachedPdf(signature, bytes)
            logger.info(
                "Generated barcode PDF for {} items in {} ms (showBorders={})",
                items.size,
                System.currentTimeMillis() - startedAt,
                showBorders,
            )
            bytes
        }
    }

    /**
     * 選択された商品のPDFを返す。
     *
     * 署名をそのままキャッシュキーにしているため、商品名や価格が変われば別のキーになり
     * 古いエントリはLRUで押し出される。生成を直列化して、同じ選択の同時要求で
     * 非力な端末のCPUを何度も使わないようにする。
     */
    fun getSelectedItemsPdf(
        items: List<ItemEntity>,
        showBorders: Boolean,
    ): ByteArray {
        val signature = signatureOf(items, showBorders)

        selectedCache[signature]?.let { cached ->
            logger.debug("Reusing cached selected barcode PDF (showBorders={})", showBorders)
            return cached
        }

        return synchronized(selectedLock) {
            selectedCache[signature]?.let { return@synchronized it }

            val startedAt = System.currentTimeMillis()
            val bytes = barcodeService.generateBarcodePdf(items, showBorders)
            selectedCache[signature] = bytes
            logger.info(
                "Generated selected barcode PDF for {} items in {} ms (showBorders={})",
                items.size,
                System.currentTimeMillis() - startedAt,
                showBorders,
            )
            bytes
        }
    }

    fun isCached(showBorders: Boolean = false): Boolean = cache.containsKey(showBorders)

    fun warmUp() {
        BORDER_VARIANTS.forEach { showBorders ->
            try {
                getAllItemsPdf(showBorders)
            } catch (e: Exception) {
                logger.warn("Failed to warm up barcode PDF cache (showBorders={}): {}", showBorders, e.message)
            }
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun warmUpInBackground() {
        if (!warmUpOnStartup) {
            logger.debug("Barcode PDF warm-up on startup is disabled")
            return
        }
        scheduleWarmUp()
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onItemsChanged(event: ItemsChangedEvent) {
        logger.debug("Rebuilding barcode PDF cache after item change (itemId={})", event.itemId)
        scheduleWarmUp()
    }

    @PreDestroy
    fun shutdown() {
        warmUpExecutor.shutdownNow()
    }

    private fun scheduleWarmUp() {
        try {
            warmUpExecutor.execute { warmUp() }
        } catch (e: RejectedExecutionException) {
            logger.debug("Barcode PDF warm-up was not scheduled: {}", e.message)
        }
    }

    private fun signatureOf(
        items: List<ItemEntity>,
        showBorders: Boolean,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(if (showBorders) 1 else 0)
        items.forEach { item ->
            digest.update("${item.id}\u0000${item.barcode}\u0000${item.name}\u0000${item.price}\u0000".toByteArray())
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private class CachedPdf(
        val signature: String,
        val bytes: ByteArray,
    )

    private companion object {
        val BORDER_VARIANTS = listOf(false, true)
        const val SELECTED_CACHE_CAPACITY = 8
        const val LOAD_FACTOR = 0.75f
    }
}
