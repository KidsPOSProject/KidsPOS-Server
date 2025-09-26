package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.config.AppProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

@Controller
@RequestMapping("/")
class TopController(
    private val environment: Environment,
    private val appProperties: AppProperties,
) {
    @GetMapping
    fun index(model: Model): String {
        model.addAttribute("title", "ダッシュボード")

        // ネットワーク情報を取得（キャッシュ利用）
        val networkHosts = getNetworkHosts()
        model.addAttribute("hosts", networkHosts)
        model.addAttribute("port", environment.getProperty("local.server.port"))

        return "index"
    }

    @Cacheable("networkHosts", unless = "#result.isEmpty()")
    fun getNetworkHosts(): List<HostBean> {
        val hosts: MutableList<HostBean> = mutableListOf()

        try {
            // ループバックインターフェースは除外し、アクティブなインターフェースのみ取得
            NetworkInterface
                .getNetworkInterfaces()
                ?.asSequence()
                ?.filter { ni -> ni.isUp && !ni.isLoopback && !ni.isVirtual }
                ?.forEach { networkInterface ->
                    networkInterface.inetAddresses
                        ?.asSequence()
                        ?.filterIsInstance<Inet4Address>() // IPv4のみ取得して高速化
                        ?.filter { inetAddress ->
                            // ローカルIPアドレスのみフィルタリング
                            !inetAddress.isLoopbackAddress &&
                                !inetAddress.isLinkLocalAddress &&
                                inetAddress.hostAddress.startsWith(appProperties.network.allowedIpPrefix)
                        }?.forEach { inetAddress ->
                            // hostNameの解決は遅いので、hostAddressのみを使用
                            hosts.add(
                                HostBean(
                                    name = networkInterface.displayName ?: inetAddress.hostAddress,
                                    address = inetAddress.hostAddress,
                                ),
                            )
                        }
                }
        } catch (e: Exception) {
            // エラーの場合は最小限の情報を返す
            try {
                val localhost = InetAddress.getLocalHost()
                if (localhost.hostAddress.startsWith(appProperties.network.allowedIpPrefix)) {
                    hosts.add(HostBean("localhost", localhost.hostAddress))
                }
            } catch (ex: Exception) {
                // ログに記録するが、エラーは表示しない
            }
        }

        return hosts.distinctBy { it.address }
    }

    data class HostBean(
        val name: String,
        val address: String,
    )
}
