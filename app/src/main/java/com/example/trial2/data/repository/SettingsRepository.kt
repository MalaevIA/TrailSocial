package com.trail2.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

object SettingsKeys {
    val DARK_THEME = booleanPreferencesKey("settings_dark_theme")
    val LANGUAGE = stringPreferencesKey("settings_language") // "ru" or "en"
}

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val isDarkTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.DARK_THEME] ?: false
    }

    val language: Flow<String> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.LANGUAGE] ?: "ru"
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.DARK_THEME] = enabled
        }
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.LANGUAGE] = language
        }
    }
}
