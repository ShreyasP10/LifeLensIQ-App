# LifeLensIQ — What It Is

LifeLensIQ is a **personal digital-life intelligence system**. It silently
watches how you actually spend your time across your **phone and laptop**,
records every moment as a timestamped data point, and turns that raw timeline
into a categorised, visualised, and ML-ready dataset — all viewable in one
place: the **web dashboard** (`LifeLensIQ-Web` repo).

It is not a timetable app and not a step counter. Those are just two of the
~30 signals it captures. The core idea: **every minute of your life becomes a
data point** — from that data, an ML model computes your *Life Intelligence
Quotient* (how productively and how balanced your days are) and eventually
suggests how to live smarter.

---

## 1. What it captures — the full data set

### From the Android phone (this app)

| Signal | How |
|--------|-----|
| App usage | which app, how long (foreground session events) |
| Screen behaviour | screen on/off/unlock, idle gaps |
| Charging | start/stop, plug type, battery % |
| Steps | step-counter sensor deltas |
| Wake-up inference | first screen-on after long idle |
| Reels & Shorts | **how many Instagram Reels / YouTube Shorts viewed** (accessibility heuristic) |
| Study sessions | manual start/stop with subject |
| Class attendance | marked from timetable slots |
| Tracking state | started/paused/resumed — data integrity |
| Timetable | imported from the TE-B JSON (context) |

### From the browser (Chrome extension)

| Signal | How |
|--------|-----|
| Tab activity | active site, domain, path, title, duration |
| Reels & Shorts counter | YouTube Shorts / Instagram & Facebook Reels detected live by content scripts |
| PDF viewing | when and how long |
| Writing sessions | keyboard bursts on Docs/Office |
| Idle gaps | desk-away segments |

### From both
- **College timetable** — so the system knows when you *should* be in class.
- **Manual entries** — anything you log by hand (offline study, phone calls) — the dashboard's Log tab.

---

## 2. How the data flows

```
Phone app ──┐
            ├─► local buffers (Room / chrome.storage) ──► Firebase Firestore
Extension ──┘                                           users/{uid}/events
                                                                   │
                                                                   ▼
                                              Web dashboard (React)
                                              score, charts, heatmap,
                                              timeline, export, insights
```

- **Offline-first:** both clients buffer events and batch-sync when online — nothing is lost.
- **One schema, one account:** phone and browser events land in the **same
  Firebase project** under your uid, tagged `device: "android"` /
  `"extension"`, with a shared 9-category vocabulary (Study, DSA,
  Development, Productivity, Entertainment, Timepass, Short-form Video,
  Utilities, Other).
- **Everything is viewable in the web version** — phone app sessions, browser
  tabs, reels/shorts counts, study sessions, attendance — merged on one
  timeline.

---

## 3. What the web dashboard shows

- Today / 7 days / 30 days: productivity score ring, stacked day chart,
  category pie, trend badges.
- GitHub-style 26-week productivity heatmap.
- Reels/shorts stats — seconds and view counts from both devices.
- Timeline — every event from both devices; category/type filters; per-site
  drilldown (hour pattern, weekday comparison, 14-day trend).
- Insights — top sites, most/least productive weekday, peak hour, late-night
  usage, distraction share, focus streak.
- Leaderboard (opt-in), Log (manual entries), Timetable view, Settings
  (category overrides).

---

## 4. Scoring & ML

- Weighted productivity score: Study/DSA/Development weight 1.0, Short-form
  Video 0 — computed from every captured second.
- ML dataset export: engineered rows (hour, day_of_week, day_segment,
  duration, gap, prev_category, is_productive…) split chronologically
  70/15/15 into train/val/test CSVs with a manifest — ready for the Life IQ
  model: predictive scheduling, habit detection, burnout anomaly flags.

---

## 5. Privacy

- Metadata only — domains, timestamps, durations. **Never** message content,
  calls, media, or location.
- Data stays on-device until you sign in; syncs only to **your own**
  Firestore account; owner-only security rules.
- Excluded from Android backups; no ads, no analytics SDKs; one-tap delete
  local/cloud; pause tracking anytime.

---

## 6. Reels & Shorts detection details

- **Browser:** content scripts recognise YouTube Shorts / Instagram Reels /
  Facebook Reels URL patterns and count views per segment.
- **Phone (prototype heuristic):** an AccessibilityService watches for
  "Reels" (Instagram/Facebook) and "Shorts" (YouTube) markers in the active
  window. Each distinct content change while the marker is visible counts one
  short viewed (swipe). Counts are aggregated per session and emitted as
  `short_video` events — identical to the extension's event type, so the
  dashboard merges both. Heuristic by design: Android offers no official API
  for in-app content; results are best-effort.
