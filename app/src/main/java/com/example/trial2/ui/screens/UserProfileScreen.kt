package com.trail2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trail2.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.FollowListType
import com.trail2.ui.components.ReportDialog
import com.trail2.ui.viewmodels.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onRouteClick: (String) -> Unit,
    onFollowListClick: (String, FollowListType) -> Unit,
    vm: UserProfileViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var showReportDialog by remember { mutableStateOf(false) }
    var showBanDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val reportSentText = stringResource(R.string.report_sent)
    val reportAlreadySentText = stringResource(R.string.report_already_sent)

    LaunchedEffect(userId) { vm.loadUser(userId) }

    LaunchedEffect(uiState.reportSent) {
        if (uiState.reportSent) {
            snackbarHostState.showSnackbar(reportSentText)
            vm.clearReportState()
        }
    }
    LaunchedEffect(uiState.reportError) {
        if (uiState.reportError == "already_sent") {
            snackbarHostState.showSnackbar(reportAlreadySentText)
            vm.clearReportState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.user?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showReportDialog = true }) {
                        Icon(Icons.Outlined.Flag, contentDescription = stringResource(R.string.report_title))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val user = uiState.user ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier.size(80.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = user.name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("@${user.username}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (user.bio.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(user.bio, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onFollowListClick(user.id, FollowListType.FOLLOWERS) }
                        ) {
                            Text("${user.followersCount}", fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.profile_followers), style = MaterialTheme.typography.bodySmall)
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onFollowListClick(user.id, FollowListType.FOLLOWING) }
                        ) {
                            Text("${user.followingCount}", fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.profile_following), style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${user.routesCount}", fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.profile_routes), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = vm::toggleFollow,
                        colors = if (user.isFollowing)
                            ButtonDefaults.outlinedButtonColors()
                        else
                            ButtonDefaults.buttonColors(),
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text(if (user.isFollowing) stringResource(R.string.route_unsubscribe) else stringResource(R.string.route_subscribe))
                    }

                    // Admin: ban/unban
                    if (uiState.isCurrentUserAdmin && !user.isAdmin) {
                        Spacer(Modifier.height(8.dp))
                        if (!user.isActive) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    stringResource(R.string.admin_banned_badge),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        OutlinedButton(
                            onClick = { showBanDialog = true },
                            modifier = Modifier.fillMaxWidth(0.6f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (user.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (user.isActive) Icons.Outlined.Block else Icons.Outlined.CheckCircle,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (user.isActive) stringResource(R.string.admin_ban_action)
                                else stringResource(R.string.admin_unban_action)
                            )
                        }
                    }
                }
            }

            if (uiState.routes.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.user_routes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(uiState.routes) { route ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onRouteClick(route.id) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(route.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${route.distanceKm} км · ${route.region}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { data ->
                vm.reportUser(data.reason, data.description)
                showReportDialog = false
            }
        )
    }

    if (showBanDialog) {
        val user = uiState.user
        AlertDialog(
            onDismissRequest = { showBanDialog = false },
            title = {
                Text(
                    if (user?.isActive == true) stringResource(R.string.admin_ban_confirm)
                    else stringResource(R.string.admin_unban_confirm)
                )
            },
            text = {
                Text(
                    if (user?.isActive == true) stringResource(R.string.admin_ban_message, user.name)
                    else stringResource(R.string.admin_unban_message, user?.name ?: "")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (user?.isActive == true) vm.banUser() else vm.unbanUser()
                    showBanDialog = false
                }) {
                    Text(
                        if (user?.isActive == true) stringResource(R.string.admin_ban_action)
                        else stringResource(R.string.admin_unban_action),
                        color = if (user?.isActive == true) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBanDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
