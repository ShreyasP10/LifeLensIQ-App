# 01 — Product Requirements Document (PRD)

**Product:** LifeIQ — Life Intelligence Quotient
**Version:** 1.0 (Prototype phase)
**Date:** 10 Aug 2026
**Owner:** Dinesh B Pawar (TE Computer Engineering, Div B, Batch B1)
**Status:** Draft for review

---

## 1. Executive Summary

LifeIQ is an Android application that **passively and actively collects a student's daily life data** — timetable (class schedule), study sessions, phone usage, attendance patterns and productivity metrics — and securely stores **every event** in Firebase. The user can **export all data in CSV / JSON** formats at any time.

The prototype exists for one purpose: **gather a rich, real personal dataset** that will be used to train the final ML model (the "Life Intelligence Quotient" score and its predictive insights). The prototype is intentionally MVP-first; after the data pipeline is proven, the app is evolved into a proper production application.

---

## 2. Product Vision

> "Every minute of my life becomes a data point. LifeIQ learns my patterns, scores my life balance, and tells me how to live smarter — starting with the timetable I already follow."

### 2.1 Goals (Prototype)

| # | Goal | Success metric |
|---|------|----------------|
| G1 | Capture timetable + activity data continuously without manual effort | ≥ 90% of scheduled events auto-captured |
| G2 | Every captured event persisted to Firebase | 0 data loss after sync; 100% of local events eventually consistent |
| G3 | User can download their full dataset | Export produces valid CSV & JSON files, usable in Excel/Python |
| G4 | Dataset ready for ML training | ≥ 4 weeks of continuous data; labeled classes for attendance/study/phone-use |

### 2.2 Non-Goals (Prototype)

- No ML inference inside the app (model runs offline/on notebook in Phase 2).
- No social features, no third-party data, no advertising.
- No web dashboard (later phase).
- No iOS/desktop support.

---

## 3. Personas

| Persona | Who | Needs |
|---------|-----|-------|
| **P1 — The Builder (primary)** | Dinesh, TE student | Own data for ML; verify pipeline works; realistic feature coverage |
| **P2 — The Beta Tester** | Classmates (B1 batch, IP elective) | Simple onboarding, visible value (statistics), data privacy trust |
| **P3 — Future End User** | Any student | Score + personalized recommendations (Phase 2) |

---

## 4. Scope

### 4.1 In Scope (Prototype — v0.1)

1. Firebase Authentication (email/password + Google Sign-In).
2. Timetable module: import timetable (from `fetch_timetable.py` JSON output), store per-user, show "today's schedule".
3. Passive collection:
   - Foreground app usage (UsageStatsManager) → app sessions.
   - Screen on/off events.
   - Charging events.
   - Step count (optional, requires sensor permission).
4. Active collection:
   - Study session tracking (manual start/stop or auto-prompt during class slots).
   - Class attendance marking (auto-suggested from timetable; user confirms).
5. Offline-first local storage (Room) + background sync (WorkManager) to Firestore.
6. Export: CSV and JSON of **all** stored data (filterable by date range/type).
7. Minimal UI (Login, Home/Dashboard, Timetable, Sessions, Export, Settings).

### 4.2 Out of Scope (v0.1)

- ML inference / LifeIQ score computation in-app.
- Location tracking, call/SMS logs, notification scraping.
- Cloud functions / analytics dashboards.
- Multi-device merge logic (single device per account is acceptable in prototype).

---

## 5. Functional Requirements

