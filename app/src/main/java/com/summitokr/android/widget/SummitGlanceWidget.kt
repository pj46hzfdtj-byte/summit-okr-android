package com.summitokr.android.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.summitokr.android.core.NetClient
import com.summitokr.android.core.Routes
import com.summitokr.android.core.SummaryData
import com.summitokr.android.core.normProgress
import com.summitokr.android.core.unwrapOrNull
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Summit OKR 桌面主组件（对照 VisOKR「桌面小组件」宣传图 06.jpg）：
 * - 评分行：周期评分 x.x/100 + 今日进度 +% + 剩余天数
 * - 彩色分段进度条（各目标按贡献占比着色）
 * - 目标列表：目标色圆点 + 标题 + 百分比，下排 KR emoji + `current→target` 数值
 * - KPI 行：今日记录 / 进行中 / 今日任务
 * 数据：刷新时优先实时拉 /summary（3s 超时），失败回退 App 侧缓存。
 */
class SummitGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val fresh = withTimeoutOrNull(3_000) {
            runCatching { NetClient.api.summary().unwrapOrNull() }.getOrNull()
        }
        if (fresh != null) WidgetSync.cacheOnly(context, fresh)
        val data = fresh ?: WidgetSync.readCache(context)

        provideContent { WidgetRoot(data, context) }
    }
}

class SummitGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummitGlanceWidget()
}

