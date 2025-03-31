package info.nukoneko.kidspos.server.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MemoryMonitor {
    private val logger = LoggerFactory.getLogger(MemoryMonitor::class.java)

    @Scheduled(fixedRate = 60000) // 1分ごとに実行
    fun reportMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        
        logger.info("Memory Usage: {} MB (Max: {} MB)", usedMemory, maxMemory)
    }
}
