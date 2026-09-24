package com.summitokr.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.summitokr.android.core.NetClient
import com.summitokr.android.core.Prefs
import com.summitokr.android.core.TokenStore
import com.summitokr.android.ui.RootNav
import com.summitokr.android.ui.SummitTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        /** 桌面组件点击直达的路由（如 summary / tasks / focus） */
        const val EXTRA_ROUTE = "extra_route"
    }

    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenStore.init(applicationContext)
        enableEdgeToEdge()
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        // 恢复自定义服务器地址（真机）
        lifecycleScope.launch {
            Prefs.flow(applicationContext).collect { ui ->
                if (ui.baseUrl.isNotBlank() && ui.baseUrl != NetClient.baseUrl) {
                    NetClient.setBaseUrl(ui.baseUrl)
                }
            }
        }
        setContent {
            val prefs = Prefs.flow(applicationContext).collectAsState(initial = Prefs.Ui())
            val dark = when (prefs.value.appearance) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            SummitTheme(seed = prefs.value.themeSeed, dark = dark) {
                RootNav(pendingRoute = pendingRoute, onRouteConsumed = { pendingRoute = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_ROUTE)?.let { pendingRoute = it }
    }
}
