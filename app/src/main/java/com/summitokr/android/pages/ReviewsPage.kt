package com.summitokr.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
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
import com.summitokr.android.core.CreateReviewReq
import com.summitokr.android.core.GoalGroup
import com.summitokr.android.core.GoalsVM
import com.summitokr.android.core.KrScore
import com.summitokr.android.core.NetClient
import com.summitokr.android.core.Objective
import com.summitokr.android.core.Review
import com.summitokr.android.core.ReviewsVM
import com.summitokr.android.core.Routes
import com.summitokr.android.core.UiState
import com.summitokr.android.core.unwrap
import com.summitokr.android.ui.EmptyState
import com.summitokr.android.ui.ErrorView
import com.summitokr.android.ui.LoadingView
import com.summitokr.android.ui.LocalSummitTokens
import com.summitokr.android.ui.PillTag
import com.summitokr.android.ui.SummitCard
import com.summitokr.android.ui.SummitTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsPage(navController: NavController) {
    val vm: ReviewsVM = viewModel()
    val reviews by vm.reviews.collectAsState()
    val t = LocalSummitTokens.current
    var picking by remember { mutableStateOf(false) }
    var formObjective by remember { mutableStateOf<Objective?>(null) }

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            SummitTopBar(
                title = { Text("复盘", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { picking = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) {
                Icon(Icons.Outlined.Add, null)
            }
        },
    ) { padding ->
      PullToRefreshBox(onRefresh = { vm.refresh() }, isRefreshing = false, modifier = Modifier.padding(padding)) {
        when (val s = reviews) {
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(s.message, onRetry = { vm.refresh() })
            is UiState.Success -> {
                if (s.data.isEmpty()) EmptyState("暂无复盘", icon = Icons.Outlined.RateReview)
                else LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(s.data) { r -> ReviewCard(r) }
                    item { Spacer(Modifier.height(60.dp)) }
                }
            }
        }
      }
    }

    if (picking) {
        ObjectivePickerDialog(
            onDismiss = { picking = false },
            onPick = { obj -> picking = false; formObjective = obj },
        )
    }
    formObjective?.let { target ->
        ReviewFormDialog(target, onDismiss = { formObjective = null }, onSave = { req ->
            vm.create(req)
            formObjective = null
        })
    }
}

@Composable
private fun ReviewCard(r: Review) {
    val t = LocalSummitTokens.current
    SummitCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PillTag(if (r.type == "final") "期末" else "期中", if (r.type == "final") MaterialTheme.colorScheme.primary else t.success)
                Spacer(Modifier.size(8.dp))
                Text("v${r.version}", fontSize = 12.sp, color = t.textTertiary)
                Spacer(Modifier.weight(1f))
                if (r.createdAt.isNotBlank()) Text(fmtDate(r.createdAt), fontSize = 12.sp, color = t.textTertiary)
            }
            Spacer(Modifier.height(8.dp))
            ReviewHero(r.selfRating, r.objectiveScore)
            if (!r.thoughts.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(r.thoughts!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
            }
        }
    }
}

/** VisOKR 风格：自评 emoji 英雄区（emoji + 大数字 + 自评·label + 目标得分），背景按阈值着色 */
@Composable
private fun ReviewHero(selfRating: Double, objectiveScore: Double?) {
    val t = LocalSummitTokens.current
    val v = Math.round(selfRating * 100).toInt()
    val (emoji, label, color) = when {
        v >= 90 -> Triple("🤩", "太棒了", t.success)
        v >= 70 -> Triple("😊", "很满意", t.success)
        v >= 60 -> Triple("🙂", "还不错", t.warning)
        v >= 40 -> Triple("😕", "不太满意", t.warning)
        else -> Triple("😣", "很不理想", t.danger)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(t.radiusControl))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 30.sp)
        Spacer(Modifier.size(12.dp))
        Text("$v", style = MaterialTheme.typography.displaySmall, color = color, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("自评 · $label", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            objectiveScore?.let {
                Text("目标得分 ${String.format("%.1f", it)}", style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
            }
        }
    }
}

@Composable
private fun ObjectivePickerDialog(onDismiss: () -> Unit, onPick: (Objective) -> Unit) {
    val goalsVM: GoalsVM = viewModel()
    val tree by goalsVM.tree.collectAsState()
    val objectives = remember(tree) {
        val list = ArrayList<Objective>()
        fun walk(groups: List<GoalGroup>) { groups.forEach { list.addAll(it.objectives); walk(it.children) } }
        (tree as? UiState.Success)?.data?.let { walk(it) }
        list
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择目标", fontWeight = FontWeight.Bold) },
        text = {
            if (objectives.isEmpty()) Text("暂无目标", color = LocalSummitTokens.current.textTertiary)
            else LazyColumn(Modifier.height(300.dp)) {
                items(objectives) { o ->
                    Column(Modifier.fillMaxWidth().clickable { onPick(o) }.padding(vertical = 10.dp)) {
                        Text(o.title, style = MaterialTheme.typography.bodyMedium)
                        Text(objectiveStatusLabel(o.status), style = MaterialTheme.typography.bodySmall, color = LocalSummitTokens.current.textTertiary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewFormDialog(obj: Objective, onDismiss: () -> Unit, onSave: (CreateReviewReq) -> Unit) {
    var selfRating by remember { mutableStateOf(70f) }
    var type by remember { mutableStateOf("midterm") }
    var thoughts by remember { mutableStateOf("") }
    var krScores by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var krs by remember { mutableStateOf<List<com.summitokr.android.core.KeyResult>?>(null) }
    androidx.compose.runtime.LaunchedEffect(obj.id) {
        runCatching { krs = NetClient.api.keyResults(obj.id).unwrap() }
    }
    // 对齐 Flutter：复盘创建用底部弹层
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("新建复盘：${obj.title.take(14)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("midterm" to "期中", "final" to "期末").forEach { (v, l) ->
                    TextButton(onClick = { type = v }) {
                        Text(l, color = if (type == v) MaterialTheme.colorScheme.primary else LocalSummitTokens.current.textTertiary, fontWeight = if (type == v) FontWeight.Bold else null)
                    }
                }
            }
            Text("自评（70 分为健康线）", style = MaterialTheme.typography.bodySmall)
            Slider(value = selfRating, onValueChange = { selfRating = it }, valueRange = 0f..100f)
            Text("${selfRating.toInt()} 分", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            krs?.forEach { kr ->
                val cur = (krScores[kr.id] ?: 70.0).toFloat()
                Column {
                    Text("${kr.emoji} ${kr.title}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    Slider(value = cur, onValueChange = { krScores = krScores + (kr.id to it.toDouble()) })
                    Text("${cur.toInt()} 分", fontSize = 11.sp, color = LocalSummitTokens.current.textTertiary)
                }
            }
            OutlinedTextField(value = thoughts, onValueChange = { thoughts = it }, label = { Text("思考（可选）") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(Modifier.size(8.dp))
                TextButton(onClick = {
                    val scores = (krs ?: emptyList()).map { KrScore(it.id, krScores[it.id] ?: 70.0) }
                    onSave(CreateReviewReq(obj.id, type, scores, selfRating.toDouble(), thoughts = thoughts.ifBlank { null }))
                }) { Text("提交") }
            }
        }
    }
}