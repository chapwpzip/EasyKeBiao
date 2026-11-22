package com.easykebiao.app.widget

import android.content.Context
import com.easykebiao.app.SleepyApp
import com.easykebiao.app.data.entity.TodoEntity
import kotlinx.coroutines.runBlocking

/**
 * 组件的数据入口：组件运行在 App 同进程里，直接复用 [SleepyApp] 的 Room 实例。
 * 读失败（库还没初始化等）返回空表 —— 组件应该显示"暂无待办"，而不是崩在桌面上。
 */
object AppWidgetData {

    fun todos(context: Context): List<TodoEntity> = runCatching {
        runBlocking { SleepyApp.get().database.todoDao().allOnce() }
    }.getOrDefault(emptyList())
}
