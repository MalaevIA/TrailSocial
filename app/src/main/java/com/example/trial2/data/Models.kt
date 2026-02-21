package com.trail2.data

data class User(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String,
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val routesCount: Int = 0,
    val isFollowing: Boolean = false
)

enum class Difficulty { EASY, MODERATE, HARD, EXPERT }

data class TrailRoute(
    val id: String,
    val author: User,
    val title: String,
    val description: String,
    val photos: List<String>,         // placeholder color hex strings used for demo
    val distanceKm: Double,
    val elevationGainM: Int,
    val durationHours: Double,
    val difficulty: Difficulty,
    val location: String,
    val region: String,
    val tags: List<String>,
    val likesCount: Int,
    val commentsCount: Int,
    val savesCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val rating: Float = 0f,
    val createdAt: String
)

data class Comment(
    val id: String,
    val author: User,
    val text: String,
    val createdAt: String,
    val likesCount: Int = 0
)
