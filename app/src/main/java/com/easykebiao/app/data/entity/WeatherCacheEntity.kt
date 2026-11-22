package com.easykebiao.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 天气缓存 —— 单行表（id 固定 1）。
 *
 * 只存「最近一次成功拉取」的原始 JSON 与拉取时刻；离线/请求失败时用它兜底，
 * 是否过期由 WeatherRepository 判定（TTL），不在这里写过期逻辑。
 *
 * 2026-11-22 新增（0.14.0）：DB 13→14 为纯 CREATE TABLE，不触碰任何既有表。
 */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "cityName") val cityName: String = "",
    @ColumnInfo(name = "lat") val lat: Double = 0.0,
    @ColumnInfo(name = "lon") val lon: Double = 0.0,
    /** 拉取成功的时刻（epoch millis），0 表示从未成功过。 */
    @ColumnInfo(name = "fetchedAt") val fetchedAt: Long = 0L,
    /** Open-Meteo 原始响应（不含任何用户数据）。 */
    @ColumnInfo(name = "payloadJson") val payloadJson: String = ""
)
