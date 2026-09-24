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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.summitokr.android.core.NetClient
import com.summitokr.android.core.Routes
import com.summitokr.android.core.unwrapOrNull
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 动机速览小组件（对照 VisOKR 宣传图 2x2 变体：渐变底 + 大引号动机 + 专注周期进度 + KR emoji 行）。
 * 数据：/summary 的 randomMotivation / activeFocusCycle（实时拉取失败回退缓存）。
 */
class SummitMotivationWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val fresh = withTimeoutOrNull(3_000) {
            runCatching { NetClient.api.summary().unwrapOrNull() }.getOrNull()
        }
        if (fresh != null) WidgetSync.cacheOnly(context, fresh)
        val data = fresh ?: WidgetSync.readCache(context)
        provideContent { MotivationRoot(data, context) }
    }
}

class SummitMotivationWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummitMotivationWidget()
}

private val GradTop = ColorProvider(Color(0xFF6366F1), Color(0xFF3730A3))

@androidx.compose.runtime.Composable
private fun MotivationRoot(data: com.summitokr.android.core.SummaryData?, context: Context) {
    val openApp = actionStartActivity(WidgetSync.openRoute(context, Routes.SUMMARY))
    val motivation = data?.randomMotivation
    val cycle = data?.activeFocusCycle
    val score = cycle?.cycleScore
    val days = data?.cycleDaysRemaining
    // 收集前 4 个 KR emoji（对照宣传图底部 emoji 圆点行）
    val emojis = ArrayList<String>()
    outer@ for (m in cycle?.objectives.orEmpty()) {
        for (kr in m.objective?.keyResults.orEmpty()) {
            if (emojis.size >= 4) break@outer
            emojis.add(kr.emoji.ifBlank { "🌟" })
        }
    }

    Box(
        GlanceModifier.fillMaxSize().background(GradTop).cornerRadius(20.dp).clickable(openApp).padding(14.dp),
    ) {
        Column(GlanceModifier.fillMaxWidth()) {
            Text("“", style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0x66FFFFFF), Color(0x66FFFFFF))))
            Text(
                motivation ?: "打开 App 获取今日动机",
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFFFFFFFF), Color(0xFFFFFFFF))),
                maxLines = 3,
            )
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (score != null) {
                    Text(
                        "专注周期 ${String.format("%.1f", score)}",
                        style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xE6FFFFFF), Color(0xE6FFFFFF))),
                    )
                } else if (days != null) {
                    Text("剩余 $days 天", style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xE6FFFFFF), Color(0xE6FFFFFF))))
                }
                Spacer(GlanceModifier.defaultWeight())
                emojis.forEach { e ->
                    Box(
                        GlanceModifier.size(24.dp).cornerRadius(12.dp)
                            .background(ColorProvider(Color(0x33FFFFFF), Color(0x33FFFFFF)))
                            .padding(3.dp),
                    ) {
                        Text(e, style = TextStyle(fontSize = 12.sp))
                    }
                    Spacer(GlanceModifier.width(4.dp))
                }
            }
        }
    }
}
