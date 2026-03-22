# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Install on device/emulator
./gradlew test                   # Run unit tests
./gradlew test --tests "com.trail2.YourTestClass"          # Single test class
./gradlew test --tests "com.trail2.YourTestClass.method"   # Single test method
./gradlew lint                   # Run lint
./gradlew clean assembleDebug    # Clean build
```

## Required Configuration

Add to `local.properties` (not committed):
```
YANDEX_MAPKIT_KEY=<your_key>
```
The key is injected via `BuildConfig.YANDEX_MAPKIT_KEY` in `App.kt`. Base URL for API is set in `build.gradle.kts` as `BuildConfig.BASE_URL` (currently `http://10.0.2.2:8000/api/v1/`).

Photo URLs are constructed via `routePhotoUrl(path)` in `ui/util/PhotoUtils.kt`, which strips `api/v1/` from `BASE_URL` and prepends the server root.

## Critical Package/Path Quirk

**Physical directory:** `app/src/main/java/com/example/trial2/`
**Declared package:** `com.trail2.*` (set in `build.gradle.kts` namespace + applicationId)

Kotlin does not require the directory path to match the package name. All imports and class references use `com.trail2.*`. Do **not** "fix" this mismatch.

## Architecture

### Tech Stack
- Kotlin + Jetpack Compose (Material 3), Compile/Target SDK 36, Min SDK 26
- Hilt 2.52 + KSP for DI, Retrofit + OkHttp + Kotlinx Serialization for networking
- Yandex MapKit 4.8.1-full, DataStore Preferences, EncryptedSharedPreferences
- MVVM: all ViewModels use `@HiltViewModel` + `StateFlow`

### Navigation
Custom state machine (`mutableStateOf<Screen>`) in `Navigation.kt` (package `com.trail2`), **NOT** `NavController`/`NavHost`. There is a dead file at `ui/navigation/Navigation.kt` — it is unused.

All `Screen` subtypes:
```
Feed, Explore, AIRoute, Profile, Login, RouteCreate, Notifications, Settings, AdminPanel
RouteDetail(routeId), RouteEdit(routeId), AIRouteResult(route), UserProfile(userId),
FollowList(userId, type), RouteMapPicker
```

`AppNavigation` checks auth state: `isOnboardingCompleted` (`null` = splash, `false` = onboarding, `true` = check login). `MainAppContent()` renders a `Scaffold` + `NavigationBar` with 4 tabs (Feed, Explore, AI-Route, Profile). Non-tab screens are rendered with early `return` before the Scaffold.

`RouteBuilderViewModel` is hoisted to `MainAppContent` level so Navigation can call `resetFully()` when leaving RouteResultScreen. Route map data is passed via private `RouteMapData` held in `MainAppContent` state, not via the `Screen` sealed class.

### Authentication
JWT-based auth with encrypted token storage:
- `TokenManager` — stores access/refresh tokens in `EncryptedSharedPreferences`, exposes `isLoggedIn: StateFlow<Boolean>`
- `AuthInterceptor` — adds `Authorization: Bearer` header (skips `/auth/*` endpoints)
- `TokenAuthenticator` — on 401, refreshes token via `POST auth/refresh` (up to 2 retries), clears tokens on failure
- `AuthRepository` — `signup()`, `login()`, `logout()` — saves tokens via `TokenManager`
- `AuthViewModel` — drives `LoginScreen` (login/signup tab switcher, delegates to `AuthRepository`)

### Data Layer (API-only, no local DB)
All data flows through Retrofit. Room was removed — there is no local database.

**DI modules** (`com.trail2.di`):
- `NetworkModule` — OkHttp (30s connect, 120s read, 60s write), Retrofit, all API interfaces
- `AuthModule` — `TokenManager`
- `DataStoreModule` — `DataStore<Preferences>` for onboarding/settings

**API interfaces** in `data/remote/api/`: `AuthApi`, `RouteApi`, `UserApi`, `CommentApi`, `NotificationApi`, `AiRouteApi`, `UploadApi`, `ReportApi`, `AdminApi`

