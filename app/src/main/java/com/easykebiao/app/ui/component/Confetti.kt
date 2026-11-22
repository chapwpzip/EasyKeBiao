package com.easykebiao.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 勾掉一条待办时撒的礼花。
 *
 * 刻意不引第三方 confetti 库：这里只用到「一堆带重力的彩色小纸片」这一条物理，
 * 自己画 42 个粒子比拉一个依赖更可控，也不会被别人的动画时长绑住。
 *
 * 用法：
 * ```
 * val confetti = remember { ConfettiController() }
 * Box { 列表(); ConfettiHost(confetti, Modifier.matchParentSize()) }
 * // 勾选成功时：confetti.burst(局部坐标)
 * ```
 */
class ConfettiController {

    internal val bursts = mutableStateListOf<ConfettiBurst>()

    /**
     * 在 [local] 撒一把。坐标是相对 [ConfettiHost] 的局部坐标 ——
     * 调用方用 `positionInRoot()` 拿到行位置后减去 overlay 原点再传进来。
     */
    fun burst(local: Offset) {
        bursts.add(ConfettiBurst(start = local, seed = System.nanoTime()))
    }

    internal fun remove(burst: ConfettiBurst) {
        bursts.remove(burst)
    }
}

internal class ConfettiBurst(val start: Offset, seed: Long) {
    private val random = Random(seed)

    val pieces: List<Piece> = List(COUNT) { index ->
        // 向上为主的一圈扇形，左右撒开
        val angle = -PI / 2 + (random.nextFloat() - 0.5f) * (PI * 1.15f)
        val speed = 620f + random.nextFloat() * 780f
        Piece(
            vx = (cos(angle) * speed).toFloat(),
            vy = (sin(angle) * speed).toFloat(),
            size = 7f + random.nextFloat() * 7f,
            color = PALETTE[index % PALETTE.size],
            spin = (random.nextFloat() - 0.5f) * 900f,
            round = random.nextBoolean(),
            delay = random.nextFloat() * 0.08f
        )
    }

    class Piece(
        val vx: Float,
        val vy: Float,
        val size: Float,
        val color: Color,
        val spin: Float,
        val round: Boolean,
        val delay: Float
    )

    private companion object {
        const val COUNT = 42
        val PALETTE = listOf(
            Color(0xFFFF3B30),
            Color(0xFFFF9500),
            Color(0xFFFFCC00),
            Color(0xFF34C759),
            Color(0xFF007AFF),
            Color(0xFFAF52DE),
            Color(0xFFFF2D55)
        )
    }
}

private const val DURATION_MS = 1050
private const val GRAVITY = 1650f
private const val DRAG = 0.86f

@Composable
fun ConfettiHost(controller: ConfettiController, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        // 复制一份再遍历：动画结束会在遍历过程中从源列表移除
        controller.bursts.toList().forEach { burst ->
            key(burst) { ConfettiLayer(burst) { controller.remove(burst) } }
        }
    }
}

@Composable
private fun ConfettiLayer(burst: ConfettiBurst, onFinish: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(burst) {
        progress.animateTo(1f, tween(durationMillis = DURATION_MS))
        onFinish()
    }
    val p = progress.value
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (p <= 0f) return@Canvas
        val t = p * DURATION_MS / 1000f
        burst.pieces.forEach { piece ->
            val local = (t - piece.delay).coerceAtLeast(0f)
            if (local <= 0f) return@forEach
            // 位移按 20 段数值积分（阻尼 + 重力），42 个粒子的开销可忽略
            var x = burst.start.x
            var y = burst.start.y
            var vx = piece.vx
            var vy = piece.vy
            val steps = 20
            val dt = local / steps
            repeat(steps) {
                x += vx * dt
                y += vy * dt
                vy += GRAVITY * dt
                vx *= DRAG
                vy *= DRAG
            }
            val fade = if (p > 0.72f) (1f - p) / 0.28f else 1f
            val color = piece.color.copy(alpha = fade.coerceIn(0f, 1f))
            rotate(degrees = piece.spin * local, pivot = Offset(x, y)) {
                if (piece.round) {
                    drawCircle(color, radius = piece.size / 2f, center = Offset(x, y))
                } else {
                    drawRect(
                        color = color,
                        topLeft = Offset(x - piece.size / 2f, y - piece.size / 2f),
                        size = Size(piece.size, piece.size * 0.62f)
                    )
                }
            }
        }
    }
}
