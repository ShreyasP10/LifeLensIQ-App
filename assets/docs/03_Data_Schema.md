# 03 — Data Schema Document

**Product:** LifeIQ
**Version:** 1.0
**Date:** 10 Aug 2026
**Covers:** Firestore collections • Room tables • Event payload schemas • CSV/JSON export formats • Timetable import JSON contract

---

## 1. Naming & Versioning Conventions

- All timestamps: **epoch milliseconds UTC** (`Long`). UI converts to local.
- Every event includes `schemaVersion` (int). Breaking payload changes increment it; exporters include it.
- IDs: `eventId` = UUID v4 string; Firestore document ID **equals** eventId (idempotent writes).
- Device ID: generated once per install (`UUID`), stored in prefs; included in every event.

---

## 2. Firestore Schema

### 2.1 Collection: `users/{uid}` — profile document

```json
{
  "userId": "firebase-uid",
  "email": "user@mail.com",
  "displayName": "Dinesh",
  "deviceId": "5f3a...c9",
  "timetableSchemaVersion": 1,
  "createdAt": "2026-08-10T06:30:00Z",
  "lastSeenAt": "2026-08-10T14:05:00Z"
}
```

### 2.2 Document: `users/{uid}/timetable` — single doc, shared web + app format

The app writes ONE doc in the **web dashboard's schema** (`LifeLensIQ-Web
docs/09_Firestore_Schema.md §5`) so the same timetable renders in both the
dashboard and the app:

```json
{
  "source": "LifeLens IQ Android app",
  "generatedAt": 1789034400000,
  "updatedAt": 1789034400000,
  "batch": "B1",
  "entries": [
    {
      "day": "MONDAY",
      "slotNo": 1,
      "startTime": "08:10",
      "endTime": "09:05",
      "subject": "IP",
      "subjectFull": "Internet Programming (IP)",
      "room": "201/202/203/204",
      "faculty": "AAG/RRK",
      "type": "LECTURE",
      "applicable": true
    }
  ]
}
```

`type` enum: `LECTURE | LAB | BREAK | LUNCH | TRAINING | MENTORING | OE | TE_HONOR | FREE`
`applicable: false` → slot hidden for this user (e.g., TE-Honor, OE when not taken).

### 2.3 Collection: `users/{uid}/events/{eventId}` — one doc per event

Unified envelope shared with the **web dashboard** (`LifeLensIQ-Web
docs/09_Firestore_Schema.md §3`): web fields (`id, ts, endTs, durationSeconds,
domain, path, title, category, eventType, device, metadata`) plus the app's
own identifiers. `category` maps Android events onto the web's 9-category
vocabulary (see `WebCategoryMapper.kt`), so app and browser events aggregate
together in dashboard scores, heatmaps and ML exports.

```json
{
  "id": "3f2a9c81-...",
  "eventId": "3f2a9c81-...",
  "userId": "firebase-uid",
  "deviceId": "5f3a...c9",
  "device": "android",
  "eventType": "APP_SESSION",
  "schemaVersion": 1,
  "ts": 1789034400000,
  "endTs": 1789034700000,
  "durationSeconds": 300,
  "timestamp": 1789034400000,
  "category": "Timepass",
  "domain": "com.instagram.android",
  "path": "com.instagram.android",
  "title": "Instagram",
  "metadata": { "...": "type-specific, see §3" }
}
```

### 2.4 (Phase 2) Collection: `users/{uid}/daily_summaries/{yyyy-mm-dd}`

Aggregated daily rollups (screen time, study time, attendance %, top apps) — reduces query cost and feeds ML directly. **Prototype stores raw events only.**

---

## 3. Event Type Payload Schemas

### `CLASS_ATTENDANCE`
```json
{ "slotRef": { "day": "TUESDAY", "slotNo": 7 },
  "subject": "IP",
  "status": "ATTENDED | SKIPPED | ONLINE | LATE",
  "markedAt": 1789034800000 }
```

### `STUDY_SESSION`
```json
{ "subject": "IP",
  "startedAt": 1789034800000,
  "endedAt": 1789040200000,
  "durationMs": 5400000,
  "locationType": "CLASS | HOME | LIBRARY",
  "linkedSlot": { "day": "TUESDAY", "slotNo": 7 },
  "notes": "" }
```

### `APP_SESSION`
```json
{ "packageName": "com.instagram.android",
  "appName": "Instagram",
  "startedAt": 1789034800000,
  "endedAt": 1789035400000,
  "durationMs": 600000,
  "category": null }
