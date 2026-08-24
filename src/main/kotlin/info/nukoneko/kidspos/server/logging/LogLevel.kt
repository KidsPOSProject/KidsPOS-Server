package info.nukoneko.kidspos.server.logging

/**
 * ログ画面で扱うログレベル
 */
enum class LogLevel(
    val severity: Int,
) {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    ;

    companion object {
        fun from(name: String?): LogLevel? = entries.firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) }
    }
}
