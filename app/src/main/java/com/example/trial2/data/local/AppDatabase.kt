package com.trail2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trail2.data.local.dao.CommentDao
import com.trail2.data.local.dao.RouteDao
import com.trail2.data.local.dao.UserDao
import com.trail2.data.local.entities.CommentEntity
import com.trail2.data.local.entities.RouteEntity
import com.trail2.data.local.entities.UserEntity

@Database(
    entities = [UserEntity::class, RouteEntity::class, CommentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun routeDao(): RouteDao
    abstract fun commentDao(): CommentDao
}
