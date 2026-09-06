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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.summitokr.android.core.GoalsVM
import com.summitokr.android.core.GoalGroup
import com.summitokr.android.core.Objective
import com.summitokr.android.core.Routes
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
import com.summitokr.android.ui.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsPage(navController: NavController) {
    val vm: GoalsVM = viewModel()
    val tree by vm.tree.collectAsState()
    val t = LocalSummitTokens.current

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            SummitTopBar(
                title = { Text("目标库", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.GANTT) }) { Icon(Icons.Outlined.Timeline, "甘特图") }
                    IconButton(onClick = { navController.navigate(Routes.VISIONS) }) { Icon(Icons.Outlined.Visibility, "愿景") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Routes.AI) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Outlined.AutoAwesome, null) },
                text = { Text("AI 助手") },
            )
        },
    ) { padding ->
        PullToRefreshBox(onRefresh = { vm.refresh() }, isRefreshing = false, modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = tree) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(s.message, onRetry = { vm.refresh() })
                is UiState.Success -> {
                    if (s.data.isEmpty()) EmptyState("暂无目标")
                    else LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        groupNodes(s.data, 0, navController)
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.groupNodes(groups: List<GoalGroup>, depth: Int, navController: NavController) {
    groups.forEach { group ->
        val color = parseHexColor(group.color)
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = (12 + depth * 12).dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Folder, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.weight(1f),
                )
                group.vision?.let { v -> PillTag("愿景·${v.content.take(8)}", color) }
            }
        }
        group.objectives.forEach { obj -> objectiveTile(obj, depth, navController) }
        if (group.children.isNotEmpty()) groupNodes(group.children, depth + 1, navController)
    }
}

private fun LazyListScope.objectiveTile(obj: Objective, depth: Int, navController: NavController) {
    val color = parseHexColor(obj.color)
    item {
        val krCount = obj.keyResultCount ?: obj.keyResults?.size ?: 0
        val statusLabel = objectiveStatusLabel(obj.status)
        SummitCard(
            modifier = Modifier.fillMaxWidth().padding(start = (8 + depth * 12).dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            onClick = { navController.navigate(Routes.goalDetail(obj.id)) },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Text("$krCount", fontSize = 11.sp, color = color) }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(obj.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    CapsuleProgress(normProgress(obj.currentProgress).toFloat(), color = color, height = 5.dp)
                }
                Spacer(Modifier.size(8.dp))
                if (obj.isLagging == true) {
                    Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                }
                Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = LocalSummitTokens.current.textTertiary)
            }
        }
    }
}

fun objectiveStatusLabel(s: String): String = when (s) {
    "unplanned" -> "未计划"
    "not_started" -> "未开始"
    "in_progress" -> "进行中"
    "pending_review" -> "待复盘"
    "completed" -> "已复盘"
    else -> s
}