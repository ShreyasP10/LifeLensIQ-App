# LifeLens IQ — Setup Guide

Setup for the LifeLens IQ Android prototype (package `com.lifelensiq.app`).

---

## 1. Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Ladybug or newer (bundles JDK 17 + Android SDK) |
| JDK | 17 |
| Android SDK | API 35 (compile/target), minSdk 26 |
| Physical Android phone | Required — Usage Access cannot be granted on emulators |
| Google account | For the Firebase project |

---

## 2. Firebase setup (required for auth + cloud sync)

The app works fully offline without Firebase, but login and Firestore sync
need a real project. The committed `app/google-services.json` is a placeholder.

1. Go to https://console.firebase.google.com → **Add project** (e.g. `lifelens-iq`).
2. **Build → Authentication → Get started → Email/Password → Enable.**
3. **Build → Firestore Database → Create database**
   - Mode: **Production** (not test mode)
   - Location: `asia-south1` (Mumbai) or your closest region
4. **Project settings → Your apps → Add app → Android**:
   - Package name must be exactly **`com.lifelensiq.app`**
   - Register, then download **`google-services.json`**
5. Replace the placeholder:
   ```
   app\google-services.json
   ```
6. **Deploy Firestore security rules** (from `firestore.rules` — one ruleset
   shared with the web dashboard, so both clients work):

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid}/{doc=**} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
       }
       match /leaderboard/{uid} {
         allow read: if true;
         allow write: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```

   Deploy: Firestore console → **Rules** tab → paste → **Publish**
   (or `firebase deploy --only firestore:rules` from the web repo).

7. Rebuild so the new config is picked up.

---

## 3. Build & run

**From Android Studio**
1. File → Open → select `Desktop\LifeLens-IQ`
2. Let Gradle sync finish, then press **Run** (phone connected, USB debugging on)

**From the command line**
```powershell
cd Desktop\LifeLens-IQ
.\gradlew.bat installDebug
```
APK output: `app\build\outputs\apk\debug\app-debug.apk`

**Run the tests**
```powershell
.\gradlew.bat testDebugUnitTest
```

---

## 4. First-run checklist (on the phone)

1. **Sign up** with email + password (or sign in if you already have an account).
2. **Notifications** — allow when prompted (Android 13+).
3. **Usage access** (required for app-usage tracking):
   - App → **Settings** tab → **Open Usage Access Settings** → allow **LifeLens IQ**
   - Go back → tap **Re-check permission** (banner on Home disappears when granted)
4. **Step tracking** (optional): Settings → **Enable step tracking** → allow Activity Recognition.
5. **Import timetable** (unlocks Home/Attendance/Sessions/class reminders):
   - Settings → **Import bundled sample (B1 + IP)** — or
   - **Import timetable JSON file…** → pick `timetable_personalized.json` from `assets/table/`
6. **Verify tracking is live**: the persistent notification *"LifeLens IQ is tracking"* should be visible. Home shows today's stats.

---

## 5. Verification checklist

| Check | How |
|-------|-----|
| App usage recorded | Use some apps → wait ≤ 30 s → Home "Screen" stat grows |
| Class reminders | With timetable imported, a notification fires 15 min before each lecture |
| Attendance | Attendance tab → mark a class → Home shows "Attendance: 1/5 marked" |
| Study sessions | Sessions tab → pick subject → Start → Stop → appears in "Today's sessions" |
| Steps | Walk a few steps → check a `STEPS` event exists (export or DB) |
| Sync to Firestore | Firestore console → `users/{uid}/events/` → docs appear within ~15 min (or tap **Sync now** in Settings) |
| Offline resilience | Airplane mode → track → enable network → events sync with no duplicates |
| Export | Home → **Export Data** → CSV (opens in Excel) and JSON (opens in Python) |

---

## 6. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Login fails with "internal error" | `google-services.json` is still the placeholder — redo §2 |
| No app events in Room | Usage access not granted → Settings → Usage Access → Re-check |
| Steps always 0 | Activity Recognition permission denied, or device lacks a step counter |
| Sync never uploads | No network / Firebase rules not deployed (401 errors) → check §2.6 |
| Data missing after reboot | OEM battery saver killed the service → add the app to "Unrestricted" / "No restrictions" battery settings (MIUI, OnePlus, etc.) |
| Tracking stops randomly | Same as above; re-open the app to restart, or Settings → Restart tracking service |

---

## 7. Project layout

```
LifeLens-IQ\
├── app\                        Android app (Gradle project root)
├── assets\docs\                PRD / TRD / schema / architecture / ML strategy
├── assets\table\               fetch_timetable.py + personal timetable (te.html, te-b.pdf)
├── gradle\                     Gradle wrapper + version catalog
└── README.md                   App overview + build notes
```
