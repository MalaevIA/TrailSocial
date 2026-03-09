package com.trail2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trail2.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val isDarkTheme by vm.isDarkTheme.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Appearance section ──
            SettingsSectionHeader(stringResource(R.string.settings_appearance))

            // Dark theme toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.DarkMode,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_dark_theme), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (isDarkTheme) stringResource(R.string.settings_dark_on) else stringResource(R.string.settings_dark_off),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { vm.setDarkTheme(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = ForestGreen)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── Language section ──
            SettingsSectionHeader(stringResource(R.string.settings_language))

            val languages = listOf(
                "ru" to stringResource(R.string.settings_lang_ru),
                "en" to stringResource(R.string.settings_lang_en)
            )

            languages.forEach { (code, label) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setLanguage(code) },
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Language,
                            contentDescription = null,
                            tint = if (language == code) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            label,
                            fontSize = 15.sp,
                            fontWeight = if (language == code) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (language == code) ForestGreen else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        RadioButton(
                            selected = language == code,
                            onClick = { vm.setLanguage(code) },
                            colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ── About section ──
            SettingsSectionHeader(stringResource(R.string.settings_about))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("TrailSocial", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.settings_version), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = ForestGreen,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
