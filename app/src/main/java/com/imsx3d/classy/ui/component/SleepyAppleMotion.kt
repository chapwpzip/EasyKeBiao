package com.imsx3d.classy.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 苹果（iOS）动效令牌 —— 只放"时间/曲线"，不碰任何业务状态。
 *
 * 为什么单独一套：Material 的 motionScheme 是"弹性+位移"的表达，iOS 是"跟手+惯性的位移"，
 * 同一段转场两者观感差别很大。这里把 iOS 的两个习惯固化下来，全 app 统一取用：
 * 1. 推入/返回：位移为主、透明度其次，340ms，曲线 (0.32, 0.72, 0, 1) —— 起手快、尾巴长，
 *    这是 UIKit 转场最接近的手感；比 Material 的 scaleIn 更"页面在滑"而不是"卡片在弹"。
 * 2. 按压反馈：按住立刻缩到 0.96，松手用高硬度低回弹的 spring 回位 —— 苹果控件的反馈是
 *    "贴手"而不是"弹一下"，所以阻尼比取 0.62、刚度 900（偏硬、几乎不回弹）。
 *
 * 修改这里 = 全 app 同步，业务页不许自己写时长与曲线（沿用 DESIGN_SYSTEM.md 的规则）。
 */
val ApplePushEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/** 推入/返回的位移时长（ms）。 */
const val ApplePushMillis: Int = 340

/** 转场里透明度的时长（ms）：比位移短，避免"两层一起灰"。 */
const val AppleFadeMillis: Int = 180

/** 同级 tab 切换的交叉淡入（ms）：比页面推入短，切换要"轻"。 */
const val AppleTabMillis: Int = 200

/** 推入位移用 tween（可被预测性返回 seek）。 */
fun applePushSpec() = tween<Float>(durationMillis = ApplePushMillis, easing = ApplePushEasing)

/** 位移类（slideIn/Out、slideInVertically）要的是 IntOffset spec，不能复用 Float 的。 */
fun appleSlideSpec() = tween<IntOffset>(durationMillis = ApplePushMillis, easing = ApplePushEasing)

/** 转场透明度。 */
fun appleFadeSpec() = tween<Float>(durationMillis = AppleFadeMillis, easing = ApplePushEasing)

/** 按压回弹：硬、快、几乎不回弹。 */
fun applePressSpec() = spring<Float>(
    dampingRatio = 0.62f,
    stiffness = 900f
)

/**
 * 按住缩小、松手回位。只观察指针事件（Initial pass 且不消费），
 * 因此可以叠在任何 clickable 外层，不影响点击、长按和滑动。
 */
fun Modifier.applePressScale(pressedScale: Float = 0.96f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = applePressSpec(),
        label = "applePressScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(pressedScale) {
            val slop = viewConfiguration.touchSlop
            awaitPointerEventScope {
                var downAt: Offset? = null
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    when (event.type) {
                        PointerEventType.Press -> {
                            pressed = true
                            downAt = event.changes.firstOrNull()?.position
                        }
                        // 手指移动超过触摸阈值就当作滚动，立刻回位；否则列表滚动时
                        // 整列条目会一直保持"被按住"的缩小状态。
                        PointerEventType.Move -> {
                            val start = downAt
                            val now = event.changes.firstOrNull()?.position
                            if (start != null && now != null && (now - start).getDistance() > slop) {
                                pressed = false
                            }
                        }
                        PointerEventType.Release, PointerEventType.Exit -> {
                            pressed = false
                            downAt = null
                        }
                    }
                }
            }
        }
}

/**
 * 列表首次出现的"错峰浮现"：给第 index 个条目一点延迟，整体读起来像一次动效而不是
 * 一堆元素同时蹦出来。只用于静态表（设置项、统计格），列表数据变化不要用。
 */
@Composable
fun AppleStagger(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(30L * index.coerceAtMost(8))
        visible = true
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = tween(durationMillis = 240, easing = ApplePushEasing)
        ) + androidx.compose.animation.slideInVertically(
            animationSpec = appleSlideSpec(),
            initialOffsetY = { it / 6 }
        )
    ) {
        Box { content() }
    }
}
