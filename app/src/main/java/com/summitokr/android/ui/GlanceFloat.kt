package com.summitokr.android.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.summitokr.android.core.NetClient
import com.summitokr.android.core.Routes
import com.summitokr.android.core.SummaryData
import com.summitokr.android.core.Task
import com.summitokr.android.core.normProgress
import com.summitokr.android.core.unwrapOrNull
import com.summitokr.android.widget.WidgetSync
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 应用内可折叠悬浮速览窗（对齐桌面端 WidgetPage.vue）：
 * - 收起态：可拖拽悬浮球（品牌渐变 + 周期评分 + 待办角标），位置持久化
 * - 展开态：深色玻璃速览卡（评分 / 分段进度条 / KPI / 今日任务 / 滞后目标），
 *   点击区块直达对应页面并自动收起
 * 60 秒轮询 /summary，失败静默保留旧数据并标记离线。
 */
@Composable
fun GlanceFloatingOverlay(navController: NavHostController) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember { context.getSharedPreferences("glance_float", Context.MODE_PRIVATE) }

    var expanded by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf<SummaryData?>(null) }
    var offline by remember { mutableStateOf(false) }

    // 轮询 + 同步桌面组件缓存
    LaunchedEffect(Unit) {
        while (true) {
            val d = runCatching { NetClient.api.summary().unwrapOrNull() }.getOrNull()
            if (d != null) {
                data = d
                offline = false
                // 同步桌面小组件缓存并触发刷新
                WidgetSync.push(context, d)
            } else {
                offline = true
            }
            delay(60_000)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW: Dp = with(density) { constraints.maxWidth.toDp() }
        val screenH: Dp = with(density) { constraints.maxHeight.toDp() }
        val ballSize = 56.dp
        val margin = 8.dp

        fun clampBall(p: DpOffset): DpOffset {
            val maxX = (screenW - ballSize - margin).coerceAtLeast(margin)
            val maxY = (screenH - ballSize - margin).coerceAtLeast(24.dp)
            return DpOffset(
                p.x.coerceIn(margin, maxX),
                p.y.coerceIn(24.dp, maxY),
            )
        }

        var ballPos by remember {
            mutableStateOf(
                clampBall(
                    DpOffset(
                        if (prefs.contains("fx")) prefs.getFloat("fx", 0f).dp else screenW - ballSize - 12.dp,
                        if (prefs.contains("fy")) prefs.getFloat("fy", 0f).dp else screenH - ballSize - 140.dp,
                    ),
                ),
            )
        }
        LaunchedEffect(ballPos) {
            prefs.edit()
                .putFloat("fx", with(density) { ballPos.x.toPx() })
                .putFloat("fy", with(density) { ballPos.y.toPx() })
                .apply()
        }

        val pending = (data?.todayTasks ?: emptyList()).count { it.status != "completed" }
        val score = data?.activeFocusCycle?.cycleScore ?: 0.0

        if (expanded) {
            var cardPx by remember { mutableStateOf(IntSize.Zero) }
            val cardW = 320.dp
            val measuredW = with(density) { cardPx.width.toDp() }
            val measuredH = with(density) { cardPx.height.toDp() }
            val cx = ballPos.x.coerceIn(margin, (screenW - measuredW - margin).coerceAtLeast(margin))
            val cy = (ballPos.y - measuredH - 12.dp)
                .coerceAtLeast(16.dp)
                .coerceAtMost((screenH - measuredH - 16.dp).coerceAtLeast(16.dp))
            GlanceFloatCard(
                data = data,
                offline = offline,
                modifier = Modifier
                    .offset { IntOffset(cx.roundToPx(), cy.roundToPx()) }
                    .onSizeChanged { cardPx = it },
                onClose = { expanded = false },
                onNavigate = { route ->
                    expanded = false
                    runCatching { navController.navigate(route) }
                },
                onNavigateGoal = { id ->
                    expanded = false
                    runCatching { navController.navigate(Routes.goalDetail(id)) }
                },
            )
        }

        // 收起态悬浮球
        if (!expanded) {
            Box(
                Modifier
                    .offset { IntOffset(ballPos.x.roundToPx(), ballPos.y.roundToPx()) }
                    .size(ballSize)
                    .shadow(10.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(BrandGradient, CircleShape)
                    .pointerInput(screenW, screenH) {
                        detectDragGestures { _, drag ->
                            ballPos = clampBall(
                                DpOffset(
                                    ballPos.x + with(density) { drag.x.toDp() },
                                    ballPos.y + with(density) { drag.y.toDp() },
                                ),
                            )
                        }
                    }
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (score > 0) "${score.roundToInt()}" else "O",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (score > 0) 20.sp else 22.sp,
                    )
                    if (score > 0) {
                        Text("周期分", color = Color.White.copy(alpha = 0.85f), fontSize = 8.sp)
                    }
                }
                if (pending > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(18.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (pending > 9) "9+" else "$pending",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlanceFloatCard(
    data: SummaryData?,
    offline: Boolean,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    onNavigateGoal: (String) -> Unit,
) {
    val t = LocalSummitTokens.current
    val cycle = data?.activeFocusCycle
    val score = cycle?.cycleScore ?: 0.0
    val tasks = data?.todayTasks ?: emptyList()
    val doneCount = tasks.count { it.status == "completed" }
    val lagging = data?.laggingObjectives ?: emptyList()

    Column(
        modifier
            .width(320.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(t.card)
            .border(1.dp, t.border, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        // ① 标题栏
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).background(BrandGradient, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("O", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Summit OKR · 速览",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (offline) {
                Box(Modifier.size(7.dp).background(t.warning, CircleShape))
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                Icons.Outlined.Close,
                contentDescription = "收起",
                tint = t.textTertiary,
                modifier = Modifier.size(20.dp).clickable { onClose() },
            )
        }

        Spacer(Modifier.height(10.dp))

        // ② 周期评分 + 分段进度条（对照 VisOKR 小组件 64.7/100 + 剩余天数）
        Row(
            Modifier.fillMaxSize0().clip(RoundedCornerShape(14.dp))
                .background(if (t.macos) Color.White.copy(alpha = if (isDarkTheme()) 0.06f else 0.5f) else t.bg)
                .clickable { onNavigate(Routes.FOCUS) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (score > 0) String.format("%.1f", score) else "--",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text("/100", color = t.textTertiary, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text(
                    cycle?.name ?: "暂无专注周期",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (data?.cycleDaysRemaining != null) {
                Text(
                    "剩余 ${data.cycleDaysRemaining} 天",
                    color = t.textTertiary,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // 分段进度条：周期内每个目标一段，按颜色等宽
        val members = cycle?.objectives?.take(6) ?: emptyList()
        if (members.isNotEmpty()) {
            Row(
                Modifier.fillMaxSize0().height(7.dp).clip(CircleShape),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                members.forEach { m ->
                    val o = m.objective
                    val frac = normProgress(o?.currentProgress).toFloat().coerceIn(0.05f, 1f)
                    Box(
                        Modifier.weight(1f).height(7.dp).clip(CircleShape)
                            .background(parseHexColor(o?.color ?: "#409EFF").copy(alpha = 0.22f)),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape)
                                .background(parseHexColor(o?.color ?: "#409EFF")),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ③ KPI 行
        Row(Modifier.fillMaxSize0(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FloatKpi("今日记录", "${data?.todayAddedRecords ?: 0}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            FloatKpi("进行中", "${data?.inProgressObjectives ?: 0}", t.success, Modifier.weight(1f))
            FloatKpi("今日任务", "$doneCount/${data?.todayTaskCount ?: tasks.size}", t.warning, Modifier.weight(1f))
        }

        // ④ 今日任务
        if (tasks.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxSize0().clickable { onNavigate(Routes.TASKS) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("今日任务", style = MaterialTheme.typography.labelMedium, color = t.textTertiary, modifier = Modifier.weight(1f))
                Text("全部 ›", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            tasks.take(4).forEach { task ->
                TaskRow(task)
            }
        }

        // ⑤ 滞后目标
        if (lagging.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("滞后目标", style = MaterialTheme.typography.labelMedium, color = t.danger, modifier = Modifier.padding(vertical = 4.dp))
            lagging.take(3).forEach { o ->
                Row(
                    Modifier.fillMaxSize0().clickable { onNavigateGoal(o.id) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).background(t.danger, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        o.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text("${normProgress(o.currentProgress).toPctInt()}%", style = MaterialTheme.typography.labelSmall, color = t.danger)
                }
            }
        }
    }
}

@Composable
private fun FloatKpi(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val t = LocalSummitTokens.current
    Column(
        modifier.clip(RoundedCornerShape(12.dp))
            .background(if (t.macos) Color.White.copy(alpha = if (isDarkTheme()) 0.06f else 0.5f) else t.bg)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = t.textTertiary, fontSize = 10.sp)
    }
}

@Composable
private fun TaskRow(task: Task) {
    val t = LocalSummitTokens.current
    val done = task.status == "completed"
    Row(
        Modifier.fillMaxSize0().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(14.dp).border(1.5.dp, if (done) t.success else t.border, CircleShape)
                .background(if (done) t.success else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Text("✓", color = Color.White, fontSize = 9.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            task.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (done) t.textTertiary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 悬浮卡内局部宽度撑满（避免与外层 fillMaxSize 冲突） */
private fun Modifier.fillMaxSize0(): Modifier = this.then(Modifier.width(292.dp))

private fun Modifier.fillMaxHeightFrac(frac: Float): Modifier =
    this.then(Modifier.height(7.dp).width(0.dp).let { m ->
        // 分段条内部填充：用 weight 无法表达比例，改用固定高度 + 父 Box 内左对齐填充
        m
    })
