package com.visokr.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.visokr.android.core.FeedbackReq
import com.visokr.android.core.NetClient
import com.visokr.android.core.unwrapOrNull
import com.visokr.android.ui.LocalVisTokens
import com.visokr.android.ui.VisCard
import com.visokr.android.ui.VisTopBar
import kotlinx.coroutines.launch

private val faqs = listOf(
    "如何创建目标？" to "在「目标库」页右下角点击 AI 助手生成目标并应用；也可在 Web 端完善层级后同步查看。",
    "什么是 70 分健康线？" to "VIS 理念：完成 70% 即健康，不苛求满分。复盘评分低于 70 会给出改进建议。",
    "专注周期如何结束？" to "「我的 → 专注周期」中点击活跃周期的「结束周期」，期末复盘后目标自动完成。",
    "如何切换主题？" to "「我的 → 外观」可切换日间/夜间/跟随系统，以及 5 套配色主题（含 macOS 玻璃质感）。",
    "真机如何连接后端？" to "「我的 → 数据 → 服务器地址」填入局域网 IP（如 http://192.168.x.x:3001/api）。",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpPage(navController: NavController) {
    val t = LocalVisTokens.current
    var type by remember { mutableStateOf("bug") }
    var content by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            VisTopBar(
                title = { Text("帮助与反馈", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Outlined.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("常见问题", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            faqs.forEach { (q, a) ->
                var expanded by remember { mutableStateOf(false) }
                VisCard(modifier = Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(q, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = t.textTertiary)
                    }
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("提交反馈", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("bug" to "问题", "feature" to "建议", "other" to "其他").forEach { (v, l) ->
                    val sel = type == v
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { type = v }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            l,
                            fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it; sent = false },
                placeholder = { Text("描述你遇到的问题或建议…") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            Button(
                onClick = {
                    val text = content.trim()
                    if (text.isEmpty()) return@Button
                    scope.launch {
                        runCatching { NetClient.api.feedback(FeedbackReq(type, text)).unwrapOrNull() }
                        sent = true
                        content = ""
                    }
                },
                enabled = content.isNotBlank(),
            ) { Text(if (sent) "已提交，感谢反馈！" else "提交") }
            Spacer(Modifier.height(24.dp))
        }
    }
}