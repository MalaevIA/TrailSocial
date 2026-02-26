package com.trail2.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

object OnboardingKeys {
    val COMPLETED       = booleanPreferencesKey("onboarding_completed")
    val CITY_IDS        = stringPreferencesKey("selected_city_ids")
    val FITNESS_LEVEL   = stringPreferencesKey("fitness_level")
    val INTEREST_IDS    = stringPreferencesKey("selected_interest_ids")
    val DISPLAY_NAME    = stringPreferencesKey("display_name")
    val EMAIL           = stringPreferencesKey("email")
}

@Singleton
class OnboardingRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[OnboardingKeys.COMPLETED] ?: false
    }

    suspend fun saveAnswers(answers: OnboardingAnswers) {
        dataStore.edit { prefs ->
            prefs[OnboardingKeys.CITY_IDS]     = answers.selectedCityIds.joinToString(",")
            prefs[OnboardingKeys.FITNESS_LEVEL] = answers.fitnessLevel?.name ?: ""
            prefs[OnboardingKeys.INTEREST_IDS]  = answers.selectedInterestIds.joinToString(",")
            prefs[OnboardingKeys.DISPLAY_NAME]  = answers.displayName
            prefs[OnboardingKeys.EMAIL]         = answers.email
        }
    }

    suspend fun markCompleted() {
        dataStore.edit { prefs ->
            prefs[OnboardingKeys.COMPLETED] = true
        }
    }

    val savedAnswers: Flow<OnboardingAnswers> = dataStore.data.map { prefs ->
        OnboardingAnswers(
            selectedCityIds     = prefs[OnboardingKeys.CITY_IDS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            fitnessLevel        = prefs[OnboardingKeys.FITNESS_LEVEL]
                                    ?.let { runCatching { FitnessLevel.valueOf(it) }.getOrNull() },
            selectedInterestIds = prefs[OnboardingKeys.INTEREST_IDS]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            displayName         = prefs[OnboardingKeys.DISPLAY_NAME] ?: "",
            email               = prefs[OnboardingKeys.EMAIL] ?: ""
        )
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
