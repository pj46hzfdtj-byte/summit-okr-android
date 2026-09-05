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
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.visokr.android.core.NotifVM
import com.visokr.android.core.Routes
import com.visokr.android.core.UiState
import com.visokr.android.ui.EmptyState
import com.visokr.android.ui.ErrorView
import com.visokr.android.ui.LoadingView
import com.visokr.android.ui.LocalVisTokens
import com.visokr.android.ui.PillTag
import com.visokr.android.ui.VisCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsPage(navController: NavController) {
    val vm: NotifVM = viewModel()
    val data by vm.data.collectAsState()
    val t = LocalVisTokens.current

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            TopAppBar(
                title = { Text("通知", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { vm.markAllRead() }) { Icon(Icons.Outlined.MarkEmailRead, "全部已读") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        when (val s = data) {
            is UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Error -> ErrorView(s.message, onRetry = { vm.refresh() }, modifier = Modifier.padding(padding))
            is UiState.Success -> {
                if (s.data.list.isEmpty()) EmptyState("暂无通知", Modifier.padding(padding))
                else LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.data.list) { n ->
                        VisCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(notifTitle(n.type), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    n.body?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = t.textTertiary, maxLines = 2) }
                                }
                                if (!n.read) PillTag("未读", MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun notifTitle(type: String): String = when (type) {
    "stale_kr" -> "KR 长期未更新"
    "cycle_ending" -> "专注周期即将结束"
    "review_pending" -> "目标待复盘"
    "task_overdue" -> "任务已过期"
    "checkin_reminder" -> "别忘了每周 Check-in"
    else -> "通知"
}