package info.nukoneko.kidspos.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

/**
 * KidsPOSサーバーアプリケーション
 *
 * Spring Bootベースの子供向けPOSシステムのメインクラス
 */
@SpringBootApplication
@ComponentScan(basePackages = ["info.nukoneko.kidspos"])
class ServerApplication

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}