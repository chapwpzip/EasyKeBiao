package com.easykebiao.app.ui.nav

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.easykebiao.app.ui.component.AppleFadeMillis
import com.easykebiao.app.ui.component.ApplePushEasing
import com.easykebiao.app.ui.component.ApplePushMillis

/**
 * 页面级转场：**iOS 推入/返回**（位移为主，视差为辅）。
 *
 * 与上游原来的 Material 方案（scaleIn/scaleOut 0.92 + fade，220ms）的区别：
 * - 原方案是"卡片从中心弹出来"，观感偏 Android 大面积转场；
 * - 现在是"新页面从右侧滑入盖住旧页面，旧页面往左退一小段并淡出"，
 *   这是 UIKit 里 push/pop 的物理关系，也是用户要的苹果感来源。
 *
 * 关键比例（照 UIKit 的手感定，不要随手改）：
 * - 新页从 +1.0 幅宽滑到 0；旧页只退 1/4 幅宽（视差，不全屏滑走，才不会"两页一起跑"）。
 * - 透明度只走 180ms：位移是主角，透明度再长就糊成两团灰。
 * - 预测性返回（手势拖动）只做可 seek 的位移，进度由 Navigation3 控制。
 *
 * Navigation3 负责 seek、取消与 commit，这里只提供有限时长的 transform，
 * 不再二次动画、也不延迟导航回调。
 */

internal const val ForwardDuration: Int = ApplePushMillis

/** 转场透明度时长（保留旧名，报告与测试引用它）。 */
internal const val ForwardFadeIn: Int = AppleFadeMillis

internal const val PredictiveDuration: Int = 200

/** 旧页退让比例：1/4 幅宽。 */
private const val ParallaxFraction = 0.25f

private val forwardSlideSpec: TweenSpec<IntOffset> = tween(
    durationMillis = ForwardDuration,
    easing = ApplePushEasing,
)

private val forwardFadeInTween: TweenSpec<Float> = tween(
    durationMillis = ForwardFadeIn,
    easing = ApplePushEasing,
)

private val predictiveCommitTween: TweenSpec<Float> = tween(
    durationMillis = PredictiveDuration,
    easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
)
internal val PredictiveTweenRef: TweenSpec<Float> = predictiveCommitTween

private val predictiveSlideSpec: TweenSpec<IntOffset> = tween(
    durationMillis = PredictiveDuration,
    easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
)

/**
 * forward (push): 新页从右侧整幅滑入 + 短淡入。
 */
internal fun sleepyForwardEnter(): EnterTransition {
    return slideInHorizontally(animationSpec = forwardSlideSpec) { full -> full } +
        fadeIn(forwardFadeInTween)
}

/**
 * forward (push): 旧页向左退 1/4 幅宽（视差）+ 淡出。
 */
internal fun sleepyForwardExit(): ExitTransition {
    return slideOutHorizontally(animationSpec = forwardSlideSpec) { full ->
        -(full * ParallaxFraction).toInt()
    } + fadeOut(forwardFadeInTween)
}

/**
 * pop (程序化返回): 上一页从左侧 1/4 处滑回原位 —— 与 forward 的视差严格对称，
 * 所以"进去再出来"不会出现两次方向不一致的位移。
 */
internal fun sleepyPopEnter(): EnterTransition {
    return slideInHorizontally(animationSpec = forwardSlideSpec) { full ->
        -(full * ParallaxFraction).toInt()
    } + fadeIn(forwardFadeInTween)
}

/**
 * pop: 当前页整幅向右滑出（盖回去的物理关系）+ 淡出。
 */
internal fun sleepyPopExit(): ExitTransition {
    return slideOutHorizontally(animationSpec = forwardSlideSpec) { full -> full } +
        fadeOut(forwardFadeInTween)
}

/**
 * NavDisplay transitionSpec 入参 — 推进时新页 enter + 旧页 exit 同时启动。
 */
internal val sleepyForwardTransform: ContentTransform = ContentTransform(
    targetContentEnter = sleepyForwardEnter(),
    initialContentExit = sleepyForwardExit(),
    targetContentZIndex = 1f,
)

/**
 * NavDisplay popTransitionSpec 入参 — 程序化 pop。
 */
internal val sleepyPopTransform: ContentTransform = ContentTransform(
    targetContentEnter = sleepyPopEnter(),
    initialContentExit = sleepyPopExit(),
    targetContentZIndex = -1f,
)

/** Seekable template; NavDisplay controls progress and cancellation. */
internal val sleepyPredictivePopTransform: ContentTransform = ContentTransform(
    // 手势拖动时"当前页跟着手指往右走、上一页从左边跟上来"，进度由 Navigation3 seek。
    targetContentEnter = slideInHorizontally(animationSpec = predictiveSlideSpec) { full ->
        -(full * ParallaxFraction).toInt()
    },
    initialContentExit = slideOutHorizontally(animationSpec = predictiveSlideSpec) { full -> full } +
        fadeOut(predictiveCommitTween),
    targetContentZIndex = -1f,
)
