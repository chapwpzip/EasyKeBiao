package com.easykebiao.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.easykebiao.app.data.dao.CourseDao
import com.easykebiao.app.data.dao.ImportDraftDao
import com.easykebiao.app.data.dao.MemoDao
import com.easykebiao.app.data.dao.PeriodTableDao
import com.easykebiao.app.data.dao.TimeTableDao
import com.easykebiao.app.data.dao.TodoDao
import com.easykebiao.app.data.dao.WeatherDao
import com.easykebiao.app.data.entity.CourseEntity
import com.easykebiao.app.data.entity.ImportDraftEntity
import com.easykebiao.app.data.entity.MemoEntity
import com.easykebiao.app.data.entity.PeriodTableEntity
import com.easykebiao.app.data.entity.TimeTableEntity
import com.easykebiao.app.data.entity.TodoEntity
import com.easykebiao.app.data.entity.TodoStepEntity
import com.easykebiao.app.data.entity.WeatherCacheEntity

@Database(
    entities = [CourseEntity::class, TimeTableEntity::class, PeriodTableEntity::class, ImportDraftEntity::class, MemoEntity::class, TodoEntity::class, TodoStepEntity::class, WeatherCacheEntity::class],
    version = 15,                            // 14 → 15: 待办加 imagePath(附图文件名), 仅 ADD COLUMN
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao
    abstract fun timeTableDao(): TimeTableDao
    abstract fun periodTableDao(): PeriodTableDao
    abstract fun importDraftDao(): ImportDraftDao
    abstract fun memoDao(): MemoDao
    abstract fun todoDao(): TodoDao
    abstract fun weatherDao(): WeatherDao

    companion object {
        private const val DB_NAME = "sleepy.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    // 严禁 fallbackToDestructiveMigration — 任意版本升会清空用户课表
                    // 任何 schema 改动必须先在 Migrations.kt 登记, 再升 version
                    .build()
                    .also { instance = it }
            }
        }
    }
}