```
`category` reserved (null in prototype; ML fills later).

### `SCREEN_ON` / `SCREEN_OFF` / `UNLOCK`
```json
{ "reason": "MANUAL | NOTIFICATION | CALL", "precedingIdleMs": 120000 }
```
`precedingIdleMs` = time since last screen-off (enables `WAKE_UP` inference client-side).

### `CHARGE_START` / `CHARGE_END`
```json
{ "plugType": "USB | AC | WIRELESS", "batteryPct": 42 }
```

### `STEPS`
```json
{ "stepDelta": 120, "cumulativeSteps": 3421 }
```

### `WAKE_UP`
```json
{ "sleptFromMs": 1789020000000, "sleptToMs": 1789034800000, "durationMs": 14800000 }
```

### `SYNC_STATUS`
```json
{ "pendingLocal": 0, "batchUploaded": 500, "syncedAt": 1789035000000 }
```

### `TRACKING_STATE`
```json
{ "state": "STARTED | STOPPED | PAUSED", "reason": "PERMISSION_REVOKED | BOOT" }
```

---

## 4. Room (Local) Schema — summary

| Table | Key fields | Purpose |
|-------|-----------|---------|
| `events` | eventId PK, eventType, payloadJson, timestamp, synced | Offline buffer + query source |
| `timetable` | id PK, day, slotNo, start/end, subject, room, faculty, type, applicable | Offline schedule |
| `sync_log` | id PK, syncedAt, batchSize, success, error | Sync history UI |

Room stores `payloadJson` (serialized payload); Firestore stores the structured `metadata` map. Exporters read either.

---

## 5. Timetable Import JSON Contract (from `fetch_timetable.py`)

**File:** `timetable_personalized.json` — generated by the agent's fetcher (already built; verify output matches).

```json
{
  "timetableSchemaVersion": 1,
  "source": "TE-B.pdf",
  "class": "TE-Div:B",
  "academicYear": "2026-2027 (SH 2026-ODD SEM)",
  "personalization": {
    "batch": "B1",
    "elective": "IP",
    "teHonorTaken": false,
    "oeTaken": false
  },
  "days": [
    {
      "day": "MONDAY",
      "slots": [
        { "slotNo": 1, "start": "08:10", "end": "09:05", "subject": "IP",
          "subjectFull": "Internet Programming (IP)", "room": "201/202/203/204",
          "faculty": "AAG/RRK", "type": "LECTURE", "applicable": true },
        { "slotNo": 2, "start": "09:05", "end": "10:00", "subject": "AISC",
          "subjectFull": "Artificial Intelligence & Soft Computing", "room": "203",
          "faculty": "MVC", "type": "LECTURE", "applicable": true },
        { "slotNo": 3, "start": "10:20", "end": "11:15", "subject": "CN-Lab",
          "subjectFull": "Computer Network Lab", "room": "314/316/307",
          "faculty": "SHM-SMP", "type": "LAB", "applicable": true, "batch": "B1" },
        { "slotNo": 8, "start": "15:35", "end": "16:30", "subject": null,
          "subjectFull": "FREE (no TE-Honor)", "room": "", "faculty": "",
          "type": "FREE", "applicable": true }
      ]
    }
  ]
}
```

**Validation rules (app-side):**
- `timetableSchemaVersion` must equal 1 (else reject with message).
- day ∈ {MONDAY..FRIDAY}; slotNo 1..8; start < end; times HH:mm 24h.
- At most one slot per (day, slotNo).
- `type` must be in enum; `applicable=false` entries still stored but hidden in UI and excluded from attendance prompts.
- Personalization block is informational; the fetcher output already resolves batch/elective/not-applicable.

---

## 6. Export Formats

### 6.1 CSV (`lifeiq_export_YYYYMMDD_HHMMSS.csv`)

- UTF-8 with BOM; header row:
```csv
schemaVersion,eventId,userId,deviceId,eventType,timestamp_utc_ms,timestamp_iso_local,payload_json,app_package,app_name,duration_ms,subject,status,step_delta,battery_pct,plug_type,notes
```
- `payload_json` column carries the raw payload; convenience columns (`app_package`…) are **flattened copies** of the most-used fields.
- Rows sorted by `timestamp_utc_ms` ascending.
- Specialized export modes (P1): `events.csv`, `timetable.csv`, `attendance.csv`, `sessions.csv` (one file per type, cleaner for ML).

### 6.2 JSON / NDJSON (`lifeiq_export_YYYYMMDD_HHMMSS.json`)

```json
{
  "exportedAt": "2026-08-10T14:05:00Z",
  "schemaVersion": 1,
  "userId": "firebase-uid",
  "deviceId": "5f3a...c9",
  "filters": { "from": null, "to": null, "eventTypes": ["APP_SESSION"] },
  "source": "LOCAL | CLOUD",
  "events": [ { ...full event object incl. payload... } ],
  "timetable": [ { ...day docs... } ],
  "count": 1234
}
```

NDJSON variant: same fields but events printed one JSON object per line (streaming-friendly).

---

## 7. Indexes & Query Patterns

| Query | Path | Index |
|-------|------|-------|
| Unsynced batch | Room `events` | `(synced, timestamp)` |
| Date-range export | Room `events` / Firestore `events` | `timestamp` (Firestore single-field auto-index) |
| Today's slots | Room `timetable` / Firestore `timetable` | `day` (in-app filter is fine; < 10 docs/day) |
| Pending count (UI) | Room | none (COUNT query) |

Firestore composite indexes needed only if "date range + eventType" becomes common — add then, not now.

---

## 8. Migration & Versioning Policy

- Room: `@Database(version = 1)`; prototype uses `fallbackToDestructiveMigration()` (no production users yet); v1.0 production will define migrations.
- Event `schemaVersion`: increment when payload fields change meaning. Exporters must tolerate older versions (read best-effort, keep `payload_json`).
- Firestore doc schema: additive changes only (new optional fields). Breaking changes → new collection version suffix (e.g., `events_v2`) rather than in-place migration.
