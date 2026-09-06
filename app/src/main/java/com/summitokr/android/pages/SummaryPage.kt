package com.summitokr.android.pages

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.summitokr.android.core.SummaryVM
import com.summitokr.android.core.UiState
import com.summitokr.android.core.normProgress
import com.summitokr.android.ui.CapsuleProgress
import com.summitokr.android.ui.ErrorView
import com.summitokr.android.ui.LoadingView
import com.summitokr.android.ui.LocalSummitTokens
import com.summitokr.android.ui.SummitCard
import com.summitokr.android.ui.SummitTopBar
import com.summitokr.android.ui.toPctInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryPage(navController: NavController) {
    val vm: SummaryVM = viewModel()
    val summary by vm.summary.collectAsState()
    val checkin by vm.checkin.collectAsState()
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
                    if (d.activeFocusCycle != null) {
                        item {
                            CycleCard(name = d.activeFocusCycle!!.name, count = d.activeFocusCycle!!.objectives.size) {
                                navController.navigate(Routes.FOCUS)
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiCell("总目标", "${d.totalObjectives}", Modifier.weight(1f))
                            KpiCell("进行中", "${d.inProgressObjectives}", Modifier.weight(1f))
                            KpiCell("已复盘", "${d.completedObjectives}", Modifier.weight(1f))
                            KpiCell("滞后", "${d.laggingObjectives.size}", Modifier.weight(1f), danger = d.laggingObjectives.isNotEmpty())
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
private fun CycleCard(name: String, count: Int, onClick: () -> Unit) {
    val t = LocalSummitTokens.current
    SummitCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Timer, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("$count 个目标", style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = t.textTertiary)
        }
    }
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