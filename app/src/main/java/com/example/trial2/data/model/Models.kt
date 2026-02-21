package com.example.trial2.data.model

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String,
    val bio: String,
    val followersCount: Int,
    val followingCount: Int,
    val routesCount: Int,
    val isFollowing: Boolean = false
)

data class Route(
    val id: String,
    val author: User,
    val title: String,
    val description: String,
    val photoUrls: List<String>,
    val distance: Double,          // km
    val elevationGain: Int,        // meters
    val duration: Int,             // minutes
    val difficulty: Difficulty,
    val tags: List<String>,
    val location: String,
    val region: String,
    val likesCount: Int,
    val commentsCount: Int,
    val savesCount: Int,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val createdAt: String,
    val gpxAvailable: Boolean = false,
    val rating: Float = 0f,
    val reviewsCount: Int = 0,
    val waypoints: List<Waypoint> = emptyList()
)

data class Waypoint(
    val name: String,
    val description: String,
    val lat: Double,
    val lng: Double
)

enum class Difficulty {
    EASY, MODERATE, HARD, EXPERT;

    fun label(): String = when (this) {
        EASY -> "Лёгкий"
        MODERATE -> "Умеренный"
        HARD -> "Сложный"
        EXPERT -> "Экстрем"
    }

    fun color(): Long = when (this) {
        EASY -> 0xFF4CAF50
        MODERATE -> 0xFFFF9800
        HARD -> 0xFFF44336
        EXPERT -> 0xFF9C27B0
    }
}

data class Comment(
    val id: String,
    val author: User,
    val text: String,
    val createdAt: String,
    val likesCount: Int = 0
)
