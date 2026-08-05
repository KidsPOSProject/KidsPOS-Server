package info.nukoneko.kidspos.server.service

import info.nukoneko.kidspos.server.config.AppProperties
import info.nukoneko.kidspos.server.controller.dto.response.PrinterStatusResponse
import info.nukoneko.kidspos.server.controller.dto.response.SystemStatusResponse
import org.springframework.stereotype.Service

@Service
class SystemStatusService(
    private val storeService: StoreService,
    private val appProperties: AppProperties,
    private val printerConnectionChecker: PrinterConnectionChecker,
) {
    fun getStatus(): SystemStatusResponse {
        val printerHosts =
            storeService
                .findAll()
                .map { it.printerUri.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

        val reachableCount =
            printerHosts.count { host ->
                printerConnectionChecker.isReachable(
                    host = host,
                    port = appProperties.receipt.printer.port,
                )
            }

        val configured = printerHosts.isNotEmpty()
        val allReachable = configured && reachableCount == printerHosts.size

        return SystemStatusResponse(
            status = "ok",
            apiVersion = appProperties.apiVersion,
            printer =
                PrinterStatusResponse(
                    configured = configured,
                    reachable = allReachable,
                    total = printerHosts.size,
                    reachableCount = reachableCount,
                ),
        )
    }
}
