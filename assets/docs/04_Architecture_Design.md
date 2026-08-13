# 04 — System Architecture Design

**Product:** LifeIQ
**Version:** 1.0
**Date:** 10 Aug 2026
**Companion docs:** `02_TRD.md`, `03_Data_Schema.md`

---

## 1. Architecture Overview

Offline-first, event-sourced architecture. Every observable thing (screen, app, class, session) produces an **immutable event** written to a local Room buffer; a background worker syncs the buffer to Firestore. Export reads either source on demand.

```
┌────────────────────────────────── ANDROID APP ──────────────────────────────────┐
│                                                                                  │
│  ┌────────────── UI (Jetpack Compose / XML) ──────────────┐                      │
│  │  Login │ Home │ Timetable │ Sessions │ Attendance │    │                      │
│  │  Export │ Settings                                      │                      │
│  └───────────────▲────────────────────────────────────────┘                      │
│                  │ StateFlow / Compose state                                     │
│  ┌───────────────┴─────────────── MVVM ──────────────────┐                       │
│  │ ViewModels ──────► Use Cases ─────► Repository (iface)│                       │
│  └───────────────▲───────────────────────┬───────────────┘                       │
│                  │                       │                                       │
│  ┌───────────────┴────────┐   ┌──────────▼────────────────┐                      │
│  │  TRACKING ENGINE       │   │  DATA LAYER              │                      │
│  │  (Foreground Service)  │   │  EventRepository          │                      │
│  │   AppUsagePoller (15s) │──▶│   ├─ Local: Room          │                      │
│  │   Screen/Charge Rx     │   │   └─ Remote: Firestore    │                      │
│  │   StepPoller (60s)     │   └──────────────▲────────────┘                      │
│  │   WakeDetector         │                  │                                   │
│  └───────────▲────────────┘   ┌──────────────┴────────────┐                      │
│              │                │  SYNC ENGINE             │                      │
│   BOOT_COMPLETED              │  SyncWorker (WorkManager)│──► Firebase Auth      │
│   Receivers                   │  Batch uploader          │──► Firestore         │
│                               └──────────────────────────┘                      │
└──────────────────────────────────────────────────────────────────────────────────┘
         │                                              ▲
         │ timetable JSON (from fetch_timetable.py)     │ export files (SAF/Share)
         ▼                                              │
   fetch_timetable.py (agent program)      CSV / JSON / (P2: XLSX, Parquet)
         │
         ▼
   TE-B.pdf (source of truth)
```

**Key principle:** all components communicate through the **EventRepository** — UI and tracking never touch Firebase directly.

---

## 2. Layers

| Layer | Contents | Rules |
|-------|----------|-------|
| **Presentation** | Compose/XML screens, ViewModels | No business logic; maps state → UI |
| **Domain** | Use cases, repository interfaces, models | Pure Kotlin, no Android/Firebase imports |
| **Data** | Room DAOs, Firestore repos, sync, tracking emitters | Implements domain interfaces; owns concurrency |
| **Infrastructure** | Firebase SDK, WorkManager, SAF, Sensors | Swappable (emulator/local server in tests) |

---

## 3. Component Detail

### 3.1 Tracking Engine (foreground service)

- `LifeLensIQTrackerService` — long-running, `START_STICKY`, own coroutine scope (`SupervisorJob` + `Dispatchers.IO`), notification with stop action.
- Emitters write through `EventEmitter` → `EventRepository.emit()` → Room insert (`@Transaction`).
- **AppUsagePoller**: every 15 s query `UsageStatsManager.queryEvents(now-15s, now)`; track current foreground package; when package changes or poll gap closes, close previous `APP_SESSION` and open new one. Filters out launcher/system packages (configurable allow-list/deny-list).
- **Screen/Charge receivers**: dynamically registered in service; also registered in a `<receiver>` for boot-time state.
- **WakeDetector**: on `SCREEN_ON`, if `precedingIdleMs > 5h` → emit `WAKE_UP` with sleep window.
- **StepPoller**: register `TYPE_STEP_COUNTER` listener; every 60 s compute delta; degrade silently if permission missing.

### 3.2 EventRepository (single source of truth)

```kotlin
class EventRepository(
    private val local: EventLocalDataSource,   // Room
    private val remote: EventRemoteDataSource  // Firestore
) : Repository {
    suspend fun emit(eventType: String, payload: Map<String, Any?>)  // write-through local
    suspend fun getUnsynced(limit: Int) = local.getUnsynced(limit)
    suspend fun syncBatch(events: List<EventEntity>): SyncResult    // remote.upload(events) + markSynced
    fun observeEvents(from: Long, to: Long): Flow<List<EventEntity>>
    fun observePendingCount(): Flow<Int>
    suspend fun exportTo(source: ExportSource, filter: ExportFilter, writer: ExportWriter)
}
```

