package com.trail2.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Shimmer brush ─────────────────────────────────────────────

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, offset),
        end = Offset(offset + 300f, offset + 300f)
    )
}

// ── Primitive skeleton block ──────────────────────────────────

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(shimmerBrush())
    )
}

@Composable
fun SkeletonCircle(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(shimmerBrush())
    )
}

// ── Route card skeleton ───────────────────────────────────────

@Composable
fun RouteCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Photo placeholder
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(180.dp), cornerRadius = 12.dp)
        // Author row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SkeletonCircle(size = 32.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SkeletonBox(modifier = Modifier.width(120.dp).height(12.dp))
                SkeletonBox(modifier = Modifier.width(80.dp).height(10.dp))
            }
        }
        // Title
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.85f).height(16.dp))
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
        // Stats row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp))
            SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp))
            SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp))
        }
    }
}

@Composable
fun RouteCardSkeletonList(count: Int = 3) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count) {
            RouteCardSkeleton()
        }
    }
}

// ── Route detail skeleton ─────────────────────────────────────

@Composable
fun RouteDetailSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Hero image
        SkeletonBox(modifier = Modifier.fillMaxWidth().height(260.dp), cornerRadius = 0.dp)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tags row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(modifier = Modifier.width(60.dp).height(24.dp), cornerRadius = 12.dp)
                SkeletonBox(modifier = Modifier.width(80.dp).height(24.dp), cornerRadius = 12.dp)
                SkeletonBox(modifier = Modifier.width(50.dp).height(24.dp), cornerRadius = 12.dp)
            }
            // Title
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.9f).height(22.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.65f).height(22.dp))
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SkeletonBox(modifier = Modifier.fillMaxWidth().height(20.dp))
                        SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // Description lines
            repeat(4) { i ->
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(if (i == 3) 0.6f else 1f)
                        .height(13.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            // Map placeholder
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(200.dp), cornerRadius = 12.dp)
        }
    }
}

// ── User profile skeleton ─────────────────────────────────────

@Composable
fun UserProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar + name
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SkeletonCircle(size = 80.dp)
            SkeletonBox(modifier = Modifier.width(150.dp).height(18.dp))
            SkeletonBox(modifier = Modifier.width(100.dp).height(13.dp))
        }
        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SkeletonBox(modifier = Modifier.width(40.dp).height(20.dp))
                    SkeletonBox(modifier = Modifier.width(60.dp).height(12.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // Route cards
        repeat(2) {
            RouteCardSkeleton()
        }
    }
}

// ── Explore skeleton ──────────────────────────────────────────

@Composable
fun ExploreSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Regions row placeholder
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                SkeletonBox(modifier = Modifier.width(90.dp).height(90.dp), cornerRadius = 12.dp)
            }
        }
        Spacer(Modifier.height(4.dp))
        // Route cards
        repeat(3) {
            RouteCardSkeleton()
        }
    }
}
