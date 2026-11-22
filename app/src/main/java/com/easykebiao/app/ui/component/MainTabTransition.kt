package com.easykebiao.app.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.zIndex

/**
 * 同级 tab 之间只做交叉淡入（不做位移/缩放）：Dock 切换是"面板换内容"，
 * 苹果的 TabBar 也是直接换。时长取 AppleTabMillis(200ms)——比页面推入短，切换要轻。
 */
@Composable
fun <T> MainTabTransition(selected: T, content: @Composable (T) -> Unit) {
    Crossfade(
        targetState = selected,
        modifier = Modifier.fillMaxSize(),
        animationSpec = tween(durationMillis = AppleTabMillis, easing = ApplePushEasing),
        label = "Main tab content"
    ) { visibleTab ->
        val active = visibleTab == selected
        Box(
            Modifier.fillMaxSize().zIndex(if (active) 1f else 0f).then(
                if (active) Modifier else Modifier.clearAndSetSemantics { }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    }
            )
        ) { content(visibleTab) }
    }
}
