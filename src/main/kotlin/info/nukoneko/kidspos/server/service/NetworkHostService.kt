package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.config.CacheConfig
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * 自ホストの LAN 側 IPv4 アドレスを列挙する。
 *
 * ネットワークインターフェースの走査は数百ミリ秒かかることがあるため、
 * 同一クラス内から呼ぶとプロキシを経由せずキャッシュが効かない。
 * 画面から使う際は必ずこのサービス越しに呼ぶこと。
 */
@Service
class NetworkHostService(
    private val appProperties: AppProperties,
) {
    private val logger = LoggerFactory.getLogger(NetworkHostService::class.java)

    data class HostBean(
        val name: String,
        val address: String,
    ) {
        val nameId: String get() = "name-${address.hashCode()}"
        val addressId: String get() = "addr-${address.hashCode()}"
    }

    @Cacheable(value = [CacheConfig.NETWORK_HOSTS_CACHE], unless = "#result.isEmpty()")
    fun findHosts(): List<HostBean> {
        val hosts = mutableListOf<HostBean>()

        try {
            NetworkInterface
                .getNetworkInterfaces()
                ?.asSequence()
                ?.filter { it.isUp && !it.isLoopback && !it.isVirtual }
                ?.forEach { networkInterface ->
                    networkInterface.inetAddresses
                        ?.asSequence()
                        ?.filterIsInstance<Inet4Address>()
                        ?.filter { isLocalAddress(it) }
                        ?.forEach { inetAddress ->
                            hosts.add(
                                HostBean(
                                    name = networkInterface.displayName ?: inetAddress.hostAddress,
                                    address = inetAddress.hostAddress,
                                ),
                            )
                        }
                }
        } catch (e: Exception) {
            logger.error("Failed to enumerate network interfaces", e)
            addLocalhostFallback(hosts)
        }

        return hosts.distinctBy { it.address }
    }

    private fun isLocalAddress(address: Inet4Address): Boolean =
        !address.isLoopbackAddress &&
            !address.isLinkLocalAddress &&
            address.hostAddress.startsWith(appProperties.network.allowedIpPrefix)

    private fun addLocalhostFallback(hosts: MutableList<HostBean>) {
        try {
            val localhost = InetAddress.getLocalHost()
            if (localhost.hostAddress.startsWith(appProperties.network.allowedIpPrefix)) {
                hosts.add(HostBean("localhost", localhost.hostAddress))
            }
        } catch (e: Exception) {
            logger.error("Failed to resolve localhost", e)
        }
    }
}
