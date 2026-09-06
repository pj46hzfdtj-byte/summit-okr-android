package com.summitokr.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var showCreate by remember { mutableStateOf(false) }
    val t = LocalSummitTokens.current

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            SummitTopBar(
                title = { Text("任务", fontWeight = FontWeight.Bold) },
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
        Column(Modifier.padding(padding)) {
            CalendarBar(
                month = month,
                selected = selected,
                hasTasks = { day -> all.any { it.scheduledAt?.startsWith(day.toString()) == true } },
                onPrev = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
                onSelect = { selected = it },
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