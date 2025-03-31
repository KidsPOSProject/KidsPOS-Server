package info.nukoneko.kidspos.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ErrorMvcAutoConfiguration::class
    ]
)
class ServerApplication

fun main(args: Array<String>) {
    // JVM引数でメモリ設定がない場合のためのデフォルト値設定
    System.setProperty("spring.datasource.hikari.maximum-pool-size", "3")
    
    runApplication<ServerApplication>(*args) {
        setDefaultProperties(
            mapOf(
                "spring.main.banner-mode" to "off",
                "server.tomcat.max-threads" to "4"
            )
        )
    }
}