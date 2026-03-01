# PulaSurveySync — Survey Response Sync Engine

Offline-first Android app for field survey data collection with a robust sync engine. Built with Kotlin, Jetpack Compose, Room, and Coroutines.

## Features

- **Offline storage**: Surveys with nested/repeating sections persisted locally via Room
- **Partial sync**: Individual response uploads with per-item success/failure tracking
- **Network degradation handling**: Early-stop after consecutive connectivity failures
- **Concurrent sync prevention**: Mutex-based single-sync guarantee
- **Error classification**: Retryable (server error, timeout) vs fatal (client error 4xx)
- **Live sync progress**: Real-time bottom sheet showing upload-by-upload status
- **Storage management**: Age-based cleanup of synced responses

## Architecture

```
┌──────────────────────────────────────────────┐
│                   UI Layer                    │
│  Dashboard │ Survey List │ Detail │ Sync Sheet│
│         Compose + ViewModel + StateFlow       │
├──────────────────────────────────────────────┤
│                 Sync Layer                    │
│  SyncEngine │ NetworkClassifier │ StorageManager│
│           Coroutines + Mutex + SharedFlow      │
├──────────────────────────────────────────────┤
│                Domain Layer                   │
│  SurveyResponse │ SyncStatus │ SyncError      │
│           SurveyRepository (interface)         │
├──────────────────────────────────────────────┤
│                 Data Layer                    │
│  Room (DAO + Entities) │ FakeApiService        │
│      SurveyMapper │ SurveyRepositoryImpl       │
└──────────────────────────────────────────────┘
```

## Package Structure

```
com.pula.survey.sync/
├── domain/model/      — SurveyResponse, SurveyAnswer (sealed), SyncStatus, SyncError, SyncResult
├── domain/repository/ — SurveyRepository interface, StorageStats, SurveyResponseSummary
├── data/local/        — Room database, entities, DAOs (claim pattern), TypeConverters
├── data/remote/       — SurveyApiService interface, FakeSurveyApiService
├── data/mapper/       — SurveyMapper (entity ↔ domain)
├── data/repository/   — SurveyRepositoryImpl
├── sync/              — SyncEngine, NetworkClassifier, StorageManager
├── di/                — AppContainer (manual DI)
├── testdata/          — TestDataGenerator (East African sample data)
└── ui/                — Compose screens (Dashboard, SurveyList, SurveyDetail, SyncBottomSheet)
```

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

**Requirements**: Android Studio Ladybug+, JDK 17+, Android SDK 36

## Run Tests

```bash
# Unit tests (SyncEngine, NetworkClassifier, FakeApi, StorageManager, Repository)
./gradlew testDebugUnitTest

# Instrumented tests (Room DAO tests — requires device/emulator)
./gradlew connectedDebugAndroidTest
```

## Demo Usage

1. Open the app → Dashboard shows "0 PENDING / 0 SYNCED / 0 FAILED"
2. Tap **"Generate 10 Surveys"** → 10 surveys with realistic East African farmer data appear as PENDING
3. Tap **"Sync Now"** (FAB) → Sync bottom sheet shows live progress
4. Watch responses upload one by one with success/failure indicators
5. Tap **"View All Surveys"** → filterable list with status chips
6. Tap any survey card → detail view with nested farm data and attachments

The FakeSurveyApiService has a 300ms delay per upload and occasional simulated failures to demonstrate partial sync and error handling.

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| No SYNCING in DB | Avoids "stuck SYNCING after crash" — items stay PENDING until upload result is known |
| Claim pattern (optimistic lock) | Prevents double-upload if eligible list is stale; future-proof for WorkManager coexistence |
| Early-stop threshold = 2 | Single timeout can be transient; 2 consecutive connectivity failures = strong signal network is down |
| JSON answers in Room column | O(1) retrieval of complete response; arbitrary nesting depth; no complex joins |
| Manual DI (AppContainer) | Avoids annotation processor overhead; flat dependency graph is explicit and testable |

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed decision records.
=======
# PulaSurveySync
Take-Home Assignment for Pula Advisors - Senior Mobile App Developer