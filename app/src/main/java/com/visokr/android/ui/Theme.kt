package com.visokr.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ============ 设计 token（与 Taro/uniapp/Flutter 三端对齐的规范字面值） ============
object VisSpec {
    val Text = Color(0xFF1F2329)
    val TextSecondary = Color(0xFF646A73)
    val TextTertiary = Color(0xFF8F959E)
    val Bg = Color(0xFFF5F6F7)
    val Card = Color(0xFFFFFFFF)
    val Border = Color(0xFFE5E7EB)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)

    val TextDark = Color(0xDEFFFFFF) // rgba(255,255,255,.87)
    val TextSecondaryDark = Color(0x8CFFFFFF) // .55
    val TextTertiaryDark = Color(0x61FFFFFF) // .38
    val BgDark = Color(0xFF16181C)
    val CardDark = Color(0xFF212327)
    val BorderDark = Color(0xFF33363B)

    // macOS 主题 Apple 语义色
    val MacosSuccess = Color(0xFF34C759)
    val MacosWarning = Color(0xFFFF9F0A)
    val MacosDanger = Color(0xFFFF3B30)
    val MacosSuccessDark = Color(0xFF32D74B)
    val MacosWarningDark = Color(0xFFFF9F0A)
    val MacosDangerDark = Color(0xFFFF453A)
}

/** 配色主题主色（仅 primary 变化；夜间提亮） */
data class SeedPalette(val light: Color, val dark: Color)

object VisSeeds {
    val all = listOf("light", "blue", "green", "purple", "macos")
    val labels = mapOf("light" to "浅色", "blue" to "蓝", "green" to "绿", "purple" to "紫", "macos" to "macOS")
    val preview = mapOf(
        "light" to Color(0xFF409EFF),
        "blue" to Color(0xFF1A73E8),
        "green" to Color(0xFF2E7D32),
        "purple" to Color(0xFF7C3AED),
        "macos" to Color(0xFF007AFF),
    )
    private val palettes: Map<String, SeedPalette> = mapOf(
        "light" to SeedPalette(Color(0xFF409EFF), Color(0xFF409EFF)),
        "blue" to SeedPalette(Color(0xFF1A73E8), Color(0xFF5E9FF5)),
        "green" to SeedPalette(Color(0xFF2E7D32), Color(0xFF66BB6A)),
        "purple" to SeedPalette(Color(0xFF7C3AED), Color(0xFFA06EF5)),
        "macos" to SeedPalette(Color(0xFF007AFF), Color(0xFF0A84FF)),
    )

    fun primary(seed: String, dark: Boolean): Color {
        val p = palettes[seed] ?: palettes["light"]!!
        return if (dark) p.dark else p.light
    }
}

fun isMacos(seed: String) = seed == "macos"

fun mix(a: Color, b: Color, t: Float): Color {
    val r = (a.red * (1 - t) + b.red * t).roundToInt() / 255f
    val g = (a.green * (1 - t) + b.green * t).roundToInt() / 255f
    val bl = (a.blue * (1 - t) + b.blue * t).roundToInt() / 255f
    return Color(r, g, bl, 1f)
}

/** 扩展 token：语义色 / macOS 标志 / 圆角 / 背景 */
@Immutable
data class VisTokens(
    val macos: Boolean,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val textTertiary: Color,
    val border: Color,
    val bg: Color,
    val card: Color,
    val radiusCard: androidx.compose.ui.unit.Dp,
    val radiusControl: androidx.compose.ui.unit.Dp,
)

val LocalVisTokens: ProvidableCompositionLocal<VisTokens> = staticCompositionLocalOf {
    VisTokens(
        macos = false,
        success = VisSpec.Success,
        warning = VisSpec.Warning,
        danger = VisSpec.Danger,
        textTertiary = VisSpec.TextTertiary,
        border = VisSpec.Border,
        bg = VisSpec.Bg,
        card = VisSpec.Card,
        radiusCard = 16.dp,
        radiusControl = 12.dp,
    )
}

@Composable
fun visTokens(): VisTokens = LocalVisTokens.current

fun buildColorScheme(seed: String, dark: Boolean): ColorScheme {
    val macos = isMacos(seed)
    val primary = VisSeeds.primary(seed, dark)
    val card = if (dark) VisSpec.CardDark else VisSpec.Card
    val text = if (dark) VisSpec.TextDark else VisSpec.Text
    val textSecondary = if (dark) VisSpec.TextSecondaryDark else VisSpec.TextSecondary
    val border = if (dark) VisSpec.BorderDark else VisSpec.Border
    val success = if (macos) (if (dark) VisSpec.MacosSuccessDark else VisSpec.MacosSuccess) else VisSpec.Success
    val danger = if (macos) (if (dark) VisSpec.MacosDangerDark else VisSpec.MacosDanger) else VisSpec.Danger
    val primaryContainer = mix(primary, card, if (dark) 0.82f else 0.86f)
    val errorContainer = mix(danger, card, 0.86f)
    val successContainer = mix(success, card, 0.86f)
    val surfaceContainerHigh = mix(text, card, if (dark) 0.06f else 0.035f)
    val surfaceContainerHighest = mix(text, card, if (dark) 0.10f else 0.06f)
    val onInverse = if (dark) VisSpec.CardDark else VisSpec.Bg
    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primary,
        secondary = primary,
        onSecondary = Color.White,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = primary,
        tertiary = success,
        onTertiary = Color.White,
        tertiaryContainer = successContainer,
        onTertiaryContainer = success,
        error = danger,
        onError = Color.White,
        errorContainer = errorContainer,
        onErrorContainer = danger,
        background = if (dark) VisSpec.BgDark else VisSpec.Bg,
        onBackground = text,
        surface = card,
        onSurface = text,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        outline = border,
        outlineVariant = border,
        inverseSurface = text,
        inverseOnSurface = onInverse,
        inversePrimary = primary,
        surfaceTint = primary,
        scrim = Color.Black,
    )
}

