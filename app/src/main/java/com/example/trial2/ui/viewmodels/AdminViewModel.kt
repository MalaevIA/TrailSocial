package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.data.Report
import com.trail2.data.ReportStatus
import com.trail2.data.TargetType
import com.trail2.data.User
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.AdminRepository
import com.trail2.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUsersUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterActive: Boolean? = null, // null = all, true = active, false = banned
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

data class AdminReportsUiState(
    val reports: List<Report> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val statusFilter: ReportStatus? = ReportStatus.PENDING,
    val targetTypeFilter: TargetType? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _usersState = MutableStateFlow(AdminUsersUiState())
    val usersState: StateFlow<AdminUsersUiState> = _usersState.asStateFlow()

    private val _reportsState = MutableStateFlow(AdminReportsUiState())
    val reportsState: StateFlow<AdminReportsUiState> = _reportsState.asStateFlow()

    fun loadUsers(page: Int = 1) {
        val state = _usersState.value
        viewModelScope.launch {
            _usersState.update { it.copy(isLoading = true, error = null) }
            val result = adminRepository.getUsers(
                page = page,
                query = state.searchQuery.ifBlank { null },
                isActive = state.filterActive
            )
            when (result) {
                is ApiResult.Success -> _usersState.update {
                    it.copy(
                        users = result.data.items,
                        isLoading = false,
                        currentPage = result.data.page,
                        totalPages = result.data.pages
                    )
                }
                is ApiResult.Error -> _usersState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _usersState.update { it.copy(isLoading = false, error = "Нет подключения") }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _usersState.update { it.copy(searchQuery = query) }
    }

    fun setFilterActive(isActive: Boolean?) {
        _usersState.update { it.copy(filterActive = isActive) }
        loadUsers()
    }

    fun searchUsers() {
        loadUsers()
    }

    fun banUser(userId: String) {
        viewModelScope.launch {
            when (val result = adminRepository.banUser(userId)) {
                is ApiResult.Success -> {
                    _usersState.update { state ->
                        state.copy(users = state.users.map { if (it.id == userId) result.data else it })
                    }
                }
                else -> {}
            }
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch {
            when (val result = adminRepository.unbanUser(userId)) {
                is ApiResult.Success -> {
                    _usersState.update { state ->
                        state.copy(users = state.users.map { if (it.id == userId) result.data else it })
                    }
                }
                else -> {}
            }
        }
    }

    fun adminDeleteRoute(routeId: String) {
        viewModelScope.launch {
            adminRepository.deleteRoute(routeId)
        }
    }

    fun adminDeleteComment(commentId: String) {
        viewModelScope.launch {
            adminRepository.deleteComment(commentId)
        }
    }

    // Reports
    fun loadReports(page: Int = 1) {
        val state = _reportsState.value
        viewModelScope.launch {
            _reportsState.update { it.copy(isLoading = true, error = null) }
            val result = reportRepository.getReports(
                page = page,
                status = state.statusFilter?.name?.lowercase(),
                targetType = state.targetTypeFilter?.name?.lowercase()
            )
            when (result) {
                is ApiResult.Success -> _reportsState.update {
                    it.copy(
                        reports = result.data.items,
                        isLoading = false,
                        currentPage = result.data.page,
                        totalPages = result.data.pages
                    )
                }
                is ApiResult.Error -> _reportsState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _reportsState.update { it.copy(isLoading = false, error = "Нет подключения") }
            }
        }
    }

    fun setReportStatusFilter(status: ReportStatus?) {
        _reportsState.update { it.copy(statusFilter = status) }
        loadReports()
    }

    fun setReportTargetTypeFilter(targetType: TargetType?) {
        _reportsState.update { it.copy(targetTypeFilter = targetType) }
        loadReports()
    }

    fun updateReportStatus(reportId: String, status: String) {
        viewModelScope.launch {
            when (val result = reportRepository.updateReportStatus(reportId, status)) {
                is ApiResult.Success -> {
                    _reportsState.update { state ->
                        state.copy(reports = state.reports.map { if (it.id == reportId) result.data else it })
                    }
                }
                else -> {}
            }
        }
    }
}
