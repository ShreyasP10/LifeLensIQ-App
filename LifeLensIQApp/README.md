# LifeLens IQ — Understand your time. Improve your life.

Android prototype for the LifeLens IQ project (see `../docs/` for PRD/TRD/schema/architecture/ML strategy).

**Stack:** Kotlin • Jetpack Compose (Material 3) • Room • WorkManager • Firebase Auth + Firestore • kotlinx.serialization

## Structure

```
app/src/main/java/com/lifeiq/app/
├── LifeIQApp.kt            Application class (DI init, notification channel)
├── MainActivity.kt         Single activity + Compose NavHost
├── di/ServiceLocator.kt    Manual DI
├── domain/                 Models, EventType, repository interfaces
├── data/
│   ├── local/              Room: entities, DAOs, AppDatabase
│   ├── remote/             Firebase Auth + Firestore sources
│   └── repository/         Implementations (offline-first)
├── tracking/               Foreground service, pollers, receivers, step tracker
├── sync/                   SyncWorker + scheduler
├── export/                 CSV / JSON exporters + use case
├── timetable/              Import/validate timetable_personalized.json
└── ui/                     Compose screens + ViewModels + theme
```

## Build & run

1. **Firebase setup (required):** create a Firebase project, enable Email/Password
   auth and Cloud Firestore (production mode), add Android app with package
   `com.lifeiq.app`, then replace `app/google-services.json` with the downloaded one.
   The current file is a **placeholder** (compiles, but auth/sync won't work until replaced).
2. Deploy Firestore rules from `../docs/02_TRD.md` §6.3.
3. `./gradlew assembleDebug` or open the folder in Android Studio and press Run.

## First-run flow

1. Sign up with email (or log in).
2. Grant **Usage access** (Settings deep link) — required for app-usage tracking.
3. Import timetable: Settings → Import timetable → pick `assets/timetable_personalized.json`
   (sample provided — matches your real TE-B timetable for B1 + IP).
4. Tracking starts automatically; syncs to Firestore every 15 min.
5. Export data from the Export screen (CSV / JSON) for your ML pipeline.
