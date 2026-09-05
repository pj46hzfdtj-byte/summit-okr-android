package com.visokr.android.pages

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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.visokr.android.core.RecycleVM
import com.visokr.android.core.Routes
import com.visokr.android.core.UiState
import com.visokr.android.ui.EmptyState
import com.visokr.android.ui.ErrorView
import com.visokr.android.ui.LoadingView
import com.visokr.android.ui.LocalVisTokens
import com.visokr.android.ui.VisCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecyclePage(navController: NavController) {
    val vm: RecycleVM = viewModel()
    val items by vm.items.collectAsState()
    val t = LocalVisTokens.current
    var confirmEmpty by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            TopAppBar(
                title = { Text("回收站", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { confirmEmpty = true }) { Icon(Icons.Outlined.DeleteForever, "清空") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        when (val s = items) {
            is UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Error -> ErrorView(s.message, onRetry = { vm.refresh() }, modifier = Modifier.padding(padding))
            is UiState.Success -> {
                if (s.data.isEmpty()) EmptyState("回收站是空的", Modifier.padding(padding), icon = Icons.Outlined.DeleteOutline)
                else LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.data) { item ->
                        VisCard(modifier = Modifier.fillMaxWidth(), onClick = { vm.restore(item) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Restore, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text("${entityTypeLabel(item.entityType)} · ${item.deletedAt.take(10)}${item.meta?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
                                }
                                IconButton(onClick = { vm.destroy(item) }) {
                                    Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("清空回收站") },
            text = { Text("确定清空回收站？此操作不可恢复。") },
            confirmButton = { TextButton(onClick = { vm.empty(); confirmEmpty = false }) { Text("清空", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("取消") } },
        )
    }
}

fun entityTypeLabel(t: String): String = when (t) {
    "objective" -> "目标"
    "key_result" -> "关键结果"
    "task" -> "任务"
    else -> t
}