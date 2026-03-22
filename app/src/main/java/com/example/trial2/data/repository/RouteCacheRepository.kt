package com.trail2.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.trail2.data.TrailRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val RECOMMENDED_CACHE_KEY = stringPreferencesKey("route_cache_recommended")
private val FEED_CACHE_KEY = stringPreferencesKey("route_cache_feed")
private const val MAX_CACHED = 20

@Singleton
class RouteCacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json
) {
    suspend fun saveRecommended(routes: List<TrailRoute>) = save(RECOMMENDED_CACHE_KEY, routes)
    suspend fun saveFeed(routes: List<TrailRoute>) = save(FEED_CACHE_KEY, routes)

    suspend fun loadRecommended(): List<TrailRoute> = load(RECOMMENDED_CACHE_KEY)
    suspend fun loadFeed(): List<TrailRoute> = load(FEED_CACHE_KEY)

    private suspend fun save(key: Preferences.Key<String>, routes: List<TrailRoute>) {
        withContext(Dispatchers.IO) {
            runCatching {
                val encoded = json.encodeToString(ListSerializer(TrailRoute.serializer()), routes.take(MAX_CACHED))
                dataStore.edit { it[key] = encoded }
            }
        }
    }

    private suspend fun load(key: Preferences.Key<String>): List<TrailRoute> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val raw = dataStore.data.first()[key] ?: return@withContext emptyList()
                json.decodeFromString(ListSerializer(TrailRoute.serializer()), raw)
            }.getOrDefault(emptyList())
        }
    }
}
