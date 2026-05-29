# SadhanaLock — Progress Tracker

> **Source of truth for both tracks.** Aman and Nilay both update this file.
> Last updated: 2026-05-28

---

## Status Overview

| Track | Owner | Status | Current Phase |
|---|---|---|---|
| **Track A — App Shell** | Aman (Emergent) | 🟡 In Progress | Phase 1 — Foundations |
| **Track B — Blocker Engine** | Nilay (Claude Code) | 🟡 In Progress | Phases 1–4 code complete — pending first compile (needs JDK 17) |
| **Design** | Vibhor | ⬜ Not Started | Design files pending |
| **Content** | Aman + Team | ⬜ Not Started | Gita/mantras/chalisas need sourcing |
| **Backend** | Aman (Emergent) | ⬜ Not Started | Boilerplate exists |

---

## Decisions Log

All decisions are final unless re-opened by team discussion.

| # | Decision | Choice | Date |
|---|---|---|---|
| 1 | Storage | Keep AsyncStorage (already wired) | 2026-05-28 |
| 2 | Auth V1 | Google Sign-In via Emergent built-in auth (Track A owns) | 2026-05-28 |
| 3 | Crash reporting | Firebase Crashlytics only | 2026-05-28 |
| 4 | IPC pattern | Module-first, SharedPrefs for crash recovery | 2026-05-28 |
| 5 | Night lock timing | Configurable, device local tz, default 10PM-6AM | 2026-05-28 |
| 6 | Cooldown | Configurable 5/15/30/60 min, default 15 | 2026-05-28 |
| 7 | Gentle vs Strict | Gentle: instant Om. Strict: 10s wait then Om | 2026-05-28 |
| 8 | Morning gate | Must open app and pray on prayer screen | 2026-05-28 |
| 9 | Clock protection | Not required — trust user | 2026-05-28 |
| 10 | Denylist override | NO — hard-coded, user cannot override | 2026-05-28 |
| 11 | Icon loading | Paginated — getAppIcon(pkg) on demand | 2026-05-28 |
| 12 | UsageStats polling | Battery-friendly (~1500-2000ms) | 2026-05-28 |
| 13 | Languages | Hindi, English, Hinglish — selection in onboarding | 2026-05-28 |
| 14 | Monetization | Free for V1 | 2026-05-28 |
| 15 | Pandit reviewer | Not required — team handles content accuracy | 2026-05-28 |
| 16 | Contract: markMorningPrayerDone() | Added — JS signals native when in-app morning prayer done | 2026-05-29 |
| 17 | Contract: getAppIcon(pkg) | Added — paginated icon loading per decision #11 | 2026-05-29 |

---

## Track A — App Shell (Aman / Emergent)

### Phase 1 — Foundations
- [x] Fix app identity (app.json → "SadhanaLock" / "sadhanalock")
- [ ] Google Sign-In auth (Emergent built-in auth)
- [ ] Design system + theme (colors.ts with all deity tokens)
- [ ] Fonts setup (Noto Sans Devanagari + Poppins)
- [ ] Navigation skeleton (Expo Router with tabs)
- [ ] Mocked native module (SadhanaBlocker.mock.ts)

### Phase 2 — Screens
- [ ] Onboarding flow (welcome → language → deity → permissions)
- [ ] Home screen (greeting, verse of day, streak, quick actions)
- [ ] Scripture reader (Gita, Chalisas, Stotrams, Aartis)
- [ ] Prayer screen (/prayer/[deity]) with Om button
- [ ] Lock list screen (installed apps with toggles)
- [ ] Panchang / Hindu Calendar
- [ ] Daily check-in (streak, calendar view)
- [ ] Settings screen
- [ ] Diagnostics screen (dev tools)

### Phase 3 — Backend + Content Sync
- [ ] Backend API endpoints (deities, gita, mantras, chalisas, festivals, checkin)
- [ ] Content sync in app (backend → local cache → bundled JSON fallback)
- [ ] Input validation + CORS fix on backend

### Phase 4 — Polish
- [ ] Notifications (festival reminders, check-in reminder)
- [ ] Offline mode (bundled JSON, sync queue)

---

## Track B — Blocker Engine (Nilay / Claude Code)

> **Code complete for Phases 1–4 (2026-05-29).** Package gate cleared (com.sadhanalock.app
> on main); `markMorningPrayerDone()` (decision #16) + `getAppIcon()` (#17) implemented.
> Last gate before first compile: **JDK 17 on the dev machine**. Bring-up in
> `frontend/modules/sadhana-blocker/README.md`.

### Phase 1 — Foundations
- [x] Module skeleton (expo-module.config.json, build.gradle, AndroidManifest)
- [x] SadhanaBlocker.types.ts (copy contract from PLAN.md §4.1, + getAppIcon)
- [x] Hello-world overlay (subsumed by full prayer overlay + `debugShowOverlay()`)
- [x] PermissionHelper.kt (Accessibility + Overlay + battery + usage-access flows)

### Phase 2 — Core Engine
- [x] AppListHelper.kt (getInstalledApps + getAppIcon paginated)
- [x] PrefsStore.kt (SharedPreferences schema, 13 keys)
- [x] SadhanaAccessibilityService.kt (foreground app detection → BlockerEngine)
- [x] OverlayController.kt + overlay_prayer.xml (prayer overlay; TYPE_ACCESSIBILITY_OVERLAY)
- [x] Gentle mode (instant Om tap)
- [x] Strict mode (10s wait then Om)

### Phase 3 — Resilience
- [x] BootReceiver.kt (survive reboot / app update)
- [x] HealthCheckWorker.kt (survive OEM kills — periodic nudge)
- [x] ScheduleManager.kt (Night Lock live; Morning Gate fully wired via markMorningPrayerDone)
- [x] UsageStatsManager fallback (UsageStatsForegroundService, ~1.8s poll)
- [~] OEM-specific handling (denylist spans Xiaomi/Samsung/OnePlus; autostart deep-links deferred to device testing)

### Phase 4 — Integration
- [x] SadhanaBlockerModule.kt (Expo Module bridge — all §4.2 methods + getAppIcon)
- [x] Event emission (onOverlayShown, onPrayerCompleted, onServiceStateChanged)
- [ ] Firebase Crashlytics integration (deferred — depends on google-services.json + Track-A package name)
- [x] ProGuard rules (keep AccessibilityService / receivers / worker)

---

## Shared / Blocking Items

### P0 — Must Do
- [x] Rename app.json (Track A — done 2026-05-29, com.sadhanalock.app)
- [ ] Generate Play Store signing key (Aman)
- [ ] Source content: 700 Gita verses + mantras for 7 deities + Chalisas + Stotrams + festivals
- [x] Resolve all BLOCKING_LOGIC.md decisions (done 2026-05-28)

### P1 — During Implementation
- [ ] Set up CI workflows (.github/workflows/)
- [ ] Set up EAS Build (eas.json)
- [ ] Build color token sync script (colors.ts ↔ colors.xml)
- [ ] Backend auth + input validation

---

## Weekly Sync Notes

### Week 1 (2026-05-28)
- All design decisions resolved
- Track A started (Emergent building foundations)
- Track B ready to start (all blockers cleared)
- PLAN.md, BLOCKING_LOGIC.md, TODOS.md pushed to repo
- Languages: Hindi, English, Hinglish
- Track B ownership: Nilay with Claude Code

---

> Both Aman and Nilay: update this file after completing any task. Check the boxes, add notes, keep it current.
