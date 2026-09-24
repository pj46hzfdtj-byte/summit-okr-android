package com.summitokr.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.summitokr.android.core.CalendarStore
import com.summitokr.android.core.Routes
import com.summitokr.android.core.Task
import com.summitokr.android.core.TasksVM
import com.summitokr.android.core.UiState
import com.summitokr.android.ui.EmptyState
import com.summitokr.android.ui.ErrorView
import com.summitokr.android.ui.LoadingView
import com.summitokr.android.ui.SummitCard
import com.summitokr.android.ui.SummitTopBar
import com.summitokr.android.ui.LocalSummitTokens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksPage(navController: NavController) {
    val vm: TasksVM = viewModel()
    val tasks by vm.tasks.collectAsState()
    val celebrate by vm.celebrate.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var weekAnchor by remember { mutableStateOf(LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())) }
    var showCreate by remember { mutableStateOf(false) }
    val t = LocalSummitTokens.current
    val context = LocalContext.current
    var toast by remember { mutableStateOf<String?>(null) }
    val calPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.WRITE_CALENDAR] == true) {
            exportDayTasks(
                context,
                tasks = (tasks as? UiState.Success)?.data
                    ?.filter { it.scheduledAt?.startsWith(selected.toString()) == true }
                    .orEmpty(),
                selected = selected,
            ) { msg -> toast = msg }
        } else {
            toast = "需要日历写入权限"
        }
    }

    // toast 2.6s 后自动消失
    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2600)
            toast = null
        }
    }

    // 完成庆祝浮层：2600ms 后自动隐藏
    LaunchedEffect(celebrate) {
        if (celebrate != null) {
            kotlinx.coroutines.delay(2600)
            vm.clearCelebrate()
        }
    }

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            SummitTopBar(
                title = { Text("任务", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = {
                            val dayTasks = (tasks as? UiState.Success)?.data
                                ?.filter { it.scheduledAt?.startsWith(selected.toString()) == true }
                                .orEmpty()
                            if (dayTasks.isEmpty()) {
                                toast = "这一天没有任务"
                            } else if (CalendarStore.hasPermission(context)) {
                                exportDayTasks(context, tasks = dayTasks, selected = selected) { msg -> toast = msg }
                            } else {
                                calPermission.launch(arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR))
                            }
                        },
                    ) { Icon(Icons.Outlined.Event, "加入系统日历") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ) { Icon(Icons.Outlined.Add, null) }
        },
    ) { padding ->
        val all = (tasks as? UiState.Success)?.data ?: emptyList()
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                WeekStrip(
                    anchor = weekAnchor,
                    selected = selected,
                    tasksByDay = { day -> all.filter { it.scheduledAt?.startsWith(day.toString()) == true } },
                    onPrevWeek = { weekAnchor = weekAnchor.minusDays(7) },
                    onNextWeek = { weekAnchor = weekAnchor.plusDays(7) },
                    onToday = {
                        val today = LocalDate.now()
                        selected = today
                        month = YearMonth.from(today)
                        weekAnchor = today.minusDays((today.dayOfWeek.value - 1).toLong())
                    },
                    onSelect = { day ->
                        selected = day
                        month = YearMonth.from(day)
                        if (day.isBefore(weekAnchor) || day.isAfter(weekAnchor.plusDays(6))) {
                            weekAnchor = day.minusDays((day.dayOfWeek.value - 1).toLong())
                        }
                    },
                )
                CalendarBar(
                    month = month,
                    selected = selected,
                    hasTasks = { day -> all.any { it.scheduledAt?.startsWith(day.toString()) == true } },
                    onPrev = { month = month.minusMonths(1) },
                    onNext = { month = month.plusMonths(1) },
                    onSelect = {
                        selected = it
                        weekAnchor = it.minusDays((it.dayOfWeek.value - 1).toLong())
                    },
                )
                PullToRefreshBox(onRefresh = { vm.refresh() }, isRefreshing = false, modifier = Modifier.weight(1f)) {
                    when (val s = tasks) {
                        is UiState.Loading -> LoadingView()
                        is UiState.Error -> ErrorView(s.message, onRetry = { vm.refresh() })
                        is UiState.Success -> {
                            val dayKey = selected.toString()
                            val dayTasks = s.data.filter { it.scheduledAt?.startsWith(dayKey) == true }
                            val now = LocalDateTime.now()
                            val overdue = s.data.filter {
                                it.status != "completed" && it.scheduledAt != null &&
                                    runCatching { LocalDateTime.parse(it.scheduledAt.take(19)) }.getOrNull()?.isBefore(now) == true
                            }.filter { it.scheduledAt?.startsWith(dayKey) != true }
                            if (dayTasks.isEmpty() && overdue.isEmpty()) {
                                EmptyState("这一天没有任务", icon = Icons.Outlined.Event)
                            } else {
                                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        Text(
                                            selected.format(DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.CHINESE)),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    }
                                    items(dayTasks) { task -> TaskRow(task, vm) }
                                    if (overdue.isNotEmpty()) {
                                        item {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("过期任务", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                TextButton(onClick = { vm.deleteOverdue { } }) { Text("一键删除") }
                                            }
                                        }
                                        items(overdue) { task -> TaskRow(task, vm) }
                                    }
                                    item { Spacer(Modifier.height(60.dp)) }
                                }
                            }
                        }
                    }
                }
            }
            // 完成庆祝浮层（VisOKR 风格）：顶部覆盖卡片
            celebrate?.let { c ->
                SummitCard(
                    modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp).padding(top = 8.dp).fillMaxWidth(),
                    containerColor = t.card,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎉", fontSize = 26.sp)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.msg, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            if (c.sub.isNotBlank()) {
                                Text(
                                    if (c.pct.isNotBlank()) "${c.sub} · ${c.pct}" else c.sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = t.textTertiary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            // 日历写入结果提示
            toast?.let { msg ->
                SummitCard(
                    modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp).padding(top = 8.dp).fillMaxWidth(),
                    containerColor = t.card,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 22.sp)
                        Spacer(Modifier.size(10.dp))
                        Text(msg, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (showCreate) {
            TaskCreateDialog(
                initialDate = selected,
                onDismiss = { showCreate = false },
                onCreate = { title, scheduledAt, repeat ->
                    vm.create(title, scheduledAt, repeat)
                    showCreate = false
                },
            )
        }
    }
}

/** 把选中日期的任务写入系统日历（按任务 id 去重，重复点击安全） */
private fun exportDayTasks(
    context: android.content.Context,
    tasks: List<Task>,
    selected: LocalDate,
    onResult: (String) -> Unit,
) {
    val items = tasks.mapNotNull { t ->
        val raw = t.scheduledAt
        if (raw == null) {
            // 无排期：写入当天全天事件
            val dayStart = selected.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            CalendarStore.CalTask(t.id, t.title, dayStart, dayStart + 24 * 3600_000L, allDay = true)
        } else {
            val millis = runCatching { Instant.parse(raw).toEpochMilli() }
                .getOrElse {
                    runCatching {
                        LocalDateTime.parse(raw.take(19)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }.getOrNull()
                }
            millis?.let { CalendarStore.CalTask(t.id, t.title, it, it + 30 * 60_000L) }
        }
    }
    if (items.isEmpty()) {
        onResult("这一天没有任务")
        return
    }
    val (added, skipped) = runCatching { CalendarStore.upsertTasks(context, items) }
        .getOrElse { return onResult("写入日历失败") }
    onResult(
        when {
            added > 0 && skipped > 0 -> "已添加 $added 条日历事件，$skipped 条已存在"
            added > 0 -> "已添加 $added 条日历事件"
            else -> "任务已在日历中"
        }
    )
}

@Composable
private fun WeekStrip(
    anchor: LocalDate,
    selected: LocalDate,
    tasksByDay: (LocalDate) -> List<Task>,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val t = LocalSummitTokens.current
    val today = LocalDate.now()
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    SummitCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevWeek, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.ChevronLeft, null, modifier = Modifier.size(18.dp))
            }
            Text(
                "${anchor.monthValue}月${anchor.dayOfMonth}日 - ${anchor.plusDays(6).monthValue}月${anchor.plusDays(6).dayOfMonth}日",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = onNextWeek, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp))
            }
            TextButton(onClick = onToday) { Text("今天", fontSize = 12.sp) }
        }
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val date = anchor.plusDays(i.toLong())
                val list = tasksByDay(date)
                val done = list.count { it.status == "completed" }
                val allDone = list.isNotEmpty() && done == list.size
                val isSel = date == selected
                val isToday = date == today
                val shape = RoundedCornerShape(t.radiusControl)
                Column(
                    Modifier.weight(1f).padding(horizontal = 2.dp).clip(shape)
                        .background(
                            when {
                                isSel -> scheme.primaryContainer
                                isToday -> scheme.primaryContainer.copy(alpha = 0.45f)
                                else -> Color.Transparent
                            },
                        )
                        .border(if (isSel) 1.5.dp else 1.dp, if (isSel) scheme.primary else Color.Transparent, shape)
                        .clickable { onSelect(date) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(labels[i], fontSize = 10.sp, color = if (isSel) scheme.onPrimaryContainer else t.textTertiary)
                    Text(
                        "${date.dayOfMonth}",
                        fontSize = 15.sp,
                        fontWeight = if (isToday || isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) scheme.onPrimaryContainer else scheme.onSurface,
                    )
                    Box(
                        Modifier.size(4.dp).clip(CircleShape)
                            .background(
                                when {
                                    list.isEmpty() -> Color.Transparent
                                    allDone -> t.success
                                    else -> scheme.primary.copy(alpha = 0.5f)
                                },
                            ),
                    )
                    Text(
                        if (list.isNotEmpty()) "$done/${list.size}" else " ",
                        fontSize = 9.sp,
                        color = if (allDone) t.success else t.textTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarBar(
    month: YearMonth,
    selected: LocalDate,
    hasTasks: (LocalDate) -> Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val first = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val offset = (first.dayOfWeek.value + 6) % 7 // Monday-first
    val today = LocalDate.now()
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.Outlined.ChevronLeft, null) }
            Text("${month.year}年${month.monthValue}月", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onNext) { Icon(Icons.Outlined.ChevronRight, null) }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(it, fontSize = 11.sp, color = LocalSummitTokens.current.textTertiary)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        val cells = offset + daysInMonth
        val rows = (cells + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val idx = r * 7 + c
                    val dayNum = idx - offset + 1
                    Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..daysInMonth) {
                            val date = month.atDay(dayNum)
                            val isSel = date == selected
                            val isToday = date == today
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape)
                                        .background(
                                            when {
                                                isSel -> scheme.primary
                                                isToday -> scheme.primaryContainer
                                                else -> Color.Transparent
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "$dayNum",
                                        fontSize = 13.sp,
                                        color = when {
                                            isSel -> scheme.onPrimary
                                            isToday -> scheme.onPrimaryContainer
                                            else -> scheme.onSurface
                                        },
                                        fontWeight = if (isToday || isSel) FontWeight.Bold else null,
                                    )
                                }
                                Box(
                                    Modifier.size(4.dp).clip(CircleShape)
                                        .background(if (hasTasks(date)) LocalSummitTokens.current.success else Color.Transparent),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, vm: TasksVM) {
    val t = LocalSummitTokens.current
    val done = task.status == "completed"
    SummitCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        containerColor = if (done) t.success.copy(alpha = 0.10f) else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.toggle(task) }) {
                Icon(
                    if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    null,
                    tint = if (done) t.success else t.textTertiary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (done) t.textTertiary else MaterialTheme.colorScheme.onSurface,
                )
                val sub = listOfNotNull(
                    task.scheduledAt?.let { fmtTime(it) },
                    if (task.repeatRule != "none") repeatLabel(task.repeatRule) else null,
                    task.contribution?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
            }
            IconButton(onClick = { vm.remove(task.id) }) {
                Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(20.dp), tint = t.textTertiary)
            }
        }
    }
}

