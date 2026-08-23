package info.nukoneko.kidspos.server.controller.dto.response

import info.nukoneko.kidspos.server.service.ApkManifestInfo

data class ApkAnalyzeResponse(
    val version: String,
    val versionCode: Int,
) {
    companion object {
        fun from(info: ApkManifestInfo): ApkAnalyzeResponse =
            ApkAnalyzeResponse(
                version = info.versionName,
                versionCode = info.versionCode,
            )
    }
}
