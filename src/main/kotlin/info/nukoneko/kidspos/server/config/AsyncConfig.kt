package info.nukoneko.kidspos.server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * 非同期処理の設定クラス
 * スレッドプールを小さく保ちメモリ使用量を抑制する
 */
@Configuration
@EnableAsync
class AsyncConfig {
    
    @Bean
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        // Raspberry Piに最適化したスレッド設定
        executor.corePoolSize = 2
        executor.maxPoolSize = 2
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("Async-")
        executor.initialize()
        return executor
    }
}