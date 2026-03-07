package com.trail2.data

data class User(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String = "",
    val bio: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val routesCount: Int = 0,
    val isFollowing: Boolean = false,
    val createdAt: String = ""
)

enum class Difficulty { EASY, MODERATE, HARD, EXPERT }

enum class RouteStatus { DRAFT, PRIVATE, PUBLISHED }

data class TrailRoute(
    val id: String,
    val author: User,
    val title: String,
    val description: String,
    val photos: List<String> = emptyList(),
    val distanceKm: Double,
    val elevationGainM: Int,
    val durationMinutes: Int,
    val difficulty: Difficulty,
    val region: String,
    val tags: List<String> = emptyList(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val savesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val createdAt: String = "",
    val status: RouteStatus = RouteStatus.PUBLISHED,
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val geometry: GeoJsonLineString? = null,
    val waypoints: List<Waypoint>? = null
)

data class GeoJsonLineString(
    val type: String = "LineString",
    val coordinates: List<List<Double>> = emptyList()
)

data class Waypoint(
    val lat: Double,
    val lng: Double,
    val name: String,
    val description: String? = null
)

data class Comment(
    val id: String,
    val author: User,
    val text: String,
    val createdAt: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val routeId: String = ""
)

data class Notification(
    val id: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val createdAt: String = "",
    val actor: User,
    val routeId: String? = null,
    val routeTitle: String? = null,
    val commentId: String? = null,
    val commentText: String? = null
)

enum class NotificationType {
    NEW_FOLLOWER, ROUTE_LIKE, NEW_COMMENT
}

data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val pages: Int
)
