package info.nukoneko.kidspos.server.service

import java.util.Calendar
import java.util.Date

/**
 * 売上集計の期間を扱う。
 *
 * 画面から渡る終了日は日付のみ（00:00:00）のため、そのまま範囲検索に使うと
 * 終了日当日の売上が 1 件も含まれない。境界は常にここで補正する。
 */
object ReportPeriod {
    fun startOfDay(date: Date): Date =
        calendarOf(date)
            .apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

    fun endOfDay(date: Date): Date =
        calendarOf(date)
            .apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

    fun previousDay(date: Date): Date = calendarOf(date).apply { add(Calendar.DAY_OF_MONTH, -1) }.time

    fun monthStart(
        year: Int,
        month: Int,
    ): Date =
        Calendar
            .getInstance()
            .apply {
                clear()
                set(year, month - 1, 1, 0, 0, 0)
            }.time

    fun monthEnd(
        year: Int,
        month: Int,
    ): Date =
        Calendar
            .getInstance()
            .apply {
                clear()
                set(year, month - 1, 1, 0, 0, 0)
                add(Calendar.MONTH, 1)
                add(Calendar.DAY_OF_MONTH, -1)
            }.time
            .let { endOfDay(it) }

    private fun calendarOf(date: Date): Calendar = Calendar.getInstance().apply { time = date }
}
