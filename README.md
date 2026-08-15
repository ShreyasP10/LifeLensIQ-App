# LifeLens IQ — Understand your time. Improve your life.

Android app for the LifeLens IQ project (see `assets/docs/` for PRD/TRD/schema/architecture/ML strategy).
LifeLens IQ passively records **app usage, screen time, pickups, steps, charging and study sessions**,
shows your day in charts and insights, and syncs bidirectionally with the LifeLens IQ **web dashboard** —
both clients read and write the same Firestore collection, so app + website data are one dataset.

**Stack:** Kotlin • Jetpack Compose (Material 3) • Room • WorkManager • Firebase Auth + Firestore • kotlinx.serialization

## Features

**Tracking (passive, no manual work)**
- App usage sessions per app, screen on/off/unlock, charging, steps (Activity Recognition),
  Reels & Shorts counts (optional Accessibility heuristic), wake detection
- Adaptive polling (15 s screen-on / 60 s screen-off) with a full **night pause (00:00–06:00)**;
  step events are batched (10 min flush) to save battery
- **Focus mode**: block chosen apps while studying — opening one pulls you back to a full-screen
  notice; ending focus writes a `STUDY_SESSION` (`locationType: FOCUS`)

**Home**
- Today's stats (study, screen, shorts, steps, sessions, pending sync)
- **Wake & sleep card**: pickups (every wake/shutdown counted), first wake, last shutdown,
  sleep estimate from last shutdown → first wake
- Daily-goal progress rings, weekly bar chart, best-day highlight
- **Productivity calendar** (GitHub-style heatmap) mirroring the website, fed by app + web data

**Activity** — today's time by category (Study / DSA / Development / Productivity / Entertainment /
Timepass / Short-form Video / Utilities / Other), donut chart, top apps, per-category drill-down,
per-app **category overrides**, and an **All / This device / Website** filter.

**Sessions** — live timer, **Focus mode**, **quick log** (add finished sessions manually),
30-day history, end-of-session summary.

**Trends** — screen time and steps over 1D / 7D / 30D / 1Y (daily or monthly buckets),
**this month vs last month** comparison, **charging discipline** (sessions, avg duration, overnight).

**Notifications** — daily summary (9 PM), **morning report on first wake** (yesterday's stats +
0–100 score + today's goals), screen-limit alert, shorts nudge, bedtime reminder — all toggleable.

**Widgets** — 4×4 and 2×2 home-screen widgets with sync button, light/dark theme, per-stat visibility.

**Sync** — offline-first Room buffer; every 15 min: uploads app events **and downloads events
written by the website** into Room (deduped by eventId). Manual "Sync now" in Settings; 90-day
automatic pruning of synced data.

**Permissions hub** — Settings → Permissions: grant/revoke notifications and activity recognition
with in-app dialogs; usage access and accessibility open the exact system page (Android restriction).

## Structure

```
app/src/main/java/com/lifelensiq/app/
├── LifeLensIQApp.kt            Application class (DI init)
├── MainActivity.kt         Single activity + Compose NavHost
├── di/ServiceLocator.kt    Manual DI
├── domain/                 Models, EventType, repository interfaces
├── data/
│   ├── local/              Room: entities, DAOs, AppDatabase
│   ├── remote/             Firebase Auth + Firestore sources
│   └── repository/         Implementations (offline-first)
├── tracking/               Foreground service, pollers, receivers, step tracker, focus block
├── sync/                   SyncWorker + scheduler (upload + download)
├── notifications/          Daily summary / morning report / alert workers + helper
├── export/                 CSV / JSON exporters + use case
├── widget/                 App widget providers + renderer + config activity
└── ui/                     Compose screens + ViewModels + theme
    ├── home/  activity/  sessions/  trends/  settings/
    ├── category/  onboarding/  auth/  export/
    └── components/         App bars, cards, charts (Canvas-drawn)
```

## Build & run

1. **Firebase setup (required):** create a Firebase project, enable Email/Password
   auth and Cloud Firestore (production mode), add Android app with package
   `com.lifelensiq.app`, then drop the downloaded `google-services.json` into
   `app/`. **This file is not committed to git** (it is in `.gitignore`), so a
   fresh clone needs this step before building.
2. Deploy Firestore rules from `firestore.rules` (or `assets/docs/02_TRD.md` §6.3) —
   the same ruleset serves the web dashboard.
3. `./gradlew assembleDebug` or open the folder in Android Studio and press Run.
4. Run tests with `./gradlew testDebugUnitTest`.

Prebuilt APKs (debug builds of the latest branch) are dropped in `apk/` and attached to
GitHub releases.

## First-run flow

1. **Onboarding** walkthrough explains what gets recorded and why.
2. Sign up with email (or log in).
3. **Settings → Permissions**: grant Usage access (required), notifications and
   step tracking (optional), Accessibility (optional, for shorts) — all from one screen.
4. Tracking starts automatically; syncs both ways every 15 min.
5. Use **Sessions** for timers / focus mode, **Trends** for longer-term numbers,
   **Settings → Export** for CSV / JSON / NDJSON.

## Privacy

See [PRIVACY_POLICY.md](PRIVACY_POLICY.md). Summary: metadata only (app usage,
screen, charging, steps, study sessions, focus sessions — no messages, media,
location); data stays on-device until you sign in, then syncs only to your own Firestore
account (rules in `firestore.rules`); your category overrides and focus-blocked app list
are stored on-device only; excluded from Android backups; no ads, analytics, or
third-party SDKs; one-tap delete for local and cloud data.