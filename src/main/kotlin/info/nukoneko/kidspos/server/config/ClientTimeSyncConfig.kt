package info.nukoneko.kidspos.server.config

import info.nukoneko.kidspos.server.service.ClientClockSynchronizer
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * クライアント時刻を拾うフィルターを登録する
 *
 * フィルター自身に @Component を付けると Web 層だけを読み込むテストにも取り込まれ、
 * サービス層の依存が解決できず起動に失敗する。登録は設定クラスに寄せる。
 */
@Configuration
class ClientTimeSyncConfig {
    @Bean
    fun clientTimeSyncFilterRegistration(clientClockSynchronizer: ClientClockSynchronizer): FilterRegistrationBean<ClientTimeSyncFilter> =
        FilterRegistrationBean(ClientTimeSyncFilter(clientClockSynchronizer)).apply {
            addUrlPatterns("/*")
        }
}
