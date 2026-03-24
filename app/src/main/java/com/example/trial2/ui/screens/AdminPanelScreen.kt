package com.trail2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.R
import com.trail2.data.Report
import com.trail2.data.ReportReason
import com.trail2.data.ReportStatus
import com.trail2.data.TargetType
import com.trail2.data.User
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.util.formatDate
import com.trail2.ui.viewmodels.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onRouteClick: (String) -> Unit = {},
    vm: AdminViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.admin_users)) },
                    icon = { Icon(Icons.Outlined.People, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.admin_reports)) },
                    icon = { Icon(Icons.Outlined.Flag, null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> AdminUsersTab(vm = vm, onUserClick = onUserClick)
                1 -> AdminReportsTab(vm = vm, onRouteClick = onRouteClick, onUserClick = onUserClick)
            }
        }
    }
}

@Composable
private fun AdminUsersTab(
    vm: AdminViewModel,
    onUserClick: (String) -> Unit
) {
    val state by vm.usersState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadUsers() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = vm::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.admin_search_users)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        vm.onSearchQueryChange("")
                        vm.searchUsers()
                    }) {
                        Icon(Icons.Filled.Clear, null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.filterActive == null,
                onClick = { vm.setFilterActive(null) },
                label = { Text(stringResource(R.string.admin_filter_all)) }
            )
            FilterChip(
                selected = state.filterActive == true,
                onClick = { vm.setFilterActive(true) },
                label = { Text(stringResource(R.string.admin_filter_active)) }
            )
            FilterChip(
                selected = state.filterActive == false,
                onClick = { vm.setFilterActive(false) },
                label = { Text(stringResource(R.string.admin_filter_banned)) }
            )
        }

        // Search button
        Button(
            onClick = { vm.searchUsers() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            Text(stringResource(R.string.admin_search))
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.users.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.admin_no_users), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.users, key = { it.id }) { user ->
                    AdminUserItem(
                        user = user,
                        onClick = { onUserClick(user.id) },
                        onBan = { vm.banUser(user.id) },
                        onUnban = { vm.unbanUser(user.id) }
                    )
                }
                // Pagination
                if (state.currentPage < state.totalPages) {
                    item {
                        TextButton(
                            onClick = { vm.loadUsers(state.currentPage + 1) },
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(stringResource(R.string.admin_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserItem(
    user: User,
    onClick: () -> Unit,
    onBan: () -> Unit,
    onUnban: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (!user.isActive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(44.dp).clip(CircleShape),
                color = if (!user.isActive) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (!user.isActive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                stringResource(R.string.admin_banned_badge),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (user.isAdmin) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ForestGreen
                        ) {
                            Text(
                                stringResource(R.string.admin_admin_badge),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    "@${user.username} · ${formatDate(user.createdAt)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${user.routesCount} маршрутов · ${user.followersCount} подписчиков",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!user.isAdmin) {
                IconButton(onClick = { showConfirmDialog = true }) {
                    Icon(
                        if (user.isActive) Icons.Outlined.Block else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (user.isActive) MaterialTheme.colorScheme.error else ForestGreen
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    if (user.isActive) stringResource(R.string.admin_ban_confirm)
                    else stringResource(R.string.admin_unban_confirm)
                )
            },
            text = {
                Text(
                    if (user.isActive) stringResource(R.string.admin_ban_message, user.name)
                    else stringResource(R.string.admin_unban_message, user.name)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (user.isActive) onBan() else onUnban()
                    showConfirmDialog = false
                }) {
                    Text(
                        if (user.isActive) stringResource(R.string.admin_ban_action) else stringResource(R.string.admin_unban_action),
                        color = if (user.isActive) MaterialTheme.colorScheme.error else ForestGreen
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun AdminReportsTab(
    vm: AdminViewModel,
    onRouteClick: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val state by vm.reportsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadReports() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Status filter tabs
        ScrollableTabRow(
            selectedTabIndex = when (state.statusFilter) {
                ReportStatus.PENDING -> 0
                ReportStatus.REVIEWED -> 1
                ReportStatus.DISMISSED -> 2
                null -> 3
            },
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 16.dp
        ) {
            Tab(
                selected = state.statusFilter == ReportStatus.PENDING,
                onClick = { vm.setReportStatusFilter(ReportStatus.PENDING) },
                text = { Text(stringResource(R.string.admin_report_pending)) }
            )
            Tab(
                selected = state.statusFilter == ReportStatus.REVIEWED,
                onClick = { vm.setReportStatusFilter(ReportStatus.REVIEWED) },
                text = { Text(stringResource(R.string.admin_report_reviewed)) }
            )
            Tab(
                selected = state.statusFilter == ReportStatus.DISMISSED,
                onClick = { vm.setReportStatusFilter(ReportStatus.DISMISSED) },
                text = { Text(stringResource(R.string.admin_report_dismissed)) }
            )
            Tab(
                selected = state.statusFilter == null,
                onClick = { vm.setReportStatusFilter(null) },
                text = { Text(stringResource(R.string.admin_filter_all)) }
            )
        }

        // Target type filter
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.targetTypeFilter == null,
                onClick = { vm.setReportTargetTypeFilter(null) },
                label = { Text(stringResource(R.string.admin_filter_all)) }
            )
            FilterChip(
                selected = state.targetTypeFilter == TargetType.ROUTE,
                onClick = { vm.setReportTargetTypeFilter(TargetType.ROUTE) },
                label = { Text(stringResource(R.string.admin_target_route)) }
            )
            FilterChip(
                selected = state.targetTypeFilter == TargetType.COMMENT,
                onClick = { vm.setReportTargetTypeFilter(TargetType.COMMENT) },
                label = { Text(stringResource(R.string.admin_target_comment)) }
            )
            FilterChip(
                selected = state.targetTypeFilter == TargetType.USER,
                onClick = { vm.setReportTargetTypeFilter(TargetType.USER) },
                label = { Text(stringResource(R.string.admin_target_user)) }
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.admin_no_reports), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.reports, key = { it.id }) { report ->
                    ReportItem(
                        report = report,
                        onNavigateToTarget = {
                            when (report.targetType) {
                                TargetType.ROUTE -> onRouteClick(report.targetId)
                                TargetType.USER -> onUserClick(report.targetId)
                                TargetType.COMMENT -> {} // No standalone comment screen
                            }
                        },
                        onMarkReviewed = { vm.updateReportStatus(report.id, "reviewed") },
                        onDismiss = { vm.updateReportStatus(report.id, "dismissed") },
                        onDeleteContent = {
                            when (report.targetType) {
                                TargetType.ROUTE -> vm.adminDeleteRoute(report.targetId)
                                TargetType.COMMENT -> vm.adminDeleteComment(report.targetId)
                                TargetType.USER -> {} // Ban through user management
                            }
                            vm.updateReportStatus(report.id, "reviewed")
                        }
                    )
                }
                if (state.currentPage < state.totalPages) {
                    item {
                        TextButton(
                            onClick = { vm.loadReports(state.currentPage + 1) },
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text(stringResource(R.string.admin_load_more))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportItem(
    report: Report,
    onNavigateToTarget: () -> Unit,
    onMarkReviewed: () -> Unit,
    onDismiss: () -> Unit,
    onDeleteContent: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onNavigateToTarget),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Target type icon
                Icon(
                    when (report.targetType) {
                        TargetType.ROUTE -> Icons.Outlined.Map
                        TargetType.COMMENT -> Icons.Outlined.Comment
                        TargetType.USER -> Icons.Outlined.Person
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when (report.targetType) {
                        TargetType.ROUTE -> stringResource(R.string.admin_target_route)
                        TargetType.COMMENT -> stringResource(R.string.admin_target_comment)
                        TargetType.USER -> stringResource(R.string.admin_target_user)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                // Status badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (report.status) {
                        ReportStatus.PENDING -> Color(0xFFFFA726)
                        ReportStatus.REVIEWED -> ForestGreen
                        ReportStatus.DISMISSED -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        when (report.status) {
                            ReportStatus.PENDING -> stringResource(R.string.admin_report_pending)
                            ReportStatus.REVIEWED -> stringResource(R.string.admin_report_reviewed)
                            ReportStatus.DISMISSED -> stringResource(R.string.admin_report_dismissed)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Reason
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Text(
                    reportReasonLabel(report.reason),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!report.description.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(report.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(4.dp))
            Text(formatDate(report.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Actions (only for pending reports)
            if (report.status == ReportStatus.PENDING) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onMarkReviewed,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.admin_action_reviewed), fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.admin_action_dismiss), fontSize = 12.sp)
                    }
                }
                if (report.targetType != TargetType.USER) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.admin_delete_content), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.admin_delete_content_confirm)) },
            text = { Text(stringResource(R.string.admin_delete_content_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteContent()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun reportReasonLabel(reason: ReportReason): String = when (reason) {
    ReportReason.SPAM -> stringResource(R.string.report_reason_spam)
    ReportReason.HARASSMENT -> stringResource(R.string.report_reason_harassment)
    ReportReason.INAPPROPRIATE -> stringResource(R.string.report_reason_inappropriate)
    ReportReason.MISINFORMATION -> stringResource(R.string.report_reason_misinformation)
    ReportReason.OTHER -> stringResource(R.string.report_reason_other)
}
