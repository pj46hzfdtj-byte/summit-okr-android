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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.visokr.android.core.FocusVM
import com.visokr.android.core.GoalsVM
import com.visokr.android.core.Objective
import com.visokr.android.core.Routes
import com.visokr.android.core.UiState
import com.visokr.android.ui.EmptyState
import com.visokr.android.ui.ErrorView
import com.visokr.android.ui.LoadingView
import com.visokr.android.ui.LocalVisTokens
import com.visokr.android.ui.PillTag
import com.visokr.android.ui.VisCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusPage(navController: NavController) {
    val vm: FocusVM = viewModel()
    val cycle by vm.cycle.collectAsState()
    val t = LocalVisTokens.current
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            TopAppBar(
                title = { Text("专注周期", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                Icon(Icons.Outlined.Add, null)
            }
        },
    ) { padding ->
        when (val s = cycle) {
            is UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Error -> ErrorView(s.message, onRetry = { vm.refresh() }, modifier = Modifier.padding(padding))
            is UiState.Success -> {
                val c = s.data
                if (c == null) EmptyState("暂无活跃专注周期", Modifier.padding(padding), icon = Icons.Outlined.Timer)
                else LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        VisCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    if (c.isActive) PillTag("活跃", MaterialTheme.colorScheme.primary)
                                    c.cycleScore?.let {
                                        Spacer(Modifier.size(8.dp))
                                        Text(String.format("%.1f 分", it), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("${fmtDate(c.startAt)} → ${fmtDate(c.endAt)}", style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
                                Spacer(Modifier.height(10.dp))
                                c.objectives.forEach { co ->
                                    var editWeight by remember(co.objectiveId) { mutableStateOf(false) }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            co.objective?.title ?: co.objectiveId.take(8),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                        )
                                        Text("${co.weight.toInt()}%", fontSize = 12.sp, color = t.textTertiary)
                                        Spacer(Modifier.size(4.dp))
                                        TextButton(onClick = { editWeight = true }) { Text("权重", fontSize = 12.sp) }
                                        if (editWeight) {
                                            WeightDialog(co.weight.toInt(), onDismiss = { editWeight = false }) { w ->
                                                vm.setWeight(c.id, co.objectiveId, w.toDouble())
                                                editWeight = false
                                            }
                                        }
                                    }
                                }
                                if (c.isActive) {
                                    Spacer(Modifier.height(6.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { vm.endCycle(c.id) }) { Text("结束周期", color = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(60.dp)) }
                }
            }
        }
    }

    if (showCreate) {
        CreateCycleDialog(onDismiss = { showCreate = false }, onCreate = { name, ids ->
            vm.create(name, ids)
            showCreate = false
        })
    }
}

@Composable
private fun WeightDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("目标权重") },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it }, label = { Text("权重（0-100）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { value.toIntOrNull()?.let { onSave(it.coerceIn(0, 100)) } }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun CreateCycleDialog(onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    val goalsVM: GoalsVM = viewModel()
    val tree by goalsVM.tree.collectAsState()
    val objectives = remember(tree) {
        val list = ArrayList<Objective>()
        fun walk(groups: List<com.visokr.android.core.GoalGroup>) {
            groups.forEach { list.addAll(it.objectives); walk(it.children) }
        }
        (tree as? UiState.Success)?.data?.let { walk(it) }
        list
    }
    var name by remember { mutableStateOf("") }
    val selected = remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建专注周期", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (objectives.isEmpty()) {
                    Text("请先创建目标", style = MaterialTheme.typography.bodySmall, color = LocalVisTokens.current.textTertiary)
                } else {
                    LazyColumn(Modifier.height(240.dp)) {
                        items(objectives) { o ->
                            val checked = selected.value.contains(o.id)
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    selected.value = if (checked) selected.value - o.id else selected.value + o.id
                                }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = checked, onCheckedChange = {
                                    selected.value = if (checked) selected.value - o.id else selected.value + o.id
                                })
                                Text(o.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.ifBlank { "专注周期" }, selected.value.toList()) },
                enabled = selected.value.isNotEmpty(),
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}