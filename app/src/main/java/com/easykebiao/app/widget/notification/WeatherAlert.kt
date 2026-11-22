package com.easykebiao.app.widget.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.easykebiao.app.MainActivity
import com.easykebiao.app.R
import com.easykebiao.app.SleepyApp
import com.easykebiao.app.data.entity.CourseEntity
import com.easykebiao.app.data.weather.WeatherCode
import com.easykebiao.app.data.weather.WeatherSnapshot
import com.easykebiao.app.util.AppPrefs
import com.easykebiao.app.util.CourseDateResolver
import com.easykebiao.app.util.TimeTableUtils
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 上课天气提醒（0.14.0，2026-11-22）。
 *
 * 规则（与设置页两个滑杆一一对应，用户可调）：
 *  · 只看**接下来 3 小时内**要开始的课（再远的等下一轮 Worker）；
 *  · 该课开始那一小时的降水概率 ≥ 阈值 → 提醒带伞；
 *  · 今天最低温比昨天低 ≥ 阈值 → 提醒加衣；
 *  · 同一门课同一天只响一次（SharedPreferences 记 key，跨重启保留）。
 *
 * 它不改动既有的上课提醒链路（CourseNotificationScheduler），只多一条独立通道，
 * 关掉天气开关就等于关掉这条通道。
 */
object WeatherAlert {

    const val CHANNEL_ID = "class_weather"
    private const val SEEN_PREFS = "weather_alert_seen"
    private const val LOOKAHEAD_MINUTES = 180L
    private const val NOTIF_ID = 0x7EAD

    /** 判断结果：要不要提醒、说什么。纯函数，便于单测。 */
    internal data class Verdict(
        val notify: Boolean,
        val rainPct: Int = 0,
        val dropC: Int = 0
    )

    /**
     * 纯规则：给定这堂课的开始时刻与天气快照，判断是否要提醒。
     * 任一阈值命中即提醒；两者都命中时合成一条（不连发两条通知）。
     */
    internal fun verdict(
        snapshot: WeatherSnapshot,
        date: LocalDate,
        start: LocalTime,
        pctThreshold: Int,
        dropThreshold: Int
    ): Verdict {
        val hour = snapshot.courseHour(date, start)
        val rain = hour?.precipProb ?: 0
        val wet = hour != null && WeatherCode.isWet(hour.code)
        val rainHit = hour != null && (rain >= pctThreshold || (wet && rain >= pctThreshold / 2))
        val drop = snapshot.coolingOn(date)?.toInt() ?: 0
        val coldHit = drop >= dropThreshold
        return Verdict(notify = rainHit || coldHit, rainPct = rain, dropC = drop.coerceAtLeast(0))
    }

    /** 通知正文：按命中的条件分三种说法，别把两个条件硬拼成一句。 */
    internal fun body(context: Context, verdict: Verdict): String = when {
        verdict.rainPct > 0 && verdict.dropC > 0 ->
            context.getString(R.string.weather_notif_both, verdict.rainPct, verdict.dropC)
        verdict.dropC > 0 -> context.getString(R.string.weather_notif_cold, verdict.dropC)
        else -> context.getString(R.string.weather_notif_rain, verdict.rainPct)
    }

    private fun seenKey(courseId: Long, date: LocalDate) = "$courseId|$date"

    private fun alreadySeen(context: Context, courseId: Long, date: LocalDate): Boolean =
        context.getSharedPreferences(SEEN_PREFS, Context.MODE_PRIVATE)
            .getBoolean(seenKey(courseId, date), false)

    private fun markSeen(context: Context, courseId: Long, date: LocalDate) {
        context.getSharedPreferences(SEEN_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(seenKey(courseId, date), true).apply()
    }

    /**
     * 跑一轮检查。由 [WeatherAlertWorker] 每 3 小时调一次（也可由设置页手动触发）。
     * 任何异常都吞掉 —— 提醒失败不该影响 App。
     */
    fun runOnce(context: Context, snapshot: WeatherSnapshot) {
        runCatching {
            if (!AppPrefs.isWeatherAlertEnabled(context)) return
            val pct = AppPrefs.getWeatherAlertPct(context)
            val drop = AppPrefs.getWeatherAlertDrop(context)
            val now = LocalDateTime.now()
            val today = now.toLocalDate()

            ensureChannel(context)
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

            coursesToday(context, today).forEach { course ->
                val start = startTimeOf(course) ?: return@forEach
                val startAt = LocalDateTime.of(today, start)
                val minutes = ChronoUnit.MINUTES.between(now, startAt)
                if (minutes < 0 || minutes > LOOKAHEAD_MINUTES) return@forEach
                if (alreadySeen(context, course.id, today)) return@forEach

                val v = verdict(snapshot, today, start, pct, drop)
                if (!v.notify) return@forEach

                show(context, course, start, body(context, v))
                markSeen(context, course.id, today)
            }
        }
    }

    /** 课程开始时刻：优先作息表（startNode → 时间），ownTime 课读自己的 startTime。 */
    internal fun startTimeOf(course: CourseEntity, timeJson: String? = null): LocalTime? {
        val raw = timeJson?.let {
            TimeTableUtils.courseTimeParts(
                courseStartNode = course.startNode,
                courseStep = course.step,
                timeJson = it,
                ownTime = course.ownTime,
                startTime = course.startTime,
                endTime = course.endTime
            )?.first
        }?.trim()?.takeIf { it.isNotEmpty() }
            ?: course.startTime.trim().takeIf { it.isNotEmpty() }
        return raw?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
    }

    /** 今日课程（同一套选表/调休口径，读库失败就当作没有课）。 */
    private fun coursesToday(context: Context, date: LocalDate): List<CourseEntity> = runCatching {
        runBlocking {
            val db = SleepyApp.get().database
            val tables = db.timeTableDao().getAll()
            val table = tables.firstOrNull { it.isDefault } ?: tables.firstOrNull() ?: return@runBlocking emptyList()
            val courses = db.courseDao().getByTable(table.id)
            val transfers = AppPrefs.getHolidayTransfers(context, table.id)
            CourseDateResolver.coursesOn(date, table.startDate, table.maxWeek, courses, transfers)
                .map { c -> c }
                .sortedBy { it.startNode }
        }
    }.getOrDefault(emptyList())

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.weather_notif_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.weather_notif_channel_desc)
                enableVibration(true)
            }
        )
    }

    private fun show(context: Context, course: CourseEntity, start: LocalTime, text: String) {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            NOTIF_ID,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = context.getString(
            R.string.weather_notif_title_course,
            course.courseName,
            String.format("%02d:%02d", start.hour, start.minute)
        )
        val n: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, n)
        }
    }
}
