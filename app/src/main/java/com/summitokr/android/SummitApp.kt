package com.summitokr.android

import android.app.Application
import com.summitokr.android.core.NetClient
import com.summitokr.android.core.Prefs
import com.summitokr.android.core.TokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** 应用入口：保证桌面组件（Widget 进程内回调）在未打开 Activity 时也能拿到令牌与服务器地址 */
class SummitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenStore.init(applicationContext)
        runCatching {
            runBlocking {
                val baseUrl = Prefs.flow(applicationContext).first().baseUrl
                if (baseUrl.isNotBlank()) NetClient.setBaseUrl(baseUrl)
            }
        }
    }
}
