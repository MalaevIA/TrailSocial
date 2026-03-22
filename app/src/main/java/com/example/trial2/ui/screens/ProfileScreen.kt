package com.trail2.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.trail2.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.FollowListType
import com.trail2.data.RouteStatus
import com.trail2.data.TrailRoute
import com.trail2.onboarding.FitnessLevel
import com.trail2.onboarding.OnboardingData
import com.trail2.onboarding.OnboardingViewModel
import com.trail2.ui.components.RouteCard
import com.trail2.ui.util.routePhotoUrl
import com.trail2.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onRouteClick: (String) -> Unit = {},
    onFollowListClick: (String, FollowListType) -> Unit = { _, _ -> },
    onSettingsClick: () -> Unit = {},
    profileVm: ProfileViewModel = hiltViewModel(),
    onboardingVm: OnboardingViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { profileVm.loadProfile() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showLogoutDialog by remember { mutableStateOf(false) }
    val profileState by profileVm.uiState.collectAsStateWithLifecycle()
    val onboardingAnswers by onboardingVm.savedAnswers.collectAsStateWithLifecycle()

    val avatarPickerLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { profileVm.uploadAvatar(it, context) }
    }

    LaunchedEffect(profileState.avatarError) {
        val error = profileState.avatarError ?: return@LaunchedEffect
        scope.launch { snackbarHostState.showSnackbar(error) }
        profileVm.clearAvatarError()
    }

    val user = profileState.user
    val fallbackName = stringResource(R.string.profile_default_title)
    val displayName = user?.name ?: onboardingAnswers.displayName.ifBlank { fallbackName }

    val selectedCityNames = remember(onboardingAnswers.selectedCityIds) {
        onboardingAnswers.selectedCityIds.map { id ->
            OnboardingData.cities.find { it.id == id }?.name ?: id
        }
    }
    val selectedInterests = remember(onboardingAnswers.selectedInterestIds) {
        OnboardingData.interests.filter { it.id in onboardingAnswers.selectedInterestIds }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val publishedRoutes = profileState.myRoutes.filter { it.status != RouteStatus.DRAFT }
    val draftRoutes = profileState.myRoutes.filter { it.status == RouteStatus.DRAFT }
    val savedRoutes = profileState.savedRoutes

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
    LazyColumn(modifier = Modifier.fillMaxWidth().padding(bottom = innerPadding.calculateBottomPadding())) {
        // Header gradient
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF1B4332), Color(0xFF52B788))))
            ) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.profile_settings), tint = Color.White)
                }
            }
        }

        // Profile info
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Box(modifier = Modifier.offset(y = (-44).dp)) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2D6A4F))
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable(enabled = !profileState.isUploadingAvatar) {
                                avatarPickerLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarUrl = user?.avatarUrl?.takeIf { it.isNotBlank() }
                        var avatarLoadFailed by remember(avatarUrl) { mutableStateOf(false) }
                        if (avatarUrl != null && !avatarLoadFailed) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(routePhotoUrl(avatarUrl))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onError = { avatarLoadFailed = true }
                            )
                        } else {
                            Text(displayName.take(1).uppercase(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (profileState.isUploadingAvatar) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                            }
                        }
                    }
                }

                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.offset(y = (-28).dp))
                if (user != null) {
                    Text("@${user.username}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.offset(y = (-24).dp))
                }

                Spacer(Modifier.height(4.dp))

                onboardingAnswers.fitnessLevel?.let { level ->
                    FitnessLevelBadge(level)
                    Spacer(Modifier.height(12.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ProfileStatItem(stringResource(R.string.profile_routes), "${user?.routesCount ?: 0}")
                    VerticalDivider(Modifier.height(36.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            user?.let { onFollowListClick(it.id, FollowListType.FOLLOWERS) }
                        }
                    ) {
                        Text("${user?.followersCount ?: 0}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(stringResource(R.string.profile_followers), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    VerticalDivider(Modifier.height(36.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            user?.let { onFollowListClick(it.id, FollowListType.FOLLOWING) }
                        }
                    ) {
                        Text("${user?.followingCount ?: 0}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(stringResource(R.string.profile_following), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (selectedCityNames.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.profile_my_cities))
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedCityNames.forEach { cityName ->
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Text(cityName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                if (selectedInterests.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.profile_interests))
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedInterests.forEach { interest ->
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(
                                    "${interest.emoji} ${interest.label}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                if (user?.bio?.isNotBlank() == true) {
                    SectionTitle(stringResource(R.string.profile_about))
                    Spacer(Modifier.height(4.dp))
                    Text(user.bio, fontSize = 14.sp)
                    Spacer(Modifier.height(20.dp))
                }
            }
        }

        // Tabs: My Routes / Saved / Drafts
        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.profile_tab_routes)) },
                    icon = { Icon(Icons.Outlined.Map, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.profile_tab_saved)) },
                    icon = { Icon(Icons.Outlined.Bookmark, null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.profile_tab_drafts)) },
                    icon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // Routes content
        val routes = when (selectedTab) {
            0 -> publishedRoutes
            1 -> savedRoutes
            else -> draftRoutes
        }
        if (routes.isEmpty()) {
            item {
                val emptyText = when (selectedTab) {
                    0 -> stringResource(R.string.profile_no_routes)
                    1 -> stringResource(R.string.profile_no_saved)
                    else -> stringResource(R.string.profile_no_drafts)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(routes, key = { it.id }) { route ->
                RouteCard(
                    route = route,
                    onClick = { onRouteClick(route.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Logout button
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_logout))
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }
    } // end Scaffold

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.profile_logout_confirm)) },
            text = { Text(stringResource(R.string.profile_logout_message)) },
            confirmButton = {
                TextButton(onClick = {
                    profileVm.logout()
                    onboardingVm.logout()
                    showLogoutDialog = false
                }) {
                    Text(stringResource(R.string.profile_logout_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun FitnessLevelBadge(level: FitnessLevel) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(level.emoji, fontSize = 16.sp)
            Text(level.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text("· ${level.description}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.75f), maxLines = 1)
        }
    }
}

@Composable
private fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}
