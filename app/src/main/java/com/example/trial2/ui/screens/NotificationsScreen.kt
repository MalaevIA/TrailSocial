package com.trail2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trail2.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.data.Notification
import com.trail2.data.NotificationType
import com.trail2.ui.util.formatDate
import com.trail2.ui.viewmodels.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onRouteClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    vm: NotificationViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.loadNotifications() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        IconButton(onClick = vm::markAllRead) {
                            Icon(Icons.Default.DoneAll, stringResource(R.string.notifications_read_all))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.notifications_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.notifications, key = { it.id }) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            vm.markRead(notification.id)
                            when (notification.type) {
                                NotificationType.NEW_FOLLOWER -> onUserClick(notification.actor.id)
                                NotificationType.ROUTE_LIKE -> notification.routeId?.let(onRouteClick)
                                NotificationType.NEW_COMMENT -> notification.routeId?.let(onRouteClick)
                                NotificationType.NEW_ROUTE -> notification.routeId?.let(onRouteClick)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val bgColor = if (!notification.isRead) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = notification.actor.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val text = when (notification.type) {
                    NotificationType.NEW_FOLLOWER -> stringResource(R.string.notification_new_follower, notification.actor.name)
                    NotificationType.ROUTE_LIKE -> stringResource(R.string.notification_route_like, notification.actor.name, notification.routeTitle ?: "")
                    NotificationType.NEW_COMMENT -> stringResource(R.string.notification_new_comment, notification.actor.name, notification.commentText ?: "")
                    NotificationType.NEW_ROUTE -> stringResource(R.string.notification_new_route, notification.actor.name, notification.routeTitle ?: "")
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = formatDate(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
