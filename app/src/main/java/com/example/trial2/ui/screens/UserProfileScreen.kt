package com.trail2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trail2.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.FollowListType
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

    LaunchedEffect(userId) { vm.loadUser(userId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.user?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
            contentPadding = PaddingValues(16.dp),
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
}
