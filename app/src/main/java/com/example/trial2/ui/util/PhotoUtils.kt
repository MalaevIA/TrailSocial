package com.trail2.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.trail2.BuildConfig
import com.trail2.ui.theme.ForestGreen

fun routePhotoUrl(path: String): String {
    val serverRoot = BuildConfig.BASE_URL.removeSuffix("api/v1/")
    return serverRoot + path.removePrefix("/")
}

@Composable
fun RoutePhotoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(ForestGreen),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Landscape,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(52.dp)
        )
    }
}