private val VisTypography = Typography(
    displaySmall = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp),
)

@Composable
fun VisTheme(
    seed: String,
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val macos = isMacos(seed)
    val scheme = buildColorScheme(seed, dark)
    val tokens = VisTokens(
        macos = macos,
        success = if (macos) (if (dark) VisSpec.MacosSuccessDark else VisSpec.MacosSuccess) else VisSpec.Success,
        warning = if (macos) (if (dark) VisSpec.MacosWarningDark else VisSpec.MacosWarning) else VisSpec.Warning,
        danger = scheme.error,
        textTertiary = if (dark) VisSpec.TextTertiaryDark else VisSpec.TextTertiary,
        border = if (dark) VisSpec.BorderDark else VisSpec.Border,
        bg = if (dark) VisSpec.BgDark else VisSpec.Bg,
        card = if (dark) VisSpec.CardDark else VisSpec.Card,
        radiusCard = if (macos) 20.dp else 16.dp,
        radiusControl = if (macos) 14.dp else 12.dp,
    )
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(tokens.radiusControl),
        medium = RoundedCornerShape(tokens.radiusControl),
        large = RoundedCornerShape(tokens.radiusCard),
        extraLarge = RoundedCornerShape(24.dp),
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalVisTokens provides tokens) {
        MaterialTheme(colorScheme = scheme, typography = VisTypography, shapes = shapes, content = content)
    }
}

/** 品牌渐变（登录 CTA / Logo 徽标） */
val BrandGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4F8DFF), Color(0xFF409EFF), Color(0xFF6C5CE7)),
    start = Offset.Zero,
    end = Offset.Infinite,
)

/** macOS 玻璃卡片底色：半透明白（light .78 / dark .08 alpha） */
@Composable
fun visCardColor(): Color {
    val t = LocalVisTokens.current
    return if (t.macos) {
        if (isDarkTheme()) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.78f)
    } else t.card
}

@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.surface == VisSpec.CardDark

@Composable
fun visCardBorder(): BorderStroke {
    val t = LocalVisTokens.current
    return BorderStroke(
        1.dp,
        if (t.macos) Color.White.copy(alpha = if (isDarkTheme()) 0.10f else 0.55f) else t.border,
    )
}

/** 全局背景：macOS 主题显示 Aurora 多色光斑，其余纯色背景 */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val t = LocalVisTokens.current
    val dark = isDarkTheme()
    Box(Modifier.fillMaxSize().background(if (t.macos) Color.Transparent else t.bg)) {
        if (t.macos) {
            Box(Modifier.fillMaxSize().background(auroraBrush(dark)))
        }
        content()
    }
}

fun auroraBrush(dark: Boolean): Brush {
    val base = if (dark) {
        listOf(Color(0xFF101318), Color(0xFF16181C), Color(0xFF1A1622))
    } else {
        listOf(Color(0xFFEAF2FF), Color(0xFFF1ECFB), Color(0xFFEAF7F5))
    }
    return Brush.linearGradient(base, start = Offset.Zero, end = Offset.Infinite)
}

@Composable
fun AuroraOverlay(modifier: Modifier = Modifier) {
    val dark = isDarkTheme()
    val blobs = if (dark) {
        listOf(
            Triple(Color(0x332B4C7E), Offset(0.08f, 0.02f), 480f),
            Triple(Color(0x2E5E3A6E), Offset(0.95f, 0.10f), 440f),
            Triple(Color(0x2E2E5E58), Offset(0.20f, 0.95f), 480f),
            Triple(Color(0x286E5A2E), Offset(0.85f, 0.72f), 380f),
        )
    } else {
        listOf(
            Triple(Color(0x6678AAFF), Offset(0.08f, 0.02f), 480f),
            Triple(Color(0x55FFBEE6), Offset(0.95f, 0.10f), 440f),
            Triple(Color(0x44A0E6DC), Offset(0.20f, 0.95f), 480f),
            Triple(Color(0x3DFFDC9F), Offset(0.85f, 0.72f), 380f),
        )
    }
    Box(modifier.fillMaxSize().drawWithContent {
        for ((color, pos, radius) in blobs) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = Offset(pos.x * size.width, pos.y * size.height),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(pos.x * size.width, pos.y * size.height),
            )
        }
        drawContent()
    })
}

/** 统一卡片 shape（供页面复用） */
@Composable
fun visCardShape(): Shape = RoundedCornerShape(LocalVisTokens.current.radiusCard)

@Composable
fun visControlShape(): Shape = RoundedCornerShape(LocalVisTokens.current.radiusControl)