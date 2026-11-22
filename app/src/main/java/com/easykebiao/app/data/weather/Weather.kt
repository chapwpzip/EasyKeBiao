package com.easykebiao.app.data.weather

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 天气领域模型（不依赖 Android，可在纯 JVM 单测里跑）。
 *
 * 数据源：Open-Meteo（https://open-meteo.com，CC-BY-4.0，免费无需 key）。
 * 我们只取三样东西：逐小时温度、逐小时降水概率、逐小时 WMO 天气代码。
 */

/** 天气大类 —— 决定图标，不参与任何业务判断。 */
enum class WeatherKind { SUN, PARTLY, CLOUD, FOG, DRIZZLE, RAIN, SNOW, THUNDER }

object WeatherCode {
    /**
     * WMO 4677 天气现象代码 → 大类。
     * 未收录的代码一律降级为 CLOUD（宁可少报晴天，也不要假装知道）。
     */
    fun kindOf(code: Int): WeatherKind = when (code) {
        0, 1 -> WeatherKind.SUN
        2 -> WeatherKind.PARTLY
        3, 45, 48 -> if (code == 45 || code == 48) WeatherKind.FOG else WeatherKind.CLOUD
        51, 53, 55, 56, 57 -> WeatherKind.DRIZZLE
        61, 63, 80, 81 -> WeatherKind.RAIN
        65, 66, 67, 82 -> WeatherKind.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
        95, 96, 99 -> WeatherKind.THUNDER
        else -> WeatherKind.CLOUD
    }

    /** 图标用的中文短标签（"雨""雪"…），只有两三个字，塞得进课程卡左侧。 */
    fun shortLabelZh(code: Int): String = when (kindOf(code)) {
        WeatherKind.SUN -> "晴"
        WeatherKind.PARTLY -> "多云"
        WeatherKind.CLOUD -> "阴"
        WeatherKind.FOG -> "雾"
        WeatherKind.DRIZZLE -> "小雨"
        WeatherKind.RAIN -> "雨"
        WeatherKind.SNOW -> "雪"
        WeatherKind.THUNDER -> "雷阵雨"
    }

    /** 是否算"要带伞"的天气。 */
    fun isWet(code: Int): Boolean = when (kindOf(code)) {
        WeatherKind.DRIZZLE, WeatherKind.RAIN, WeatherKind.SNOW, WeatherKind.THUNDER -> true
        else -> false
    }
}

/** 某个整点的天气。 */
data class WeatherHour(
    val at: LocalDateTime,
    val celsius: Double,
    val code: Int,
    val precipProb: Int
) {
    val kind: WeatherKind get() = WeatherCode.kindOf(code)
}

/** 某一天的汇总（用于"今天降温多少度"这类判断）。 */
data class WeatherDay(
    val date: LocalDate,
    val minCelsius: Double,
    val maxCelsius: Double,
    val code: Int,
    val precipProbMax: Int
)

data class WeatherSnapshot(
    val city: String,
    val lat: Double,
    val lon: Double,
    val fetchedAt: Long,
    val hours: List<WeatherHour>,
    val days: List<WeatherDay>
) {
    val isEmpty: Boolean get() = hours.isEmpty() && days.isEmpty()

    fun ageMillis(now: Long = System.currentTimeMillis()): Long = (now - fetchedAt).coerceAtLeast(0)

    fun isStale(now: Long = System.currentTimeMillis(), ttlMillis: Long = DEFAULT_TTL_MS): Boolean =
        fetchedAt <= 0L || ageMillis(now) > ttlMillis

    /** 取某个整点的天气；那一小时没有数据就返回 null（宁可不显示，也不拿别的小时顶替）。 */
    fun hourAt(date: LocalDate, hour: Int): WeatherHour? =
        hours.firstOrNull { it.at.toLocalDate() == date && it.at.hour == hour }

    /** 取某课开始时刻的天气：先找整点，再退到同日最接近的一小时。 */
    fun courseHour(date: LocalDate, start: LocalTime): WeatherHour? {
        hourAt(date, start.hour)?.let { return it }
        val target = LocalDateTime.of(date, start)
        return hours
            .filter { it.at.toLocalDate() == date }
            .minByOrNull { kotlin.math.abs(Duration.between(it.at, target).toMinutes()) }
    }

    fun dayOf(date: LocalDate): WeatherDay? = days.firstOrNull { it.date == date }

    /**
     * 相对「前一天」的最低温降幅（正数 = 今天更冷）；缺任一天的数据就返回 null。
     * 注意不能拿 days 的首尾去猜 —— 请求里带 past_days=1，最早那条就是昨天。
     */
    fun coolingOn(date: LocalDate): Double? {
        val today = dayOf(date) ?: return null
        val previous = dayOf(date.minusDays(1)) ?: return null
        return previous.minCelsius - today.minCelsius
    }

    companion object {
        /** 缓存 3 小时：比这更旧就重新拉，但显示时仍可用旧数据（UI 不区分）。 */
        const val DEFAULT_TTL_MS: Long = 3 * 60 * 60 * 1000L
    }
}