@androidx.compose.runtime.Composable
private fun WidgetRoot(data: SummaryData?, context: Context) {
    val openSummary = actionStartActivity(WidgetSync.openRoute(context, Routes.SUMMARY))
    Box(
        GlanceModifier
            .fillMaxSize()
            .background(CardColor)
            .cornerRadius(20.dp)
            .clickable(openSummary)
            .padding(14.dp),
    ) {
        if (data == null) {
            EmptyHint(context)
        } else {
            val cycle = data.activeFocusCycle
            val score = cycle?.cycleScore
            val days = data.cycleDaysRemaining
            val delta = data.todayProgressDelta
            val members = cycle?.objectives?.take(3).orEmpty()

            Column(GlanceModifier.fillMaxWidth()) {
                // ① 评分行：64.7 /100  +5.2% …… 剩余 13 天
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        score?.let { String.format("%.1f", it) } ?: "--",
                        style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = PrimaryColor),
                    )
                    Spacer(GlanceModifier.width(3.dp))
                    Text("/100", style = TextStyle(fontSize = 10.sp, color = SubColor))
                    Spacer(GlanceModifier.width(8.dp))
                    if (delta != null && delta > 0) {
                        Text(
                            "+${String.format("%.1f", delta * 100)}%",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WSuccess, WSuccess)),
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    if (days != null) {
                        Text("剩余 $days 天", style = TextStyle(fontSize = 11.sp, color = SubColor))
                    }
                }

                // ② 彩色分段进度条（12 等分 cell 堆叠模拟比例，Glance RowScope 仅 defaultWeight）
                if (members.isNotEmpty()) {
                    Spacer(GlanceModifier.height(7.dp))
                    val totalCells = 8
                    val cellColors = ArrayList<Color>(totalCells)
                    members.forEachIndexed { mi, m ->
                        val p = normProgress(m.objective?.currentProgress)
                        val cells = (totalCells * p / members.size).toInt()
                        repeat(cells.coerceAtMost(totalCells - cellColors.size)) {
                            cellColors.add(KR_COLORS[mi % KR_COLORS.size])
                        }
                    }
                    while (cellColors.size < totalCells) cellColors.add(WTrack)
                    Row(GlanceModifier.fillMaxWidth().height(7.dp).cornerRadius(4.dp)) {
                        cellColors.forEach { c ->
                            Box(
                                GlanceModifier.defaultWeight().height(7.dp)
                                    .background(if (c == WTrack) TrackColor else ColorProvider(c, c)),
                            ) {}
                        }
                    }
                }

                // ③ 目标列表：色点 + 标题 + 百分比；下排 KR emoji + current→target
                Spacer(GlanceModifier.height(8.dp))
                Box(
                    GlanceModifier.fillMaxWidth()
                        .background(InnerColor)
                        .cornerRadius(14.dp)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Column(GlanceModifier.fillMaxWidth()) {
                        if (members.isEmpty()) {
                            val lag = data.laggingObjectives.firstOrNull()
                            if (lag != null) {
                                Row(
                                    GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("⚠️", style = TextStyle(fontSize = 11.sp))
                                    Spacer(GlanceModifier.width(5.dp))
                                    Text(
                                        lag.title,
                                        style = TextStyle(fontSize = 11.sp, color = ColorProvider(WWarning, WWarning)),
                                        maxLines = 1,
                                        modifier = GlanceModifier.defaultWeight(),
                                    )
                                    Text(
                                        "${(normProgress(lag.currentProgress) * 100).toInt()}%",
                                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WWarning, WWarning)),
                                    )
                                }
                            } else {
                                Text(
                                    "暂无专注周期，点击查看",
                                    style = TextStyle(fontSize = 12.sp, color = SubColor),
                                    modifier = GlanceModifier.padding(vertical = 8.dp),
                                )
                            }
                        } else {
                            members.forEachIndexed { i, m ->
                                val o = m.objective
                                val c = parseHexColor(o?.color, KR_COLORS[i % KR_COLORS.size])
                                val p = normProgress(o?.currentProgress)
                                Row(
                                    GlanceModifier.fillMaxWidth().padding(top = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        GlanceModifier.size(7.dp).cornerRadius(4.dp)
                                            .background(ColorProvider(c, c)),
                                    ) {}
                                    Spacer(GlanceModifier.width(6.dp))
                                    Text(
                                        o?.title ?: "目标",
                                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextColor),
                                        maxLines = 1,
                                        modifier = GlanceModifier.defaultWeight(),
                                    )
                                    Text(
                                        "${(p * 100).toInt()}%",
                                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(c, c)),
                                    )
                                }
                                // KR 行：emoji + current→target（对照宣传图 KR 卡片）
                                val krs = o?.keyResults.orEmpty()
                                if (krs.isNotEmpty()) {
                                    Row(
                                        GlanceModifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Spacer(GlanceModifier.width(13.dp))
                                        krs.take(3).forEach { kr ->
                                            Text(kr.emoji.ifBlank { "🌟" }, style = TextStyle(fontSize = 11.sp))
                                            Spacer(GlanceModifier.width(2.dp))
                                            Text(
                                                "${kr.currentValue.trimNum()}→${kr.targetValue.trimNum()}  ",
                                                style = TextStyle(fontSize = 10.sp, color = SubColor),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ④ KPI 行：今日记录 / 进行中目标 / 今日任务
                Spacer(GlanceModifier.height(8.dp))
                val doneCount = data.todayTasks.count { it.status == "completed" }
                val taskCount = data.todayTaskCount ?: data.todayTasks.size
                Row(GlanceModifier.fillMaxWidth()) {
                    WidgetKpi("今日记录", "${data.todayAddedRecords ?: 0}", PrimaryColor, GlanceModifier.defaultWeight())
                    WidgetKpi(
                        "进行中",
                        "${data.inProgressObjectives}",
                        ColorProvider(WSuccess, WSuccess),
                        GlanceModifier.defaultWeight().padding(horizontal = 4.dp),
                    )
                    WidgetKpi(
                        "今日任务",
                        "$doneCount/$taskCount",
                        ColorProvider(WWarning, WWarning),
                        GlanceModifier.defaultWeight().padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetKpi(label: String, value: String, color: ColorProvider, modifier: GlanceModifier) {
    Box(
        modifier
            .background(InnerColor)
            .cornerRadius(12.dp)
            .padding(vertical = 7.dp),
    ) {
        Column(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color))
            Text(label, style = TextStyle(fontSize = 10.sp, color = SubColor))
        }
    }
}

@androidx.compose.runtime.Composable
private fun EmptyHint(context: Context) {
    val openApp = actionStartActivity(WidgetSync.openRoute(context, Routes.LOGIN))
    Column(
        GlanceModifier.fillMaxSize().clickable(openApp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Summit OKR", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryColor))
        Spacer(GlanceModifier.height(6.dp))
        Text("点击查看目标进度", style = TextStyle(fontSize = 12.sp, color = SubColor))
    }
}
