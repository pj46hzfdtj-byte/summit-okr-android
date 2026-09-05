package com.visokr.android.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.visokr.android.core.AiMotivationsReq
import com.visokr.android.core.AiPlanGoalReq
import com.visokr.android.core.AiPlanGoalResult
import com.visokr.android.core.AiPlanTaskResult
import com.visokr.android.core.AiPlanTasksReq
import com.visokr.android.core.AiSuggestScoreReq
import com.visokr.android.core.AiSuggestScoreResult
import com.visokr.android.core.AiVM
import com.visokr.android.core.CreateKeyResultReq
import com.visokr.android.core.CreateObjectiveReq
import com.visokr.android.core.CreateTaskReq
import com.visokr.android.core.GoalGroup
import com.visokr.android.core.GoalsVM
import com.visokr.android.core.NetClient
import com.visokr.android.core.Objective
import com.visokr.android.core.Routes
import com.visokr.android.core.UiState
import com.visokr.android.core.unwrap
import com.visokr.android.core.unwrapOrNull
import com.visokr.android.ui.LocalVisTokens
import com.visokr.android.ui.VisCard
import com.visokr.android.ui.VisTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPage(navController: NavController) {
    val vm: AiVM = viewModel()
    val usage by vm.usage.collectAsState()
    val t = LocalVisTokens.current
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("规划目标", "拆解任务", "复盘评分", "动机建议")

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            VisTopBar(
                title = { Text("AI 助手", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } },
                actions = {
                    usage?.let {
                        Text("${it.used}/${it.limit}", fontSize = 12.sp, color = t.textTertiary, modifier = Modifier.padding(end = 12.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label, fontSize = 13.sp) })
                }
            }
            when (tab) {
                0 -> PlanGoalTab(vm, navController)
                1 -> PlanTasksTab(vm, navController)
                2 -> ScoreTab(vm)
                else -> MotivateTab(vm)
            }
        }
    }
}

@Composable
private fun RunButton(onRun: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    FilledTonalButton(
        onClick = {
            if (!busy) scope.launch {
                busy = true
                runCatching { onRun() }
                busy = false
            }
        },
        enabled = !busy,
    ) {
        if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        else Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(if (busy) "生成中…" else "生成")
    }
}

