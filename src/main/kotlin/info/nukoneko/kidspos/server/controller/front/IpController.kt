package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.config.AppProperties
import org.slf4j.LoggerFactory
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
@RequestMapping("/ip")
class IpController(
    private val environment: Environment,
    private val appProperties: AppProperties
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping
    fun index(model: Model): String {
        val startTime = System.currentTimeMillis()

        // キャッシュされたネットワーク情報を使用
        val hosts = getNetworkHosts()

        val elapsedTime = System.currentTimeMillis() - startTime
        logger.debug("IP page loaded in {}ms", elapsedTime)

        model.addAttribute("title", "ip")
        model.addAttribute("hosts", hosts)
        model.addAttribute("port", environment.getProperty("local.server.port"))
        return "ip/index"
    }

    @Cacheable("networkHosts", unless = "#result.isEmpty()")
    fun getNetworkHosts(): List<HostBean> {
        val hosts: MutableList<HostBean> = mutableListOf()

        try {
            // ループバックインターフェースは除外し、アクティブなインターフェースのみ取得
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.filter { ni -> ni.isUp && !ni.isLoopback && !ni.isVirtual }
                ?.forEach { networkInterface ->
                    networkInterface.inetAddresses?.asSequence()
                        ?.filterIsInstance<Inet4Address>()  // IPv4のみ取得して高速化
                        ?.filter { inetAddress ->
                            // ローカルIPアドレスのみフィルタリング
                            !inetAddress.isLoopbackAddress &&
                            !inetAddress.isLinkLocalAddress &&
                            inetAddress.hostAddress.startsWith(appProperties.network.allowedIpPrefix)
                        }
                        ?.forEach { inetAddress ->
                            // hostNameの解決は遅いので、hostAddressのみを使用
                            hosts.add(HostBean(
                                name = networkInterface.displayName ?: inetAddress.hostAddress,
                                address = inetAddress.hostAddress
                            ))
                        }
                }
        } catch (e: Exception) {
            logger.error("Error getting network interfaces: ", e)
            // エラーの場合は最小限の情報を返す
            try {
                val localhost = InetAddress.getLocalHost()
                if (localhost.hostAddress.startsWith(appProperties.network.allowedIpPrefix)) {
                    hosts.add(HostBean("localhost", localhost.hostAddress))
                }
            } catch (ex: Exception) {
                logger.error("Error getting localhost: ", ex)
            }
        }

        return hosts.distinctBy { it.address }
    }

    data class HostBean(
        val name: String,
        val address: String
    ) {
        // IDは不要なので削除（ビューで必要なら簡単な計算で生成）
        val nameId: String get() = "name-${address.hashCode()}"
        val addressId: String get() = "addr-${address.hashCode()}"
    }
}
