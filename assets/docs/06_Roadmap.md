# 06 — Development Roadmap

**Product:** LifeIQ
**Version:** 1.0
**Date:** 10 Aug 2026

---

## 1. Phase Overview

```
Phase 0  Prototype (v0.1)      — data collection MVP          [now → ~5 weeks]
Phase 1  Validation + polish   — 4-week data run, pipeline   [weeks 5–9]
Phase 2  Production v1.0       — proper app, summaries, UX   [after ML baseline]
Phase 3  ML integration        — score in-app, insights      [M3/M4 from 05]
```

---

## 2. Phase 0 — Prototype (v0.1) — 5 weeks

### Week 1 — Project setup + Auth
- [ ] Android Studio project, Gradle Kotlin DSL, minSdk 26 / targetSdk 35.
- [ ] Firebase project + `google-services.json` + Firestore rules deployed.
- [ ] Auth screens (email/password + Google), session persistence, profile doc on first login.
- [ ] Navigation skeleton (7 screens), Material 3 theme.
- **Acceptance:** login → logout → relogin works on device.

### Week 2 — Timetable module + Tracking engine (screen/app)
- [ ] Timetable import (SAF JSON from `fetch_timetable.py`), validator, preview, save to Room + Firestore.
- [ ] Weekly grid UI + "today" screen with current/next slot.
- [ ] Foreground service skeleton + persistent notification + BOOT_COMPLETED receiver.
- [ ] `UsageStatsManager` poller → `APP_SESSION` events; screen on/off receivers.
- **Acceptance:** app sessions appear in Room within 60 s; timetable grid matches `TE-B_Timetable_Detailed.html`.

### Week 3 — Active tracking + Attendance
- [ ] Attendance screen (today's slots → one-tap Attended/Skipped/Online/Late).
- [ ] Study sessions (FAB start/stop, subject tag, optional notes).
- [ ] Charge events + wake-up detector (+ steps if permission granted).
- **Acceptance:** attendance and sessions sync and appear in exports.

### Week 4 — Sync + Export
- [ ] `SyncWorker` (15 min periodic + on-open), batched Firestore uploads, pending count, sync log.
- [ ] Export screen: CSV (BOM) + JSON/NDJSON, date/type filters, SAF save + share.
- [ ] Settings: permissions status, sync policy (Wi-Fi only), delete local/cloud, logout.
- **Acceptance:** airplane-mode test passes (0 loss, 0 duplicates).

### Week 5 — Hardening + 4-week data run begins
- [ ] Battery optimization pass (polling intervals, no wakelocks).
- [ ] Crash handling (Crashlytics optional), error toasts, edge cases from TRD §11.
- [ ] Unit + integration tests (Room, sync via emulator, exporters).
- [ ] **GO/NO-GO:** daily review prompt (P1) if label coverage low in first 3 days.
- **Acceptance:** 3-day continuous soak on personal device; Firestore shows all events.

---

## 3. Phase 1 — Validation Run (4 weeks)

- [ ] Collect ≥ 4 weeks of labeled data; export weekly to `data/` archive.
- [ ] Weekly quality check (ML doc §3 metrics: label coverage ≥ 80%, sync completeness ≥ 90%).
- [ ] Fix collection gaps discovered; version any schema changes.
- [ ] Build feature-engineering notebook (ML doc §6) — run on first 2 weeks as dry run.
- **Exit criteria:** dataset targets from ML doc §4 met.

---

## 4. Phase 2 — Production v1.0

- [ ] Room migrations (no destructive fallback), SQLCipher option.
- [ ] `daily_summaries` rollups; Firestore composite indexes; batched cost controls.
- [ ] `daily_score` ingestion (model output fed back to app).
- [ ] Timetable hosted API (fetcher as Cloud Function) → auto-refresh, no manual import.
- [ ] Accessibility Service extension (reels/photo-view events — FR P2 features).
- [ ] Beta testers (B1 batch): consent flow, onboarding polish, A/B of session UX.
- [ ] XLSX/Parquet export (P2 formats).

---

## 5. Phase 3 — ML Integration

- [ ] On-device or cloud inference (TFLite / ML Kit / server API).
- [ ] Home dashboard: LifeIQ score trend, "what changed today" insights.
- [ ] Predictive alerts ("You'll likely skip IP tomorrow — it's 1 AM and you've been on reels 2h").

---

## 6. Milestone Summary

| Milestone | Date target | Exit criteria |
|-----------|-------------|---------------|
| M0 — Prototype v0.1 usable | End week 5 | All Phase-0 acceptance items ✅ |
| M1 — Data run complete | End week 9 | 4 weeks labeled data, exports archived |
| M2 — ML baseline | End week 10 | LightGBM baselines for 2 tasks |
| M3 — LifeIQ score defined | End week 11 | Formula calibrated vs self-reports |
| M4 — Score in app | Phase 2 | `daily_score` events visible in UI |

---

## 7. Resource Notes

- **Single developer** (owner) + agent assistance on `fetch_timetable.py` (done) and app scaffolding.
- **Costs:** Firebase Spark (free) sufficient for prototype (~28k docs/mo). Upgrade to Blaze only for Phase-2 cloud functions.
- **Time budget per week:** ~6–10 h (prototype pace) unless deadlines change.

---

## 8. Open Decisions Log

| # | Decision | Owner | Status |
|---|----------|-------|--------|
| 1 | Compose vs XML final call | Agent + owner | Default: Compose (UI layer swappable) |
| 2 | Steps P0 vs P2 | Owner | Default: P2 |
| 3 | Cloud-export option needed? | Owner | Default: local-only for v0.1 |
| 4 | Daily review prompt in v0.1? | Owner | Default: yes (label coverage) |
| 5 | Hilt DI now or manual | Agent | Default: manual for prototype |
