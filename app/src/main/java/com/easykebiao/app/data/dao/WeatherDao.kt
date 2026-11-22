package com.easykebiao.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.easykebiao.app.data.entity.WeatherCacheEntity

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather_cache WHERE id = 1")
    suspend fun getOnce(): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache")
    suspend fun clear()
}
