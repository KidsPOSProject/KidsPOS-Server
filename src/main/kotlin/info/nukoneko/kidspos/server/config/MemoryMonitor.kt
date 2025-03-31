package info.nukoneko.kidspos.server.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * メモリ使用状況をモニタリングするコンポーネント。
 * 本番環境では無効化するため、dev プロファイルでのみ有効。
 */
@Component
@Profile("dev")
class MemoryMonitor {
    private val logger = LoggerFactory.getLogger(MemoryMonitor::class.java)
    
    @Scheduled(fixedRate = 60000) // 1分ごとに実行
    fun reportMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        
        logger.info("Memory Usage - Used: {}MB, Free: {}MB, Total: {}MB, Max: {}MB",
            usedMemory, freeMemory, totalMemory, maxMemory)
    }
}