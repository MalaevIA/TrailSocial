package com.trail2.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.trail2.ui.components.UserAvatar
import com.trail2.ui.components.UserProfileSkeleton
import com.trail2.BuildConfig
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.theme.MossGreen
import com.trail2.ui.util.YooKassaContract
import com.trail2.ui.viewmodels.UserProfileViewModel
import ru.yoomoney.sdk.kassa.payments.Checkout
import ru.yoomoney.sdk.kassa.payments.TokenizationResult
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.Amount
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentParameters
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentMethodType
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.SavePaymentMethod
import java.util.Currency

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
    val context = LocalContext.current

    val yooKassaLauncher = rememberLauncherForActivityResult(YooKassaContract()) { result: TokenizationResult? ->
        result?.paymentToken?.let { vm.confirmPayment(it) }
    }

    val threeDsLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        vm.verifyPayment()
    }

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

    LaunchedEffect(uiState.paymentConfirmationUrl) {
        val url = uiState.paymentConfirmationUrl ?: return@LaunchedEffect
        threeDsLauncher.launch(ru.yoomoney.sdk.kassa.payments.Checkout.create3dsIntent(context, url))
        vm.clearConfirmationUrl()
    }

    if (uiState.showPaymentSheet) {
        val plan = uiState.creatorPlan
        ModalBottomSheet(onDismissRequest = vm::hidePaymentSheet) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Outlined.Star, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(40.dp))
                Text(stringResource(R.string.subscription_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (plan != null) {
                    Text(
                        stringResource(R.string.subscription_price_month, plan.price),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                    if (plan.description.isNotBlank()) {
                        Text(
                            plan.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (uiState.paymentConfirmError != null) {
                    Text(uiState.paymentConfirmError!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                val subscriptionTitleStr = stringResource(R.string.subscription_title)
                val subscriptionSubtitleStr = stringResource(R.string.subscription_payment_subtitle)
                Button(
                    onClick = {
                        val p = plan ?: return@Button
                        val price = p.price.toBigDecimalOrNull() ?: return@Button
                        val paymentIntent = Checkout.createTokenizeIntent(
                            context,
                            PaymentParameters(
                                amount = Amount(price, Currency.getInstance("RUB")),
                                title = subscriptionTitleStr,
                                subtitle = p.description.ifBlank { subscriptionSubtitleStr },
                                clientApplicationKey = BuildConfig.YOOKASSA_CLIENT_KEY,
                                shopId = BuildConfig.YOOKASSA_SHOP_ID,
                                savePaymentMethod = SavePaymentMethod.OFF,
                                paymentMethodTypes = setOf(PaymentMethodType.BANK_CARD)
                            )
                        )
                        vm.hidePaymentSheet()
                        yooKassaLauncher.launch(paymentIntent)
                    },
                    enabled = !uiState.isConfirmingPayment,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    if (uiState.isConfirmingPayment) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.subscription_purchase))
                    }
                }
                OutlinedButton(onClick = vm::hidePaymentSheet, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.height(16.dp))
            }
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
        if (uiState.isLoading && uiState.user == null) {
            UserProfileSkeleton()
            return@Scaffold
        }

        val user = uiState.user ?: return@Scaffold

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize().padding(padding),
            isRefreshing = uiState.isLoading,
            onRefresh = { vm.loadUser(userId) }
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    UserAvatar(avatarUrl = user.avatarUrl, name = user.name, size = 80)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(user.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (uiState.creatorPlan != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ForestGreen
                            ) {
                                Text(
                                    stringResource(R.string.user_paid_content),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
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

                    val plan = uiState.creatorPlan
                    if (plan != null && user.id != uiState.currentUserId) {
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = vm::showPaymentSheet,
                            modifier = Modifier.fillMaxWidth(0.8f),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                        ) {
                            Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.subscription_subscribe_for, plan.price))
                        }
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
        } // end PullToRefreshBox
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
