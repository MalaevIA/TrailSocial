package com.trail2.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Расширение для Context — создаёт единый DataStore для всего приложения
val Context.onboardingDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "onboarding_prefs")

// ─────────────────────────────────────────────
// Ключи хранилища
// ─────────────────────────────────────────────
object OnboardingKeys {
    val COMPLETED       = booleanPreferencesKey("onboarding_completed")
    val CITY_IDS        = stringPreferencesKey("selected_city_ids")    // JSON-строка через запятую
    val FITNESS_LEVEL   = stringPreferencesKey("fitness_level")           // имя enum
    val INTEREST_IDS    = stringPreferencesKey("selected_interest_ids")  // через запятую
    val DISPLAY_NAME    = stringPreferencesKey("display_name")
    val EMAIL           = stringPreferencesKey("email")
    // Пароль в DataStore не храним — только в памяти до отправки на сервер
}

// ─────────────────────────────────────────────
// Репозиторий
// ─────────────────────────────────────────────
class OnboardingRepository(private val context: Context) {

    /** true, если онбординг уже пройден (показываем основное приложение) */
    val isOnboardingCompleted: Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[OnboardingKeys.COMPLETED] ?: false
    }

    /** Сохраняет все данные опроса локально */
    suspend fun saveAnswers(answers: OnboardingAnswers) {
        context.onboardingDataStore.edit { prefs ->
            prefs[OnboardingKeys.CITY_IDS]      = answers.selectedCityIds.joinToString(",")
            prefs[OnboardingKeys.FITNESS_LEVEL]  = answers.fitnessLevel?.name ?: ""
            prefs[OnboardingKeys.INTEREST_IDS]   = answers.selectedInterestIds.joinToString(",")
            prefs[OnboardingKeys.DISPLAY_NAME]   = answers.displayName
            prefs[OnboardingKeys.EMAIL]          = answers.email
        }
    }

    /** Помечает онбординг как пройденный */
    suspend fun markCompleted() {
        context.onboardingDataStore.edit { prefs ->
            prefs[OnboardingKeys.COMPLETED] = true
        }
    }

    /** Читает сохранённые ответы (нужно, например, для экрана профиля при повторном входе) */
    val savedAnswers: Flow<OnboardingAnswers> = context.onboardingDataStore.data.map { prefs ->
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
        context.onboardingDataStore.edit { it.clear() }
    }
    // ─────────────────────────────────────────
    // ОТПРАВКА НА СЕРВЕР (закомментировано — нет бэкенда)
    // ─────────────────────────────────────────
    /*
    suspend fun sendToServer(answers: OnboardingAnswers): Result<Unit> {
        return try {
            val body = RegisterRequest(
                displayName  = answers.displayName,
                email        = answers.email,
                password     = answers.password,
                cityIds      = answers.selectedCityIds,
                fitnessLevel = answers.fitnessLevel?.name,
                interestIds  = answers.selectedInterestIds
            )
            val response = ApiService.create().register(body)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Сервер вернул ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Пример Retrofit-интерфейса:
    // interface ApiService {
    //     @POST("api/auth/register")
    //     suspend fun register(@Body body: RegisterRequest): Response<Unit>
    //
    //     companion object {
    //         fun create(): ApiService = Retrofit.Builder()
    //             .baseUrl("https://your-backend.com/")
    //             .addConverterFactory(GsonConverterFactory.create())
    //             .build()
    //             .create(ApiService::class.java)
    //     }
    // }
    //
    // data class RegisterRequest(
    //     val displayName: String,
    //     val email: String,
    //     val password: String,
    //     val cityIds: List<String>,
    //     val fitnessLevel: String?,
    //     val interestIds: List<String>
    // )
    */
}
