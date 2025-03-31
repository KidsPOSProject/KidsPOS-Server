package info.nukoneko.kidspos.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    exclude = [
        JmxAutoConfiguration::class
    ]
)
@EnableAsync
@EnableScheduling
class ServerApplication

fun main(args: Array<String>) {
    System.setProperty("spring.jmx.enabled", "false")
    runApplication<ServerApplication>(*args) {
        setDefaultProperties(
            mapOf(
                "spring.jmx.enabled" to "false",
                "spring.config.location" to "classpath:/application.yaml"
            )
        )
    }
}