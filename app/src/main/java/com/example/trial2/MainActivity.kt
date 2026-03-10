package com.trail2

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.ui.theme.VerstaTheme
import com.trail2.ui.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* результат не блокирует приложение — карта просто не покажет локацию */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val isDarkTheme by settingsVm.isDarkTheme.collectAsStateWithLifecycle()
            val language by settingsVm.language.collectAsStateWithLifecycle()

            val context = LocalContext.current
            val localizedContext = remember(language) {
                val locale = Locale(language)
                Locale.setDefault(locale)
                val config = android.content.res.Configuration(context.resources.configuration).apply {
                    setLocale(locale)
                }
                val localizedResources = context.createConfigurationContext(config).resources
                // ContextWrapper preserves the Activity chain (needed by Hilt)
                // while overriding getResources() for localized strings
                object : android.content.ContextWrapper(context) {
                    override fun getResources(): android.content.res.Resources = localizedResources
                }
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                VerstaTheme(darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }
}
