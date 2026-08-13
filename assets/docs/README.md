# LifeIQ — Life Intelligence Quotient

**Project:** Personal life-data collection app (prototype → production) for building an ML model that computes and improves your "Life Intelligence Quotient".

**Target Platform:** Android (Java / Kotlin) — Jetpack Compose or XML
**Backend:** Firebase (Authentication + Cloud Firestore)

---

## Project Context

- **Owner:** TE Computer Engineering student (Division B, Batch B1, Program Elective-1: Internet Programming).
- **Motivation:** The final-year ML model needs real personal data (study schedule adherence, phone usage, class attendance, productivity patterns). This Android app is the **data-collection prototype**. After validating the data pipeline, it will be turned into a proper production application.
- **Existing assets in repo root:**
  - `TE-B.pdf` — official college timetable (source of truth).
  - `fetch_timetable.py` — agent-built program that parses `TE-B.pdf` and outputs full + personalized (B1/IP) timetable details.
  - `TE-B_Timetable_Detailed.html` — human-readable timetable document.

---

## Documentation Index

| # | Document | Description | Status |
|---|----------|-------------|--------|
| 1 | [`01_PRD.md`](01_PRD.md) | Product Requirements Document — what we build and why | Draft |
| 2 | [`02_TRD.md`](02_TRD.md) | Technical Requirements Document — how we build it (stack, modules, sync, export) | Draft |
| 3 | [`03_Data_Schema.md`](03_Data_Schema.md) | Data model — Room (local), Firestore (cloud), CSV/JSON export formats | Draft |
| 4 | [`04_Architecture_Design.md`](04_Architecture_Design.md) | System architecture — components, flows, background services, sync strategy | Draft |
| 5 | [`05_ML_Data_Strategy.md`](05_ML_Data_Strategy.md) | How collected data feeds the ML model — features, labeling, volumes, pipeline | Draft |
| 6 | [`06_Roadmap.md`](06_Roadmap.md) | Development roadmap — phases, milestones, acceptance criteria | Draft |

---

## Document Conventions

- Requirement IDs: `FR-xx` (functional), `NFR-xx` (non-functional), `DR-xx` (data), `ML-xx` (ML).
- Priorities: **P0** (must have for prototype), **P1** (must have for v1 production), **P2** (nice to have).
- All timestamps stored as **epoch milliseconds (UTC)** in cloud, rendered in device-local time in UI.
- All documents versioned; first version date: **10 Aug 2026**.
