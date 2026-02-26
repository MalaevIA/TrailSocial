package com.trail2.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["authorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("authorId"), Index("region"), Index("difficulty")]
)
data class RouteEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val title: String,
    val description: String,
    val photosJson: String,       // JSON-массив строк hex-цветов / url
    val distanceKm: Double,
    val elevationGainM: Int,
    val durationHours: Double,
    val difficulty: String,       // Difficulty enum name
    val location: String,
    val region: String,
    val tagsJson: String,         // JSON-массив строк
    val likesCount: Int,
    val commentsCount: Int,
    val savesCount: Int,
    val isLiked: Boolean,
    val isSaved: Boolean,
    val rating: Float,
    val createdAt: String
)
