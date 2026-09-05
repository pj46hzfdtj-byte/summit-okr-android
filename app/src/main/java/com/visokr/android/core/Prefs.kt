package com.visokr.android.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "visokr_prefs")

object Prefs {
    val KEY_APPEARANCE = stringPreferencesKey("appearance") // light | dark | system
    val KEY_THEME_SEED = stringPreferencesKey("themeSeed") // light | blue | green | purple | macos
    val KEY_BASE_URL = stringPreferencesKey("baseUrl")

    data class Ui(
        val appearance: String = "system",
        val themeSeed: String = "light",
        val baseUrl: String = "",
    )

    fun flow(context: Context): Flow<Ui> = context.dataStore.data.map { p ->
        Ui(
            appearance = p[KEY_APPEARANCE] ?: "system",
            themeSeed = p[KEY_THEME_SEED] ?: "light",
            baseUrl = p[KEY_BASE_URL] ?: "",
        )
    }

    suspend fun setAppearance(context: Context, v: String) = context.dataStore.edit { it[KEY_APPEARANCE] = v }
    suspend fun setThemeSeed(context: Context, v: String) = context.dataStore.edit { it[KEY_THEME_SEED] = v }
    suspend fun setBaseUrl(context: Context, v: String) = context.dataStore.edit { it[KEY_BASE_URL] = v }
}