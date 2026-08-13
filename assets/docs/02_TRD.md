# 02 — Technical Requirements Document (TRD)

**Product:** LifeIQ — Life Intelligence Quotient (Android)
**Version:** 1.0 (Prototype phase)
**Date:** 10 Aug 2026
**Companion docs:** `01_PRD.md`, `03_Data_Schema.md`, `04_Architecture_Design.md`

---

## 1. Technology Stack

| Layer | Choice | Rationale |
|-------|--------|-----------|
| Language | **Kotlin** (primary); Java-compatible where team prefers | Modern, coroutine-friendly, Google recommended |
| UI | **Jetpack Compose** (primary, Material 3) — XML fallback documented in §3 | Faster iteration for MVPs; XML keeps team comfort |
| Architecture | MVVM + Repository + Use Cases (Clean-lite) | Testable, separates tracking from UI |
| Local DB | **Room** (SQLite) | Offline-first buffer, reactive `Flow` support |
| Cloud DB | **Firebase Cloud Firestore** | Real-time, per-user security rules, batched writes |
| Auth | **Firebase Authentication** (email/password + Google) | Zero-backend auth with session persistence |
| Background | **WorkManager** (sync) + **Foreground Service** (tracking) | Reliable, respects Doze/App Standby |
| Sensors/Usage | `UsageStatsManager`, `SensorManager`, `BroadcastReceiver` | OS-native passive capture |
| DI | **Hilt** (optional in prototype; manual DI acceptable) | Testability; skip if delaying |
| Async | Kotlin Coroutines + Flow | Simple reactive pipelines |
| JSON | kotlinx.serialization / Gson | Export + payloads |
| CSV | OpenCSV or manual writer (UTF-8 BOM) | Excel compatibility |
| Networking | Firebase SDK (no Retrofit needed in prototype; Retrofit reserved for timetable API in Phase 2) | Fewer deps |
| Testing | JUnit4, Mockito, Robolectric, Compose UI test, Firestore Emulator | Fast feedback |

**Build config:** Gradle (Kotlin DSL) • `minSdk 26` • `targetSdk 35` • `compileSdk 35` • AGP 8.x

---

## 2. Module Breakdown (Gradle modules)

```
:app                          — UI (Compose/XML), Activities, Navigation, DI wiring
:data:local                   — Room DB, DAOs, entities, migrations
:data:remote                  — Firebase Auth + Firestore repositories
:domain                       — Use cases, repository interfaces, models
:tracking                     — Foreground service, UsageStats poller, receivers,
                                 step sensor, event emitter
:sync                         — SyncWorker, batch uploader, sync state
:export                       — CSV/JSON writers, filters, file share
:utils                        — Time utils, UUID, logging, prefs
```

Prototype can start as **single-module** and split later; keep packages aligned to the module map above.

---

## 3. UI Decision: Jetpack Compose vs XML

| Criteria | Jetpack Compose | XML + ViewBinding |
|----------|-----------------|-------------------|
| Speed of MVP screens | Faster (declarative) | Slower, more boilerplate |
| Team familiarity | Requires Compose learning | Universal Android skill |
| State handling | Built-in (`State`, `ViewModel`-friendly) | Manual (LiveData/observe) |
| Reusable components | Material 3 themes built-in | XML drawables/styles |
| Testing | Compose UI test | Espresso |

**Decision:** Use **Jetpack Compose (Material 3)** for all new screens. The tracking/service layer is 100% UI-agnostic (works with either). If the agent building the app is more comfortable with XML, the same ViewModels + Repositories plug into XML layouts unchanged — **UI is swappable by design.**

Screens (v0.1): `Login` → `Home` → `Timetable` → `Sessions` → `Attendance` → `Export` → `Settings`. Single-Activity + Compose Navigation (or Navigation Component for XML).

---

## 4. Authentication Design

- `FirebaseAuth` email/password sign-up + sign-in; Google via `CredentialManager` (new) / legacy `GoogleSignInClient`.
- Persistence: Firebase handles token refresh automatically → `onStart` check `currentUser != null` to auto-route to Home.
- On first auth success: create `users/{uid}` profile doc (displayName, email, createdAt, deviceId).
- Auth state observed via `Flow` in a `SessionManager`; logout clears Room data **only after user confirmation** (prototype: keep local data; cloud intact).
- Errors mapped to user-friendly messages (weak password, user exists, wrong credentials, network).

---

## 5. Local Data Layer (Room)

### 5.1 Entities (full schema in `03_Data_Schema.md`)

