package com.visokr.android.core

import android.content.Context
import com.visokr.android.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** 令牌存储：同步读（Authenticator 在 OkHttp 线程回调），SharedPreferences 持久化 */
object TokenStore {
    private const val PREF = "visokr_tokens"
    private lateinit var sp: android.content.SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    }

    var accessToken: String?
        get() = if (::sp.isInitialized) sp.getString("accessToken", null) else null
        set(v) { if (::sp.isInitialized) sp.edit().putString("accessToken", v).apply() }
    var refreshToken: String?
        get() = if (::sp.isInitialized) sp.getString("refreshToken", null) else null
        set(v) { if (::sp.isInitialized) sp.edit().putString("refreshToken", v).apply() }

    fun setTokens(access: String, refresh: String) {
        if (::sp.isInitialized) sp.edit().putString("accessToken", access).putString("refreshToken", refresh).apply()
    }

    fun clear() { if (::sp.isInitialized) sp.edit().clear().apply() }
}

class ApiException(val status: Int?, message: String) : Exception(message)

object NetClient {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Volatile
    var baseUrl: String = normalize(BuildConfig.API_BASE_URL)
        private set

    private fun normalize(input: String): String {
        var u = input.trim()
        if (u.endsWith("/")) u = u.dropLast(1)
        if (!u.endsWith("/api")) u = "$u/api"
        return "$u/"
    }

    private val refreshing = AtomicBoolean(false)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Content-Type", "application/json")
                TokenStore.accessToken?.let { req.header("Authorization", "Bearer $it") }
                chain.proceed(req.build())
            }
            .authenticator(Authenticator { _, response ->
                if (response.code != 401) return@Authenticator null
                val path = response.request.url.encodedPath
                if (path.contains("/auth/")) return@Authenticator null
                val rt = TokenStore.refreshToken ?: return@Authenticator null
                if (!refreshing.compareAndSet(false, true)) return@Authenticator null
                try {
                    val resp = api.refresh(RefreshReq(rt)).execute()
                    val body = resp.body()
                    if (resp.isSuccessful && body?.code == 0 && body.data != null) {
                        TokenStore.setTokens(body.data.accessToken, body.data.refreshToken)
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${body.data.accessToken}")
                            .build()
                    } else {
                        TokenStore.clear()
                        null
                    }
                } catch (_: Exception) {
                    null
                } finally {
                    refreshing.set(false)
                }
            })
            .build()
    }

    @Volatile
    private var cached: Pair<String, Api>? = null

    val api: Api
        get() {
            cached?.let { if (it.first == baseUrl) return it.second }
            synchronized(this) {
                cached?.let { if (it.first == baseUrl) return it.second }
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()
                val created = retrofit.create(Api::class.java)
                cached = baseUrl to created
                return created
            }
        }

    /** 真机切换服务器地址后重建 Retrofit */
    fun setBaseUrl(url: String) {
        val u = url.trim()
        if (u.isEmpty()) return
        baseUrl = normalize(u)
        cached = null
    }
}

/** 统一解包：code==0 返回 data，否则抛 ApiException */
suspend fun <T> ApiResp<T>.unwrap(): T {
    if (code != 0) throw ApiException(code, message ?: "请求失败")
    return data ?: throw ApiException(0, "空响应")
}

fun <T> ApiResp<T>.unwrapOrNull(): T? = if (code == 0) data else null