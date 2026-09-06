package com.summitokr.android.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.summitokr.android.core.Vision
import com.summitokr.android.core.VisionVM
import com.summitokr.android.core.UiState
import com.summitokr.android.core.normProgress
import com.summitokr.android.ui.CapsuleProgress
import com.summitokr.android.ui.EmptyState
import com.summitokr.android.ui.ErrorView
import com.summitokr.android.ui.LoadingView
import com.summitokr.android.ui.LocalSummitTokens
import com.summitokr.android.ui.PillTag
import com.summitokr.android.ui.SummitCard
import com.summitokr.android.ui.SummitTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionPage(navController: NavController) {
    val vm: VisionVM = viewModel()
    val visions by vm.visions.collectAsState()
    val t = LocalSummitTokens.current
    var editing by remember { mutableStateOf<Vision?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            SummitTopBar(
                title = { Text("愿景", fontWeight = FontWeight.Bold) },
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
        when (val s = visions) {
            is UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Error -> ErrorView(s.message, Modifier.padding(padding)) { vm.refresh() }
            is UiState.Success -> {
                if (s.data.isEmpty()) EmptyState("暂无愿景", Modifier.padding(padding), icon = Icons.Outlined.VisibilityOff)
                else LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(s.data) { v ->
                        var menu by remember(v.id) { mutableStateOf(false) }
                        SummitCard(
                            modifier = Modifier.aspectRatio(0.95f).clickable { menu = true },
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PillTag(visionStatusLabel(v.status), visionStatusColor(v.status))
                                    Spacer(Modifier.weight(1f))
                                    if (v.startAge != null && v.endAge != null) {
                                        Text("${v.startAge}-${v.endAge}岁", fontSize = 11.sp, color = t.textTertiary)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(v.content, style = MaterialTheme.typography.bodyMedium, maxLines = 5, modifier = Modifier.weight(1f, fill = true))
                                if (v.progress != null) {
                                    Spacer(Modifier.height(6.dp))
                                    CapsuleProgress(normProgress(v.progress).toFloat())
                                    Spacer(Modifier.height(4.dp))
                                    Text("${(normProgress(v.progress) * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        if (menu) {
                            AlertDialog(
                                onDismissRequest = { menu = false },
                                title = { Text("愿景操作") },
                                text = {
                                    Column {
                                        MenuRow(Icons.Outlined.Edit, "编辑") { editing = v; menu = false }
                                        if (v.status != "achieved") {
                                            MenuRow(Icons.Outlined.EmojiEvents, "标记实现") { vm.achieve(v.id); menu = false }
                                        } else {
                                            MenuRow(Icons.Outlined.RestartAlt, "恢复状态") { vm.reset(v.id); menu = false }
                                        }
                                        MenuRow(Icons.Outlined.DeleteOutline, "删除", danger = true) { vm.remove(v.id); menu = false }
                                    }
                                },
                                confirmButton = { TextButton(onClick = { menu = false }) { Text("取消") } },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editing != null) {
        VisionEditDialog(
            initial = editing,
            onDismiss = { showCreate = false; editing = null },
            onSave = { content, sa, ea ->
                if (editing == null) vm.create(content, sa, ea) else vm.update(editing!!.id, content, sa, ea)
                showCreate = false; editing = null
            },
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(10.dp))
        Text(label, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisionEditDialog(initial: Vision?, onDismiss: () -> Unit, onSave: (String, Int?, Int?) -> Unit) {
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var startAge by remember { mutableStateOf(initial?.startAge?.toString() ?: "") }
    var endAge by remember { mutableStateOf(initial?.endAge?.toString() ?: "") }
    // 对齐 Flutter：愿景创建/编辑用底部弹层
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (initial == null) "新建愿景" else "编辑愿景", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("愿景内容") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startAge, onValueChange = { startAge = it }, label = { Text("起始年龄") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true,
                )
                OutlinedTextField(
                    value = endAge, onValueChange = { endAge = it }, label = { Text("结束年龄") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.size(8.dp))
                TextButton(
                    onClick = { onSave(content.trim(), startAge.toIntOrNull(), endAge.toIntOrNull()) },
                    enabled = content.isNotBlank(),
                ) { Text("保存") }
            }
        }
    }
}

@Composable
private fun visionStatusColor(status: String): Color {
    val t = LocalSummitTokens.current
    return when (status) {
        "in_progress" -> MaterialTheme.colorScheme.primary
        "achieved" -> t.success
        "expired" -> t.warning
        else -> t.textTertiary
    }
}

fun visionStatusLabel(s: String): String = when (s) {
    "upcoming" -> "待启动"
    "in_progress" -> "进行中"
    "achieved" -> "已实现"
    "expired" -> "已过期"
    else -> s
}
