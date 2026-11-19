package com.nevoit.glasense.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.nevoit.glasense.theme.tokens.Yellow500

@Immutable
data class GlasenseColors(
    val background: Color,

    // for switches
    val activeTrack: Color,
    val inactiveTrack: Color,
    val activeThumb: Color,
    val inactiveThumb: Color,

    // scrim
    val scrimLight: Color,
    val scrimNormal: Color,
    val scrimMedium: Color,
    val scrimBold: Color,

    // for hierarchical cards
    val pageBackground: Color,
    val cardBackground: Color,

    // for bottom sheets
    val elevatedPageBackground: Color,
    val elevatedCardBackground: Color,

    val primary: Color,
    val onPrimary: Color,

    // main font colors
    val content: Color,
    val contentVariant: Color,

    val highlightText: Color,

    // error color
    val error: Color,
    val onError: Color,

    // for segmented control
    val segmentedControlBackground: Color = scrimNormal,
    val onSegmentedControlBackground: Color = contentVariant,
    val segmentedControlIndicator: Color,
    val onSegmentedControlIndicator: Color = content,
    val shadow: Color
)

val GlasenseLightPalette = GlasenseColors(
    background = Color.White,
    activeTrack = Color(0xFF34C759),
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color(0xFFF2F2F7),
    cardBackground = Color.White,
    elevatedPageBackground = Color(0xFFF3F4F6),
    elevatedCardBackground = Color.White,
    scrimLight = Color.Black.copy(alpha = 0.025f),
    scrimNormal = Color.Black.copy(alpha = 0.05f),
    scrimMedium = Color.Black.copy(alpha = 0.1f),
    scrimBold = Color.Black.copy(alpha = 0.2f),
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    content = Color.Black,
    contentVariant = Color.Black.copy(.6f),
    highlightText = Yellow500,
    error = Color(0xFFFF3B30),
    onError = Color.White,
    segmentedControlIndicator = Color.White,
    shadow = Color.Black.copy(.5f)
)

val GlasenseDarkPalette = GlasenseColors(
    background = Color.Black,
    activeTrack = Color(0xFF32D74B),
    inactiveTrack = Color(0xFF787880).copy(.25f),
    activeThumb = Color.White,
    inactiveThumb = Color.White,
    pageBackground = Color.Black,
    cardBackground = Color(0xFF1C1C1E),
    elevatedPageBackground = Color(0xFF1C1C1E),
    elevatedCardBackground = Color(0xFF2C2C2E),
    scrimLight = Color.White.copy(alpha = 0.05f),
    scrimNormal = Color.White.copy(alpha = 0.1f),
    scrimMedium = Color.White.copy(alpha = 0.2f),
    scrimBold = Color.White.copy(alpha = 0.4f),
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    content = Color.White,
    contentVariant = Color.White.copy(.6f),
    highlightText = Yellow500,
    error = Color(0xFFFF453A),
    onError = Color.White,
    segmentedControlIndicator = Color(0xFF636366),
    shadow = Color.Black
)