```kotlin
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val eventId: String,          // UUID — also Firestore doc ID
    val userId: String,
    val deviceId: String,
    val eventType: String,                    // "APP_SESSION", "SCREEN_ON", ...
    val payloadJson: String,                  // flexible fields
    val timestamp: Long,                      // epoch millis UTC
    val schemaVersion: Int,
    val synced: Boolean = false,
    val createdAt: Long
)

@Entity(tableName = "timetable")
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val day: String,                          // MONDAY..FRIDAY
    val slotNo: Int,                          // 1..8
    val startTime: String,                    // "08:10"
    val endTime: String,                      // "09:05"
    val subject: String,                      // "IP", "AISC", ...
    val subjectFull: String,                  // "Internet Programming (IP)"
    val room: String,
    val faculty: String,
    val type: String,                         // LECTURE | LAB | BREAK | LUNCH | TRAINING | MENTORING | OE | TE_HONOR
    val applicable: Boolean = true            // false for TE-Honor/OE if user doesn't take them
)

@Entity(tableName = "sync_log")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val syncedAt: Long,
    val batchSize: Int,
    val success: Boolean,
    val error: String? = null
)
```

### 5.2 DAOs (key queries)

```kotlin
@Dao interface EventDao {
    @Insert suspend fun insert(event: EventEntity)
    @Query("SELECT * FROM events WHERE synced = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsynced(limit: Int): List<EventEntity>
    @Query("UPDATE events SET synced = 1 WHERE eventId IN (:ids)")
    suspend fun markSynced(ids: List<String>)
    @Query("SELECT * FROM events WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp ASC")
    fun getBetween(from: Long, to: Long): Flow<List<EventEntity>>
    @Query("SELECT COUNT(*) FROM events WHERE synced = 0") fun pendingCount(): Flow<Int>
    @Query("DELETE FROM events WHERE synced = 1 AND timestamp < :cutoff") 
    suspend fun pruneSynced(cutoff: Long): Int
}
```

---

## 6. Remote Data Layer (Firestore)

### 6.1 Paths

```
users/{uid}                                    — profile
users/{uid}/timetable                       — one doc, web-dashboard format ({source, generatedAt, batch, entries[]})
users/{uid}/events/{eventId}                   — one doc per event (idempotent)
users/{uid}/export_jobs/{jobId}                — (Phase 2: server-side export)
```

### 6.2 Write strategy

- `SyncWorker` reads up to 500 unsynced events → `FirebaseFirestore` batched `set()` (doc id = eventId) → on full success `markSynced`.
- No `FieldValue.increment` conflicts: each doc is write-once.
- Retry policy: `setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30s, 6h)`.
- Timetable writes: `set` per day-doc (upsert by `day`).

### 6.3 Firestore Security Rules (must be deployed)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isOwner(uid) {
      return request.auth != null && request.auth.uid == uid;
    }
    match /users/{uid} {
      allow read, update, delete: if isOwner(uid);
      allow create: if isOwner(uid) && request.resource.data.createdAt is timestamp;
      match /{document=**} {
        allow read, write: if isOwner(uid);
      }
    }
  }
}
```

---

## 7. Tracking Engine

### 7.1 Foreground service `LifeLensIQTrackerService`

- Type: `Service`, started on app launch + `BOOT_COMPLETED`.
- Notification (required for fg service): "LifeIQ is tracking your digital life" with stop button.
- Foreground type: `FOREGROUND_SERVICE_TYPE_DATA_SYNC` (targetSdk 35).
- Pollers run as coroutines in the service scope:

| Poller | Interval | Source | Emits |
|--------|----------|--------|-------|
| AppUsagePoller | 15 s | `UsageStatsManager.queryEvents()` | `APP_SESSION_START/END` (dedupe via package + time windows) |
| ScreenPoller | event-driven | `ACTION_SCREEN_ON/OFF`, `USER_PRESENT` | `SCREEN_ON/OFF`, `UNLOCK` |
| ChargePoller | event-driven | `ACTION_POWER_CONNECTED/DISCONNECTED` | `CHARGE_START/END` (+ plug type via BatteryManager) |
| StepPoller | 60 s | `Sensor.TYPE_STEP_COUNTER` | `STEPS` (delta) |
| WakeDetector | on screen-on | logic: idle > 5 h since last screen-on | `WAKE_UP` |

### 7.2 Event emission contract

```kotlin
interface EventEmitter {
    suspend fun emit(eventType: String, payload: Map<String, Any?>)
}
// impl: writes EventEntity(timestamp = now, payloadJson = json(payload)) to Room
```

### 7.3 Usage-access UX

- Check `UsageStatsManager.isAppUsageAccessEnabled()` on launch.
- If disabled → rationale dialog → deep link `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
- Home shows a red banner until granted.

---

## 8. Sync Design (WorkManager)

```kotlin
class SyncWorker(ctx, params) : CoroutineWorker(ctx, params) {
    // 1. val events = eventDao.getUnsynced(limit = 500)
    // 2. if empty -> success
    // 3. batch write to Firestore (set, merge = false)
    // 4. eventDao.markSynced(ids)
    // 5. if more pending -> enqueue one-time worker (chain)
}
```

