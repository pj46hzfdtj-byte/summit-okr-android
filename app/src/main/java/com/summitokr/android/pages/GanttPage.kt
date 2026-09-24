package com.summitokr.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.summitokr.android.core.GanttVM
import com.summitokr.android.core.UiState
import com.summitokr.android.core.normProgress
import com.summitokr.android.ui.EmptyState
import com.summitokr.android.ui.ErrorView
import com.summitokr.android.ui.LoadingView
import com.summitokr.android.ui.LocalSummitTokens
import com.summitokr.android.ui.SummitTopBar
import com.summitokr.android.ui.parseHexColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private const val PX_PER_DAY = 12f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GanttPage(navController: NavController) {
    val vm: GanttVM = viewModel()
    val data by vm.data.collectAsState()
    val t = LocalSummitTokens.current

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            SummitTopBar(
                title = { Text("甘特图", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        when (val s = data) {
            is UiState.Loading -> LoadingView(Modifier.padding(padding))
            is UiState.Error -> ErrorView(s.message, Modifier.padding(padding)) { vm.refresh() }
            is UiState.Success -> {
                val d = s.data
                if (d.items.isEmpty()) EmptyState("暂无排期", Modifier.padding(padding))
                else {
                    var filter by remember { mutableStateOf("all") }
                    val isActive = { it: com.summitokr.android.core.GanttItem -> it.status == "in_progress" || it.status == "pending_review" }
                    val counts = mapOf(
                        "all" to d.items.size,
                        "active" to d.items.count(isActive),
                        "lagging" to d.items.count { it.isLagging },
                        "completed" to d.items.count { it.status == "completed" },
                    )
                    val filtered = when (filter) {
                        "active" -> d.items.filter(isActive)
                        "lagging" -> d.items.filter { it.isLagging }
                        "completed" -> d.items.filter { it.status == "completed" }
                        else -> d.items
                    }
                    val start = parseLocalDate(d.rangeStart) ?: LocalDate.now()
                    val end = parseLocalDate(d.rangeEnd) ?: start.plusDays(90)
                    val totalDays = ChronoUnit.DAYS.between(start, end).coerceAtLeast(1).toInt()
                    val chartWidth = PX_PER_DAY.dp * totalDays
                    val today = parseLocalDate(d.todayLine) ?: LocalDate.now()
                    val todayX = ChronoUnit.DAYS.between(start, today).coerceIn(0, totalDays.toLong()) * PX_PER_DAY

                    LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("all" to "全部", "active" to "进行中", "lagging" to "滞后", "completed" to "已完成").forEach { (key, label) ->
                                    val sel = filter == key
                                    Box(
                                        Modifier.clip(CircleShape)
                                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .clickable { filter = key }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            "$label ${counts[key] ?: 0}",
                                            fontSize = 12.sp,
                                            fontWeight = if (sel) FontWeight.SemiBold else null,
                                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        items(filtered) { item ->
                            val color = parseHexColor(item.color)
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    item.title,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    color = if (item.isLagging) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                Box(Modifier.fillMaxWidth().height(22.dp).horizontalScroll(rememberScrollState())) {
                                    Box(Modifier.width(chartWidth).height(22.dp)) {
                                        val barStart = ChronoUnit.DAYS.between(start, parseLocalDate(item.startAt) ?: start).coerceIn(0, totalDays.toLong()) * PX_PER_DAY
                                        val barEnd = ChronoUnit.DAYS.between(start, parseLocalDate(item.endAt) ?: (parseLocalDate(item.startAt) ?: start).plusDays(7)).coerceIn(0, totalDays.toLong()) * PX_PER_DAY
                                        val barWidth = (barEnd - barStart).coerceAtLeast(8f)
                                        // 轨道
                                        Box(
                                            Modifier
                                                .offset(x = barStart.dp)
                                                .width(barWidth.dp)
                                                .height(12.dp)
                                                .align(Alignment.CenterStart)
                                                .clip(CircleShape)
                                                .background(color.copy(alpha = 0.25f)),
                                        ) {
                                            Box(
                                                Modifier.fillMaxWidth(normProgress(item.currentProgress).toFloat().coerceIn(0.02f, 1f)).fillMaxHeight().background(color),
                                            )
                                        }
                                        // 今日线
                                        Box(
                                            Modifier
                                                .offset(x = todayX.dp)
                                                .width(2.dp)
                                                .height(22.dp)
                                                .align(Alignment.CenterStart)
                                                .background(MaterialTheme.colorScheme.error),
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Text("今日 ${d.todayLine.take(10)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseLocalDate(iso: String?): LocalDate? = try {
    if (iso.isNullOrBlank()) null else Instant.parse(if (iso.endsWith("Z")) iso else iso + "Z").atZone(ZoneId.systemDefault()).toLocalDate()
} catch (_: Exception) {
    try { LocalDate.parse(iso?.take(10) ?: return null) } catch (_: Exception) { null }
}
