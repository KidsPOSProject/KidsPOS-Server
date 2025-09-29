package info.nukoneko.kidspos.server.controller.dto.response

import info.nukoneko.kidspos.server.entity.ApkVersionEntity
import java.time.LocalDateTime

data class ApkVersionResponse(
    val id: Long?,
    val version: String,
    val versionCode: Int,
    val fileName: String,
    val fileSize: Long,
    val releaseNotes: String?,
    val downloadUrl: String,
    val uploadedAt: LocalDateTime,
    val isActive: Boolean,
) {
    companion object {
        fun from(entity: ApkVersionEntity): ApkVersionResponse =
            ApkVersionResponse(
                id = entity.id,
                version = entity.version,
                versionCode = entity.versionCode,
                fileName = entity.fileName,
                fileSize = entity.fileSize,
                releaseNotes = entity.releaseNotes,
                downloadUrl = "/api/apk/download/${entity.id}",
                uploadedAt = entity.uploadedAt,
                isActive = entity.isActive,
            )
    }
}

data class UpdateCheckResponse(
    val hasUpdate: Boolean,
    val latestVersion: ApkVersionResponse?,
)
