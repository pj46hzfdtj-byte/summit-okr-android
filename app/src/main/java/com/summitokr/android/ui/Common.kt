package com.summitokr.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** 解析 #RRGGBB / #AARRGGBB */
fun parseHexColor(s: String?): Color {
    if (s.isNullOrBlank()) return Color(0xFF409EFF)
    return try {
        val hex = s.removePrefix("#")
        val v = when (hex.length) {
            6 -> 0xFF000000L or hex.toLong(16)
            8 -> hex.toLong(16)
            3 -> {
                val r = hex[0]; val g = hex[1]; val b = hex[2]
                0xFF000000L or ("$r$r$g$g$b$b".toLong(16))
            }
            else -> 0xFF409EFF
        }
        Color(v)
    } catch (_: Exception) {
        Color(0xFF409EFF)
    }
}

/** 规范卡片：底色 + 1px 描边 + 阴影（通用 0,2,12 .04 / macOS 0,4,20 .07） */
@Composable
fun SummitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val t = LocalSummitTokens.current
    val shape = RoundedCornerShape(t.radiusCard)
    // 柔和投影（对齐 Flutter：通用 elevation≈2 / macOS≈4），用原生 shadow 避免描边伪影
    val shadowElev = if (t.macos) 4.dp else 2.dp
    Box(
        modifier
            .shadow(shadowElev, shape, clip = false)
            .clip(shape)
            .background(visCardColor())
            .border(visCardBorder().width, visCardBorder().brush, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(contentPadding)) { content() }
    }
}

/** 统一顶栏：macOS 主题下标题居中（对齐 Flutter appBarTheme.centerTitle: macos） */
@androidx.compose.material3.ExperimentalMaterial3Api
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SummitTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: androidx.compose.material3.TopAppBarColors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(),
) {
    if (LocalSummitTokens.current.macos) {
        androidx.compose.material3.CenterAlignedTopAppBar(
            title = title, modifier = modifier, navigationIcon = navigationIcon, actions = actions, colors = colors,
        )
    } else {
        androidx.compose.material3.TopAppBar(
            title = title, modifier = modifier, navigationIcon = navigationIcon, actions = actions, colors = colors,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(vertical = 6.dp),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
fun PillTag(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 胶囊进度条（主色或自定义色） */
@Composable
fun CapsuleProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 6.dp,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape),
        color = color,
        trackColor = color.copy(alpha = 0.15f),
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.Inbox) {
    val t = LocalSummitTokens.current
    Column(
        modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = t.textTertiary.copy(alpha = 0.55f))
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = t.textTertiary)
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
fun ErrorView(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(modifier.fillMaxWidth().padding(top = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.TextButton(onClick = onRetry) { Text("重试") }
    }
}

fun Double.toPctInt(): Int = (this * 100).roundToInt()