package com.trail2.data.remote.mappers

import com.trail2.ai_route.GeneratedRoute
import com.trail2.ai_route.RoutePoint
import com.trail2.data.*
import com.trail2.data.remote.dto.*

fun UserPublicDto.toDomain() = User(
    id = id,
    name = displayName,
    username = username,
    avatarUrl = avatarUrl ?: "",
    bio = bio ?: "",
    followersCount = followersCount,
    followingCount = followingCount,
    routesCount = routesCount,
    isFollowing = false,
    createdAt = createdAt,
    isAdmin = isAdmin,
    isActive = isActive
)

fun UserProfileDto.toDomain() = User(
    id = id,
    name = displayName,
    username = username,
    avatarUrl = avatarUrl ?: "",
    bio = bio ?: "",
    followersCount = followersCount,
    followingCount = followingCount,
    routesCount = routesCount,
    isFollowing = isFollowing,
    createdAt = createdAt,
    isAdmin = isAdmin,
    isActive = isActive
)

fun RouteResponseDto.toDomain() = TrailRoute(
    id = id,
    author = author?.toDomain() ?: User(id = "", name = "", username = ""),
    title = title,
    description = description ?: "",
    photos = photos ?: emptyList(),
    distanceKm = distanceKm ?: 0.0,
    elevationGainM = elevationGainM?.toInt() ?: 0,
    durationMinutes = durationMinutes ?: 0,
    difficulty = parseDifficulty(difficulty),
    region = region ?: "",
    tags = tags ?: emptyList(),
    likesCount = likesCount,
    commentsCount = commentsCount,
    savesCount = savesCount,
    isLiked = isLiked,
    isSaved = isSaved,
    createdAt = createdAt,
    status = parseRouteStatus(status),
    startLat = startLat,
    startLng = startLng,
    endLat = endLat,
    endLng = endLng,
    geometry = geometry?.toDomain(),
    waypoints = waypoints?.map { it.toDomain() }
)

fun GeoJsonLineStringDto.toDomain() = GeoJsonLineString(
    type = type,
    coordinates = coordinates
)

fun WaypointDto.toDomain() = Waypoint(
    lat = lat,
    lng = lng,
    name = name,
    description = description
)

fun CommentResponseDto.toDomain() = Comment(
    id = id,
    author = author?.toDomain() ?: User(id = "", name = "", username = ""),
    text = text,
    createdAt = createdAt,
    likesCount = likesCount,
    isLiked = isLiked,
    routeId = routeId
)

fun NotificationResponseDto.toDomain() = Notification(
    id = id,
    type = parseNotificationType(type),
    isRead = isRead,
    createdAt = createdAt,
    actor = actor.toDomain(),
    routeId = routeId,
    routeTitle = routeTitle,
    commentId = commentId,
    commentText = commentText
)

fun GeneratedRouteDto.toDomain() = GeneratedRoute(
    title = title,
    description = description,
    region = region ?: "",
    distanceKm = distanceKm ?: 0.0,
    elevationGainM = elevationGainM?.toInt() ?: 0,
    durationMin = durationMinutes ?: 0,
    difficulty = difficulty ?: "MODERATE",
    points = waypoints?.let { wps ->
        wps.mapIndexed { idx, wp ->
            RoutePoint(
                lat = wp.lat,
                lon = wp.lng,
                title = wp.name,
                description = wp.description ?: "",
                type = when (idx) {
                    0 -> "start"
                    wps.lastIndex -> "finish"
                    else -> "waypoint"
                }
            )
        }
    } ?: emptyList(),
    tips = tips,
    tags = tags,
    highlights = highlights,
    geometry = geometry?.let {
        GeoJsonLineString(it.type, it.coordinates)
    },
    photos = photos ?: emptyList()
)

fun RegionInfoDto.toDomain() = RegionInfo(
    name = name,
    routeCount = routeCount,
    photoUrl = photoUrl
)

fun <T, R> PaginatedResponseDto<T>.toDomain(mapper: (T) -> R) = PaginatedResponse(
    items = items.map(mapper),
    total = total,
    page = page,
    pageSize = pageSize,
    pages = pages
)

private fun parseDifficulty(value: String?): Difficulty = when (value?.lowercase()) {
    "easy" -> Difficulty.EASY
    "moderate" -> Difficulty.MODERATE
    "hard" -> Difficulty.HARD
    "expert" -> Difficulty.EXPERT
    else -> Difficulty.MODERATE
}

private fun parseRouteStatus(value: String): RouteStatus = when (value.lowercase()) {
    "draft" -> RouteStatus.DRAFT
    "private" -> RouteStatus.PRIVATE
    "published" -> RouteStatus.PUBLISHED
    else -> RouteStatus.PUBLISHED
}

fun ReportResponseDto.toDomain() = Report(
    id = id,
    reporterId = reporterId,
    targetType = parseTargetType(targetType),
    targetId = targetId,
    reason = parseReportReason(reason),
    description = description,
    status = parseReportStatus(status),
    createdAt = createdAt
)

private fun parseTargetType(value: String): TargetType = when (value) {
    "route" -> TargetType.ROUTE
    "comment" -> TargetType.COMMENT
    "user" -> TargetType.USER
    else -> TargetType.ROUTE
}

private fun parseReportReason(value: String): ReportReason = when (value) {
    "spam" -> ReportReason.SPAM
    "harassment" -> ReportReason.HARASSMENT
    "inappropriate" -> ReportReason.INAPPROPRIATE
    "misinformation" -> ReportReason.MISINFORMATION
    "other" -> ReportReason.OTHER
    else -> ReportReason.OTHER
}

private fun parseReportStatus(value: String): ReportStatus = when (value) {
    "pending" -> ReportStatus.PENDING
    "reviewed" -> ReportStatus.REVIEWED
    "dismissed" -> ReportStatus.DISMISSED
    else -> ReportStatus.PENDING
}

private fun parseNotificationType(value: String): NotificationType = when (value) {
    "new_follower" -> NotificationType.NEW_FOLLOWER
    "route_like" -> NotificationType.ROUTE_LIKE
    "new_comment" -> NotificationType.NEW_COMMENT
    "new_route" -> NotificationType.NEW_ROUTE
    else -> NotificationType.NEW_FOLLOWER
}
