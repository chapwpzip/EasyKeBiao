package com.easykebiao.app.widget

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * issue#44 调休映射合约锁: widget / 通知必须用共享 helper,
 * 不能直接取 date.dayOfWeek.value 或 DateUtils.todayDayOfWeek。
 */
class HolidayTransferWidgetContractTest {

    private fun src(rel: String): String = sequenceOf(
        System.getProperty("sleepy.test.root")?.let { java.io.File(it, "app/$rel") },
        java.io.File("app/$rel"),
    ).filterNotNull().firstOrNull { it.isFile }?.readText() ?: error("Unable to load $rel")

    private val consumers = listOf(
        "src/main/java/com/easykebiao/app/widget/TodayWidget.kt",
        "src/main/java/com/easykebiao/app/widget/TwoDayWidget.kt",
        "src/main/java/com/easykebiao/app/widget/WeekViewWidget.kt",
        "src/main/java/com/easykebiao/app/widget/WeekListWidget.kt",
        "src/main/java/com/easykebiao/app/widget/WeekGridWidgetProvider.kt",
        "src/main/java/com/easykebiao/app/widget/WidgetCompactWindow.kt",
        "src/main/java/com/easykebiao/app/widget/notification/CourseNotificationScheduler.kt"
    )

    @Test
    fun all_consumers_use_shared_date_and_week_resolution() {
        consumers.forEach { rel ->
            val text = src(rel)
            assertTrue(
                "$rel must resolve full course dates, including source week parity",
                text.contains("coursesOn(")
            )
        }
    }
}
