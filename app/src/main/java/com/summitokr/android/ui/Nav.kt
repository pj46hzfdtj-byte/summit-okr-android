package com.summitokr.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.summitokr.android.core.Routes
import com.summitokr.android.pages.AiPage
import com.summitokr.android.pages.FocusPage
import com.summitokr.android.pages.GanttPage
import com.summitokr.android.pages.GoalDetailPage
import com.summitokr.android.pages.GoalsPage
import com.summitokr.android.pages.HelpPage
import com.summitokr.android.pages.LoginPage
import com.summitokr.android.pages.MePage
import com.summitokr.android.pages.NotificationsPage
import com.summitokr.android.pages.RecyclePage
import com.summitokr.android.pages.ReviewsPage
import com.summitokr.android.pages.SummaryPage
import com.summitokr.android.pages.TasksPage
import com.summitokr.android.pages.VisionPage



private data class TabSpec(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

@Composable
fun RootNav(pendingRoute: String? = null, onRouteConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    var loggedIn by remember { mutableStateOf(com.summitokr.android.core.TokenStore.accessToken != null) }
    val t = LocalSummitTokens.current

    // 桌面组件点击直达路由（登录后生效）
    androidx.compose.runtime.LaunchedEffect(pendingRoute, loggedIn) {
        if (pendingRoute != null && loggedIn) {
            runCatching { navController.navigate(pendingRoute) }
            onRouteConsumed()
        }
    }

    // 对齐 Flutter main.dart 的 AppBackground：macOS 主题下全局挂 Aurora 渐变+光斑背景，
    // 各页 Scaffold 已设 Transparent 容器色，自然透出（登录页自带，不重复包）。
    Box(Modifier.fillMaxSize().then(if (t.macos) Modifier.background(auroraBrush(isDarkTheme())) else Modifier.background(t.bg))) {
        if (t.macos) AuroraOverlay()
        NavHost(
            navController = navController,
            startDestination = if (loggedIn) Routes.SUMMARY else Routes.LOGIN,
        ) {
        composable(Routes.LOGIN) {
            LoginPage(
                onLoggedIn = {
                    loggedIn = true
                    navController.navigate(Routes.SUMMARY) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SUMMARY) {
            ShellScaffold(selected = 0, navController = navController, onLoggedOut = {
                loggedIn = false
                navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
            }) { SummaryPage(navController) }
        }
        composable(Routes.GOALS) {
            ShellScaffold(selected = 1, navController = navController, onLoggedOut = {}) { GoalsPage(navController) }
        }
        composable(Routes.TASKS) {
            ShellScaffold(selected = 2, navController = navController, onLoggedOut = {}) { TasksPage(navController) }
        }
        composable(Routes.ME) {
            ShellScaffold(selected = 3, navController = navController, onLoggedOut = {
                loggedIn = false
                navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
            }) { MePage(navController) }
        }
        composable(
            Routes.GOAL_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            GoalDetailPage(objectiveId = entry.arguments?.getString("id") ?: "", navController = navController)
        }
        composable(Routes.FOCUS) { FocusPage(navController) }
        composable(Routes.REVIEWS) { ReviewsPage(navController) }
        composable(Routes.VISIONS) { VisionPage(navController) }
        composable(Routes.GANTT) { GanttPage(navController) }
        composable(Routes.AI) { AiPage(navController) }
        composable(Routes.RECYCLE) { RecyclePage(navController) }
        composable(Routes.HELP) { HelpPage(navController) }
        composable(Routes.NOTIFICATIONS) { NotificationsPage(navController) }
        }
        // 应用内可折叠悬浮速览窗（对齐桌面端悬浮窗）：登录态叠加在最上层
        if (loggedIn) {
            GlanceFloatingOverlay(navController)
        }
    }
}

@Composable
private fun ShellScaffold(
    selected: Int,
    navController: androidx.navigation.NavHostController,
    onLoggedOut: () -> Unit,
    content: @Composable Modifier.() -> Unit,
) {
    val tabs = listOf(
        TabSpec(Routes.SUMMARY, "摘要", Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
        TabSpec(Routes.GOALS, "目标库", Icons.Outlined.AccountTree, Icons.Filled.AccountTree),
        TabSpec(Routes.TASKS, "任务", Icons.Outlined.EventNote, Icons.Filled.EventNote),
        TabSpec(Routes.ME, "我的", Icons.Outlined.Person, Icons.Filled.Person),
    )
    val t = LocalSummitTokens.current
    // 对齐 Flutter ShellPage：macOS 下导航栏顶部圆角 22 + 半透明玻璃底（.82 alpha）
    val navBar: @Composable () -> Unit = {
        NavigationBar(
            containerColor = if (t.macos) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp(),
        ) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = {
                            if (selected != i) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(if (selected == i) tab.selectedIcon else tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
    }
    Scaffold(
        containerColor = if (t.macos) androidx.compose.ui.graphics.Color.Transparent else t.bg,
        // 外层不再消费状态栏 inset：内页 TopAppBar 自行加 statusBarsPadding，避免双层叠加导致头部过高
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (t.macos) {
                Box(
                    Modifier.fillMaxWidth().background(
                        t.card.copy(alpha = 0.82f),
                        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    ),
                ) { navBar() }
            } else navBar()
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content(Modifier) }
    }
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())