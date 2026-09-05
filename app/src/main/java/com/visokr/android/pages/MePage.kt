package com.visokr.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.visokr.android.core.AuthVM
import com.visokr.android.core.NetClient
import com.visokr.android.core.Prefs
import com.visokr.android.core.Routes
import com.visokr.android.ui.LocalVisTokens
import com.visokr.android.ui.VisCard
import com.visokr.android.ui.VisSeeds
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MePage(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = Prefs.flow(context).collectAsState(initial = Prefs.Ui())
    val authVM: AuthVM = viewModel()
    val t = LocalVisTokens.current
    var showServer by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = if (t.macos) Color.Transparent else t.bg,
        topBar = {
            TopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (t.macos) Color.Transparent else MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                VisCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("V", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("VIS OKR 用户", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("demo@visokr.com", style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
                        }
                        TextButton(onClick = {
                            authVM.logout()
                            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                        }) {
                            Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("退出")
                        }
                    }
                }
            }
            item {
                SectionCard("外观") {
                    Text("显示模式", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppearanceChip("日间", "light", Icons.Outlined.WbSunny, prefs.value.appearance) {
                            scope.launch { Prefs.setAppearance(context, it) }
                        }
                        AppearanceChip("夜间", "dark", Icons.Outlined.Nightlight, prefs.value.appearance) {
                            scope.launch { Prefs.setAppearance(context, it) }
                        }
                        AppearanceChip("跟随系统", "system", Icons.Outlined.SettingsBrightness, prefs.value.appearance) {
                            scope.launch { Prefs.setAppearance(context, it) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("配色主题", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        VisSeeds.all.forEach { seed ->
                            val selected = prefs.value.themeSeed == seed
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                scope.launch { Prefs.setThemeSeed(context, seed) }
                            }) {
                                Box(
                                    Modifier.size(34.dp).clip(CircleShape).background(VisSeeds.preview[seed] ?: Color.Gray),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(VisSeeds.labels[seed] ?: "", fontSize = 10.sp, color = t.textTertiary)
                            }
                        }
                    }
                }
            }
            item {
                SectionCard("功能入口") {
                    NavTile(Icons.Outlined.Timer, "专注周期") { navController.navigate(Routes.FOCUS) }
                    NavTile(Icons.Outlined.RateReview, "复盘") { navController.navigate(Routes.REVIEWS) }
                    NavTile(Icons.Outlined.Visibility, "愿景") { navController.navigate(Routes.VISIONS) }
                    NavTile(Icons.Outlined.Timeline, "甘特图") { navController.navigate(Routes.GANTT) }
                    NavTile(Icons.Outlined.AutoAwesome, "AI 助手") { navController.navigate(Routes.AI) }
                    NavTile(Icons.Outlined.MarkEmailUnread, "通知") { navController.navigate(Routes.NOTIFICATIONS) }
                    NavTile(Icons.Outlined.DeleteOutline, "回收站") { navController.navigate(Routes.RECYCLE) }
                    NavTile(Icons.Outlined.HelpOutline, "帮助与反馈") { navController.navigate(Routes.HELP) }
                }
            }
            item {
                SectionCard("数据") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showServer = true }) {
                        Icon(Icons.Outlined.PersonOutline, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("服务器地址", style = MaterialTheme.typography.bodyMedium)
                            Text(NetClient.baseUrl, style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
                        }
                        Icon(Icons.Outlined.ChevronRight, null, tint = t.textTertiary)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showServer) {
        var url by remember { mutableStateOf(NetClient.baseUrl) }
        AlertDialog(
            onDismissRequest = { showServer = false },
            title = { Text("服务器地址") },
            text = {
                Column {
                    OutlinedTextField(value = url, onValueChange = { url = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("模拟器默认 http://10.0.2.2:3001/api；真机请填局域网 IP", style = MaterialTheme.typography.bodySmall, color = t.textTertiary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    NetClient.setBaseUrl(url)
                    scope.launch { Prefs.setBaseUrl(context, NetClient.baseUrl) }
                    showServer = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showServer = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    VisCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun AppearanceChip(label: String, value: String, icon: ImageVector, current: String, onSelect: (String) -> Unit) {
    val selected = current == value
    Box(
        Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else LocalVisTokens.current.bg)
            .clickable { onSelect(value) }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(15.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(4.dp))
            Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun NavTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    val t = LocalVisTokens.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, null, tint = t.textTertiary, modifier = Modifier.size(18.dp))
    }
}