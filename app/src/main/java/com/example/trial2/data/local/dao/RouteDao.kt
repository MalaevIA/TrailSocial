package com.trail2.data.local.dao

import androidx.room.*
import com.trail2.data.local.entities.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY createdAt DESC")
    fun getAll(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id")
    fun getById(id: String): Flow<RouteEntity?>

    @Query("SELECT * FROM routes WHERE region = :region ORDER BY createdAt DESC")
    fun getByRegion(region: String): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE difficulty = :difficulty ORDER BY createdAt DESC")
    fun getByDifficulty(difficulty: String): Flow<List<RouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<RouteEntity>)

    @Update
    suspend fun update(route: RouteEntity)

    @Query("UPDATE routes SET isLiked = :liked, likesCount = likesCount + :delta WHERE id = :routeId")
    suspend fun updateLiked(routeId: String, liked: Boolean, delta: Int)

    @Query("UPDATE routes SET isSaved = :saved, savesCount = savesCount + :delta WHERE id = :routeId")
    suspend fun updateSaved(routeId: String, saved: Boolean, delta: Int)

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun delete(routeId: String)
}
