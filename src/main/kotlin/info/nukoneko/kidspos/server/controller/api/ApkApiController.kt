package info.nukoneko.kidspos.server.controller.api

import info.nukoneko.kidspos.server.controller.dto.response.ApkVersionResponse
import info.nukoneko.kidspos.server.controller.dto.response.UpdateCheckResponse
import info.nukoneko.kidspos.server.service.ApkVersionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/apk")
@Tag(name = "APK管理", description = "APKバージョン管理API")
class ApkApiController(
    private val apkVersionService: ApkVersionService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/version/latest")
    @Operation(summary = "最新バージョン情報の取得", description = "最新のAPKバージョン情報を取得します")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功"),
        ApiResponse(responseCode = "404", description = "バージョンが存在しない"),
    )
    fun getLatestVersion(): ResponseEntity<ApkVersionResponse> {
        val latestVersion =
            apkVersionService.getLatestVersion()
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(ApkVersionResponse.from(latestVersion))
    }

    @GetMapping("/version/check")
    @Operation(summary = "アップデート確認", description = "現在のバージョンコードを基に新しいバージョンがあるか確認します")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功"),
    )
    fun checkForUpdate(
        @Parameter(description = "現在のバージョンコード", required = true)
        @RequestParam currentVersionCode: Int,
    ): ResponseEntity<UpdateCheckResponse> {
        val newerVersion = apkVersionService.checkForUpdate(currentVersionCode)

        return if (newerVersion != null) {
            ResponseEntity.ok(
                UpdateCheckResponse(
                    hasUpdate = true,
                    latestVersion = ApkVersionResponse.from(newerVersion),
                ),
            )
        } else {
            ResponseEntity.ok(
                UpdateCheckResponse(
                    hasUpdate = false,
                    latestVersion = null,
                ),
            )
        }
    }

    @GetMapping("/version/all")
    @Operation(summary = "全バージョン一覧の取得", description = "有効な全てのAPKバージョン情報を取得します")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功"),
    )
    fun getAllVersions(): ResponseEntity<List<ApkVersionResponse>> {
        val versions =
            apkVersionService
                .getAllVersions()
                .map { ApkVersionResponse.from(it) }
        return ResponseEntity.ok(versions)
    }

    @GetMapping("/download/{id}")
    @Operation(summary = "APKファイルのダウンロード", description = "指定されたIDのAPKファイルをダウンロードします")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功"),
        ApiResponse(responseCode = "404", description = "ファイルが存在しない"),
    )
    fun downloadApk(
        @Parameter(description = "APKバージョンID", required = true)
        @PathVariable id: Long,
    ): ResponseEntity<Resource> =
        try {
            val apkFile = apkVersionService.getApkFile(id)
            val apkVersion = apkVersionService.getVersionById(id)
            val resource = FileSystemResource(apkFile)

            ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${apkVersion.fileName}\"")
                .header(HttpHeaders.CONTENT_LENGTH, apkFile.length().toString())
                .body(resource)
        } catch (e: Exception) {
            logger.error("APKダウンロードエラー: ${e.message}", e)
            ResponseEntity.notFound().build()
        }

    @GetMapping("/download/latest")
    @Operation(summary = "最新APKファイルのダウンロード", description = "最新バージョンのAPKファイルをダウンロードします")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "成功"),
        ApiResponse(responseCode = "404", description = "ファイルが存在しない"),
    )
    fun downloadLatestApk(): ResponseEntity<Resource> {
        val latestVersion =
            apkVersionService.getLatestVersion()
                ?: return ResponseEntity.notFound().build()

        return downloadApk(latestVersion.id)
    }

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "APKファイルのアップロード", description = "新しいAPKファイルをアップロードします")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "アップロード成功"),
        ApiResponse(responseCode = "400", description = "バリデーションエラー"),
        ApiResponse(responseCode = "409", description = "バージョンが既に存在"),
    )
    fun uploadApk(
        @Parameter(description = "APKファイル", required = true)
        @RequestPart("file") file: MultipartFile,
        @Parameter(description = "バージョン名（例：1.2.3）", required = true)
        @RequestParam version: String,
        @Parameter(description = "バージョンコード（整数）", required = true)
        @RequestParam versionCode: Int,
        @Parameter(description = "リリースノート")
        @RequestParam(required = false) releaseNotes: String?,
    ): ResponseEntity<ApkVersionResponse> =
        try {
            val apkVersion = apkVersionService.uploadApk(file, version, versionCode, releaseNotes)
            ResponseEntity.status(HttpStatus.CREATED).body(ApkVersionResponse.from(apkVersion))
        } catch (e: Exception) {
            logger.error("APKアップロードエラー: ${e.message}", e)
            ResponseEntity.badRequest().build()
        }

    @DeleteMapping("/version/{id}")
    @Operation(summary = "APKバージョンの削除", description = "指定されたAPKバージョンを削除します")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "削除成功"),
        ApiResponse(responseCode = "404", description = "バージョンが存在しない"),
    )
    fun deleteVersion(
        @Parameter(description = "APKバージョンID", required = true)
        @PathVariable id: Long,
    ): ResponseEntity<Void> =
        try {
            apkVersionService.deleteVersion(id)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            logger.error("APK削除エラー: ${e.message}", e)
            ResponseEntity.notFound().build()
        }

    @PutMapping("/version/{id}/deactivate")
    @Operation(summary = "APKバージョンの無効化", description = "指定されたAPKバージョンを無効化します")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "無効化成功"),
        ApiResponse(responseCode = "404", description = "バージョンが存在しない"),
    )
    fun deactivateVersion(
        @Parameter(description = "APKバージョンID", required = true)
        @PathVariable id: Long,
    ): ResponseEntity<ApkVersionResponse> =
        try {
            val deactivated = apkVersionService.deactivateVersion(id)
            ResponseEntity.ok(ApkVersionResponse.from(deactivated))
        } catch (e: Exception) {
            logger.error("APK無効化エラー: ${e.message}", e)
            ResponseEntity.notFound().build()
        }
}
