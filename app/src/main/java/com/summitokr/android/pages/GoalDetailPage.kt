package com.summitokr.android.pages

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.summitokr.android.core.GoalDetailVM
import com.summitokr.android.core.Memo
import com.summitokr.android.core.NetClient
import com.summitokr.android.core.Routes
import com.summitokr.android.core.TrendPoint
import com.summitokr.android.core.UiState
import com.summitokr.android.core.normProgress
import com.summitokr.android.core.unwrap
import com.summitokr.android.ui.CapsuleProgress
import com.summitokr.android.ui.ErrorView
import com.summitokr.android.ui.LoadingView
import com.summitokr.android.ui.LocalSummitTokens
import com.summitokr.android.ui.PillTag
import com.summitokr.android.ui.RingProgress
import com.summitokr.android.ui.SummitCard
import com.summitokr.android.ui.SummitTopBar
import com.summitokr.android.ui.parseHexColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailPage(objectiveId: String, navController: NavController) {
    val vm: GoalDetailVM = viewModel(key = "goal_$objectiveId") { GoalDetailVM(objectiveId) }
    val detail by vm.detail.collectAsState()
    val trends by vm.trends.collectAsState()
    val t = LocalSummitTokens.current

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            SummitTopBar(
                title = { Text("目标详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
      PullToRefreshBox(onRefresh = { vm.refresh() }, isRefreshing = false, modifier = Modifier.padding(padding).fillMaxSize()) {
        when (val s = detail) {
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(s.message, onRetry = { vm.refresh() })
            is UiState.Success -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val obj = s.data.objective
                val color = parseHexColor(obj.color)
                item { HeaderCard(obj.title, color, objectiveStatusLabel(obj.status), obj.startAt, obj.endAt, normProgress(obj.currentProgress).toFloat(), obj.isLagging == true, normProgress(obj.expectedProgress).toFloat()) }
                if (obj.motivations.isNotEmpty()) item { ChipsCard("动机", obj.motivations, Icons.Outlined.FavoriteBorder) }
                if (obj.feasibilities.isNotEmpty()) item { ChipsCard("可行性", obj.feasibilities, Icons.Outlined.Build) }
                item { Text("关键结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(s.data.keyResults) { kr ->
                    var expanded by remember(kr.id) { mutableStateOf(false) }
                    var showRecord by remember(kr.id) { mutableStateOf(false) }
                    var showMemos by remember(kr.id) { mutableStateOf(false) }
                    val progress = (kr.currentProgress ?: run {
                        val span = kr.targetValue - kr.initialValue
                        if (span == 0.0) 0.0 else ((kr.currentValue - kr.initialValue) / span)
                    }).coerceIn(0.0, 1.0)
                    // 对齐 Flutter Dismissible：KR 行滑动删除（确认后调 deleteKeyResult）
                    var confirmDelete by remember(kr.id) { mutableStateOf(false) }
                    val dismissState = rememberSwipeToDismissBoxState(
                        // 不真正移除行（避免确认前卡片塌陷消失），删除由确认框触发
                        confirmValueChange = {
                            if (it != SwipeToDismissBoxValue.Settled) confirmDelete = true
                            false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        modifier = Modifier.clip(RoundedCornerShape(t.radiusCard)),
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(t.radiusCard))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        },
                    ) {
                        SummitCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickableRow { expanded = !expanded }) {
                                Text(kr.emoji, fontSize = 18.sp)
                                Spacer(Modifier.size(8.dp))
                                Text(kr.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(6.dp))
                            CapsuleProgress(progress.toFloat())
                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${calcLabel(kr.calculationType)} · ${fmtNum(kr.initialValue)} → ${fmtNum(kr.targetValue)}（当前 ${fmtNum(kr.currentValue)}）· 权重 ${kr.weight.toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = t.textTertiary,
                                )
                                trends[kr.id]?.let { pts -> if (pts.isNotEmpty()) TrendChart(pts, color) }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = { showRecord = true }) {
                                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("记录")
                                    }
                                    OutlinedButton(onClick = { showMemos = true }) {
                                        Icon(Icons.Outlined.StickyNote2, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("备忘")
                                    }
                                }
                            }
                        }
                    }
                    }
                    // 弹层与对话框移出 SummitCard，避免影响卡片布局
                    if (showRecord) {
                        RecordSheet(kr.title, kr.currentValue, onDismiss = { showRecord = false }) { value, note ->
                            vm.addRecord(kr.id, value, note)
                            showRecord = false
                        }
                    }
                    if (showMemos) {
                        MemoSheet(kr.id) { showMemos = false }
                    }
                    if (confirmDelete) {
                        AlertDialog(
                            onDismissRequest = { confirmDelete = false },
                            title = { Text("删除关键结果") },
                            text = { Text("确定删除「${kr.title}」？将移入回收站。") },
                            confirmButton = {
                                TextButton(onClick = { confirmDelete = false; vm.deleteKeyResult(kr.id) }) {
                                    Text("删除", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
      }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

@Composable
private fun HeaderCard(title: String, color: Color, status: String, startAt: String?, endAt: String?, progress: Float, lagging: Boolean, expected: Float) {
    SummitCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // VisOKR 风格：完成度圆环
                RingProgress(progress, modifier = Modifier.size(64.dp), color = color) {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = color)
                    Text(status, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
                }
            }
            if (!startAt.isNullOrBlank() && !endAt.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("${fmtDate(startAt)} → ${fmtDate(endAt)}", style = MaterialTheme.typography.bodySmall, color = LocalSummitTokens.current.textTertiary)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CapsuleProgress(progress, modifier = Modifier.weight(1f), color = color, height = 8.dp)
                Spacer(Modifier.size(8.dp))
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            if (lagging) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("进度滞后（预期 ${(expected * 100).toInt()}%）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ChipsCard(title: String, items: List<String>, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SummitCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(4.dp))
                Text(title, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                items.forEach { PillTag(it, MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

/** 简化趋势图：柱状累计值 */
@Composable
private fun TrendChart(points: List<TrendPoint>, color: Color) {
    val maxV = points.maxOf { kotlin.math.abs(it.cumulativeValue) }.coerceAtLeast(1.0)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom,
        ) {
            points.takeLast(30).forEach { p ->
                val h = (kotlin.math.abs(p.cumulativeValue) / maxV).toFloat().coerceIn(0.04f, 1f)
                Box(
                    Modifier.size(width = 6.dp, height = (88 * h).dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(color.copy(alpha = 0.75f)),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "${fmtDate(points.first().recordedAt).take(5)} … ${fmtDate(points.last().recordedAt).take(5)}",
            style = MaterialTheme.typography.labelSmall,
            color = LocalSummitTokens.current.textTertiary,
        )
    }
}

/** KR 记录：底部弹层（对齐 Flutter showModalBottomSheet） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordSheet(title: String, current: Double, onDismiss: () -> Unit, onSave: (Double, String?) -> Unit) {
    var value by remember { mutableStateOf(current.toString()) }
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("数值") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.size(8.dp))
                TextButton(onClick = { val v = value.toDoubleOrNull(); if (v != null) onSave(v, note.ifBlank { null }) }) { Text("保存") }
            }
        }
    }
}

@Composable
private fun MemoSheet(krId: String, onClose: () -> Unit) {
    val memos = remember { mutableStateOf<List<Memo>?>(null) }
    val input = remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(refreshKey, krId) {
        runCatching { memos.value = NetClient.api.memos("key_result", krId).unwrap() }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("备忘", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().height(280.dp)) {
                val list = memos.value
                if (list == null) LoadingView()
                else if (list.isEmpty()) Box(Modifier.fillMaxWidth(), Alignment.Center) { Text("暂无备忘", color = LocalSummitTokens.current.textTertiary) }
                else LazyColumn(Modifier.weight(1f)) {
                    items(list) { m ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(m.content, style = MaterialTheme.typography.bodyMedium)
                            Text(fmtDateTime(m.createdAt), style = MaterialTheme.typography.labelSmall, color = LocalSummitTokens.current.textTertiary)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input.value,
                        onValueChange = { input.value = it },
                        placeholder = { Text("记录一条备忘…") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = {
                        val text = input.value.trim()
                        if (text.isNotEmpty()) {
                            CoroutineScope(Dispatchers.Main).launch {
                                runCatching {
                                    NetClient.api.createMemo(com.summitokr.android.core.CreateMemoReq("key_result", krId, text)).unwrap()
                                    input.value = ""
                                    refreshKey++
                                }
                            }
                        }
                    }) { Text("添加") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("关闭") } },
    )
}

fun calcLabel(type: String): String = when (type) {
    "sum" -> "求和"
    "final" -> "最终值"
    "average" -> "平均值"
    "max" -> "最大值"
    "custom" -> "自定义"
    else -> type
}

fun fmtNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else String.format("%.1f", v)

fun fmtDate(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
} catch (_: Exception) { iso.take(10) }

fun fmtDateTime(iso: String): String = try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
} catch (_: Exception) { iso.take(16) }