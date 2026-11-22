package com.easykebiao.app.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 天气解析与查询口径 —— 只用纯 JVM，不碰 Android。
 *
 * 盯的三件事：
 * ① 真实 Open-Meteo 响应能解析出小时与日汇总；
 * ② 脏数据（空数组、长度不齐、缺字段、非 JSON）只丢坏的那条，不整体炸；
 * ③ 「这堂课那一小时」的取数口径：整点优先，缺失退到同日最近，跨日不外借。
 */
class WeatherParseTest {

    private val day = LocalDate.of(2026, 11, 22)

    private val payload = """
        {
          "latitude": 25.25,
          "longitude": 110.25,
          "timezone": "Asia/Shanghai",
          "hourly": {
            "time": ["2026-11-22T08:00", "2026-11-22T09:00", "2026-11-22T14:00"],
            "temperature_2m": [12.4, 13.0, 18.6],
            "precipitation_probability": [10, 80, 5],
            "weather_code": [0, 61, 2]
          },
          "daily": {
            "time": ["2026-11-21", "2026-11-22"],
            "temperature_2m_min": [15.0, 8.0],
            "temperature_2m_max": [22.0, 19.0],
            "weather_code": [3, 61],
            "precipitation_probability_max": [20, 80]
          }
        }
    """.trimIndent()

    @Test
    fun parses_hourly_and_daily_from_real_shaped_payload() {
        val snap = WeatherParse.parse("桂林", 25.25, 110.25, 1000L, payload)
        assertNotNull(snap)
        requireNotNull(snap)
        assertEquals(3, snap.hours.size)
        assertEquals(2, snap.days.size)
        assertEquals("桂林", snap.city)
        assertEquals(13.0, snap.hours[1].celsius, 0.001)
        assertEquals(80, snap.hours[1].precipProb)
        assertEquals(WeatherKind.RAIN, snap.hours[1].kind)
    }

    @Test
    fun empty_or_broken_payload_yields_null() {
        assertNull(WeatherParse.parse("x", 0.0, 0.0, 1L, ""))
        assertNull(WeatherParse.parse("x", 0.0, 0.0, 1L, "not json at all"))
        assertNull(WeatherParse.parse("x", 0.0, 0.0, 1L, """{"hourly":{"time":[],"temperature_2m":[]}}"""))
    }

    @Test
    fun ragged_arrays_only_drop_the_bad_rows() {
        val ragged = """
            {"hourly":{"time":["2026-11-22T08:00","2026-11-22T09:00"],
                       "temperature_2m":[10.0],
                       "precipitation_probability":[30],
                       "weather_code":[0]}}
        """.trimIndent()
        val snap = WeatherParse.parse("x", 0.0, 0.0, 1L, ragged)
        assertNotNull(snap)
        assertEquals("第二条缺温度，应被丢掉", 1, snap!!.hours.size)
        assertEquals(8, snap.hours[0].at.hour)
    }

    @Test
    fun course_hour_prefers_exact_hour_then_nearest_within_same_day() {
        val snap = WeatherParse.parse("桂林", 25.25, 110.25, 1000L, payload)!!
        assertEquals(80, snap.courseHour(day, java.time.LocalTime.of(9, 50))!!.precipProb)
        // 11:00 没有数据 → 退到同日最近的一条（09:00 或 14:00 距离相同，取先命中的 09:00 侧）
        val fallback = snap.courseHour(day, java.time.LocalTime.of(11, 0))
        assertNotNull(fallback)
        assertEquals(day, fallback!!.at.toLocalDate())
        // 别的日期不外借
        assertNull(snap.courseHour(day.plusDays(3), java.time.LocalTime.of(9, 0)))
    }

    @Test
    fun cooling_is_yesterday_low_minus_today_low() {
        val snap = WeatherParse.parse("桂林", 25.25, 110.25, 1000L, payload)!!
        assertEquals(7.0, snap.coolingOn(day)!!, 0.001)
        assertNull("缺少前一天数据时不猜", snap.coolingOn(day.plusDays(1)))
        assertEquals(8.0, snap.dayOf(day)!!.minCelsius, 0.001)
    }

    @Test
    fun wet_codes_map_to_expected_kinds() {
        assertEquals(WeatherKind.SUN, WeatherCode.kindOf(0))
        assertEquals(WeatherKind.PARTLY, WeatherCode.kindOf(2))
        assertEquals(WeatherKind.FOG, WeatherCode.kindOf(45))
        assertEquals(WeatherKind.DRIZZLE, WeatherCode.kindOf(51))
        assertEquals(WeatherKind.RAIN, WeatherCode.kindOf(63))
        assertEquals(WeatherKind.SNOW, WeatherCode.kindOf(75))
        assertEquals(WeatherKind.THUNDER, WeatherCode.kindOf(95))
        assertEquals("未知代码降级为阴", WeatherKind.CLOUD, WeatherCode.kindOf(1234))
        assertTrue(WeatherCode.isWet(61))
        assertFalse(WeatherCode.isWet(0))
    }

    @Test
    fun stale_snapshot_is_detected_by_ttl() {
        val snap = WeatherParse.parse("桂林", 25.25, 110.25, 1_000L, payload)!!
        assertTrue(snap.isStale(now = 1_000L + WeatherSnapshot.DEFAULT_TTL_MS + 1))
        assertFalse(snap.isStale(now = 1_000L + 60_000L))
        assertEquals(LocalDateTime.of(2026, 11, 22, 9, 0), snap.hours[1].at)
    }
}
