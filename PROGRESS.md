# SadhanaLock — Progress Tracker

> **Source of truth for both tracks.** Aman and Nilay both update this file.
> Last updated: 2026-05-28

---

## Status Overview

| Track | Owner | Status | Current Phase |
|---|---|---|---|
| **Track A — App Shell** | Aman (Emergent) | 🟡 In Progress | Phase 1 — Foundations |
| **Track B — Blocker Engine** | Nilay (Claude Code) | ⬜ Not Started | Waiting to start |
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

---

## Track A — App Shell (Aman / Emergent)

### Phase 1 — Foundations
- [ ] Fix app identity (app.json → "SadhanaLock" / "sadhanalock")
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

### Phase 1 — Foundations
- [ ] Module skeleton (expo-module.config.json, build.gradle, AndroidManifest)
- [ ] SadhanaBlocker.types.ts (copy contract from PLAN.md §4.1)
- [ ] Hello-world overlay (show "Hare Krishna" on top of any app)
- [ ] PermissionHelper.kt (Accessibility + Overlay permission flows)

### Phase 2 — Core Engine
- [ ] AppListHelper.kt (getInstalledApps + getAppIcon paginated)
- [ ] PrefsStore.kt (SharedPreferences schema, 13 keys)
- [ ] SadhanaAccessibilityService.kt (foreground app detection)
- [ ] OverlayService.kt + overlay_prayer.xml (prayer overlay)
- [ ] Gentle mode (instant Om tap)
- [ ] Strict mode (10s wait then Om)

### Phase 3 — Resilience
- [ ] BootReceiver.kt (survive reboot)
- [ ] HealthCheckWorker.kt (survive OEM kills)
- [ ] ScheduleManager.kt (Night Lock + Morning Prayer Gate)
- [ ] UsageStatsManager fallback
- [ ] OEM-specific handling (Xiaomi, Samsung, OnePlus)

### Phase 4 — Integration
- [ ] SadhanaBlockerModule.kt (Expo Module bridge — all methods from §4.2)
- [ ] Event emission (onOverlayShown, onPrayerCompleted, onServiceStateChanged)
- [ ] Firebase Crashlytics integration
- [ ] ProGuard rules

---

## Shared / Blocking Items

### P0 — Must Do
- [ ] Rename app.json (Track A — do immediately)
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
