package info.nukoneko.kidspos.server.service

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
    private val printerStatusService: PrinterStatusService,
) {
    fun getStatus(): StatusResponse =
        StatusResponse(
            status = STATUS_OK,
            version = buildProperties.ifAvailable?.version ?: UNKNOWN_VERSION,
            apiVersion = API_VERSION,
            printer = printerStatusService.getStatus(),
        )

    companion object {
        const val API_VERSION = 1
        const val STATUS_OK = "OK"
        const val UNKNOWN_VERSION = "unknown"
    }
}
