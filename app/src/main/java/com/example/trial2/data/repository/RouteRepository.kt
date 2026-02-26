package com.trail2.data.repository

import com.trail2.data.Difficulty
import com.trail2.data.TrailRoute
import com.trail2.data.local.dao.RouteDao
import com.trail2.data.local.dao.UserDao
import com.trail2.data.local.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val routeDao: RouteDao,
    private val userDao: UserDao
) {
    fun getAllRoutes(): Flow<List<TrailRoute>> =
        combine(routeDao.getAll(), userDao.getAll()) { routes, users ->
            val usersById = users.associateBy { it.id }
            routes.mapNotNull { route ->
                val author = usersById[route.authorId] ?: return@mapNotNull null
                route.toDomain(author.toDomain())
            }
        }

    fun getRouteById(id: String): Flow<TrailRoute?> =
        routeDao.getById(id).map { routeEntity ->
            routeEntity ?: return@map null
            // Подгружаем автора — используем combine для реактивности,
            // но здесь допустимо собрать Flow вручную
            null // TODO: wire with user DAO in full reactive chain
        }

    fun getRoutesByRegion(region: String): Flow<List<TrailRoute>> =
        combine(routeDao.getByRegion(region), userDao.getAll()) { routes, users ->
            val usersById = users.associateBy { it.id }
            routes.mapNotNull { route ->
                val author = usersById[route.authorId] ?: return@mapNotNull null
                route.toDomain(author.toDomain())
            }
        }

    fun getRoutesByDifficulty(difficulty: Difficulty): Flow<List<TrailRoute>> =
        combine(routeDao.getByDifficulty(difficulty.name), userDao.getAll()) { routes, users ->
            val usersById = users.associateBy { it.id }
            routes.mapNotNull { route ->
                val author = usersById[route.authorId] ?: return@mapNotNull null
                route.toDomain(author.toDomain())
            }
        }

    suspend fun toggleLike(routeId: String, currentlyLiked: Boolean) {
        val liked = !currentlyLiked
        val delta = if (liked) 1 else -1
        routeDao.updateLiked(routeId, liked, delta)
    }

    suspend fun toggleSave(routeId: String, currentlySaved: Boolean) {
        val saved = !currentlySaved
        val delta = if (saved) 1 else -1
        routeDao.updateSaved(routeId, saved, delta)
    }
}
