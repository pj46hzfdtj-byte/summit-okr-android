package com.summitokr.android.core

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import androidx.core.content.ContextCompat
import java.util.TimeZone

/**
 * 系统日历桥接：把 Summit OKR 的任务写入手机日历。
 * 使用本地账户日历「Summit OKR」，事件 description 携带 `summitokr:task:<id>` 标记用于去重。
 */
object CalendarStore {
    private const val ACCOUNT_NAME = "Summit OKR"

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** 查找或创建本应用的专用日历，返回 calendarId */
    private fun ensureCalendarId(ctx: Context): Long {
        val resolver = ctx.contentResolver
        resolver.query(
            Calendars.CONTENT_URI,
            arrayOf(Calendars._ID),
            "${Calendars.ACCOUNT_NAME} = ? AND ${Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(ACCOUNT_NAME, CalendarContract.ACCOUNT_TYPE_LOCAL),
            null,
        )?.use { c -> if (c.moveToFirst()) return c.getLong(0) }

        val values = ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Calendars.NAME, ACCOUNT_NAME)
            put(Calendars.CALENDAR_DISPLAY_NAME, ACCOUNT_NAME)
            put(Calendars.CALENDAR_COLOR, 0xFF4F46E5.toInt())
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
            put(Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }
        // 本地账户日历创建需以 sync-adaptor 身份写入（CAL_SYNC_ADAPTER 列校验）
        val insertUri = Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val uri = resolver.insert(insertUri, values)
            ?: throw IllegalStateException("无法创建系统日历")
        return ContentUris.parseId(uri)
    }

    data class CalTask(
        val id: String,
        val title: String,
        val startMillis: Long,
        val endMillis: Long,
        val allDay: Boolean = false,
    )

    /** 批量写入事件，已存在（同任务标记）的跳过。返回 (新增数, 跳过数)。 */
    fun upsertTasks(ctx: Context, tasks: List<CalTask>): Pair<Int, Int> {
        val calId = ensureCalendarId(ctx)
        val resolver = ctx.contentResolver
        var added = 0
        var skipped = 0
        for (t in tasks) {
            val desc = "summitokr:task:${t.id}"
            var exists = false
            resolver.query(
                Events.CONTENT_URI,
                arrayOf(Events._ID),
                "${Events.CALENDAR_ID} = ? AND ${Events.DESCRIPTION} = ?",
                arrayOf(calId.toString(), desc),
                null,
            )?.use { c -> exists = c.moveToFirst() }
            if (exists) {
                skipped++
                continue
            }
            val values = ContentValues().apply {
                put(Events.CALENDAR_ID, calId)
                put(Events.TITLE, t.title.ifBlank { "任务" })
                put(Events.DESCRIPTION, desc)
                put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(Events.ALL_DAY, if (t.allDay) 1 else 0)
                put(Events.DTSTART, t.startMillis)
                put(Events.DTEND, t.endMillis)
            }
            runCatching {
                val uri = resolver.insert(Events.CONTENT_URI, values) ?: return@runCatching
                added++
                // 定时任务附加提前 10 分钟提醒
                if (!t.allDay) {
                    runCatching {
                        resolver.insert(
                            Reminders.CONTENT_URI,
                            ContentValues().apply {
                                put(Reminders.EVENT_ID, ContentUris.parseId(uri))
                                put(Reminders.MINUTES, 10)
                                put(Reminders.METHOD, Reminders.METHOD_ALERT)
                            },
                        )
                    }
                }
            }
        }
        return added to skipped
    }
}
