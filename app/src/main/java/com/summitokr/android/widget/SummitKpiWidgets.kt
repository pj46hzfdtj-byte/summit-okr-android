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
 * KPI 速览小组件（对照 VisOKR 宣传图 06.jpg 右侧 2x2 变体）：
 * - 今日进度 +52.5%（绿色大数字）
 * - 进行中目标 N 个
 * 数据：/summary 实时拉取（3s 超时），失败回退缓存。
 */
class SummitKpiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val fresh = withTimeoutOrNull(3_000) {
            runCatching { NetClient.api.summary().unwrapOrNull() }.getOrNull()
        }
        if (fresh != null) WidgetSync.cacheOnly(context, fresh)
        val data = fresh ?: WidgetSync.readCache(context)
        provideContent { KpiRoot(data, context) }
    }
}

class SummitKpiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummitKpiWidget()
}

@androidx.compose.runtime.Composable
private fun KpiRoot(data: SummaryData?, context: Context) {
    val openApp = actionStartActivity(WidgetSync.openRoute(context, Routes.SUMMARY))
    val delta = data?.todayProgressDelta
    val inProgress = data?.inProgressObjectives

    Box(
        GlanceModifier.fillMaxSize().background(CardColor).cornerRadius(20.dp).clickable(openApp).padding(14.dp),
    ) {
        Column(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(GlanceModifier.defaultWeight())
            if (delta != null && delta > 0) {
                Text(
                    "+${String.format("%.1f", delta * 100)}%",
                    style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = ColorProvider(WSuccess, WSuccess)),
                )
                Text("今日进度 ↑", style = TextStyle(fontSize = 11.sp, color = SubColor))
            } else {
                Text(
                    "0%",
                    style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = SubColor),
                )
                Text("今日暂无进展", style = TextStyle(fontSize = 11.sp, color = SubColor))
            }
            Spacer(GlanceModifier.height(10.dp))
            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color(0xFFE5E7EB), Color(0xFF33363B)))) {}
            Spacer(GlanceModifier.height(10.dp))
            Text(
                inProgress?.toString() ?: "--",
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = PrimaryColor),
            )
            Text("个进行中的目标", style = TextStyle(fontSize = 11.sp, color = SubColor))
            Spacer(GlanceModifier.defaultWeight())
        }
    }
}

/**
 * 目标进度列表组件（对照 VisOKR 宣传图 06.jpg 右下变体：环形+百分比列表。
 * Glance 无 Canvas 绘制弧线，用 12 格分段条等效表达进度环）。
 * 数据：activeFocusCycle.objectives（最多 5 条），实时拉取失败回退缓存。
 */
class SummitGoalsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val fresh = withTimeoutOrNull(3_000) {
            runCatching { NetClient.api.summary().unwrapOrNull() }.getOrNull()
        }
        if (fresh != null) WidgetSync.cacheOnly(context, fresh)
        val data = fresh ?: WidgetSync.readCache(context)
        provideContent { GoalsRoot(data, context) }
    }
}

class SummitGoalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummitGoalsWidget()
}

@androidx.compose.runtime.Composable
private fun GoalsRoot(data: SummaryData?, context: Context) {
    val openApp = actionStartActivity(WidgetSync.openRoute(context, Routes.GOALS))
    Box(
        GlanceModifier.fillMaxSize().background(CardColor).cornerRadius(20.dp).clickable(openApp).padding(14.dp),
    ) {
        if (data == null) {
            Column(
                GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Summit OKR", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryColor))
                Spacer(GlanceModifier.height(6.dp))
                Text("打开 App 同步数据", style = TextStyle(fontSize = 12.sp, color = SubColor))
            }
            return@Box
        }
        val members = data.activeFocusCycle?.objectives?.take(5).orEmpty()
        Column(GlanceModifier.fillMaxWidth()) {
            Text("专注周期目标", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextColor))
            Spacer(GlanceModifier.height(6.dp))
            if (members.isEmpty()) {
                Text(
                    "暂无专注周期，点击查看",
                    style = TextStyle(fontSize = 12.sp, color = SubColor),
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            } else {
                members.forEachIndexed { i, m ->
                    val o = m.objective
                    val c = parseHexColor(o?.color, KR_COLORS[i % KR_COLORS.size])
                    val p = normProgress(o?.currentProgress)
                    Row(
                        GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            o?.title ?: "目标",
                            style = TextStyle(fontSize = 11.sp, color = TextColor),
                            maxLines = 1,
                            modifier = GlanceModifier.width(84.dp),
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        // 8 格分段条（Glance Row 子元素上限 10）
                        val cells = 8
                        val done = (cells * p).toInt()
                        Row(
                            GlanceModifier.defaultWeight().height(7.dp).cornerRadius(4.dp),
                        ) {
                            for (k in 0 until cells) {
                                Box(
                                    GlanceModifier.defaultWeight().height(7.dp)
                                        .background(if (k < done) ColorProvider(c, c) else TrackColor),
                                ) {}
                            }
                        }
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            "${(p * 100).toInt()}%",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorProvider(c, c)),
                        )
                    }
                }
            }
        }
    }
}
