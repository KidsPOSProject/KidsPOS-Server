package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.controller.dto.response.PrinterStatusResponse
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * プリンターへの到達性を確認するサービス
 *
 * 到達不能なプリンターは1台あたり接続タイムアウト分だけ待たされるため、
 * 全台を並列に確認したうえで結果を一定時間キャッシュする。
 */
@Service
class PrinterStatusService(
    private val storeService: StoreService,
    private val appProperties: AppProperties,
    private val printerConnectionChecker: PrinterConnectionChecker,
) {
    private val logger = LoggerFactory.getLogger(PrinterStatusService::class.java)

    private val executor =
        ThreadPoolExecutor(
            0,
            MAX_PARALLEL_CHECKS,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            SynchronousQueue(),
            { runnable -> Thread(runnable, "printer-status").apply { isDaemon = true } },
            // 上限を超えた分は呼び出しスレッドで実行する。確認の取りこぼしより遅延を選ぶ
            ThreadPoolExecutor.CallerRunsPolicy(),
        )

    private val lock = Any()
    private var cached: PrinterStatusResponse? = null
    private var cachedAt = 0L

    fun getStatus(): PrinterStatusResponse =
        synchronized(lock) {
            val now = System.nanoTime()
            val current = cached
            if (current != null && now - cachedAt < cacheDurationNanos()) {
                return current
            }
            val fresh = check()
            cached = fresh
            cachedAt = now
            fresh
        }

    private fun cacheDurationNanos(): Long {
        val seconds = appProperties.receipt.printer.statusCacheSeconds
        return TimeUnit.SECONDS.toNanos(seconds.toLong())
    }

    private fun check(): PrinterStatusResponse {
        val hosts =
            storeService
                .findAll()
                .map { it.printerUri.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        if (hosts.isEmpty()) {
            return PrinterStatusResponse(configured = false, reachable = false, total = 0, reachableCount = 0)
        }

        val port = appProperties.receipt.printer.port
        val futures = hosts.map { host -> executor.submit(Callable { printerConnectionChecker.isReachable(host, port) }) }
        val reachableCount = futures.count { awaitReachable(it) }

        return PrinterStatusResponse(
            configured = true,
            reachable = reachableCount == hosts.size,
            total = hosts.size,
            reachableCount = reachableCount,
        )
    }

    private fun awaitReachable(future: Future<Boolean>): Boolean =
        try {
            // 名前解決は接続タイムアウトの対象外なので、確認そのものにも上限を設ける
            future.get(WAIT_LIMIT_MILLIS, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            logger.warn("プリンターの到達確認が時間内に終わりませんでした", e)
            false
        } catch (e: InterruptedException) {
            future.cancel(true)
            Thread.currentThread().interrupt()
            false
        } catch (e: Exception) {
            logger.warn("プリンターの到達確認に失敗しました", e)
            false
        }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    companion object {
        const val MAX_PARALLEL_CHECKS = 16
        const val KEEP_ALIVE_SECONDS = 60L
        const val GRACE_MILLIS = 2_000L
        const val WAIT_LIMIT_MILLIS = PrinterConnectionChecker.DEFAULT_TIMEOUT_MILLIS + GRACE_MILLIS
    }
}
