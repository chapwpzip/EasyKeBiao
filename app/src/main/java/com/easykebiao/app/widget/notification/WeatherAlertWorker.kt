package com.easykebiao.app.widget.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.easykebiao.app.data.weather.WeatherRepository
import java.util.concurrent.TimeUnit

/**
 * 天气巡检（0.14.0，2026-11-22）。
 *
 * 每 3 小时跑一次：拉一次天气 → 看接下来 3 小时内要开始的课 → 该提醒就提醒。
 * **不做**精确闹钟：天气本来就有几小时的容差，用 WorkManager 的周期任务足够，
 * 也让"关掉天气开关就彻底不响"这条承诺变得容易兑现（Worker 里先查开关）。
 */
class WeatherAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        return runCatching {
            val snapshot = WeatherRepository.refreshIfStale(ctx, force = true)
            if (snapshot != null) WeatherAlert.runOnce(ctx, snapshot)
            Result.success()
        }.getOrElse { Result.success() }   // 天气失败永远不重试排队，等下一轮
    }

    companion object {
        private const val WORK_NAME = "weather_alert_periodic"

        fun schedule(context: Context) {
            runCatching {
                val request = PeriodicWorkRequestBuilder<WeatherAlertWorker>(3, TimeUnit.HOURS)
                    .setInitialDelay(10, TimeUnit.MINUTES)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }

        fun cancel(context: Context) {
            runCatching { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }
        }
    }
}
