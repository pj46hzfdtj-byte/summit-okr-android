package com.visokr.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.visokr.android.core.NetClient
import com.visokr.android.core.Prefs
import com.visokr.android.core.TokenStore
import com.visokr.android.ui.RootNav
import com.visokr.android.ui.VisTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenStore.init(applicationContext)
        enableEdgeToEdge()
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
            VisTheme(seed = prefs.value.themeSeed, dark = dark) {
                RootNav()
            }
        }
    }
}