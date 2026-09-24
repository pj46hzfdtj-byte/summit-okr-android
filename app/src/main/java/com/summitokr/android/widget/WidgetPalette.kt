package com.summitokr.android.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider

/**
 * 桌面组件共享配色（RemoteViews 无动态主题：固定 VisOKR 语义色 + 日间/夜间双值）。
 * 三个组件变体（主速览 / 动机 / 今日任务）共用。
 */
internal val WPrimary = Color(0xFF4F46E5)
internal val WSuccess = Color(0xFF22C55E)
internal val WWarning = Color(0xFFF59E0B)
internal val WPink = Color(0xFFEC4899)
internal val WSky = Color(0xFF0EA5E9)

internal val KR_COLORS = listOf(WPrimary, WSuccess, WPink, WWarning, WSky)
internal val WTrack = Color(0xFFE5E7EB)

internal val TextColor = ColorProvider(Color(0xFF1F2329), Color(0xDEFFFFFF))
internal val SubColor = ColorProvider(Color(0xFF8F959E), Color(0x8CFFFFFF))
internal val InnerColor = ColorProvider(Color(0xFFF3F5F9), Color(0xFF2A2D33))
internal val CardColor = ColorProvider(Color(0xFFFFFFFF), Color(0xFF212327))
internal val PrimaryColor = ColorProvider(WPrimary, Color(0xFF8B93F8))
internal val TrackColor = ColorProvider(WTrack, Color(0xFF33363B))
internal val DoneColor = ColorProvider(Color(0xFF22C55E), Color(0xFF4ADE80))
internal val MissColor = ColorProvider(Color(0xFFEF4444), Color(0xFFF87171))

/** "#4F46E5" / "4f46e5" -> Color；非法时回退 */
internal fun parseHexColor(hex: String?, fallback: Color): Color {
    var s = hex?.removePrefix("#") ?: return fallback
    if (s.length == 3) s = s.map { "$it$it" }.joinToString("")
    if (s.length != 6) return fallback
    val v = s.toIntOrNull(16) ?: return fallback
    return Color(0xFF000000L or v.toLong())
}

/** 15.0 -> "15"，15.5 -> "15.5" */
internal fun Double.trimNum(): String =
    if (this == toLong().toDouble()) toLong().toString() else String.format("%.1f", this)