**Error handling**: `ApiResult<T>` sealed class (`Success`/`Error`/`NetworkError`) + `safeApiCall()` wrapper that parses error JSON `detail` field (handles both string and array forms).

**DTO ↔ Domain mapping** in `data/remote/mappers/DtoMappers.kt`. Domain models in `data/Models.kt`. Paginated responses use `PaginatedResponseDto<T>` → `PaginatedResponse<T>` (fields: `items`, `total`, `page`, `pageSize`, `pages`).

**Coordinate convention**: GeoJSON uses `[lng, lat]`, Yandex MapKit `Point(lat, lng)`. Conversions are explicit throughout.

**Image upload**: `UploadRepository.uploadImage(file)` → `POST upload/image` (multipart). Returns a server-relative path; use `routePhotoUrl(path)` to build the full URL for display.

### Admin & Moderation
- `AdminApi` / `AdminRepository` — list users (with search + active filter), ban/unban, delete route/comment
- `ReportApi` / `ReportRepository` — create report (`target_type`: `route`/`comment`/`user`, `reason`, optional `description`), list reports (filter by status/target_type), update report status
- `AdminViewModel` — drives `AdminPanelScreen`
- `UserProfileViewModel` — drives `UserProfileScreen`, combines `UserRepository`, `RouteRepository`, `ReportRepository`, `AdminRepository`
- `ReportDialog` composable (`ui/components/ReportDialog.kt`) — reusable modal for filing reports
- `RouteMapView` composable (`ui/components/RouteMapView.kt`) — reusable Yandex MapView with route polyline, start/end markers, and intermediate dot markers; takes GeoJSON `[lng, lat]` pairs; used in `RouteDetailScreen` and `RouteResultScreen`

### AI Route Builder
All AI-route classes live in the `ai_route/` package (not `ui/viewmodels/`):
- `RouteBuilderModels.kt` — form enums (`TripPurpose`, `TripDuration`, `TripDistance`, `Terrain`, `GroupType`, `Pace`), `RouteBuilderForm`, `GeneratedRoute`, `RoutePoint`, and `DEMO_ROUTE` (offline fallback constant)
- `RouteBuilderViewModel` — hoisted to `MainAppContent` level (see Navigation section)
- `RouteBuilderRepository` — async polling logic

Async generation with polling:
1. `RouteBuilderScreen` — 5-step form (`BuilderStep` enum: GOAL → PARAMS → TERRAIN → DETAILS → EXTRAS)
2. `RouteBuilderViewModel` — step navigation, validation, generation trigger, poll-count progress tracking
3. `RouteBuilderRepository.generateRoute(form, onPollProgress)`:
   - `POST ai/generate-route` → receives `task_id`
   - Polls `GET ai/tasks/{taskId}` every 2 seconds (timeout: 5 minutes)
   - `status: "completed"` → returns `GeneratedRoute`, `"failed"` → returns error
4. `RouteResultScreen` + `RouteResultViewModel` — displays result with `RoutePointCard`, publishes route via `RouteRepository.createRoute()`, save/bookmark after publish

### Manual Route Creation
Two-screen flow with waypoint support:
1. `RouteCreateScreen` — form (title, description, region, difficulty, tags, photos) + map preview + waypoint list
2. `RouteMapPickerScreen` — interactive map for building routes:
   - **Tap map** to add waypoints → naming dialog (title + optional description)
   - **Address search** via Yandex `SuggestSession` → tap suggestion to add as waypoint
   - `PedestrianRouter` builds walking route between points (fallback: straight-line + Haversine)
   - MapKit route callbacks are dispatched to the main thread via `Handler(Looper.getMainLooper())`
   - Undo/clear controls, distance + point count display

Data flow: `RouteMapPickerScreen` → `onRouteSelected` callback → `Navigation.kt` `RouteMapData` → `RouteCreateViewModel.setRouteCoordinates()` → `submit()` converts `WaypointEntry` to `WaypointDto` and sends to API.

