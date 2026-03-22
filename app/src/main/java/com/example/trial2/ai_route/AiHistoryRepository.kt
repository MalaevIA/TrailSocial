package com.trail2.ai_route

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val AI_HISTORY_KEY = stringPreferencesKey("ai_route_history")
private const val MAX_HISTORY = 5

@Singleton
class AiHistoryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    val history: Flow<List<GeneratedRoute>> = dataStore.data.map { prefs ->
        val raw = prefs[AI_HISTORY_KEY] ?: return@map emptyList()
        runCatching {
            json.decodeFromString(ListSerializer(GeneratedRoute.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    suspend fun save(route: GeneratedRoute) {
        dataStore.edit { prefs ->
            val current = runCatching {
                val raw = prefs[AI_HISTORY_KEY] ?: "[]"
                json.decodeFromString(ListSerializer(GeneratedRoute.serializer()), raw)
            }.getOrDefault(emptyList())

            val updated = (listOf(route) + current).take(MAX_HISTORY)
            prefs[AI_HISTORY_KEY] = json.encodeToString(ListSerializer(GeneratedRoute.serializer()), updated)
        }
    }
}