### 3.3 Sync Engine

- Periodic `SyncWorker` (15 min, `NetworkType.CONNECTED`) + one-time worker on app foreground.
- Batch size 500; loops until queue empty (chains itself via `OneTimeWorkRequest`).
- Failure → exponential backoff (30 s → 6 h). Success → `markSynced` + optional prune.
- `SyncLogEntity` written each run → surfaced on Home ("Last synced 2 min ago · 0 pending").

### 3.4 Export Module

- `ExportUseCase.execute(filter, format, destination)`:
  1. Read events (Room by default; Firestore option for full history).
  2. Stream to writer (CSV with BOM / JSON / NDJSON).
  3. SAF `CreateDocument` → `Uri` → ShareSheet.
- Runs on `Dispatchers.IO`; progress via callback/Flow; cancellable.
- Timetable + profile included in JSON export; CSV specialized modes (P1).

### 3.5 Timetable Module

- `TimetableImporter.validate(json)` → preview → `save(entities)` (Room + Firestore upsert per day).
- `TodayUseCase` computes current/next slot from `timetable` table + `System.currentTimeMillis()`.
- `AttendanceSuggester` (P1): WorkManager check hourly near slot start → notification with quick actions (Attended/Skipped).

---

## 4. Concurrency & Threading

| Concern | Mechanism |
|---------|-----------|
| DB writes | Room suspend functions (IO pool) |
| Polling loop | Service scope coroutine, `delay(15s)` between polls |
| Sync | WorkManager (its own executor) |
| Export | `Dispatchers.IO`, chunked |
| UI updates | ViewModel `StateFlow` → Compose collectAsState |
| Serialization | kotlinx.serialization (IO-safe) |

No global mutable singletons except scoped service/application-level `ServiceLocator` (or Hilt).

---

## 5. Lifecycle & Start/Stop Strategy

```
App install → first run → Auth → permission onboarding (usage access, notifications)
      │
      ▼
startLifeiqTracker()  ──►  Foreground service starts
      │                        ▲
      │   reboot (BOOT_COMPLETED receiver)
      └──► sync enqueue (WorkManager) ◄── every 15 min / app foreground
      │
      ▼
Logout → confirm dialog → stop service, keep local data (privacy note shown)
```

- Service stops only on explicit logout or user toggle in Settings ("Pause tracking").
- WorkManager persists across reboots automatically (no BOOT handling needed for sync).

---

## 6. Dependencies (initial)

```
androidx.core, lifecycle-viewmodel-compose, activity-compose, compose ui/material3
room-runtime/ktx (2.6+), room-compiler
firebase-auth, firebase-firestore, google-services plugin
androidx.work:work-runtime-ktx
kotlinx-coroutines-android, kotlinx-serialization-json
androidx.documentfile (SAF)
test: junit, mockito, robolectric, work-testing, firebase-emulator-client
(optional) hilt-android, hilt-work
```

---

## 7. Failure & Degradation Matrix

| Failure | Degraded behavior |
|---------|-------------------|
| Firebase unreachable | Local-only mode; banner "offline — will sync later"; export still works (Room) |
| Usage access revoked | `TRACKING_STATE(PAUSED)` event; banner; service keeps running (screen/charge only) |
| Sensor missing | Steps off; UI hides step stats |
| Storage full | Export fails with message; Room prune on next sync |
| Crash of service | START_STICKY restarts; WorkManager re-schedules; missing intervals are visible in data (time gaps) — acceptable for prototype |
| Time wrong/clock jump | Events keep device timestamp; `clockSkew` not addressed in prototype (documented limitation) |

---

## 8. Testing Architecture

- **Firestore Emulator** mirrors production rules in CI/local tests (sync idempotency, rules rejection).
- **WorkManager test dispatcher** drives `SyncWorker` deterministically.
- **Fake emitters** simulate 10k events for export benchmarks and DB soak tests.
- UI tests with Compose test rule; navigation smoke tests for all 7 screens.

---

## 9. Security Model

| Asset | Protection |
|-------|-----------|
| Auth | Firebase Auth (email + Google); token auto-refresh |
| Firestore | Rules = owner-only (`request.auth.uid == uid`); no public reads |
| Local DB | Room unencrypted (prototype); SQLCipher option in v1 if required |
| Export files | App-internal → user-shared via SAF; never uploaded anywhere |
| Secrets | Only `google-services.json` (per-project); no keys in code or repo (it's git-ignored) |
| Network | TLS enforced by Firebase SDK |
| Privacy | No content capture; minimal permissions; clear onboarding rationale |
