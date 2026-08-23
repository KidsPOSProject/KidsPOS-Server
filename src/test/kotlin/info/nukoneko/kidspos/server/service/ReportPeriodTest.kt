package info.nukoneko.kidspos.server.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.Date

class ReportPeriodTest {
    @Test
    fun `startOfDay は同じ日の 0 時 0 分 0 秒 0 ミリ秒を返す`() {
        val result = ReportPeriod.startOfDay(dateOf(2026, 8, 21, 15, 30, 45, 123))

        assertFieldsEqual(dateOf(2026, 8, 21, 0, 0, 0, 0), result)
    }

    @Test
    fun `endOfDay は同じ日の 23 時 59 分 59 秒 999 ミリ秒を返す`() {
        val result = ReportPeriod.endOfDay(dateOf(2026, 8, 21, 15, 30, 45, 123))

        assertFieldsEqual(dateOf(2026, 8, 21, 23, 59, 59, 999), result)
    }

    @Test
    fun `endOfDay は日付のみの指定でも当日の終端まで広げる`() {
        val endDate = dateOf(2026, 8, 21, 0, 0, 0, 0)

        val result = ReportPeriod.endOfDay(endDate)

        assertEquals(true, result.after(dateOf(2026, 8, 21, 23, 59, 0, 0)))
        assertEquals(true, result.before(dateOf(2026, 8, 22, 0, 0, 0, 0)))
    }

    @Test
    fun `monthStart は月初の 0 時を返す`() {
        val result = ReportPeriod.monthStart(2026, 8)

        assertFieldsEqual(dateOf(2026, 8, 1, 0, 0, 0, 0), result)
    }

    @Test
    fun `monthEnd は月末日の終端を返す`() {
        val result = ReportPeriod.monthEnd(2026, 8)

        assertFieldsEqual(dateOf(2026, 8, 31, 23, 59, 59, 999), result)
    }

    @Test
    fun `monthEnd は 30 日までの月でも正しい月末を返す`() {
        val result = ReportPeriod.monthEnd(2026, 4)

        assertFieldsEqual(dateOf(2026, 4, 30, 23, 59, 59, 999), result)
    }

    @Test
    fun `monthEnd は閏年の 2 月で 29 日を返す`() {
        val result = ReportPeriod.monthEnd(2024, 2)

        assertFieldsEqual(dateOf(2024, 2, 29, 23, 59, 59, 999), result)
    }

    @Test
    fun `monthEnd は平年の 2 月で 28 日を返す`() {
        val result = ReportPeriod.monthEnd(2026, 2)

        assertFieldsEqual(dateOf(2026, 2, 28, 23, 59, 59, 999), result)
    }

    @Test
    fun `monthEnd は 12 月で年をまたがない`() {
        val result = ReportPeriod.monthEnd(2026, 12)

        assertFieldsEqual(dateOf(2026, 12, 31, 23, 59, 59, 999), result)
    }

    private fun assertFieldsEqual(
        expected: Date,
        actual: Date,
    ) = assertEquals(expected.time, actual.time)

    private fun dateOf(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
        millis: Int,
    ): Date =
        Calendar
            .getInstance()
            .apply {
                clear()
                set(year, month - 1, day, hour, minute, second)
                set(Calendar.MILLISECOND, millis)
            }.time
}
