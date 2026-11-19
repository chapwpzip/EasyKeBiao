package com.imsx3d.classy.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 桌面组件的「彩带」帧。
 *
 * 为什么只能画一张图：RemoteViews 里没有属性动画、没有自定义 View，桌面上的组件由**别的进程**渲染，
 * 我们只能一帧一帧地把 RemoteViews 推过去。所以「彩带飞出」做成两帧：
 * 第一帧把勾选的行换成实心勾，第二帧在同一张卡上叠一层随机彩带位图，1.2 秒后推回正常列表。
 *
 * 片子的分布刻意贴着卡片上部往两侧散开（和 App 内的 Confetti 一个方向感），中间留白不给文字压住。
 */
object WidgetConfettiRenderer {

    private val PALETTE = intArrayOf(
        Color.parseColor("#FF3B30"),
        Color.parseColor("#FF9500"),
        Color.parseColor("#FFCC00"),
        Color.parseColor("#34C759"),
        Color.parseColor("#007AFF"),
        Color.parseColor("#AF52DE"),
        Color.parseColor("#FF2D55"),
        Color.parseColor("#5AC8FA")
    )

    fun render(widthPx: Int, heightPx: Int, density: Float, seed: Long = System.currentTimeMillis()): Bitmap {
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val random = Random(seed)

        val originX = w * 0.42f
        val originY = h * 0.52f
        val reach = minOf(w.toFloat(), h.toFloat())

        repeat(PIECES) { index ->
            // 朝上偏左/右的扇形，越靠边飞得越远
            val spread = (random.nextFloat() - 0.5f) * 2.0f
            val angle = (-Math.PI / 2.0 + spread * 1.05).toFloat()
            val distance = (0.22f + random.nextFloat() * 0.72f) * reach * 1.5f
            val x = originX + cos(angle) * distance
            val y = originY + sin(angle) * distance + distance * 0.34f

            paint.color = PALETTE[random.nextInt(PALETTE.size)]
            paint.alpha = 170 + random.nextInt(80)

            val size = (5.0f + random.nextFloat() * 8.0f) * density / 2.4f
            val tall = size * (0.45f + random.nextFloat() * 0.55f)

            canvas.save()
            canvas.rotate(random.nextFloat() * 360f, x, y)
            if (index % 5 == 0) {
                canvas.drawCircle(x, y, size * 0.42f, paint)
            } else {
                canvas.drawRect(RectF(x - size / 2f, y - tall / 2f, x + size / 2f, y + tall / 2f), paint)
            }
            canvas.restore()
        }
        return bitmap
    }

    private const val PIECES = 30
}