private fun fmtTime(iso: String): String = try {
    Instant.parse(if (iso.endsWith("Z")) iso else iso + "Z")
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (_: Exception) { "" }

fun repeatLabel(r: String): String = when (r) {
    "daily" -> "每天"
    "weekly" -> "每周"
    "monthly" -> "每月"
    "yearly" -> "每年"
    "weekdays" -> "工作日"
    else -> r
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCreateDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onCreate: (String, String?, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var repeat by remember { mutableStateOf("none") }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    // 对齐 Flutter TaskEditSheet：底部弹层表单（圆角顶部 24dp）
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("新建任务", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("任务标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Text(date.format(DateTimeFormatter.ofPattern("M月d日")))
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    Text(time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "时间")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("none", "daily", "weekly", "weekdays").forEach { r ->
                    val sel = repeat == r
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { repeat = r }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(repeatLabel(r), fontSize = 12.sp, color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.size(8.dp))
                TextButton(
                    onClick = {
                        val dt = time ?: LocalTime.of(9, 0)
                        val iso = LocalDateTime.of(date, dt).atZone(ZoneId.systemDefault()).toInstant().toString()
                        onCreate(title.trim(), iso, repeat)
                    },
                    enabled = title.isNotBlank(),
                ) { Text("保存") }
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    showDate = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
    if (showTime) {
        val timeState = remember { mutableStateOf(TimePickerState(9, 0)) }
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("选择时间") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("时  ", fontSize = 14.sp)
                        OutlinedTextField(
                            value = timeState.value.h.toString(),
                            onValueChange = { v -> timeState.value = timeState.value.copy(h = v.toIntOrNull()?.coerceIn(0, 23) ?: 0) },
                            modifier = Modifier.size(width = 72.dp, height = 64.dp),
                            singleLine = true,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text("分  ", fontSize = 14.sp)
                        OutlinedTextField(
                            value = timeState.value.m.toString(),
                            onValueChange = { v -> timeState.value = timeState.value.copy(m = v.toIntOrNull()?.coerceIn(0, 59) ?: 0) },
                            modifier = Modifier.size(width = 72.dp, height = 64.dp),
                            singleLine = true,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { time = LocalTime.of(timeState.value.h, timeState.value.m); showTime = false }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消") } },
        )
    }
}

private data class TimePickerState(val h: Int, val m: Int)