### FR-1 Authentication

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.1 | User can register with email + password via Firebase Auth | P0 |
| FR-1.2 | User can sign in with Google account | P1 |
| FR-1.3 | Session persists across app restarts; auto re-login | P0 |
| FR-1.4 | Sign-out clears local user data (or keeps with confirm dialog) | P0 |
| FR-1.5 | On first login, app asks to import timetable JSON (from agent's fetcher output) or fetch later | P0 |
| FR-1.6 | Invalid credentials show clear error messages; loading states on buttons | P0 |

### FR-2 Timetable Module

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.1 | Import timetable from JSON file produced by `fetch_timetable.py` (validate against schema, show summary before saving) | P0 |
| FR-2.2 | Store timetable entries: day, slot, time range, subject, room, faculty, type (Lecture/Lab/Break/Training/Mentoring/OE/TE-Honor) | P0 |
| FR-2.3 | Mark entries as "not applicable" for this user (e.g., TE-Honor, OE) at import time — the personalized output of the fetcher already handles this | P0 |
| FR-2.4 | "Today" screen shows current/next class based on device time; highlights ongoing slot | P0 |
| FR-2.5 | Weekly timetable view (Mon–Sat) with slots from the PDF (8:10–9:05 … 3:35–4:30 + breaks) | P1 |
| FR-2.6 | Sync timetable to Firestore per user (`users/{uid}/timetable`) | P0 |
| FR-2.7 | Detect conflicts if timetable re-imported (same day+slot) — overwrite with confirmation | P1 |

### FR-3 Passive Data Collection (Phone Usage)

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.1 | Foreground app changes captured (package, app name, start/end time) via UsageStatsManager polling (~15 s) | P0 |
| FR-3.2 | Screen ON / OFF events captured with timestamps | P0 |
| FR-3.3 | Device unlock (`USER_PRESENT`) events | P1 |
| FR-3.4 | Charging start/end (connected/disconnected) + charger type | P1 |
| FR-3.5 | Step counter deltas (requires Activity Recognition permission; graceful degradation if denied) | P2 |
| FR-3.6 | All events written **immediately** to Room with `synced=false` | P0 |
| FR-3.7 | No raw content captured (no message text, no URLs, no photos) — metadata only | P0 |
| FR-3.8 | Background collection survives reboot: auto-start via BOOT_COMPLETED receiver + persistent notification | P0 |

### FR-4 Study & Attendance Tracking

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.1 | Auto-suggest "Start study session" when a class slot begins (notification) | P1 |
| FR-4.2 | Manual study session: start/stop with subject tag (maps to timetable slot if inside slot time) | P0 |
| FR-4.3 | Attendance: at class start, user confirms "Attended / Skipped / Online"; stored with slot reference | P0 |
| FR-4.4 | Each session stores: subject, start, end, duration, location type (class/home), notes (optional free text) | P0 |
| FR-4.5 | Sessions and attendance entries are separate event types in storage | P0 |

### FR-5 Local Storage & Sync

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-5.1 | Room database stores all events offline-first | P0 |
| FR-5.2 | Every event carries: eventId (UUID), userId, deviceId, eventType, payload JSON, timestamp | P0 |
| FR-5.3 | SyncWorker (WorkManager, network-constrained) uploads unsynced events in batches (≤ 500 per batch) | P0 |
| FR-5.4 | Sync retries with backoff; no event lost on failure (stays local) | P0 |
| FR-5.5 | Idempotent writes: Firestore doc ID = eventId, so retries never duplicate | P0 |
| FR-5.6 | UI shows last-sync time and pending count | P0 |
| FR-5.7 | Sync also runs on-demand when user opens the app (if connected) | P1 |
| FR-5.8 | Optional "sync on Wi-Fi only" setting (default: any network) | P1 |

### FR-6 Export & Download

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-6.1 | Export all user data as **CSV** (comma-separated, UTF-8 with BOM for Excel) | P0 |
| FR-6.2 | Export as **JSON** (single array / NDJSON option) | P0 |
| FR-6.3 | Export source: local Room (default, works offline) with option to fetch full cloud history | P0 |
| FR-6.4 | Filters: date range (from/to), event type, subject | P1 |
| FR-6.5 | Export writes file to app-specific external storage / Downloads and offers Share sheet | P0 |
| FR-6.6 | Files named `lifeiq_export_YYYYMMDD_HHMMSS.csv|.json` | P0 |
| FR-6.7 | Export progress indicator for large datasets; runs on background thread | P0 |
| FR-6.8 | Future formats reserved: XLSX, Parquet (P2) | P2 |

### FR-7 Dashboard / UI

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-7.1 | Login screen (email/password, Google button) | P0 |
| FR-7.2 | Home: today's schedule (next class), today's stats (screen time, study time, attendance %), sync status | P0 |
| FR-7.3 | Timetable screen (weekly grid) | P1 |
| FR-7.4 | Sessions screen (list + start/stop FAB) | P0 |
| FR-7.5 | Attendance screen (today's classes, tap to mark) | P0 |
| FR-7.6 | Export screen (format, filters, export button) | P0 |
| FR-7.7 | Settings: account, permissions status, sync policy, delete local/cloud data, logout | P0 |
| FR-7.8 | UI built with **Jetpack Compose** (primary) or **XML** (fallback) — decision in TRD | P0 |

### FR-8 Data Management

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-8.1 | Delete local data (confirm dialog, keeps cloud data) | P1 |
| FR-8.2 | Delete cloud data for user (Firestore rules allow user to delete own documents; UI confirmation) | P1 |
| FR-8.3 | Account deletion via Firebase (optional, P2) | P2 |

---

## 6. Data Requirements (summary — details in `03_Data_Schema.md`)

| Event type | Captured fields | Feeds ML feature |
|------------|-----------------|------------------|
| `class_started` / `class_attended` | slot ref, subject, attendance status, timestamp | Attendance rate |
| `study_session` | subject, start, end, duration, type | Study hours |
| `app_session` | package, app name, start, end, duration | Phone usage / distraction |
| `screen_on` / `screen_off` | timestamp | Wake cycles, screen time |
| `charge_start` / `charge_end` | timestamp, charger type | Device patterns |
| `steps` | timestamp, step delta | Physical activity |
| `sync_status` | timestamp, pending count | Data completeness |

---

## 7. Non-Functional Requirements

| ID | Requirement | Target | Priority |
|----|-------------|--------|----------|
| NFR-1 | Battery impact | < 2% per day (polling intervals, no wakelocks) | P0 |
| NFR-2 | Data usage | Batched sync; compression optional; Wi-Fi-only mode | P1 |
| NFR-3 | Storage footprint | Local DB < 100 MB; auto-prune synced events older than 90 days (configurable) | P1 |
| NFR-4 | Reliability | Foreground service + WorkManager retry; events never silently dropped | P0 |
| NFR-5 | Security | HTTPS only (Firebase); Firestore rules restrict every path to owner `uid`; no secrets in code | P0 |
| NFR-6 | Privacy | Metadata only; explicit permission rationale; user controls deletion | P0 |
| NFR-7 | Performance | UI responsive: data ops on background threads; export < 10 s for 100k events | P1 |
| NFR-8 | Compatibility | minSdk 26 (Android 8.0), targetSdk 35 (Android 15); foreground service types declared | P0 |
| NFR-9 | Localization | English only (prototype) | P2 |
| NFR-10 | Offline | Full app function without network; sync resumes automatically | P0 |

---

## 8. Permissions Required

| Permission | Purpose | When requested |
|------------|---------|----------------|
| `PACKAGE_USAGE_STATS` | Foreground app tracking (special access — deep link to Settings) | Onboarding flow with explanation |
| `ACTIVITY_RECOGNITION` | Step counter | Onboarding (optional; skip allowed) |
| `POST_NOTIFICATIONS` | Persistent service notification + session suggestions (Android 13+) | Onboarding |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Background collection/sync | Declared in manifest |
| `RECEIVE_BOOT_COMPLETED` | Restart tracking after reboot | Declared in manifest |
| Storage/Downloads | Export files (use MediaStore / SAF — no legacy storage permission needed) | At export time via SAF |

---

## 9. User Stories (top 12)

1. As a user, I sign up with my email once, and I'm logged in forever after.
2. As a user, I import my timetable JSON and immediately see today's schedule.
3. As a user, LifeIQ automatically records which apps I use and when — I do nothing.
4. As a user, I get a "class starts soon" notification and one-tap attendance marking.
5. As a user, I can start/stop a study session even outside class time.
6. As a user, my data uploads to the cloud silently whenever I have internet.
7. As a user, I can download everything as CSV to open in Excel.
8. As a user, I can download everything as JSON to feed to Python/ML scripts.
9. As a user, I can filter an export to last month only.
10. As a user, I can see if the background tracker is running (persistent notification).
11. As a user, I can delete all my cloud data with one confirmed tap.
12. As a user, the app works fully offline and syncs later.

---

## 10. Acceptance Criteria (v0.1 prototype demo)

- [ ] Auth: register → login → auto-relogin after app kill. (FR-1.x)
- [ ] Timetable: import `fetch_timetable.py` JSON (personalized B1/IP version) → weekly grid renders correctly → documents saved to Firestore. (FR-2.x)
- [ ] Tracking: after granting usage access, `app_session` events appear in Room within 60 s and in Firestore after next sync. (FR-3.x, FR-5.x)
- [ ] Attendance: marking attended/absent for today's slots persists and syncs. (FR-4.x)
- [ ] Export: CSV opens in Excel with columns matching schema; JSON parses with `json.load()`; ≥ 1,000 events exported. (FR-6.x)
- [ ] Offline test: airplane mode → track events → enable network → all pending events appear in Firestore with no duplicates. (FR-5.x)
- [ ] Security: unauthenticated Firestore access to any `/users/{uid}/...` path is rejected. (NFR-5)

---

## 11. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Firestore write cost grows with event volume | Cost | Batched writes, daily summaries instead of raw events for high-frequency types (v1) |
| UsageStatsManager delayed/fake data on some OEMs | Data quality | Note device model in events; accept limitation in prototype |
| Background restrictions on Android 13–15 | Data loss | Foreground service + `BOOT_COMPLETED`; document user steps for OEM battery exemptions |
| Battery drain complaints from testers | Adoption | Conservative polling; show battery impact in Settings |
| ML needs labels it doesn't have | Model quality | Attendance + session events ARE the labels — design them explicitly (see `05_ML_Data_Strategy.md`) |
| Schema changes mid-collection | Pipeline breakage | Version every event type in schema; export includes `schemaVersion` field |

---

## 12. Open Questions

1. Should step data be P0 or P2? (Default: P2, requires `ACTIVITY_RECOGNITION`.)
2. Export from cloud ("full history even after local prune") — needed for prototype? (Default: yes, if small dataset; else local-only.)
3. Timetable import UX: file picker of JSON vs paste text vs automatic fetch from a hosted API? (Default: file picker; hosted API is Phase 2.)
