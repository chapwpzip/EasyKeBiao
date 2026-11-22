package com.easykebiao.app.ui.component

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.easykebiao.app.ui.theme.DockMaterialPolicy
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

/**
 * 毛玻璃（iOS 26 liquid glass）底板。
 *
 * 原理：玻璃元素要"看见"它下面画了什么，所以**被采样的内容**得单独画进一个 layer，
 * 玻璃自己再从这个 layer 里读像素做模糊。调用方按三步走：
 *
 * 1. `val backdrop = rememberAppGlassBackdrop()`  —— 在页面/容器里持有一份；
 * 2. 内容容器挂 `Modifier.appGlassContent(backdrop)` —— 标记"这些像素可以被取样"；
 * 3. 玻璃元素（必须与内容是**兄弟**，不能是子节点，否则会取样到自己）挂
 *    `Modifier.appGlassSurface(backdrop, shape)` —— 拿到模糊后的玻璃面。
 *
 * 设备兜底：真模糊要 Android 13+ / 硬件加速 / 非省电模式（见 [DockMaterialPolicy]）。
 * 不满足时不是"没有效果"，而是退化成半透明染色的同一块面料（[appGlassFallback] 的
 * alpha 与描边一致），观感仍是玻璃，只是不做背景模糊 —— 这样任何机型都不会看起来像没做。
 */

/** 每页一份玻璃底板；与内容容器一一对应。 */
@Composable
fun rememberAppGlassBackdrop(): LayerBackdrop = rememberLayerBackdrop()

/** 标记"这一层的内容可以被玻璃取样"。null 时原样返回（玻璃不可用时不影响布局）。 */
fun Modifier.appGlassContent(backdrop: LayerBackdrop?): Modifier =
    if (backdrop != null) this.layerBackdrop(backdrop) else this

/** 省电模式开关（省电时不做实时模糊，省电优先）。 */
@Composable
private fun rememberPowerSaveMode(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val power = remember { context.getSystemService(PowerManager::class.java) }
    var powerSave by remember { mutableStateOf(power?.isPowerSaveMode == true) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                powerSave = power?.isPowerSaveMode == true
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return powerSave
}

/** 当前设备能不能做真模糊。 */
@Composable
fun rememberGlassBlurAvailable(): Boolean {
    val view = LocalView.current
    val powerSave = rememberPowerSaveMode()
    return DockMaterialPolicy.useGlass(true, Build.VERSION.SDK_INT, view.isHardwareAccelerated, powerSave)
}

/**
 * 玻璃面料本体：模糊背景 + 染色 + 发丝高光描边 + 柔和投影。
 *
 * @param tint 染色（一般给 surfaceContainer / cardBackground；按钮给 primary 的淡染）
 * @param alpha 染色不透明度：0.62–0.72 之间最像苹果，低于 0.5 文字会不稳。
 * @param blurRadius 背景模糊半径，dock 12dp / 卡片 10dp / 小按钮 8dp。
 */
@Composable
fun Modifier.appGlassSurface(
    backdrop: LayerBackdrop,
    shape: Shape = RoundedCornerShape(50),
    tint: Color,
    alpha: Float = 0.68f,
    blurRadius: androidx.compose.ui.unit.Dp = 12.dp,
    lensRadius: androidx.compose.ui.unit.Dp = 8.dp,
    elevation: androidx.compose.ui.unit.Dp = 10.dp
): Modifier {
    val blurOk = rememberGlassBlurAvailable()
    val surface = if (blurOk) {
        Modifier.drawPlainBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(blurRadius.toPx())
                if (lensRadius > 0.dp) lens(lensRadius.toPx() * 1.5f, lensRadius.toPx())
            },
            onDrawSurface = { drawRect(tint.copy(alpha = alpha)) }
        )
    } else {
        Modifier.clip(shape).background(tint.copy(alpha = alpha))
    }
    return this
        .shadow(elevation = elevation, shape = shape, clip = false)
        .then(surface)
        // 发丝高光：苹果的玻璃边缘是一道极淡的白线，不是灰描边
        .border(width = 0.75.dp, color = Color.White.copy(alpha = 0.38f), shape = shape)
}

/**
 * 内容里的按钮用的"玻璃感"—— 这类元素是被采样的那一层里的子节点，取样会取到自己，
 * 所以不做真模糊，只保留同一套视觉语言：半透明淡染 + 发丝高光 + 内描边。
 */
fun Modifier.appGlassTint(
    shape: Shape,
    tint: Color,
    alpha: Float = 0.16f,
    borderAlpha: Float = 0.30f
): Modifier = this
    .clip(shape)
    .background(tint.copy(alpha = alpha))
    .border(width = 0.75.dp, color = Color.White.copy(alpha = borderAlpha), shape = shape)
