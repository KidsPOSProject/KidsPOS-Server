package info.nukoneko.kidspos.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import info.nukoneko.kidspos.server.entity.SettingEntity
import info.nukoneko.kidspos.server.repository.SettingRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SettingService {
    @Autowired
    private lateinit var repository: SettingRepository

    @Autowired
    private lateinit var environment: Environment

    fun findAllSetting(): List<SettingEntity> {
        return repository.findAll()
    }

    fun findSetting(key: String): SettingEntity? {
        return repository.findByIdOrNull(key)
    }

    fun savePrinterHostPort(storeId: Int, host: String, port: Int) {
        repository.save(SettingEntity("${KEY_PRINTER}_$storeId", "$host:$port"))
    }

    fun findPrinterHostPortById(storeId: Int): Pair<String, Int>? {
        val setting: SettingEntity? = repository.findByIdOrNull("${KEY_PRINTER}_$storeId")
        return setting?.let {
            val matchResult = REGEX_HOST_PORT.matchEntire(it.value) ?: return null
            return if (matchResult.groupValues.size != 2) {
                null
            } else {
                Pair(matchResult.groupValues[0], matchResult.groupValues[1].toInt())
            }
        }
    }

    /**
     * 基本的にクライアント端末で読み込むためのQRコードに使われるtext
     */
    fun generateSettingJson(): String? {
        val serverHost = getMyHost() ?: return null
        val serverPort = getMyPort() ?: return null
        val obj: ApplicationSetting = ApplicationSetting(
                serverHost,
                serverPort.toInt()
        )
        return ObjectMapper().writeValueAsString(obj)
    }

    /**
     * 環境によってとても遅いのでAjaxで動的に読み込んだほうがいい
     */
    private fun getMyHost(): String? {
        return "192.168.11.6"
//        val inf = NetworkInterface.getNetworkInterfaces()
//        while (inf?.hasMoreElements() == true) {
//            val addressed = inf.nextElement()?.inetAddresses
//            while (addressed?.hasMoreElements() == true) {
//                val iNet = addressed.nextElement()
//                if (iNet.hostName.contains("KidsPOS")) {
//                    return iNet.hostName
//                }
//                if (iNet.hostAddress.startsWith("192.168.")) {
//                    return iNet.hostAddress
//                }
//            }
//        }
//        return null
    }

    private fun getMyPort(): String? {
        return environment.getProperty("local.server.port")
    }

    private companion object {
        private const val KEY_PRINTER = "printer"

        private val REGEX_HOST_PORT = "(.*):([0-9]{2,5})".toRegex()
    }

    data class ApplicationSetting(
            val serverHost: String,
            val serverPort: Int
    )
}