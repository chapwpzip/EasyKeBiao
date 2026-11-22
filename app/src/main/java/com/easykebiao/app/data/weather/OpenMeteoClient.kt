package com.easykebiao.app.data.weather

import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Open-Meteo 取数（免费、无需 key、不限次数的非商业额度）。
 *
 * 只用 HttpURLConnection —— 项目里没有 OkHttp，为一次 GET 引一个网络库不划算。
 * 一律带超时；失败抛 IOException 交给 [WeatherRepository] 静默降级。
 */
object OpenMeteoClient {

    private const val BASE = "https://api.open-meteo.com/v1/forecast"

    internal fun buildUrl(lat: Double, lon: Double): String = buildString {
        append(BASE)
        append("?latitude=").append(lat)
        append("&longitude=").append(lon)
        append("&timezone=Asia%2FShanghai")
        append("&forecast_days=3")
        // past_days=1：拿昨天的最低温，才能算"今天比昨天冷多少"（降温提醒）
        append("&past_days=1")
        append("&hourly=temperature_2m,precipitation_probability,weather_code")
        append("&daily=temperature_2m_min,temperature_2m_max,weather_code,precipitation_probability_max")
    }

    /** 同步阻塞调用；必须在 IO 线程里 invoke。 */
    fun fetch(lat: Double, lon: Double, timeoutSeconds: Int = 12): String {
        val conn = (URL(buildUrl(lat, lon)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TimeUnit.SECONDS.toMillis(timeoutSeconds.toLong()).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(timeoutSeconds.toLong()).toInt()
            setRequestProperty("User-Agent", "EasyKeBiao/Android (weather)")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) error("Open-Meteo HTTP $code")
            return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            runCatching { conn.disconnect() }
        }
    }
}
