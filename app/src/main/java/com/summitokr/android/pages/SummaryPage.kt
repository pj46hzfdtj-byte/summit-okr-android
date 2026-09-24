package com.summitokr.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.summitokr.android.core.KeyResult
import com.summitokr.android.core.Routes
import com.summitokr.android.core.SummaryVM
import com.summitokr.android.core.UiState
import com.summitokr.android.core.normProgress
import com.summitokr.android.ui.CapsuleProgress
import com.summitokr.android.ui.ErrorView
import com.summitokr.android.ui.LoadingView
import com.summitokr.android.ui.LocalSummitTokens
import com.summitokr.android.ui.RingProgress
import com.summitokr.android.ui.SummitCard
import com.summitokr.android.ui.SummitTopBar
import com.summitokr.android.ui.fmtNumLocal
import com.summitokr.android.ui.toPctInt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryPage(navController: NavController) {
    val vm: SummaryVM = viewModel()
    val summary by vm.summary.collectAsState()
    val checkin by vm.checkin.collectAsState()
    var recordKr by remember { mutableStateOf<KeyResult?>(null) }
    val t = LocalSummitTokens.current

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            SummitTopBar(
                title = { Text("摘要", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.NOTIFICATIONS) }) {
                        Icon(Icons.Outlined.NotificationsNone, contentDescription = "通知")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(onRefresh = { vm.refresh() }, isRefreshing = false, modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = summary) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(s.message, onRetry = { vm.refresh() })
                is UiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val d = s.data
                    if (!d.randomMotivation.isNullOrBlank()) {
                        item { MotivationCard(d.randomMotivation!!) }
                    }
                    item { CheckinCard(checkin, vm) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("📝", "今日添加记录", "${d.todayAddedRecords ?: 0}", MaterialTheme.colorScheme.primary, Modifier.weight(1f), null)
                            StatCard("🎯", "进行中目标", "${d.inProgressObjectives}", t.success, Modifier.weight(1f), null)
                            StatCard("🔔", "今日任务", "${d.todayTaskCount ?: d.todayTasks.size}", MaterialTheme.colorScheme.error, Modifier.weight(1f)) {
                                navController.navigate(Routes.TASKS)
                            }
                        }
                    }
                    if (d.activeFocusCycle != null) {
                        val cycle = d.activeFocusCycle!!
                        item {
                            CycleRingCard(
                                name = cycle.name,
                                startAt = cycle.startAt,
                                endAt = cycle.endAt,
                                score = cycle.cycleScore,
                                daysRemaining = d.cycleDaysRemaining,
                                todayDelta = d.todayProgressDelta,
                                objectiveCount = cycle.objectives.size,
                                onManage = { navController.navigate(Routes.FOCUS) },
                                onObjective = { navController.navigate(Routes.goalDetail(it)) },
                                krChips = cycle.objectives.mapNotNull { m ->
                                    val o = m.objective ?: return@mapNotNull null
                                    Triple(o.id, o.title, o.keyResults ?: emptyList())
                                },
                                onAddRecord = { kr -> recordKr = kr },
                            )
                        }
                    } else {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                KpiCell("总目标", "${d.totalObjectives}", Modifier.weight(1f))
                                KpiCell("进行中", "${d.inProgressObjectives}", Modifier.weight(1f))
                                KpiCell("已复盘", "${d.completedObjectives}", Modifier.weight(1f))
                                KpiCell("滞后", "${d.laggingObjectives.size}", Modifier.weight(1f), danger = d.laggingObjectives.isNotEmpty())
                            }
                        }
                    }
                    if (d.todayTasks.isNotEmpty()) {
                        item { Text("今日待办", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(d.todayTasks) { task ->
                            SummitCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { vm.toggleTask(task.id, true) }) {
                                        Icon(Icons.Outlined.RadioButtonUnchecked, null, tint = t.textTertiary)
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(task.title, style = MaterialTheme.typography.bodyMedium)
                                        task.description?.let {
                                            Text(it, style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (d.laggingObjectives.isNotEmpty()) {
                        item { Text("滞后目标", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        items(d.laggingObjectives) { obj ->
                            SummitCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(Routes.goalDetail(obj.id)) },
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.TrendingDown, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.size(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(obj.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                        Spacer(Modifier.height(6.dp))
                                        val cur = normProgress(obj.currentProgress).toFloat()
                                        val exp = normProgress(obj.expectedProgress).toFloat()
                                        CapsuleProgress(cur, color = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "当前 ${(cur * 100).toInt()}% · 预期 ${(exp * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = t.textTertiary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    recordKr?.let { kr ->
        KrRecordDialog(kr, onDismiss = { recordKr = null }, onSubmit = { value, note ->
            vm.addRecord(kr.id, value, note)
            recordKr = null
        })
    }
}

@Composable
private fun MotivationCard(text: String) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(com.summitokr.android.ui.BrandGradient)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Bolt, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.size(12.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CheckinCard(state: UiState<com.summitokr.android.core.CheckInStatus>, vm: SummaryVM) {
    val t = LocalSummitTokens.current
    if (state !is UiState.Success) return
    val s = state.data
    SummitCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (s.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                null,
                tint = if (s.done) t.success else t.textTertiary,
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("每周 Check-in", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("连续 ${s.streak} 周 · KR 更新 ${s.krUpdatedCount}/${s.totalActiveKrCount}", style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
            }
            if (s.done) {
                Text("已打卡", color = t.success, style = MaterialTheme.typography.labelMedium)
            } else {
                FilledTonalButton(onClick = { vm.checkinNow() }) { Text("打卡") }
            }
        }
    }
}

@Composable
private fun StatCard(emoji: String, label: String, value: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)?) {
    val t = LocalSummitTokens.current
    SummitCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 18.sp, color = color)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = t.textTertiary, maxLines = 1)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CycleRingCard(
    name: String,
    startAt: String,
    endAt: String,
    score: Double?,
    daysRemaining: Int?,
    todayDelta: Double?,
    objectiveCount: Int,
    krChips: List<Triple<String, String, List<KeyResult>>>,
    onManage: () -> Unit,
    onObjective: (String) -> Unit,
    onAddRecord: (KeyResult) -> Unit,
) {
    val t = LocalSummitTokens.current
    SummitCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯 $name", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("管理 ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onManage))
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${fmtDateShort(startAt)} → ${fmtDateShort(endAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = t.textTertiary,
            )
            if (daysRemaining != null) {
                Spacer(Modifier.size(8.dp))
                Text(
                    "剩余 $daysRemaining 天",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (daysRemaining <= 7) MaterialTheme.colorScheme.error else t.warning,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val pct = (score ?: 0.0).toFloat() / 100f
            RingProgress(progress = pct, modifier = Modifier.size(84.dp), color = MaterialTheme.colorScheme.primary) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${(score ?: 0.0).toInt()}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text("%", fontSize = 11.sp, color = t.textTertiary)
                    }
                    Text("周期进度", style = MaterialTheme.typography.labelSmall, color = t.textTertiary)
                }
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${deltaText(todayDelta)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = t.success,
                        )
                        Text("今日增加进度", style = MaterialTheme.typography.labelSmall, color = t.textTertiary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("$objectiveCount 个", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("进行中目标", style = MaterialTheme.typography.labelSmall, color = t.textTertiary)
                    }
                }
            }
        }
        krChips.forEach { (objId, objTitle, krs) ->
            if (krs.isEmpty()) return@forEach
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clickable { onObjective(objId) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(objTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(16.dp), tint = t.textTertiary)
            }
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                krs.forEach { kr ->
                    KrChip(kr = kr, onClick = { onAddRecord(kr) })
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun KrChip(kr: KeyResult, onClick: () -> Unit) {
    val t = LocalSummitTokens.current
    val shape = RoundedCornerShape(t.radiusControl)
    Column(
        Modifier.clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text("${kr.emoji} ${kr.title}", style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.widthIn(max = 180.dp))
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${fmtNumLocal(kr.initialValue)} → ${fmtNumLocal(kr.targetValue)}",
                style = MaterialTheme.typography.labelSmall,
                color = t.textTertiary,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.size(6.dp))
            Text("＋", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun KrRecordDialog(kr: KeyResult, onDismiss: () -> Unit, onSubmit: (Double, String?) -> Unit) {
    var value by remember { mutableStateOf(kr.currentValue.toString()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加记录 · ${kr.emoji} ${kr.title}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("数值（${fmtNumLocal(kr.initialValue)} → ${fmtNumLocal(kr.targetValue)}）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { value.toDoubleOrNull()?.let { onSubmit(it, note.ifBlank { null }) } }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun fmtDateShort(iso: String): String = try {
    val d = parseIsoLocalDate(iso)
    String.format("%02d/%02d", d.monthValue, d.dayOfMonth)
} catch (_: Exception) { "" }

private fun parseIsoLocalDate(iso: String): LocalDate =
    Instant.parse(if (iso.endsWith("Z")) iso else iso + "Z").atZone(ZoneId.systemDefault()).toLocalDate()

private fun deltaText(delta: Double?): String {
    if (delta == null) return "0"
    return String.format("%.1f", delta * 100.0).removeSuffix(".0")
}

@Composable
private fun KpiCell(label: String, value: String, modifier: Modifier = Modifier, danger: Boolean = false) {
    val t = LocalSummitTokens.current
    SummitCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
        }
    }
}