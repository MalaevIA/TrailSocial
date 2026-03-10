package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.data.Comment
import com.trail2.data.RouteStatus
import com.trail2.data.TrailRoute
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.dto.GeoJsonLineStringDto
import com.trail2.data.remote.dto.RouteUpdateDto
import com.trail2.data.remote.dto.WaypointDto
import com.trail2.data.repository.AdminRepository
import com.trail2.data.repository.CommentRepository
import com.trail2.data.repository.ReportRepository
import com.trail2.data.repository.RouteRepository
import com.trail2.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteDetailUiState(
    val route: TrailRoute? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val commentText: String = "",
    val isSendingComment: Boolean = false,
    val isNavigating: Boolean = false,
    val clonedRouteId: String? = null,
    val isCloning: Boolean = false,
    val currentUserId: String? = null,
    val isAdmin: Boolean = false,
    val reportSent: Boolean = false,
    val reportError: String? = null
) {
    val isOwnRoute: Boolean
        get() = currentUserId != null && route?.author?.id == currentUserId
}

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val commentRepository: CommentRepository,
    private val userRepository: UserRepository,
    private val reportRepository: ReportRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteDetailUiState())
    val uiState: StateFlow<RouteDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = userRepository.getMe()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(currentUserId = result.data.id, isAdmin = result.data.isAdmin)
                }
                else -> {}
            }
        }
    }

    fun loadRoute(routeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, clonedRouteId = null) }
            when (val result = routeRepository.getRouteById(routeId)) {
                is ApiResult.Success -> _uiState.update { it.copy(route = result.data, isLoading = false) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Нет подключения") }
            }
            loadComments(routeId)
        }
    }

    private fun loadComments(routeId: String) {
        viewModelScope.launch {
            when (val result = commentRepository.getComments(routeId)) {
                is ApiResult.Success -> _uiState.update { it.copy(comments = result.data.items) }
                else -> {}
            }
        }
    }

    fun onCommentTextChange(text: String) {
        _uiState.update { it.copy(commentText = text) }
    }

    fun sendComment() {
        val routeId = _uiState.value.route?.id ?: return
        val text = _uiState.value.commentText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingComment = true) }
            when (val result = commentRepository.createComment(routeId, text)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            comments = it.comments + result.data,
                            commentText = "",
                            isSendingComment = false
                        )
                    }
                }
                else -> _uiState.update { it.copy(isSendingComment = false) }
            }
        }
    }

    fun toggleLike() {
        val route = _uiState.value.route ?: return
        viewModelScope.launch {
            val result = if (route.isLiked) {
                routeRepository.unlikeRoute(route.id)
            } else {
                routeRepository.likeRoute(route.id)
            }
            if (result is ApiResult.Success) {
                _uiState.update { state ->
                    val current = state.route ?: return@update state
                    state.copy(route = current.copy(
                        isLiked = !current.isLiked,
                        likesCount = current.likesCount + if (current.isLiked) -1 else 1
                    ))
                }
            }
        }
    }

    fun toggleSave() {
        val route = _uiState.value.route ?: return
        viewModelScope.launch {
            val result = if (route.isSaved) {
                routeRepository.unsaveRoute(route.id)
            } else {
                routeRepository.saveRoute(route.id)
            }
            if (result is ApiResult.Success) {
                _uiState.update { state ->
                    val current = state.route ?: return@update state
                    state.copy(route = current.copy(
                        isSaved = !current.isSaved,
                        savesCount = current.savesCount + if (current.isSaved) -1 else 1
                    ))
                }
            }
        }
    }

    fun startNavigation() {
        _uiState.update { it.copy(isNavigating = true) }
    }

    fun stopNavigation() {
        _uiState.update { it.copy(isNavigating = false) }
    }

    fun cloneToDraft() {
        val route = _uiState.value.route ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCloning = true) }
            val result = routeRepository.createRoute(
                title = route.title,
                description = route.description.ifBlank { null },
                region = route.region.ifBlank { null },
                distanceKm = route.distanceKm,
                elevationGainM = route.elevationGainM.toDouble(),
                durationMinutes = route.durationMinutes,
                difficulty = route.difficulty.name.lowercase(),
                photos = route.photos.ifEmpty { null },
                tags = route.tags.ifEmpty { null },
                status = "draft",
                startLat = route.startLat,
                startLng = route.startLng,
                endLat = route.endLat,
                endLng = route.endLng,
                geometry = route.geometry?.let { GeoJsonLineStringDto(it.type, it.coordinates) },
                waypoints = route.waypoints?.map { WaypointDto(it.lat, it.lng, it.name, it.description ?: "") }
            )
            when (result) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isCloning = false, clonedRouteId = result.data.id)
                }
                else -> _uiState.update { it.copy(isCloning = false) }
            }
        }
    }

    fun deleteRoute() {
        val route = _uiState.value.route ?: return
        viewModelScope.launch {
            routeRepository.deleteRoute(route.id)
        }
    }

    fun publishDraft() {
        val route = _uiState.value.route ?: return
        viewModelScope.launch {
            val result = routeRepository.updateRoute(
                routeId = route.id,
                update = RouteUpdateDto(status = "published")
            )
            if (result is ApiResult.Success) {
                _uiState.update { state ->
                    state.copy(route = result.data)
                }
            }
        }
    }

    fun toggleFollow() {
        val route = _uiState.value.route ?: return
        val author = route.author
        viewModelScope.launch {
            val result = if (author.isFollowing) {
                userRepository.unfollow(author.id)
            } else {
                userRepository.follow(author.id)
            }
            if (result is ApiResult.Success) {
                _uiState.update { state ->
                    val current = state.route ?: return@update state
                    state.copy(route = current.copy(
                        author = current.author.copy(isFollowing = !current.author.isFollowing)
                    ))
                }
            }
        }
    }

    fun reportRoute(reason: String, description: String?) {
        val routeId = _uiState.value.route?.id ?: return
        viewModelScope.launch {
            when (val result = reportRepository.createReport("route", routeId, reason, description)) {
                is ApiResult.Success -> _uiState.update { it.copy(reportSent = true, reportError = null) }
                is ApiResult.Error -> {
                    if (result.code == 409) {
                        _uiState.update { it.copy(reportError = "already_sent") }
                    } else {
                        _uiState.update { it.copy(reportError = result.message) }
                    }
                }
                is ApiResult.NetworkError -> _uiState.update { it.copy(reportError = "network") }
            }
        }
    }

    fun reportComment(commentId: String, reason: String, description: String?) {
        viewModelScope.launch {
            when (val result = reportRepository.createReport("comment", commentId, reason, description)) {
                is ApiResult.Success -> _uiState.update { it.copy(reportSent = true, reportError = null) }
                is ApiResult.Error -> {
                    if (result.code == 409) {
                        _uiState.update { it.copy(reportError = "already_sent") }
                    }
                }
                else -> {}
            }
        }
    }

    fun clearReportState() {
        _uiState.update { it.copy(reportSent = false, reportError = null) }
    }

    fun adminDeleteRoute() {
        val routeId = _uiState.value.route?.id ?: return
        viewModelScope.launch {
            adminRepository.deleteRoute(routeId)
        }
    }

    fun adminDeleteComment(commentId: String) {
        viewModelScope.launch {
            when (adminRepository.deleteComment(commentId)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(comments = state.comments.filter { it.id != commentId })
                    }
                }
                else -> {}
            }
        }
    }
}
