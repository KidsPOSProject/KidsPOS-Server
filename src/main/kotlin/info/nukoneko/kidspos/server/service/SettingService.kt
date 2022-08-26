package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.entity.SettingEntity
import info.nukoneko.kidspos.server.repository.SettingRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SettingService {
    @Autowired
    private lateinit var repository: SettingRepository


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
        val setting: SettingEntity? =
            repository.findByIdOrNull("${KEY_PRINTER}_$storeId")
        return setting?.let {
            val matchResult =
                REGEX_HOST_PORT.matchEntire(it.value) ?: return null
            return if (matchResult.groupValues.size != 2) {
                null
            } else {
                Pair(
                    matchResult.groupValues[0],
                    matchResult.groupValues[1].toInt()
                )
            }
        }
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