@Composable
private fun PlanGoalTab(vm: AiVM, navController: NavController) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<AiPlanGoalResult?>(null) }
    val checked = remember { mutableStateOf(setOf<Int>()) }
    var applied by remember { mutableStateOf(false) }
    val goalsVM: GoalsVM = viewModel()
    val tree by goalsVM.tree.collectAsState()
    val firstGroupId = remember(tree) {
        var id: String? = null
        fun walk(gs: List<GoalGroup>) { gs.forEach { if (id == null) id = it.id; walk(it.children) } }
        (tree as? UiState.Success)?.data?.let { walk(it) }
        id
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = input, onValueChange = { input = it; result = null; applied = false },
            label = { Text("描述你想达成的目标…") }, placeholder = { Text("如：三个月内跑完半程马拉松") },
            modifier = Modifier.fillMaxWidth(), minLines = 2,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            RunButton {
                if (input.isBlank()) return@RunButton
                result = NetClient.api.aiPlanGoal(AiPlanGoalReq(input.trim())).unwrap()
                vm.refreshUsage()
            }
        }
        result?.let { r ->
            Text(r.objective.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (r.objective.motivations.isNotEmpty()) BulletList("动机", r.objective.motivations)
            if (r.objective.feasibilities.isNotEmpty()) BulletList("可行性", r.objective.feasibilities)
            Text("关键结果", style = MaterialTheme.typography.labelLarge)
            r.keyResults.forEachIndexed { i, kr ->
                val sel = checked.value.contains(i)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickableRow {
                    checked.value = if (sel) checked.value - i else checked.value + i
                }) {
                    Checkbox(checked = sel, onCheckedChange = { checked.value = if (sel) checked.value - i else checked.value + i })
                    Column {
                        Text(kr.title, fontSize = 13.sp)
                        Text("${fmtNum(kr.initialValue)} → ${fmtNum(kr.targetValue)}", fontSize = 11.sp, color = LocalVisTokens.current.textTertiary)
                    }
                }
            }
            FilledTonalButton(
                onClick = {
                    val gid = firstGroupId ?: return@FilledTonalButton
                    val snapshot = r
                    scope.launch {
                        runCatching {
                            val created = NetClient.api.createObjective(
                                CreateObjectiveReq(gid, snapshot.objective.title, motivations = snapshot.objective.motivations, feasibilities = snapshot.objective.feasibilities),
                            ).unwrap()
                            checked.value.forEach { idx ->
                                val kr = snapshot.keyResults[idx]
                                NetClient.api.createKeyResult(
                                    CreateKeyResultReq(created.id, kr.title, kr.emoji, kr.initialValue, kr.targetValue, kr.calculationType, kr.weight),
                                ).unwrapOrNull()
                            }
                            applied = true
                            goalsVM.refresh()
                        }
                    }
                },
                enabled = checked.value.isNotEmpty() && firstGroupId != null && !applied,
            ) { Text(if (applied) "已应用到目标库" else "应用到目标库（${checked.value.size}）") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanTasksTab(vm: AiVM, navController: NavController) {
    val scope = rememberCoroutineScope()
    val goalsVM: GoalsVM = viewModel()
    val tree by goalsVM.tree.collectAsState()
    val objectives = remember(tree) {
        val list = ArrayList<Objective>()
        fun walk(gs: List<GoalGroup>) { gs.forEach { list.addAll(it.objectives); walk(it.children) } }
        (tree as? UiState.Success)?.data?.let { walk(it) }
        list
    }
    var picked by remember { mutableStateOf<Objective?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AiPlanTaskResult?>(null) }
    val checked = remember { mutableStateOf(setOf<Int>()) }
    var createdCount by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = picked?.title ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("选择目标") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                objectives.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(o.title, maxLines = 1) },
                        onClick = { picked = o; expanded = false; result = null; checked.value = emptySet(); createdCount = 0 },
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            RunButton {
                val o = picked ?: return@RunButton
                result = NetClient.api.aiPlanTasks(AiPlanTasksReq(objectiveId = o.id)).unwrap()
                vm.refreshUsage()
            }
        }
        result?.let { r ->
            r.tasks.forEachIndexed { i, task ->
                val sel = checked.value.contains(i)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickableRow {
                    checked.value = if (sel) checked.value - i else checked.value + i
                }) {
                    Checkbox(checked = sel, onCheckedChange = { checked.value = if (sel) checked.value - i else checked.value + i })
                    Column {
                        Text(task.title, fontSize = 13.sp)
                        task.contribution?.let { Text(it, fontSize = 11.sp, color = LocalVisTokens.current.textTertiary) }
                    }
                }
            }
            FilledTonalButton(
                onClick = {
                    val o = picked ?: return@FilledTonalButton
                    val snapshot = r
                    val idxs = checked.value.toList()
                    scope.launch {
                        runCatching {
                            idxs.forEach { idx ->
                                val task = snapshot.tasks[idx]
                                NetClient.api.createTask(
                                    CreateTaskReq(task.title, objectiveId = o.id, description = task.description, scheduledAt = task.scheduledAt, repeatRule = task.repeatRule, contribution = task.contribution),
                                ).unwrapOrNull()
                            }
                            createdCount = idxs.size
                            goalsVM.refresh()
                        }
                    }
                },
                enabled = checked.value.isNotEmpty() && createdCount == 0,
            ) { Text(if (createdCount > 0) "已创建 $createdCount 个任务" else "创建任务（${checked.value.size}）") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreTab(vm: AiVM) {
    val goalsVM: GoalsVM = viewModel()
    val tree by goalsVM.tree.collectAsState()
    val objectives = remember(tree) {
        val list = ArrayList<Objective>()
        fun walk(gs: List<GoalGroup>) { gs.forEach { list.addAll(it.objectives); walk(it.children) } }
        (tree as? UiState.Success)?.data?.let { walk(it) }
        list
    }
    var picked by remember { mutableStateOf<Objective?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AiSuggestScoreResult?>(null) }
    val t = LocalVisTokens.current

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = picked?.title ?: "", onValueChange = {}, readOnly = true,
                label = { Text("选择目标") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                objectives.forEach { o -> DropdownMenuItem(text = { Text(o.title, maxLines = 1) }, onClick = { picked = o; expanded = false; result = null }) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            RunButton {
                val o = picked ?: return@RunButton
                result = NetClient.api.aiSuggestScore(AiSuggestScoreReq(o.id)).unwrap()
                vm.refreshUsage()
            }
        }
        result?.let { r ->
            VisCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("建议自评：${r.selfRating.toInt()} 分", style = MaterialTheme.typography.titleSmall)
                    r.krScores.forEach { kr ->
                        Text("KR ${kr.keyResultId.take(6)}… → ${kr.score.toInt()} 分", fontSize = 13.sp)
                    }
                    r.reasoning?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("提示：70 分为健康线", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun MotivateTab(vm: AiVM) {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<List<String>?>(null) }
    val t = LocalVisTokens.current

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = input, onValueChange = { input = it; result = null },
            label = { Text("目标标题") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            RunButton {
                if (input.isBlank()) return@RunButton
                result = NetClient.api.aiMotivations(AiMotivationsReq(input.trim())).unwrap().motivations
                vm.refreshUsage()
            }
        }
        result?.forEach { m ->
            VisCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.FavoriteBorder, null, tint = t.danger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(m, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

@Composable
private fun BulletList(title: String, items: List<String>) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        items.forEach { Text("· $it", fontSize = 13.sp) }
    }
}