### Walking Navigation
`RouteDetailScreen` includes a "Start Walking" button that opens a full-screen map overlay:
- GPS `UserLocationLayer` with heading tracking
- Route polyline + start/finish markers
- Camera auto-fits to route bounds
- State managed via `RouteDetailViewModel.isNavigating`

### Yandex MapKit Patterns
- **MapKit lifecycle**: `MapKitFactory.getInstance().onStart()/onStop()` must be called — required even for Search/Suggest API without a MapView (see `CitySurveyScreen`, `RouteMapPickerScreen`)
- **SuggestSession**: `searchManager.createSuggestSession()` → `suggest(query, boundingBox, options, listener)` — listener takes `SuggestResponse`, not a list
- **PedestrianRouter**: `requestRoutes()` with `RequestPoint` (first/last = `WAYPOINT`, middle = `VIAPOINT`). Always pass `RouteOptions(FitnessOptions())`, **never** `RouteOptions()` — the default constructor leaves `fitnessOptions` as Java `null`, causing a JNI `GetBooleanField` crash in the native binding.
- **Map markers**: Custom bitmap via `ImageProvider.fromBitmap()` on placemarks
- **Polyline style**: 5px stroke `#2D6A4F` (forest green) + 2px white outline

### Localization
Full ru/en support. Default locale is Russian (`values/strings.xml`), English in `values-en/strings.xml`. All screens use `stringResource()`. Locale switching in `MainActivity` via `ContextWrapper` pattern that preserves the Activity context chain for Hilt compatibility.

Date formatting: `formatDate(isoString)` in `ui/util/DateUtils.kt` — returns relative text ("сегодня"/"yesterday"/etc.) or locale-formatted date.

### Settings
`SettingsScreen` + `SettingsViewModel` — handle locale switching (ru/en via `MainActivity` restart with `ContextWrapper`), account info display, and logout. Locale preference is persisted in `DataStore<Preferences>`.

### Onboarding
5-step flow (Welcome → City → Fitness → Interests → Profile) managed by `OnboardingViewModel` + `OnboardingRepository`. Persisted to `DataStore<Preferences>` with `SharingStarted.Eagerly` (not `WhileSubscribed` — that caused data loss).

City selection uses Yandex Suggest for worldwide search + preset popular cities.

### Theme Colors
Defined in `ui/theme/Theme.kt`, import as needed — do not use raw hex literals:
`ForestGreen` (#2D6A4F), `MossGreen` (#52B788), `SageGreen` (#95D5B2), `PaleGreen` (#D8F3DC), `EarthBrown` (#774936), `WarmSand` (#E9C46A), `StoneGray` (#495057), `DarkForest` (#1B4332), `CreamWhite` (#FFFBF5).

### Key Enums
- `Difficulty`: EASY, MODERATE, HARD, EXPERT — used in feed filters and route creation
- `RouteStatus`: DRAFT, PRIVATE, PUBLISHED
- `NotificationType`: NEW_FOLLOWER, ROUTE_LIKE, NEW_COMMENT
- `FollowListType`: FOLLOWERS, FOLLOWING — used by `FollowListScreen`

### Common Pitfalls
- `SearchFactory.initialize()` does not exist in MapKit 4.8.1 — search is auto-initialized
- `TransportFactory.initialize()` does not exist — transport is auto-initialized by `MapKitFactory.initialize()`
- `SuggestListener.onResponse` takes `SuggestResponse`, not `MutableList<SuggestItem>`
- `RouteOptions()` leaves `fitnessOptions = null` → JNI crash; always use `RouteOptions(FitnessOptions())`
- MapKit route/suggest callbacks arrive on a background thread — dispatch state updates via `Handler(Looper.getMainLooper()).post { }`
- Race conditions in StateFlow `.update {}`: always read current state inside the lambda (e.g., `state.route?.let { current -> ... }` not a captured variable)
- Screens using `AnimatedContent` in navigation are recreated on every visit — use `LaunchedEffect(Unit)` for data reload
