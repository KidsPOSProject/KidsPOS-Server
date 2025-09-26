package info.nukoneko.kidspos.server.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "apk_versions")
data class ApkVersionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false, unique = true)
    val version: String = "",
    @Column(name = "version_code", nullable = false)
    val versionCode: Int = 0,
    @Column(name = "file_name", nullable = false)
    val fileName: String = "",
    @Column(name = "file_size", nullable = false)
    val fileSize: Long = 0,
    @Column(name = "file_path", nullable = false)
    val filePath: String = "",
    @Column(name = "release_notes", columnDefinition = "TEXT")
    val releaseNotes: String? = null,
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,
    @Column(name = "uploaded_at", nullable = false)
    val uploadedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    @PreUpdate
    fun preUpdate() {
        @Suppress("REASSIGNED_PARAMETER")
        var updatedAt = LocalDateTime.now()
    }
}