/** Open-Meteo 响应 DTO —— 字段名与 API 逐字对齐，缺字段一律给默认值，避免解析炸掉。 */
@Serializable
internal data class OpenMeteoResponse(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "",
    val hourly: OpenMeteoHourly? = null,
    val daily: OpenMeteoDaily? = null
)

@Serializable
internal data class OpenMeteoHourly(
    val time: List<String> = emptyList(),
    val temperature_2m: List<Double?> = emptyList(),
    val precipitation_probability: List<Int?> = emptyList(),
    val weather_code: List<Int?> = emptyList()
)

@Serializable
internal data class OpenMeteoDaily(
    val time: List<String> = emptyList(),
    val temperature_2m_min: List<Double?> = emptyList(),
    val temperature_2m_max: List<Double?> = emptyList(),
    val weather_code: List<Int?> = emptyList(),
    val precipitation_probability_max: List<Int?> = emptyList()
)

object WeatherParse {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 解析 Open-Meteo 响应。任何"少数组/长度不齐/空值"都只丢那一条，不整体失败；
     * 全空时返回 null（调用方据此保持"没有天气"的状态，而不是显示 0℃）。
     */
    fun parse(
        city: String,
        lat: Double,
        lon: Double,
        fetchedAt: Long,
        payload: String
    ): WeatherSnapshot? {
        val dto = runCatching { json.decodeFromString<OpenMeteoResponse>(payload) }.getOrNull() ?: return null
        val hours = buildList {
            val h = dto.hourly ?: return@buildList
            for (i in h.time.indices) {
                val at = parseLocalDateTime(h.time.getOrNull(i)) ?: continue
                val t = h.temperature_2m.getOrNull(i) ?: continue
                add(
                    WeatherHour(
                        at = at,
                        celsius = t,
                        code = h.weather_code.getOrNull(i) ?: 0,
                        precipProb = h.precipitation_probability.getOrNull(i) ?: 0
                    )
                )
            }
        }
        val days = buildList {
            val d = dto.daily ?: return@buildList
            for (i in d.time.indices) {
                val date = runCatching { LocalDate.parse(d.time[i]) }.getOrNull() ?: continue
                val min = d.temperature_2m_min.getOrNull(i) ?: continue
                val max = d.temperature_2m_max.getOrNull(i) ?: continue
                add(
                    WeatherDay(
                        date = date,
                        minCelsius = min,
                        maxCelsius = max,
                        code = d.weather_code.getOrNull(i) ?: 0,
                        precipProbMax = d.precipitation_probability_max.getOrNull(i) ?: 0
                    )
                )
            }
        }
        if (hours.isEmpty() && days.isEmpty()) return null
        return WeatherSnapshot(city = city, lat = lat, lon = lon, fetchedAt = fetchedAt, hours = hours, days = days)
    }

    /** Open-Meteo 在有 timezone 参数时给的是本地时间字符串（无偏移），直接当 LocalDateTime 用。 */
    internal fun parseLocalDateTime(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(raw) }
            .recoverCatching { LocalDateTime.parse(raw.replace(' ', 'T')) }
            .getOrNull()
    }
}
