package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.controller.dto.response.PrinterStatusResponse
import info.nukoneko.kidspos.server.controller.dto.response.StatusResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import org.springframework.stereotype.Service

/**
 * サーバーステータス・バージョン情報サービス
 */
@Service
class StatusService(
    private val buildProperties: ObjectProvider<BuildProperties>,
    private val storeService: StoreService,
    private val appProperties: AppProperties,
    private val printerConnectionChecker: PrinterConnectionChecker,
) {
    fun getStatus(): StatusResponse =
        StatusResponse(
            status = STATUS_OK,
            version = buildProperties.ifAvailable?.version ?: UNKNOWN_VERSION,
            apiVersion = API_VERSION,
            printer = checkPrinterStatus(),
        )

    private fun checkPrinterStatus(): PrinterStatusResponse {
        val printerHosts =
            storeService
                .findAll()
                .map { it.printerUri.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        val reachableCount =
            printerHosts.count { host ->
                printerConnectionChecker.isReachable(host, appProperties.receipt.printer.port)
            }
        return PrinterStatusResponse(
            configured = printerHosts.isNotEmpty(),
            reachable = printerHosts.isNotEmpty() && reachableCount == printerHosts.size,
            total = printerHosts.size,
            reachableCount = reachableCount,
        )
    }

    companion object {
        const val API_VERSION = 1
        const val STATUS_OK = "OK"
        const val UNKNOWN_VERSION = "unknown"
    }
}
