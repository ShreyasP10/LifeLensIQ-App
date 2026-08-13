# 05 — ML Data Collection Strategy

**Purpose:** Ensure the prototype collects a dataset that is actually *usable* for the final ML model (the "Life Intelligence Quotient" — scoring and predicting academic/life outcomes).

**Date:** 10 Aug 2026

---

## 1. The ML Problem

LifeIQ is a **multi-task supervised learning system** over daily life streams:

| Task | Type | Target variable (label) |
|------|------|--------------------------|
| LifeIQ score | Regression | Daily score 0–100 |
| Attendance prediction | Classification | Will I attend tomorrow's IP class? (binary) |
| Study-time prediction | Regression | Hours studied today |
| Distraction detection | Classification | Is this app session productive or distracting? |
| Sleep/wake inference | Sequence | Sleep windows from screen events |
| Schedule adherence | Regression | % of planned slots executed |

**Data source:** exclusively the events collected by this app. No third-party data.

---

## 2. Feature → Event Mapping

| ML Feature | Event types feeding it | Collected by |
|------------|------------------------|--------------|
| Academic engagement | `CLASS_ATTENDANCE`, `STUDY_SESSION`, `WAKE_UP` on class days | App (active) |
| Phone usage / distraction | `APP_SESSION`, `SCREEN_ON/OFF` | App (passive) |
| Screen / digital habits | `SCREEN_ON/OFF`, `UNLOCK`, `CHARGE_*` | App (passive) |
| Physical activity | `STEPS` | App (optional sensor) |
| Schedule adherence | `timetable` (planned) vs `CLASS_ATTENDANCE` (actual) | App + timetable import |
| Time-of-day patterns | timestamps of all events | App |
| Data completeness | `SYNC_STATUS`, `TRACKING_STATE` (used as meta-features / quality flags) | App |

---

## 3. Labels — the critical design decision

**Rule: every label must be an explicit event, not inferred later.**

- Attendance (Attended/Skipped/Online/Late) is a **user-confirmed label** at class time → this is your training target.
- Study sessions (start/stop + subject) are user-confirmed → regression target.
- Everything else (screens, apps) is **unlabeled context** that will be combined with the labels above.

**Implications for the app (must implement):**
1. Attendance prompts must be **easy** (one tap) — high label coverage beats fancy UX.
2. Session start/stop must be **fast** (FAB on Home + notification action).
3. Add a **daily review** (P1): at 9 PM notification asking "Did you study IP today? How many hours? 0/1/2/3+?" — captures labels for days with missed sessions.
4. Optional **ground-truth diary** (P2): daily mood/productivity slider 1–10 → regression target for wellness models.

**Label quality metrics (track during prototype):**
- Attendance label coverage: % of class slots with a status → target ≥ 80%.
- Session label coverage: ≥ 1 study session/day on ≥ 80% of days.

---

## 4. Dataset Targets (prototype run)

| Metric | Target |
|--------|--------|
| Collection duration | ≥ 4 weeks continuous |
| Class slots captured | ≥ 25 lecture slots (your real weekly schedule: IP ×3, AISC, SE, CN, MDM, IKS lectures) |
| Attendance labels | ≥ 80% of slots |
| Study sessions | ≥ 80 sessions |
| App sessions | ≥ 20,000 events (usage stats are naturally high-volume) |
| Days with complete sync | ≥ 90% of days (verify via `SYNC_STATUS`) |

**Dataset size estimate:** 4 weeks × ~1,000 events/day ≈ **28k events** ≈ 20–40 MB CSV — ideal for a single-person dataset, easy to export, split, and train.

---

## 5. Data Pipeline (post-export)

```
Firestore (cloud copy)  ──manual export──►  CSV / JSON / NDJSON
        │                                        │
        └──(optional: cloud function zip export)─┘
                                                  ▼
                                    Python pipeline (your notebook)
                                    ├─ load (pandas / jsonlines)
                                    ├─ clean (drop device artifacts, dedupe by eventId)
                                    ├─ feature-engineer (per-day rollups:
                                    │    screen time, study time, attendance %,
                                    │    top apps, distraction minutes, wake time,
                                    │    sleep hours, class adherence)
                                    ├─ label-merge (daily review + attendance)
                                    └─ train (LightGBM / XGBoost / LSTM) + validate
```

**Recommended workflow for YOU (the only user of the prototype):**
1. Run app daily; check "pending sync = 0" before bed.
2. Weekly export CSV + JSON to Google Drive.
3. Keep one `data/` folder with dated exports (never overwrite).

---

## 6. Feature Engineering Blueprint (for the final model)

Per-day feature vector (41 features — example):

```
[ weekday, isExamWeek,
  wakeTime, sleepTime, sleepHours, wakeCount,
  totalScreenMs, totalScreenPctOfDay,
  appSessionCount, avgSessionLenMs, top1AppTimeMs, top2AppTimeMs, top3AppTimeMs,
  socialTimeMs, videoTimeMs, chatTimeMs (by package groups),
  distractionMinutes (reels/shorts packages),
  classCountPlanned, classCountAttended, attendanceRate,
  studyMinutes, studySessionCount, longestStudyBlockMs,
  afternoonStudyMs, eveningStudyMs, lateNightStudyMs,
  chargeCount, batteryLowEvents,
  stepsDelta, workoutDays,
  syncCompleteness, trackingActiveHours ]
```

Label examples: `dailyScore` (manual diary), `attendedTomorrow_IP` (classification target), `studyHours`.

---

## 7. Anti-Patterns to Avoid (data-quality killers)

| Trap | Consequence | Mitigation |
|------|-------------|------------|
| Importing raw `payload_json` into models | Leakage / feature explosion | Always feature-engineer to per-day rollups |
| Using attendance as both feature and label | Target leakage | Features built from *yesterday & before*; label is *today* |
| Missing timestamps (device clock changes) | Broken ordering | Store epoch UTC; check monotonicity in cleaning script |
| Low label coverage | Small effective dataset | Daily review prompt + one-tap UI (see §3) |
| Syncing only raw events, no summaries | Firestore bloat/cost | Keep raw for prototype; add `daily_summaries` in v1 |

---

## 8. ML Milestones (after prototype data collection)

1. **M1 — Exploratory (week 5):** load exports, plot screen-time vs study-time, verify label coverage, fix collection gaps.
2. **M2 — Baseline (week 6):** LightGBM on daily rollups predicting next-day attendance and study hours (train/val split by date; last 20% days = val).
3. **M3 — Score (week 7):** define LifeIQ formula (weighted: academic 40%, focus 25%, health 20%, consistency 15% — example), calibrate with self-reported daily score.
4. **M4 — App integration (Phase 2):** export `daily_score` events back to the app so users see their LifeIQ trend.

---

## 9. Legal & Ethical Notes

- You are collecting **your own data** in the prototype — no consent issues. Tester data (P2 persona) requires explicit consent + opt-out + delete-all. Store a `consentVersion` field in the profile doc when testers join.
- Keep exports private (don't push `data/` to public repos).
- If the app later ships publicly: on-device inference preferred; Firebase export must be user-initiated only (already the design).
