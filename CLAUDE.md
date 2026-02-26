# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.trail2.YourTestClass"

# Run a single test method
./gradlew test --tests "com.trail2.YourTestClass.methodName"

# Run lint
./gradlew lint

# Clean build
./gradlew clean assembleDebug
```

## Required Configuration

Add `YANDEX_MAPKIT_KEY=<your_key>` to `local.properties` (not committed). Without it, the map composables will fail to initialize — the key is injected via `BuildConfig.YANDEX_MAPKIT_KEY` in `App.kt`.

## Critical Package/Path Quirk

**Physical directory:** `app/src/main/java/com/example/trial2/`
**Declared package:** `com.trail2.*` (set in `build.gradle.kts` namespace)

Kotlin does not require the directory path to match the package name. All imports and class references use `com.trail2.*`. Do not "fix" this mismatch.

## Architecture

### Navigation
Navigation uses a **custom state machine** (`mutableStateOf<Screen>`), NOT `NavController`/`NavHost`. The active navigation host is `Navigation.kt` (at package root, `package com.trail2`). There is a dead file at `ui/navigation/Navigation.kt` (`package com.trail2.ui.navigation`) — it is unused and should be ignored.

`AppNavigation` checks onboarding completion via `OnboardingViewModel.isOnboardingCompleted: StateFlow<Boolean?>` (`null` = loading splash, `false` = show onboarding, `true` = show main app). Main app uses `MainAppContent()` with a `Scaffold` + `NavigationBar` over 4 tabs.

### MVVM + Hilt DI
All ViewModels use `@HiltViewModel` + `StateFlow`. DI modules live in `com.trail2.di`:
- `DatabaseModule` — provides `AppDatabase` (Room), `RouteDao`, `UserDao`, `CommentDao`
- `DataStoreModule` — provides `DataStore<Preferences>` for onboarding persistence

### Room Database
Database name: `trail_social.db`. Entities: `UserEntity`, `RouteEntity`, `CommentEntity`.
**Seeding:** On first install (`RoomDatabase.Callback.onCreate`), `DatabaseModule` seeds all tables from `SampleData.kt`.
**Serialization:** `photos` stored as comma-separated string, `tags` as pipe-separated string in `RouteEntity`. Mappers in `data/local/Mappers.kt` handle Entity ↔ Domain conversions.

### Data Flow (Feed example)
`FeedScreen` → `RouteViewModel` → `RouteRepository` → `RouteDao` + `UserDao` (combined via `Flow.combine`) → `Mappers.toDomain()` → UI state via `StateFlow`.

### Onboarding
5-step flow (Welcome → City → Fitness → Interests → Profile) managed by `OnboardingViewModel` + `OnboardingRepository`. Answers persisted to `DataStore<Preferences>`. Completion flag stored in same DataStore; checked on every app launch.

### AI Route Builder
`RouteBuilderScreen` drives a 5-step form (`BuilderStep` enum). `RouteBuilderViewModel` handles step navigation and validation. `RouteBuilderRepository.generateRoute()` is currently a **stub** — no real AI/API call implemented. The generated `GeneratedRoute` is passed directly to `RouteResultScreen` via the custom navigation state machine.

## Known Incomplete Areas

- `RouteRepository.getRouteById()` returns `null` (marked `TODO`) — `RouteDetailScreen` has no live data
- No Retrofit/API layer — all data is local Room (seeded from `SampleData`)
- No real authentication — onboarding saves credentials to DataStore in plaintext
- Yandex MapKit is initialized but map composables are not yet integrated into screens