- Schedule: `PeriodicWorkRequest` every 15 min with `NetworkType.CONNECTED`; also one-time on app open (`OneTimeWorkRequest`).
- `ExistingPeriodicWorkPolicy.KEEP` to prevent duplicates.
- Sync status exposed via `SyncLogEntity` + `StateFlow` in `SyncViewModel` for the Home screen.
- Conflict handling: doc id = eventId ⇒ retry-safe. If Firestore rejects (rules/permission), surface actionable error in Settings.

---

## 9. Export Module

### 9.1 CSV writer

- Header row per schema (see `03_Data_Schema.md` §4), events as rows, payload fields flattened to columns (`app_package`, `app_name`, `duration_ms`, …).
- Encoding: **UTF-8 with BOM** (Excel-safe). Delimiter `,`; fields with commas quoted; null → empty.
- Chunked writing (e.g., 5,000 rows per chunk) to bound memory.

### 9.2 JSON writer

- Format: single array `[ {...}, ... ]` plus optional `--ndjson` variant (one event per line — ideal for Python streaming).
- Always includes `schemaVersion`, `exportedAt`, `userId`.

### 9.3 Flow

```
ExportScreen (filters) -> ExportUseCase -> read source (Room | Firestore)
  -> writer (background Dispatchers.IO) -> SAF CreateDocument("lifeiq_export_*.csv|json")
  -> ShareSheet / Open
```

### 9.4 Formats roadmap

| Format | v0.1 | Notes |
|--------|------|-------|
| CSV | ✅ | BOM, Excel-ready |
| JSON / NDJSON | ✅ | Python-ready |
| XLSX | P2 | via Apache POI or CSV→XLSX conversion |
| Parquet | P2 | for direct ML ingestion |

---

## 10. Timetable Integration (with the agent's fetcher)

**Contract between `fetch_timetable.py` output and the app:**

1. Fetcher produces `timetable_personalized.json` (schema in `03_Data_Schema.md` §5) — user imports it in-app (SAF file picker) **or** it is bundled at build time for the prototype.
2. App validates JSON against schema (required fields, day/slot ranges), shows a preview summary ("37 lectures, 5 labs, 2 training slots, 3 marked not-applicable"), then saves to Room + Firestore.
3. Phase 2: fetcher runs server-side (Cloud Function or cron) publishing to Firestore; app reads directly.

**Schema versioning:** `timetableSchemaVersion: 1` in the JSON; app rejects unknown versions with a clear message.

---

## 11. Error Handling & Edge Cases

| Case | Handling |
|------|----------|
| Firestore quota/offline | Events remain local; sync retries; UI shows pending count |
| Duplicate event on retry | `set(eventId)` is idempotent |
| App killed mid-write | Room transactions (`@Transaction`) atomic; sync runs next cycle |
| Device reboots | `BOOT_COMPLETED` restarts service + reschedules WorkManager |
| Usage access revoked | Poller emits `TRACKING_PAUSED` event; Home banner shown |
| Sensor permission denied | Steps silently disabled (feature flag in settings) |
| Export file locked | SAF creates a new document; retry on error with toast |
| Timetable import invalid JSON | Validation errors shown per-field |

---

## 12. Performance & Battery Budget

- Polling cost: AppUsage 15 s queries are cheap (~0.2% battery/day estimate); steps 60 s.
- **No** `PARTIAL_WAKE_LOCK`; rely on WorkManager for sync (Doze-safe).
- Export: bound chunk size; stream writes; cancellable.
- Database: indices on `(synced, timestamp)`, `(timestamp)`; WAL mode; monthly prune of synced events > 90 days (setting).

---

## 13. Testing Strategy

| Level | Scope | Tools |
|-------|-------|-------|
| Unit | DAOs (Room in-memory), export writers, wake detector logic, timetable validator | JUnit4, Robolectric |
| Repository | Sync upsert/idempotency against **Firestore Emulator** | Firebase Emulator Suite |
| Integration | Service + WorkManager with test dispatchers | WorkManager Testing |
| UI | Login flow, import, export share, attendance taps | Compose UI test / Espresso |
| Manual (acceptance) | 3-day continuous tracking on physical device; airplane-mode sync test; Excel/Python open test of exports | Device lab |

---

## 14. Firebase Project Setup Checklist

- [ ] Create Firebase project `lifeiq-<owner>` (Spark plan for prototype).
- [ ] Enable **Authentication** → Email/Password + Google.
- [ ] Enable **Cloud Firestore** (production mode), deploy rules from §6.3.
- [ ] Add Android app (package `com.lifelensiq.app`), download `google-services.json` → `app/`.
- [ ] Add Gradle plugins: `com.google.gms.google-services`, `com.google.firebase.crashlytics` (optional).
- [ ] Install **Firebase Emulator Suite** locally for sync tests.
- [ ] Set Firestore indexes: `users/{uid}/events` on `timestamp` (single-field default is fine; composite only if date+type filters grow).

---

## 15. Definition of Done (technical)

For every feature: implementation ✅ → unit tests ✅ → manual device test ✅ → event schema documented ✅ → sync verified in Firestore ✅ → export verified (CSV in Excel, JSON in Python) ✅.
