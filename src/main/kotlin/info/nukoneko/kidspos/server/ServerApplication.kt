package info.nukoneko.kidspos.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        RepositoryRestMvcAutoConfiguration::class
    ]
)
@EnableAsync  // Enable async processing
@EnableScheduling  // Enable scheduled tasks
class ServerApplication

fun main(args: Array<String>) {
    System.setProperty("spring.jmx.enabled", "false")  // Disable JMX to save memory
    runApplication<ServerApplication>(*args) {
        setDefaultProperties(mapOf(
            "spring.main.lazy-initialization" to "true",  // Lazy init to reduce startup memory
            "spring.main.banner-mode" to "off"  // Disable Spring banner to reduce log output
        ))
    }
}