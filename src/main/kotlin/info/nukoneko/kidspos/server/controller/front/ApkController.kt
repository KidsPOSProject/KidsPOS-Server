package info.nukoneko.kidspos.server.controller.front

import info.nukoneko.kidspos.server.service.ApkVersionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/apk")
class ApkController(
    private val apkVersionService: ApkVersionService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun index(model: Model): String {
        val versions = apkVersionService.getAllVersions()
        val latestVersion = apkVersionService.getLatestVersion()

        model.addAttribute("versions", versions)
        model.addAttribute("latestVersion", latestVersion)
        model.addAttribute("title", "APKバージョン管理")

        return "apk/index"
    }

    @GetMapping("/upload")
    fun uploadForm(model: Model): String {
        model.addAttribute("title", "APKアップロード")
        return "apk/upload"
    }

    @PostMapping("/upload")
    fun uploadApk(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("version") version: String,
        @RequestParam("versionCode") versionCode: Int,
        @RequestParam("releaseNotes", required = false) releaseNotes: String?,
        redirectAttributes: RedirectAttributes,
    ): String =
        try {
            val apkVersion = apkVersionService.uploadApk(file, version, versionCode, releaseNotes)
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "APKバージョン ${apkVersion.version} をアップロードしました",
            )
            "redirect:/apk"
        } catch (e: Exception) {
            logger.error("APKアップロードエラー: ${e.message}", e)
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "アップロードに失敗しました: ${e.message}",
            )
            "redirect:/apk/upload"
        }

    @PostMapping("/{id}/deactivate")
    fun deactivateVersion(
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes,
    ): String =
        try {
            val deactivated = apkVersionService.deactivateVersion(id)
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "バージョン ${deactivated.version} を無効化しました",
            )
            "redirect:/apk"
        } catch (e: Exception) {
            logger.error("APK無効化エラー: ${e.message}", e)
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "無効化に失敗しました: ${e.message}",
            )
            "redirect:/apk"
        }

    @PostMapping("/{id}/delete")
    fun deleteVersion(
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes,
    ): String =
        try {
            apkVersionService.deleteVersion(id)
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "APKバージョンを削除しました",
            )
            "redirect:/apk"
        } catch (e: Exception) {
            logger.error("APK削除エラー: ${e.message}", e)
            redirectAttributes.addFlashAttribute(
                "errorMessage",
                "削除に失敗しました: ${e.message}",
            )
            "redirect:/apk"
        }
}
