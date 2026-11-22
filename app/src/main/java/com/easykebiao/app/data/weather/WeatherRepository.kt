package com.easykebiao.app.data.weather

import android.content.Context
import com.easykebiao.app.data.AppDatabase
import com.easykebiao.app.data.entity.WeatherCacheEntity
import com.easykebiao.app.util.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 天气仓库 —— 进程内一份内存快照 + Room 一份持久缓存。
 *
 * 设计取舍：
 *  · UI 读 [memory]（同步、可被 Compose 直接读），不碰数据库；
 *  · 拉取一律走 [refreshIfStale]，失败**静默**（返回旧快照或 null），天气不该弹错误框；
 *  · 城市与开关都在 AppPrefs，换城市会把缓存作废（避免显示上一个城市的天气）。
 */
object WeatherRepository {

    @Volatile
    private var memory: WeatherSnapshot? = null

    @Volatile
    private var loadedFromDisk = false

    /** UI 用：当前可显示的快照（没有就是 null）。 */
    fun peek(): WeatherSnapshot? = memory

    /** 供测试/切换城市时清空内存态。 */
    fun forget() {
        memory = null
        loadedFromDisk = false
    }

    /**
     * 需要时拉一次。
     * @param force 忽略 TTL（设置页"立即刷新"、城市切换后）
     * @return 可用于显示的快照（可能是旧的）；从未成功过则为 null
     */
    suspend fun refreshIfStale(context: Context, force: Boolean = false): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            if (!AppPrefs.isWeatherEnabled(context)) return@withContext null
            val city = AppPrefs.getWeatherCity(context)
            val dao = AppDatabase.get(context).weatherDao()

            // 1) 进程内没有就从磁盘捞（含"关掉再打开"的场景）
            if (!loadedFromDisk || memory == null) {
                val row = dao.getOnce()
                if (row != null && row.cityName == city.name && row.payloadJson.isNotBlank()) {
                    WeatherParse.parse(row.cityName, row.lat, row.lon, row.fetchedAt, row.payloadJson)
                        ?.let { parsed ->
                            memory = parsed
                            loadedFromDisk = true
                        }
                } else {
                    loadedFromDisk = true
                }
            }

            val current = memory
            if (!force && current != null && current.city == city.name && !current.isStale()) {
                return@withContext current
            }

            // 2) 真正去拉：失败就把旧数据留着（哪怕过期），一次都不成功才返回 null
            val fetchedAt = System.currentTimeMillis()
            val payload = runCatching { OpenMeteoClient.fetch(city.lat, city.lon) }.getOrNull()
            if (payload == null) return@withContext current

            val parsed = WeatherParse.parse(city.name, city.lat, city.lon, fetchedAt, payload)
                ?: return@withContext current

            runCatching {
                dao.upsert(
                    WeatherCacheEntity(
                        id = 1,
                        cityName = city.name,
                        lat = city.lat,
                        lon = city.lon,
                        fetchedAt = fetchedAt,
                        payloadJson = payload
                    )
                )
            }
            memory = parsed
            parsed
        }
}
