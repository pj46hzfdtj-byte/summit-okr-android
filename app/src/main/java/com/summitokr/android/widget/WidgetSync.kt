package com.summitokr.android.widget

import android.content.Context
import android.content.Intent
import com.summitokr.android.MainActivity
import com.summitokr.android.core.NetClient
import com.summitokr.android.core.SummaryData
import com.summitokr.android.core.unwrapOrNull
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 桌面组件数据通道：
 * - App 侧（悬浮窗轮询 / 摘要页刷新）调用 [push]：写缓存 + 立即刷新所有组件实例
 * - 组件侧 [load]：优先实时拉取 /summary，失败回退缓存
 */
object WidgetSync {
    private const val PREF = "widget_cache"
    private const val KEY_DATA = "summary_json"
    private const val KEY_AT = "updated_at"

    fun cacheOnly(context: Context, data: SummaryData) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_DATA, NetClient.json.encodeToString(SummaryData.serializer(), data))
            .putLong(KEY_AT, System.currentTimeMillis())
            .apply()
    }

    fun readCache(context: Context): SummaryData? {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val s = sp.getString(KEY_DATA, null) ?: return null
        return runCatching { NetClient.json.decodeFromString(SummaryData.serializer(), s) }.getOrNull()
    }

    suspend fun push(context: Context, data: SummaryData) = withContext(Dispatchers.IO) {
        cacheOnly(context, data)
        updateAllWidgets(context)
    }

    /** 组件刷新入口：拉最新数据（失败保留缓存），并触发组件重绘 */
    suspend fun refreshAll(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            val d = NetClient.api.summary().unwrapOrNull()
            if (d != null) cacheOnly(context, d)
        }
        updateAllWidgets(context)
    }

    /** 刷新全部组件变体 */
    private suspend fun updateAllWidgets(context: Context) {
        runCatching { SummitGlanceWidget().updateAll(context) }
        runCatching { SummitMotivationWidget().updateAll(context) }
        runCatching { SummitTasksWidget().updateAll(context) }
        runCatching { SummitKpiWidget().updateAll(context) }
        runCatching { SummitGoalsWidget().updateAll(context) }
    }

    fun openRoute(context: Context, route: String): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_ROUTE, route)
        }
}
