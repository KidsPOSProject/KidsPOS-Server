package info.nukoneko.kidspos.server.util

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * MemoryMonitor - Monitors JVM memory usage and logs it periodically.
 * Only active when the 'monitoring' profile is enabled.
 * 
 * To enable, add `-Dspring.profiles.active=monitoring` to JVM arguments.
 */
@Component
@Profile("monitoring")
class MemoryMonitor {
    private val logger = LoggerFactory.getLogger(MemoryMonitor::class.java)
    
    @Scheduled(fixedRate = 60000) // Log every minute
    fun reportMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        
        logger.info("Memory Usage: {}MB / {}MB (max: {}MB)", usedMemory, totalMemory, maxMemory)
        
        // Log warning if memory usage is high (over 80%)
        if (usedMemory > totalMemory * 0.8) {
            logger.warn("High memory usage detected: {}% of available memory is used", 
                        String.format("%.1f", (usedMemory.toDouble() / totalMemory.toDouble()) * 100))
        }
    }
    
    @Scheduled(fixedRate = 300000) // Run GC suggestion every 5 minutes
    fun suggestGCIfNeeded() {
        val runtime = Runtime.getRuntime()
        val memoryUsageRatio = (runtime.totalMemory() - runtime.freeMemory()).toDouble() / runtime.totalMemory()
        
        // If memory usage is over 70%, suggest a GC
        if (memoryUsageRatio > 0.7) {
            logger.info("Suggesting garbage collection due to high memory usage")
            System.gc() // Request garbage collection
        }
    }
}