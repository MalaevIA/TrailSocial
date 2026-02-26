package com.trail2.data.local

import com.trail2.data.Comment
import com.trail2.data.Difficulty
import com.trail2.data.TrailRoute
import com.trail2.data.User
import com.trail2.data.local.entities.CommentEntity
import com.trail2.data.local.entities.RouteEntity
import com.trail2.data.local.entities.UserEntity

// ── UserEntity ↔ User ─────────────────────────────────────

fun UserEntity.toDomain() = User(
    id = id,
    name = name,
    username = username,
    avatarUrl = avatarUrl,
    bio = bio,
    followersCount = followersCount,
    followingCount = followingCount,
    routesCount = routesCount,
    isFollowing = isFollowing
)

fun User.toEntity() = UserEntity(
    id = id,
    name = name,
    username = username,
    avatarUrl = avatarUrl,
    bio = bio,
    followersCount = followersCount,
    followingCount = followingCount,
    routesCount = routesCount,
    isFollowing = isFollowing
)

// ── RouteEntity ↔ TrailRoute ──────────────────────────────

fun RouteEntity.toDomain(author: User) = TrailRoute(
    id = id,
    author = author,
    title = title,
    description = description,
    photos = photosJson.split(",").filter { it.isNotBlank() },
    distanceKm = distanceKm,
    elevationGainM = elevationGainM,
    durationHours = durationHours,
    difficulty = runCatching { Difficulty.valueOf(difficulty) }.getOrDefault(Difficulty.MODERATE),
    location = location,
    region = region,
    tags = tagsJson.split("|").filter { it.isNotBlank() },
    likesCount = likesCount,
    commentsCount = commentsCount,
    savesCount = savesCount,
    isLiked = isLiked,
    isSaved = isSaved,
    rating = rating,
    createdAt = createdAt
)

fun TrailRoute.toEntity() = RouteEntity(
    id = id,
    authorId = author.id,
    title = title,
    description = description,
    photosJson = photos.joinToString(","),
    distanceKm = distanceKm,
    elevationGainM = elevationGainM,
    durationHours = durationHours,
    difficulty = difficulty.name,
    location = location,
    region = region,
    tagsJson = tags.joinToString("|"),
    likesCount = likesCount,
    commentsCount = commentsCount,
    savesCount = savesCount,
    isLiked = isLiked,
    isSaved = isSaved,
    rating = rating,
    createdAt = createdAt
)

// ── CommentEntity ↔ Comment ───────────────────────────────

fun CommentEntity.toDomain(author: User) = Comment(
    id = id,
    author = author,
    text = text,
    createdAt = createdAt,
    likesCount = likesCount
)
