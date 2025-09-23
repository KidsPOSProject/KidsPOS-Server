package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.config.AppProperties
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

@Controller
@RequestMapping("/ip")
class IpController(
    private val environment: Environment,
    private val appProperties: AppProperties
) {

    @GetMapping
    fun index(model: Model): String {
        val hosts: MutableList<HostBean> = mutableListOf()
        val testMIP = NetworkInterface.getNetworkInterfaces()
        while (testMIP.hasMoreElements()) {
            val testNI = testMIP.nextElement() as NetworkInterface
            val testInA = testNI.inetAddresses
            while (testInA.hasMoreElements()) {
                val testIP = testInA.nextElement() as InetAddress
                if (testIP.hostName.contains(":0:") || testIP.hostAddress.contains(
                        ":0:"
                    )
                ) {
                    continue
                }
                hosts.add(HostBean(testIP.hostName, testIP.hostAddress))
            }
        }

        model.addAttribute("title", "ip")
        model.addAttribute(
            "hosts",
            hosts.filter { it.address.startsWith(appProperties.network.allowedIpPrefix) }.distinct()
        )
        model.addAttribute("port", environment.getProperty("local.server.port"))
        return "ip/index"
    }

    class HostBean(val name: String, val address: String) {
        val nameId: String = UUID.randomUUID().toString()
        val addressId: String = UUID.randomUUID().toString()
    }
}
