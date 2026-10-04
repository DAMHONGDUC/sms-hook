package com.dd.sms.hook.shared

import com.dd.sms.hook.shared.data.serialization.AppJson
import com.dd.sms.hook.shared.data.serialization.NameValueDto
import com.dd.sms.hook.shared.domain.time.TimeUtils
import com.dd.sms.hook.shared.presentation.ui.UiFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class SharedUtilsTest {
    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    private val now: Long = ZonedDateTime.of(2026, 9, 28, 1, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `range starts at local midnight days minus one ago`() {
        val expected: Long = ZonedDateTime.of(2026, 9, 22, 0, 0, 0, 0, zone).toInstant().toEpochMilli()

        assertEquals(expected, TimeUtils.startOfRange(7, now, zone))
    }

    @Test
    fun `days of range are oldest first and end today`() {
        val days: List<LocalDate> = TimeUtils.daysOfRange(3, now, zone)

        assertEquals(listOf(LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 28)), days)
    }

    @Test
    fun `local date depends on the zone, not UTC`() {
        // 01:30 in Hanoi is still the previous day in UTC.
        assertEquals(LocalDate.of(2026, 9, 28), TimeUtils.toLocalDate(now, zone))
        assertEquals(LocalDate.of(2026, 9, 27), TimeUtils.toLocalDate(now, ZoneId.of("UTC")))
    }

    @Test
    fun `formatting follows the given locale`() {
        assertEquals("28/09", TimeUtils.formatDayLabel(LocalDate.of(2026, 9, 28), Locale.US))
        assertEquals("01:30", TimeUtils.formatTime(now, Locale.US, zone))
        assertEquals("2023-11-14T22:13:20Z", TimeUtils.toIso(1_700_000_000_000L))
    }

    @Test
    fun `durations switch to seconds at one second and are isolated left to right`() {
        assertEquals("⁦999 ms⁩", UiFormat.duration(999))
        assertEquals("⁦1.0 s⁩", UiFormat.duration(1000))
        assertEquals("⁦12.3 s⁩", UiFormat.duration(12_345))
        assertEquals("⁦50%⁩", UiFormat.percent(0.5f))
    }

    @Test
    fun `json pairs round trip and blank decodes to empty`() {
        val pairs: List<NameValueDto> = listOf(NameValueDto("a", "\"quoted\""), NameValueDto("b", ""))

        assertEquals(pairs, AppJson.decodePairs(AppJson.encodePairs(pairs)))
        assertEquals(emptyList<NameValueDto>(), AppJson.decodePairs(""))
    }
}
