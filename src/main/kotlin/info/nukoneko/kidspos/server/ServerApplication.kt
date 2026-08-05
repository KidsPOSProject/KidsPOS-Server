package info.nukoneko.kidspos.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * KidsPOSサーバーアプリケーション
 *
 * Spring Bootベースの子供向けPOSシステムのメインクラス
 */
@SpringBootApplication(scanBasePackages = ["info.nukoneko.kidspos"])
class ServerApplication

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}
