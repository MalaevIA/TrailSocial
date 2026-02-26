package com.trail2.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trail2.data.SampleData
import com.trail2.data.local.AppDatabase
import com.trail2.data.local.dao.CommentDao
import com.trail2.data.local.dao.RouteDao
import com.trail2.data.local.dao.UserDao
import com.trail2.data.local.entities.CommentEntity
import com.trail2.data.local.toEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        // `lateinit var` даёт callback доступ к instance ПОСЛЕ того как база построена
        lateinit var instance: AppDatabase
        instance = Room.databaseBuilder(ctx, AppDatabase::class.java, "trail_social.db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Первый запуск — заполняем тестовыми данными
                    CoroutineScope(Dispatchers.IO).launch {
                        val userDao    = instance.userDao()
                        val routeDao   = instance.routeDao()
                        val commentDao = instance.commentDao()

                        userDao.insertAll(SampleData.users.map { it.toEntity() })
                        routeDao.insertAll(SampleData.routes.map { it.toEntity() })
                        commentDao.insertAll(SampleData.comments.map { comment ->
                            CommentEntity(
                                id         = comment.id,
                                routeId    = "r1",
                                authorId   = comment.author.id,
                                text       = comment.text,
                                createdAt  = comment.createdAt,
                                likesCount = comment.likesCount
                            )
                        })
                    }
                }
            })
            .build()
        return instance
    }

    @Provides
    fun provideRouteDao(db: AppDatabase): RouteDao = db.routeDao()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideCommentDao(db: AppDatabase): CommentDao = db.commentDao()
}
