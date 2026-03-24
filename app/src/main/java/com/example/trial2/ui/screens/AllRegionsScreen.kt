package com.trail2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.R
import com.trail2.data.RegionInfo
import com.trail2.ui.viewmodels.ExploreViewModel

private val allRegionsColors = listOf(
    "2D6A4F", "E76F51", "457B9D", "E63946",
    "264653", "6D6875", "2A9D8F", "F4A261",
    "A8DADC", "E9C46A", "52B788", "774936"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRegionsScreen(
    regions: List<RegionInfo>,
    regionColors: List<String>,
    onBack: () -> Unit,
    vm: ExploreViewModel = hiltViewModel()
) {
    val colors = regionColors.ifEmpty { allRegionsColors }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.explore_all_regions), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(regions.size) { i ->
                RegionCard(
                    region = regions[i],
                    colorHex = colors[i % colors.size],
                    onClick = {
                        vm.searchByRegion(regions[i].name)
                        onBack()
                    }
                )
            }
        }
    }
}
