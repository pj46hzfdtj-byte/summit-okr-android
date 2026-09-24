package com.summitokr.android.widget

import android.content.Context
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
import com.summitokr.android.core.unwrapOrNull
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

/** scheduledAt 兼容 ISO 字符串与 epoch 毫秒字符串两种格式 */
private fun parseMillis(raw: String?): Long? {
    if (raw.isNullOrEmpty()) return null
    raw.toLongOrNull()?.let { return it }
    return runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
}

/**
 * 今日任务小组件（对照 VisOKR 宣传图 4x2 变体：完成进度条 + 任务列表 ✓/○/❌ 三态 + 周期评分）。
 * ✓ 已完成、○ 未到点、❌ 已过期（pending 且计划时间早于当前）。
 * 数据：/summary 的 todayTasks（最多 5 条），实时拉取失败回退缓存。
 */
class SummitTasksWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val fresh = withTimeoutOrNull(3_000) {
            runCatching { NetClient.api.summary().unwrapOrNull() }.getOrNull()
        }
        if (fresh != null) WidgetSync.cacheOnly(context, fresh)
        val data = fresh ?: WidgetSync.readCache(context)
        provideContent { TasksRoot(data, context) }
    }
}

class SummitTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummitTasksWidget()
}

@androidx.compose.runtime.Composable
private fun TasksRoot(data: SummaryData?, context: Context) {
    val openApp = actionStartActivity(WidgetSync.openRoute(context, Routes.TASKS))
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
        val tasks = data.todayTasks.take(5)
        val done = tasks.count { it.status == "completed" }
        val total = data.todayTaskCount ?: tasks.size
        val score = data.activeFocusCycle?.cycleScore

        Column(GlanceModifier.fillMaxWidth()) {
            // 头部：今日任务 done/total + 周期评分
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("今日任务", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor))
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    "$done/$total",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryColor),
                )
                if (score != null) {
                    Spacer(GlanceModifier.width(8.dp))
                    Text("⚡${String.format("%.0f", score)}", style = TextStyle(fontSize = 11.sp, color = SubColor))
                }
            }

            // 完成进度条
            Spacer(GlanceModifier.height(6.dp))
            val cells = 8
            Row(GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp)) {
                val doneCells = if (total > 0) (cells * done / total) else 0
                for (i in 0 until cells) {
                    Box(
                        GlanceModifier.defaultWeight().height(6.dp)
                            .background(if (i < doneCells) DoneColor else TrackColor),
                    ) {}
                }
            }

            // 任务列表
            Spacer(GlanceModifier.height(4.dp))
            if (tasks.isEmpty()) {
                Text(
                    "今日暂无任务 🎉",
                    style = TextStyle(fontSize = 13.sp, color = SubColor),
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            } else {
                tasks.forEach { t ->
                    val isDone = t.status == "completed"
                    val startMillis = parseMillis(t.scheduledAt)
                    val isMissed = !isDone && startMillis != null && startMillis < System.currentTimeMillis()
                    Row(
                        GlanceModifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            when {
                                isDone -> "✓"
                                isMissed -> "❌"
                                else -> "○"
                            },
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isDone -> DoneColor
                                    isMissed -> MissColor
                                    else -> SubColor
                                },
                            ),
                        )
                        Spacer(GlanceModifier.width(7.dp))
                        Text(
                            t.title,
                            style = TextStyle(fontSize = 12.sp, color = if (isDone) SubColor else TextColor),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        startMillis?.let { ms ->
                            val hm = runCatching {
                                SimpleDateFormat("HH:mm", Locale.US).format(Date(ms))
                            }.getOrNull()
                            if (hm != null) Text(hm, style = TextStyle(fontSize = 10.sp, color = if (isMissed) MissColor else SubColor))
                        }
                    }
                }
            }
        }
    }
}
