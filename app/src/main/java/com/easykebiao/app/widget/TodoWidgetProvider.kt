package com.easykebiao.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.easykebiao.app.MainActivity
import com.easykebiao.app.R
import com.easykebiao.app.SleepyApp
import com.easykebiao.app.data.entity.TodoEntity
import com.easykebiao.app.widget.notification.TodoReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 待办组件（4×2，可拉伸）。
 *
 * 从「一张位图」改成 RemoteViews 行列表，只为这一件事：**能在桌面上直接勾掉**。
 * 位图版没有可点的子区域，勾选必须进 App；现在每一行的圈是独立点击目标（广播回来改库再重推）。
 *
 * 数据变化（App 内增删改 / 闹钟响）由 [refresh] 主动推，不依赖 updatePeriodMillis 那 30 分钟。
 */
class TodoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, awm: AppWidgetManager, ids: IntArray) {
        push(context.applicationContext, awm, ids)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        awm: AppWidgetManager,
        id: Int,
        newOptions: Bundle
    ) {
        push(context.applicationContext, awm, intArrayOf(id))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (todoId <= 0L) return
        val app = context.applicationContext
        // 广播里改库：一条记录，几十毫秒，收完就走
        var justCompleted: TodoEntity? = null
        runCatching {
            runBlocking {
                val dao = SleepyApp.get().database.todoDao()
                val todo = dao.getById(todoId) ?: return@runBlocking
                val now = System.currentTimeMillis()
                val done = !todo.isCompleted
                dao.setCompleted(todoId, done, if (done) now else null, now)
                if (done) {
                    justCompleted = todo.copy(isCompleted = true, completedAt = now)
                    TodoReminderScheduler.cancel(app, todoId)
                    TodoReminderScheduler.dismissNotification(app, todoId)
                }
            }
        }.onFailure { Log.w(TAG, "toggle failed id=$todoId", it) }

        val awm = AppWidgetManager.getInstance(app)
        if (awm != null) {
            val ids = if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                intArrayOf(widgetId)
            } else {
                idsOf(app, awm)
            }
            val celebrate = justCompleted
            if (celebrate != null) {
                celebrateThenSettle(app, awm, ids, celebrate)
            } else {
                push(app, awm, ids)
            }
        }
    }

    companion object {
        private const val TAG = "TodoWidget"
        private const val ACTION_TOGGLE = "com.easykebiao.app.widget.TODO_TOGGLE"
        private const val EXTRA_TODO_ID = "todoId"
        private const val MAX_ROWS = 6
        private const val FALLBACK_WIDTH_DP = 250
        private const val FALLBACK_HEIGHT_DP = 110
        /** 实心勾单独停一下，让「圈 → 勾」这一跳看得见，再叠彩带 */
        private const val CHECK_HOLD_MILLIS = 180L
        /** 彩带那张图停留多久 */
        private const val CONFETTI_MILLIS = 1200L

        private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** 应用内数据变了就调它；没有放置组件时直接返回。 */
        fun refresh(context: Context) {
            val app = context.applicationContext
            val awm = AppWidgetManager.getInstance(app) ?: return
            val ids = idsOf(app, awm)
            if (ids.isEmpty()) return
            push(app, awm, ids)
        }

        private fun idsOf(app: Context, awm: AppWidgetManager): IntArray = try {
            awm.getAppWidgetIds(ComponentName(app, TodoWidgetProvider::class.java)) ?: IntArray(0)
        } catch (t: Throwable) {
            Log.w(TAG, "getAppWidgetIds failed", t)
            IntArray(0)
        }

        private fun push(app: Context, awm: AppWidgetManager, ids: IntArray) {
            ioScope.launch {
                for (id in ids) {
                    if (id == AppWidgetManager.INVALID_APPWIDGET_ID) continue
                    try {
                        pushOne(app, awm, id)
                    } catch (t: Throwable) {
                        Log.e(TAG, "push failed id=$id", t)
                    }
                }
            }
        }

        /**
         * 勾掉一条之后的三帧（桌面组件放不了动画，只能一帧帧推）：
         * ① 那一行变成实心勾 → ② 叠上彩带 → ③ 1.2 秒后回到正常列表（已完成的不再显示）。
         * 放在同一个协程里顺序执行，避免三帧乱序落到桌面上。
         */
        private fun celebrateThenSettle(app: Context, awm: AppWidgetManager, ids: IntArray, todo: TodoEntity) {
            ioScope.launch {
                val targets = ids.filter { it != AppWidgetManager.INVALID_APPWIDGET_ID }
                suspend fun frame(celebrate: TodoEntity?, confetti: Boolean) {
                    for (id in targets) {
                        try {
                            pushOne(app, awm, id, celebrate, confetti)
                        } catch (t: Throwable) {
                            Log.e(TAG, "celebrate frame failed id=$id", t)
                        }
                    }
                }
                frame(todo, false)
                delay(CHECK_HOLD_MILLIS)
                frame(todo, true)
                delay(CONFETTI_MILLIS)
                frame(null, false)
            }
        }

        private fun pushOne(
            app: Context,
            awm: AppWidgetManager,
            id: Int,
            celebrate: TodoEntity? = null,
            confetti: Boolean = false
        ) {
            val todos = AppWidgetData.todos(app)
            val open = todos.filter { !it.isCompleted }
            val today = LocalDate.now().toEpochDay()

            val views = RemoteViews(app.packageName, R.layout.widget_todo_list)
            views.setTextViewText(R.id.widget_todo_title, app.getString(R.string.widget_todo_label))
            // 计数只报「还没做完的」；空的时候留白，别和下面那行说明重复同一句话
            views.setTextViewText(
                R.id.widget_todo_count,
                if (open.isEmpty()) "" else app.getString(R.string.widget_todo_count, open.size)
            )
            views.removeAllViews(R.id.widget_todo_rows)
            views.setOnClickPendingIntent(R.id.widget_todo_header, openAppIntent(app))

            if (celebrate != null) {
                // 刚勾掉的那条先留在原位显示成「带勾 + 删除线」，让它和其他行一起参与这一帧
                views.addView(R.id.widget_todo_rows, buildRow(app, celebrate, id, today, checked = true))
            }

            // 已完成的待办不进组件：桌面上只留还要做的事
            val slots = if (celebrate != null) MAX_ROWS - 1 else MAX_ROWS
            if (open.isEmpty() && celebrate == null) {
                val empty = RemoteViews(app.packageName, R.layout.widget_todo_row)
                empty.setViewVisibility(R.id.widget_todo_check, View.INVISIBLE)
                empty.setTextViewText(
                    R.id.widget_todo_row_title,
                    app.getString(
                        if (todos.isEmpty()) R.string.widget_todo_empty else R.string.widget_todo_all_done
                    )
                )
                empty.setTextViewText(R.id.widget_todo_row_due, "")
                views.addView(R.id.widget_todo_rows, empty)
            } else {
                open.sortedWith(
                    compareBy<TodoEntity>({ it.dueDate == null }, { it.dueDate ?: Long.MAX_VALUE })
                ).take(slots.coerceAtLeast(0)).forEach { todo ->
                    views.addView(R.id.widget_todo_rows, buildRow(app, todo, id, today))
                }
            }

            if (confetti) {
                val (widthPx, heightPx) = widgetSizePx(app, awm, id)
                views.setViewVisibility(R.id.widget_todo_celebrate, View.VISIBLE)
                views.setImageViewBitmap(
                    R.id.widget_todo_celebrate,
                    WidgetConfettiRenderer.render(widthPx, heightPx, app.resources.displayMetrics.density)
                )
            } else {
                views.setViewVisibility(R.id.widget_todo_celebrate, View.GONE)
            }
            awm.updateAppWidget(id, views)
        }

        /** 组件的像素尺寸（彩带位图要按它画）；拿不到就按 4×2 的默认尺寸兜底。 */
        private fun widgetSizePx(app: Context, awm: AppWidgetManager, id: Int): Pair<Int, Int> {
            val density = app.resources.displayMetrics.density
            val options = runCatching { awm.getAppWidgetOptions(id) }.getOrNull()
            val widthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
            val heightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
            val width = ((if (widthDp > 0) widthDp else FALLBACK_WIDTH_DP) * density).toInt()
            val height = ((if (heightDp > 0) heightDp else FALLBACK_HEIGHT_DP) * density).toInt()
            return width.coerceAtLeast(1) to height.coerceAtLeast(1)
        }

        /**
         * 把待办附图解码成组件里用的小位图：目标 34dp，按 inSampleSize 抽稀，
         * 用圆角 BitmapShader 贴一层，和 App 里的缩略图观感一致。
         */
        private fun thumbBitmap(app: Context, name: String): Bitmap? = runCatching {
            val file = com.easykebiao.app.data.memo.MemoImages.file(app, name)
            if (!file.exists()) return@runCatching null
            val density = app.resources.displayMetrics.density
            val target = (34f * density).toInt().coerceAtLeast(1)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return@runCatching null
            val scaled = Bitmap.createScaledBitmap(raw, target, target, true)
            val out = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            val radius = target * 0.24f
            canvas.drawRoundRect(0f, 0f, target.toFloat(), target.toFloat(), radius, radius, paint)
            out
        }.getOrNull()

        private fun buildRow(
            app: Context,
            todo: TodoEntity,
            widgetId: Int,
            today: Long,
            checked: Boolean = false
        ): RemoteViews {
            val row = RemoteViews(app.packageName, R.layout.widget_todo_row)
            row.setTextViewText(R.id.widget_todo_row_title, todo.title.ifBlank { app.getString(R.string.memo_untitled) })
            row.setTextViewText(R.id.widget_todo_row_due, dueLabel(app, todo.dueDate, today))
            if (checked) {
                row.setImageViewResource(R.id.widget_todo_check, R.drawable.widget_todo_check_on)
                row.setInt(
                    R.id.widget_todo_row_title,
                    "setPaintFlags",
                    Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
                )
            } else {
                row.setImageViewResource(R.id.widget_todo_check, R.drawable.widget_todo_check_off)
            }
            // 附图：待办有图就在标题左侧画一张小缩略图（RemoteViews 只能塞位图）
            val thumb = todo.imagePath?.let { thumbBitmap(app, it) }
            if (thumb != null) {
                row.setViewVisibility(R.id.widget_todo_row_thumb, View.VISIBLE)
                row.setImageViewBitmap(R.id.widget_todo_row_thumb, thumb)
            } else {
                row.setViewVisibility(R.id.widget_todo_row_thumb, View.GONE)
            }
            // 圈 = 切换完成；整行 = 切换完成（点标题就是勾掉它，进 App 走顶栏）
            row.setOnClickPendingIntent(R.id.widget_todo_check, toggleIntent(app, todo.id, widgetId))
            row.setOnClickPendingIntent(R.id.widget_todo_row, toggleIntent(app, todo.id, widgetId))
            return row
        }

        private fun toggleIntent(app: Context, todoId: Long, widgetId: Int): PendingIntent {
            val intent = Intent(app, TodoWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
                putExtra(EXTRA_TODO_ID, todoId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = android.net.Uri.parse("easykebiao://widget-todo/$todoId")
            }
            return PendingIntent.getBroadcast(
                app,
                (todoId + widgetId).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** 与 App 内同一套口径：今天 / 明天 / 昨天 / N 天后 / 日期。 */
        private fun dueLabel(app: Context, dueDate: Long?, today: Long): String {
            if (dueDate == null) return ""
            return when (val diff = dueDate - today) {
                0L -> app.getString(R.string.todo_due_today)
                1L -> app.getString(R.string.todo_due_tomorrow)
                -1L -> app.getString(R.string.todo_due_yesterday)
                in 2L..6L -> app.getString(R.string.todo_days_after, diff)
                in -30L..-2L -> app.getString(R.string.todo_section_overdue)
                else -> LocalDate.ofEpochDay(dueDate).format(
                    DateTimeFormatter.ofPattern(app.getString(R.string.todo_date_pattern), Locale.getDefault())
                )
            }
        }

        private fun openAppIntent(app: Context): PendingIntent {
            val intent = Intent(app, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // App 常已在后台（singleTask → onNewIntent 复用），不带 tab 就会停在上次那页
                putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_MEMO)
            }
            return PendingIntent.getActivity(
                app,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
