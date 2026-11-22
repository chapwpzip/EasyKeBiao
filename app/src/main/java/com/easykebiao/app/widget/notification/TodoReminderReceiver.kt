package com.easykebiao.app.widget.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.easykebiao.app.SleepyApp
import com.easykebiao.app.widget.TodoWidgetProvider
import kotlinx.coroutines.runBlocking

/**
 * 待办闹钟的实际执行者。
 *
 * - [TodoReminderScheduler.ACTION_FIRE]：到点 → 拉一条通知（带「完成 / 稍后」两个按钮）
 * - [TodoReminderScheduler.ACTION_COMPLETE]：用户点通知上的「完成」→ 直接勾掉、撤闹钟、清通知
 * - [TodoReminderScheduler.ACTION_SNOOZE]：点「稍后」→ 顺延 10 分钟再响
 *
 * 收广播时进程可能刚被拉起，数据库访问走 runBlocking（只有一条记录，几十毫秒）。
 */
class TodoReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(TodoReminderScheduler.EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) return
        val app = SleepyApp.get()
        val dao = app.database.todoDao()
        when (intent.action) {
            TodoReminderScheduler.ACTION_FIRE -> {
                val todo = runCatching { runBlocking { dao.getById(todoId) } }.getOrNull() ?: return
                if (todo.isCompleted) {
                    TodoReminderScheduler.cancel(context, todoId)
                    return
                }
                TodoReminderScheduler.showNotification(context, todo)
            }

            TodoReminderScheduler.ACTION_COMPLETE -> {
                runCatching {
                    runBlocking {
                        val now = System.currentTimeMillis()
                        dao.setCompleted(todoId, true, now, now)
                    }
                }
                TodoReminderScheduler.cancel(context, todoId)
                TodoReminderScheduler.dismissNotification(context, todoId)
                runCatching { TodoWidgetProvider.refresh(app) }
            }

            TodoReminderScheduler.ACTION_SNOOZE -> {
                TodoReminderScheduler.dismissNotification(context, todoId)
                runCatching { TodoReminderScheduler.snooze(context, todoId) }
            }
        }
    }
}

/** 重启后 AlarmManager 的登记不保留，开机补排一次。 */
class TodoReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        runCatching { TodoReminderScheduler.rescheduleAll(context.applicationContext) }
    }
}
