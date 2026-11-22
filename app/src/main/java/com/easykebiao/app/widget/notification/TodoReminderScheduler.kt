package com.easykebiao.app.widget.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.easykebiao.app.R
import com.easykebiao.app.data.entity.TodoEntity
import kotlinx.coroutines.runBlocking

/**
 * 待办闹钟：一条待办最多一个 [AlarmManager] 闹钟，按 [TodoEntity.reminderAt] 的绝对时刻触发。
 *
 * 刻意不掺进课表那套 [CourseNotificationScheduler]（那里面有厂商流体云/一轮多通知的复杂逻辑），
 * 待办提醒只要「到点响一声、点完成或稍后」这一条直线路径。
 *
 * 精确闹钟：Android 12+ 用户可能关掉「允许设定精确闹钟」，此时降级成 `setWindow`（±10 分钟），
 * 提醒仍然会响，只是不保证准点 —— 不为一个待办去要一个可能被拒的系统权限。
 */
object TodoReminderScheduler {

    const val CHANNEL_ID = "todo_reminder"
    const val EXTRA_TODO_ID = "todoId"
    const val ACTION_FIRE = "com.easykebiao.app.action.TODO_REMIND"
    const val ACTION_COMPLETE = "com.easykebiao.app.action.TODO_REMIND_COMPLETE"
    const val ACTION_SNOOZE = "com.easykebiao.app.action.TODO_REMIND_SNOOZE"

    /** 稍后再响的间隔 */
    const val SNOOZE_MILLIS = 10 * 60 * 1000L

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun pendingIntent(context: Context, todoId: Long, action: String): PendingIntent {
        val intent = Intent(context, TodoReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TODO_ID, todoId)
            // requestCode 用 id，不同待办的闹钟互不覆盖
            data = android.net.Uri.parse("easykebiao://todo/$todoId")
        }
        return PendingIntent.getBroadcast(
            context,
            todoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 重新登记一条待办的闹钟（先撤旧的，避免重复响）。 */
    fun schedule(context: Context, todo: TodoEntity) {
        cancel(context, todo.id)
        val at = todo.reminderAt ?: return
        if (todo.isCompleted) return
        if (at <= System.currentTimeMillis()) return
        val am = alarmManager(context)
        val pi = pendingIntent(context, todo.id, ACTION_FIRE)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        runCatching {
            if (exactAllowed) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60 * 1000L, pi)
            }
        }
    }

    fun cancel(context: Context, todoId: Long) {
        runCatching {
            alarmManager(context).cancel(pendingIntent(context, todoId, ACTION_FIRE))
        }
    }

    fun snooze(context: Context, todoId: Long) {
        val dao = com.easykebiao.app.SleepyApp.get().database.todoDao()
        runBlocking {
            val todo = dao.getById(todoId) ?: return@runBlocking
            val at = System.currentTimeMillis() + SNOOZE_MILLIS
            dao.setReminderAt(todoId, at)
            schedule(context, todo.copy(reminderAt = at))
        }
    }

    /** 开机 / 升级后把还没响的闹钟全部重排（AlarmManager 的登记在重启后不保留）。 */
    fun rescheduleAll(context: Context) {
        val dao = com.easykebiao.app.SleepyApp.get().database.todoDao()
        val pending = runCatching { runBlocking { dao.pendingRemindersOnce() } }.getOrDefault(emptyList())
        pending.forEach { schedule(context, it) }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.todo_reminder_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.todo_reminder_channel_desc)
                enableVibration(true)
            }
        )
    }

    /** 到点那条通知：点「完成」直接勾掉，点「稍后」10 分钟后再响。 */
    fun showNotification(context: Context, todo: TodoEntity) {
        ensureChannel(context)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val open = Intent(context, com.easykebiao.app.MainActivity::class.java).apply {
            putExtra(
                com.easykebiao.app.MainActivity.EXTRA_OPEN_TAB,
                com.easykebiao.app.MainActivity.TAB_MEMO
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            todo.id.toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = todo.notes.ifBlank { context.getString(R.string.todo_reminder_body) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(todo.title.ifBlank { context.getString(R.string.memo_untitled) })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(
                0,
                context.getString(R.string.todo_notif_complete),
                pendingIntent(context, todo.id, ACTION_COMPLETE)
            )
            .addAction(
                0,
                context.getString(R.string.todo_notif_snooze),
                pendingIntent(context, todo.id, ACTION_SNOOZE)
            )
            .build()
        runCatching { nm.notify(todo.id.toInt(), notification) }
    }

    fun dismissNotification(context: Context, todoId: Long) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching { nm.cancel(todoId.toInt()) }
    }
}
