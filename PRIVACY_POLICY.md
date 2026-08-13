# Privacy Policy — LifeLens IQ

**Effective date:** prototype stage. This policy describes how the LifeLens IQ
Android app collects, stores, and protects your data.

## 1. What the app collects

All data is collected **on-device** and stays local until you sign in, after
which it syncs to your personal Firebase account. Only **metadata** is
recorded — no message content, media, keyboard input, screenshots, calls, or
contacts are ever captured.

| Category | What is recorded | Why |
|----------|------------------|-----|
| App usage | Package name of the foreground app + duration | Time-usage analytics |
| Screen | Screen on/off/unlock timestamps | Session segmentation |
| Charging | Charging start/end timestamps | Battery-aware insights |
| Steps | Step counts (Activity Recognition) | Activity insights |
| Wake | Screen-wake timestamps | Sleep/interruption insights |
| Study | Subject, session start/end | Self-tracking |
| Attendance | Timetable slots marked attended | Attendance insights |
| Tracking state | Start/pause/resume events | Data integrity |
| Sync status | Sync timestamp + result | Reliability |

Your **timetable** (subjects, times, rooms) is imported from a file you choose.
The app does **not** read your calendar, location, or other accounts.

## 2. Where data is stored

- **Locally:** Room database inside the app's private sandbox on your device.
- **Cloud (optional, after sign-in):** Firebase Cloud Firestore, under the
  document path `users/{your-uid}/…`, which only you (and your Firebase
  project) can access. Firebase security rules deny all other users.
- **Backups:** app data is **excluded** from Android Auto Backup
  (`allowBackup="false"`).

## 3. How data is shared

- Data is **never sold or shared** with third parties.
- CSV/JSON exports are written to a folder you choose via the system file
  picker — you control where they go.
- The app contains **no advertising, analytics SDKs, or tracking libraries**
  beyond Firebase (auth + storage of your own data).

## 4. Your rights

- **Review:** export everything as CSV or JSON from the Export screen.
- **Delete locally:** Settings → Delete local data (removes all on-device records).
- **Delete cloud:** Settings → Delete cloud data (removes your Firestore documents).
- **Stop tracking:** revoke Usage Access / Activity Recognition permissions or
  uninstall the app.

## 5. Contact

This is a personal academic prototype. Questions: raise an issue on the
GitHub repository `ShreyasP10/LifeLensIQ-App`.
