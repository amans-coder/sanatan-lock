# SadhanaLock — Master Build Plan
## Split-Stack Strategy: Emergent (Expo) + Nilay with Claude Code (Kotlin)

> **"The app that makes you pray before you scroll."**
>
> Document owner: Aman (Track A + overall) / Nilay (Track B) / Vibhor (design)
> Last updated: 2026-05-28 (all decisions resolved)
> Status: Ready for execution
> Repo: `github.com/amans-coder/sanatan-lock`

---

## 0. Executive Summary

SadhanaLock will be built as a **single GitHub repo** containing **two co-located deliverables** that talk to each other through a strict, versioned native-module contract:

| Track | Owner | Workspace | What it produces |
|---|---|---|---|
| **Track A — App Shell** | Emergent (Claude in Emergent IDE) | `/frontend/` (Expo React Native) | The full user-facing app: onboarding, scripture reader, prayer screens, deity selection, panchang, daily check-in, notifications, settings, content sync, beautiful UI |
| **Track B — Blocker Engine** | Nilay (with Claude Code) | `/frontend/modules/sadhana-blocker/` (Kotlin native module) | The AccessibilityService, overlay window, app-detection logic, BootReceiver, HealthCheckWorker, permission flows |

The two tracks share a **single GitHub repo**, communicate via a **typed Expo Module API**, and never block each other thanks to a **mocked native module** that the Emergent side uses until Track B lands its first real implementation.

**Outcome:** A single APK where the Expo app calls `SadhanaBlocker.setBlockedApps(...)` in TypeScript and the Kotlin AccessibilityService quietly does its job in the background.

---

## 1. Architecture (One Diagram)

```
┌────────────────────────────────────────────────────────────────────┐
│                       SadhanaLock APK (single binary)              │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │   EXPO / REACT NATIVE LAYER  (built by Emergent)             │  │
│  │   • Onboarding, Home, Deity Picker                           │  │
│  │   • Scripture Reader (Gita, Chalisas, Stotrams)              │  │
│  │   • Prayer Screen (full-screen, Devanagari, "Om" button)     │  │
│  │   • Lock List Screen (pick which apps to block)              │  │
│  │   • Panchang + Festivals, Daily Check-in                     │  │
│  │   • Settings, Permissions explainers, Diagnostics            │  │
│  │   • MongoDB-backed content sync + offline cache              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                              ▲       ▼                             │
│        TypeScript Expo Module API (`SadhanaBlocker`)               │
│                              ▲       ▼                             │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │   KOTLIN NATIVE MODULE  (built by Claude Code)               │  │
│  │   • SadhanaBlockerModule.kt   ← Expo Module bridge           │  │
│  │   • SadhanaAccessibilityService.kt   ← detects foreground app│  │
│  │   • OverlayService.kt   ← shows prayer overlay window        │  │
│  │   • BootReceiver.kt   ← survives reboot                      │  │
│  │   • HealthCheckWorker.kt   ← survives OEM kills              │  │
│  │   • PermissionHelper.kt   ← Accessibility + Overlay perms    │  │
│  │   • AppListHelper.kt   ← installed-apps enumeration          │  │
│  │   • Shared SharedPreferences read/write                      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
                              ▲
                              │
                     Shared content & state
                     (SharedPreferences + bundled JSON)
```

---

## 2. Repo Structure (final, post-merge)

```
sadhana-lock/                            ← GitHub repo root
├── README.md
├── PLAN.md                              ← THIS FILE (master plan)
├── CONTRIBUTING.md                      ← workflow rules for both tracks
├── TODOS.md                              ← deferred work (P0/P1/P2 priorities)
│
├── docs/
│   ├── NATIVE_MODULE_CONTRACT.md        ← the API contract (source of truth)
│   ├── DESIGN_HANDOFF.md                ← Figma → code workflow
│   ├── BLOCKING_LOGIC.md             ← blocker engine design decisions (8 open items)
│   ├── REVIEW-ENG.md                    ← (existing) technical risks
│   ├── REVIEW-DESIGN.md                 ← (existing) Hindu design rules
│   ├── REVIEW-DEVEX.md
│   └── REVIEW-TUNE.md
│
├── design/                              ← Vibhor's exports (PNG/SVG/icons)
│   ├── deities/                         ← canonical deity assets
│   ├── icons/
│   ├── splash/
│   └── playstore/
│
├── content/                             ← raw content JSON (Aman + Nilay)
│   ├── deities.json
│   ├── mantras/
│   ├── chalisas/
│   ├── stotrams/
│   ├── gita/                            ← Bhagavad Gita verses
│   └── festivals.json
│
├── backend/                             ← FastAPI + MongoDB (optional sync layer)
│   ├── server.py
│   ├── requirements.txt
│   └── .env
│
└── frontend/                            ← Expo React Native app
    ├── app.json                         ← Expo config (plugins listed here)
    ├── package.json
    ├── app/                             ← Expo Router screens
    │   ├── _layout.tsx
    │   ├── index.tsx                    ← onboarding gate
    │   ├── (tabs)/
    │   │   ├── _layout.tsx
    │   │   ├── home.tsx
    │   │   ├── scriptures.tsx
    │   │   ├── lock.tsx                 ← lock list screen
    │   │   └── settings.tsx
    │   ├── prayer/[deity].tsx
    │   ├── scripture/[id].tsx
    │   └── onboarding/
    ├── src/
    │   ├── store/                       ← Zustand
    │   ├── content/                     ← bundled JSON imports
    │   ├── components/
    │   ├── hooks/
    │   └── utils/
    └── modules/
        └── sadhana-blocker/             ← ★ THE NATIVE MODULE (Claude Code's home)
            ├── README.md                ← Claude Code's brief
            ├── expo-module.config.json
            ├── package.json
            ├── src/
            │   ├── index.ts             ← TS API (SHARED with Track A)
            │   ├── SadhanaBlocker.types.ts
            │   └── SadhanaBlocker.mock.ts  ← used until real Kotlin lands
            └── android/
                ├── build.gradle
                ├── src/main/AndroidManifest.xml
                └── src/main/java/com/sadhanalock/blocker/
                    ├── SadhanaBlockerModule.kt
                    ├── service/
                    │   ├── SadhanaAccessibilityService.kt
                    │   ├── OverlayService.kt
                    │   └── BootReceiver.kt
                    ├── worker/
                    │   └── HealthCheckWorker.kt
                    ├── helpers/
                    │   ├── PermissionHelper.kt
                    │   ├── AppListHelper.kt
                    │   └── PrefsStore.kt
                    ├── overlay/
                    │   ├── OverlayView.kt
                    │   └── res/layout/overlay_prayer.xml
                    └── util/
```

**Why this layout works:**
- Single `git clone` gives Claude Code everything it needs.
- Emergent edits `/frontend/app/` and `/frontend/src/` freely; Claude Code edits `/frontend/modules/sadhana-blocker/` freely. Merge conflicts are nearly impossible by design.
- `docs/NATIVE_MODULE_CONTRACT.md` is the single shared source of truth — both sides commit changes to it via PR.

---

## 3. Scope Split — Who Builds What

### ✅ Emergent (Track A) builds:
1. Entire Expo Router scaffolding, navigation, theming, fonts.
2. **All UI screens** (onboarding, home, deity picker, scripture reader, prayer screen, lock list, check-in, panchang, settings, diagnostics).
3. **All content pipeline:** bundled JSON loaders, MongoDB sync, offline cache, Verse-of-the-Day rotation.
4. **Daily check-in** (Mongo collection + UI).
5. **Hindu Calendar / Panchang** using `mhah-panchang` (JS port of jyotish).
6. **Festival list + notifications** (`expo-notifications`).
7. **Deity personalization theme system.**
8. **Onboarding flow** (welcome → language → deity selection → permission explainers).
9. **Mocked native module** (`SadhanaBlocker.mock.ts`) so the Expo app runs end-to-end on Emergent's preview *before* the Kotlin lands.
10. **TypeScript types & contract** for the native module (lives in `/frontend/modules/sadhana-blocker/src/`).
11. **Expo Config Plugin** stub (just enough to register the module).
12. **CI / EAS Build configuration** (so Claude Code's APK builds work).
13. **Play Store metadata, screenshots, privacy policy** (under `/docs/playstore/`).

### ✅ Nilay with Claude Code (Track B) builds:
1. `SadhanaAccessibilityService.kt` — listens to `TYPE_WINDOW_STATE_CHANGED`, identifies foreground app's package name.
2. `OverlayService.kt` + `overlay_prayer.xml` — renders the prayer overlay using `TYPE_ACCESSIBILITY_OVERLAY`.
3. `BootReceiver.kt` — re-arms the service after reboot.
4. `HealthCheckWorker.kt` — `WorkManager` periodic check that AccessibilityService is alive; nudges user if not.
5. `PermissionHelper.kt` — programmatic open of Accessibility settings + Overlay permission.
6. `AppListHelper.kt` — `PackageManager.getInstalledApplications()` (with `QUERY_ALL_PACKAGES` declaration).
7. `PrefsStore.kt` — read/write the shared state schema (defined in §5).
8. `SadhanaBlockerModule.kt` — the Expo Module bridge: exposes everything from §4 to TypeScript.
9. **AndroidManifest entries** for the service, receiver, permissions.
10. **`accessibility_service_config.xml`** — proper declaration that survives Google Play review.
11. **Gentle vs Strict mode logic.**
12. **Diagnostic event emission** to JS layer (so the Expo Diagnostics screen can show "service alive: yes/no").
13. **UsageStatsManager fallback** — polling-based foreground-app detection if Google rejects AccessibilityService. See `docs/BLOCKING_LOGIC.md` §4.
14. **ScheduleManager.kt** — Nightly Lock (hard block after configurable time) + Morning Prayer Gate (first prayer of the day unlocks all apps). See `docs/BLOCKING_LOGIC.md` §2.
15. **Firebase Crashlytics integration** — native crash reporting with custom keys (`device_oem`, `detection_method`, `overlay_state`). Crashlytics ONLY, no other Firebase services.

### ❌ Out of scope for V1 (both tracks):
- iOS (Apple Screen Time API is a different beast; revisit V2).
- Mascot / gamification / community.
- Audio recitations.
- Dark mode.
- Monetization.
- Breathe mode (One Sec-style pause — dilutes spiritual identity, revisit V2).
- Home screen widget (verse + streak counter — V1.1).
- iOS (deferred to V2).

---

## 4. The Native Module Contract (Source of Truth)

> **This is the API that lives in `/frontend/modules/sadhana-blocker/src/index.ts`.**
> **Both sides must respect it. Any change requires a PR that updates the mock too.**

### 4.1 TypeScript Surface

```ts
// /frontend/modules/sadhana-blocker/src/SadhanaBlocker.types.ts

export type BlockMode = 'gentle' | 'strict';

export interface InstalledApp {
  packageName: string;       // e.g. "com.instagram.android"
  appName: string;           // e.g. "Instagram"
  iconBase64: string | null; // PNG base64, null if extraction fails
  isSystem: boolean;
}

export interface BlockerState {
  isAccessibilityEnabled: boolean;
  isOverlayPermissionGranted: boolean;
  isBatteryOptimizationIgnored: boolean;
  blockedPackages: string[];
  mode: BlockMode;
  selectedDeity: string;     // e.g. "krishna"
}

export interface PrayerContentForOverlay {
  deity: string;
  mantraDevanagari: string;
  mantraTransliteration: string;
  mantraMeaning: string;
  bgColorHex: string;        // deity-themed
  deityImageBase64: string;
}

export interface OverlayShownEvent {
  packageName: string;
  timestamp: number;
}

export interface PrayerCompletedEvent {
  packageName: string;
  durationMs: number;
  timestamp: number;
}
```

### 4.2 Methods (TS → Kotlin)

```ts
// /frontend/modules/sadhana-blocker/src/index.ts (signatures only)

// --- Permissions ---
requestAccessibilityPermission(): Promise<void>;       // opens Settings intent
isAccessibilityEnabled(): Promise<boolean>;
requestOverlayPermission(): Promise<void>;
isOverlayPermissionGranted(): Promise<boolean>;
requestIgnoreBatteryOptimization(): Promise<void>;
isBatteryOptimizationIgnored(): Promise<boolean>;

// --- App discovery ---
getInstalledApps(): Promise<InstalledApp[]>;           // user-installed only

// --- Configuration ---
setBlockedApps(packages: string[]): Promise<void>;
getBlockedApps(): Promise<string[]>;
setBlockingMode(mode: BlockMode): Promise<void>;
getBlockingMode(): Promise<BlockMode>;

// --- Content handoff (Expo writes, Kotlin reads) ---
setOverlayContent(content: PrayerContentForOverlay): Promise<void>;
setSelectedDeity(deityId: string): Promise<void>;

// --- Lifecycle ---
startBlockerService(): Promise<void>;                  // enable
stopBlockerService(): Promise<void>;                   // user pause
getBlockerState(): Promise<BlockerState>;

// --- Morning Gate (added 2026-05-28, contract-change #1) ---
markMorningPrayerDone(): Promise<void>;                // JS tells native: morning prayer completed in-app

// --- App icons (paginated, added 2026-05-28) ---
getAppIcon(packageName: string): Promise<string | null>; // single icon on demand (base64)

// --- Manual trigger (testing) ---
debugShowOverlay(): Promise<void>;
```

### 4.3 Events (Kotlin → TS)

```ts
// Subscribed via Expo Module's EventEmitter
'onOverlayShown'        → OverlayShownEvent
'onPrayerCompleted'     → PrayerCompletedEvent
'onServiceStateChanged' → { isRunning: boolean }
```

### 4.4 Errors

All methods reject with an `Error` whose `message` is one of:
- `ERR_PERMISSION_DENIED`
- `ERR_SERVICE_NOT_RUNNING`
- `ERR_INVALID_PACKAGE`
- `ERR_INTERNAL`

---

## 5. Shared State Schema (SharedPreferences)

> **UPDATE (2026-05-28):** SharedPreferences is now used for **persistence only** (crash recovery).
> Runtime state flows through the Expo Module bridge directly. See `docs/BLOCKING_LOGIC.md` §1.

Single `SharedPreferences` file: `sadhana_blocker_prefs`. Both sides agree on keys:

| Key | Type | Writer | Reader | Description |
|---|---|---|---|---|
| `blocked_packages` | `Set<String>` | Module | Service (cold start) | apps to intercept |
| `mode` | `String` (`gentle`/`strict`) | Module | Service (cold start) | unlock difficulty |
| `selected_deity` | `String` | Module | Service (cold start) | which deity for overlay |
| `overlay_content_json` | `String` (serialized `PrayerContentForOverlay`) | Module | Service (cold start) | what to render |
| `service_enabled` | `Boolean` | Module | Service (cold start) | master kill switch |
| `last_prayer_ts` | `Long` | Service | Module | for streak tracking |
| `prayer_count_today` | `Int` | Service | Module | for daily stats |
| `night_lock_enabled` | `Boolean` | Module | ScheduleManager | night lock toggle |
| `night_lock_start_hour` | `Int` | Module | ScheduleManager | e.g., 22 (10 PM) |
| `night_lock_end_hour` | `Int` | Module | ScheduleManager | e.g., 6 (6 AM) |
| `morning_prayer_done_today` | `Boolean` | Service | ScheduleManager | reset at night_lock_end |
| `cooldown_minutes` | `Int` | Module | Service | unlock window duration |
| `detection_method` | `String` | Module | Service | `accessibility` / `usage_stats` |

> **Note:** Do NOT store deity images in SharedPreferences. Use file storage + URI. See `docs/BLOCKING_LOGIC.md` §9.

---

## 6. Tech Stack (Authoritative)

| Layer | Choice | Notes |
|---|---|---|
| **App framework** | Expo SDK 54 (React Native, TypeScript, Expo Router) | Emergent default |
| **State** | Zustand | lightweight, no provider drilling |
| **Local KV** | `@/src/utils/storage` (AsyncStorage wrapper) | AsyncStorage is already wired and works. Keep it. |
| **Content sync** | FastAPI + MongoDB (Emergent default) | replaces Firestore |
| **Auth** | Google Sign-In via Emergent built-in auth | Emergent handles the auth flow. Track A (Emergent) owns this. |
| **Panchang** | `mhah-panchang` (npm) | JS port of jyotish; accuracy validated by Nilay |
| **Notifications** | `expo-notifications` | requires dev build |
| **Animation** | `react-native-reanimated` (already in Expo) | for prayer screen entrance |
| **Fonts** | `expo-font` + Noto Sans Devanagari + Poppins | bundled |
| **Icons** | `@expo/vector-icons` (Lucide subset) | no emoji icons |
| **Native blocker** | Kotlin, AccessibilityService, WindowManager `TYPE_ACCESSIBILITY_OVERLAY` | inside Expo Module |
| **Build** | EAS Build via Emergent Publish button | produces signed APK |
| **Repo** | GitHub, single monorepo | "Save to GitHub" from Emergent |
| **Target** | Android only, minSdk 26, targetSdk 34 | iOS deferred to V2 |
| **Crash reporting** | Firebase Crashlytics (Android SDK + `@react-native-firebase/crashlytics`) | Crashlytics ONLY — no other Firebase services. Overrides original "no Firebase" rule. |

---

## 7. The "Never Block" Pattern (Critical)

> Track A and Track B are written in different IDEs in different cities. We MUST make sure neither ever waits for the other.

### Rule 1 — Contract before code.
`docs/NATIVE_MODULE_CONTRACT.md` (auto-generated from `SadhanaBlocker.types.ts`) is committed **before any implementation**. Both sides code against it.

### Rule 2 — Emergent ships a mock from Day 1.
`SadhanaBlocker.mock.ts` simulates every method:
- `getInstalledApps()` returns 10 fake apps.
- `setBlockedApps()` resolves instantly.
- `isAccessibilityEnabled()` returns a value driven by a dev toggle in the Diagnostics screen.
- `onOverlayShown` event can be fired manually from the Diagnostics screen.

A `__DEV__` flag (or `Constants.appOwnership === 'expo'`) picks between mock and real native module. **The Expo app on Emergent's preview always uses the mock.**

### Rule 3 — Claude Code can develop without the Expo app.
Track B includes a tiny **example Android app** (`/frontend/modules/sadhana-blocker/example-android/`) that imports the module and calls every method. Claude Code can install this and validate the blocker on a real phone without ever opening the Expo project.

### Rule 4 — Integration day is a single PR.
When both sides are ready, one final PR flips the import:
```ts
// before
import SadhanaBlocker from './SadhanaBlocker.mock';
// after
import SadhanaBlocker from './SadhanaBlocker.real';
```

---

## 8. Design Handoff (Vibhor → Emergent + Claude Code)

Vibhor exports from Figma into `/design/`:

| Folder | What | Used by |
|---|---|---|
| `/design/deities/` | 7 deity PNGs at 1x/2x/3x | Emergent (in-app), Claude Code (overlay) |
| `/design/icons/` | Lucide-style custom icons SVG | Emergent |
| `/design/splash/` | splash @ 1x/2x/3x | Emergent (configures `expo-splash-screen`) |
| `/design/overlay/` | overlay XML mockup + assets | Nilay (Claude Code) |
| `/design/playstore/` | feature graphic, screenshots frames | Aman (Play Store listing) |

**Asset naming convention:** `deity_krishna.png`, `deity_krishna@2x.png`, `deity_krishna@3x.png`.

**Color tokens** live in `/frontend/src/theme/colors.ts` AND `/frontend/modules/sadhana-blocker/android/src/main/res/values/colors.xml`. They must stay in sync; the contract doc lists every token.

Per `REVIEW-DESIGN.md`:
- Background `#FFF8F0` (warm off-white, NEVER pure white).
- Accent `#E65100` (saffron passes AA).
- Deity colors: Krishna `#1565C0`, Shiva `#37474F`, Hanuman `#D84315`, Lakshmi `#FBC02D`, Ganesh `#EF6C00`, Durga `#C62828`, Saraswati `#FFF59D`.
- Devanagari 15–25% larger than Latin.
- Overlay tone: **invitational** ("A moment with Krishna awaits"), never punitive.

---

## 9. Week-by-Week Plan

### Week 1 — Foundations (parallel, zero handoff)

| Vibhor | Aman | Nilay | Emergent (Track A) | Nilay with Claude Code (Track B) |
|---|---|---|---|---|
| Design system in Figma | Set up MongoDB cluster + FastAPI sync schema | Source Gita JSON + DharmicData | Scaffold Expo app, fonts, theme, navigation skeleton | Create `/frontend/modules/sadhana-blocker/` skeleton, write the contract `.ts` types, ship mock |
| 5 key screens drafted | Curate mantras for 7 deities | Source Chalisas/Aartis | Build mocked Diagnostics screen with toggles for every native method | Set up Android Studio project, AccessibilityService permission boilerplate, AndroidManifest |
| | Create GitHub org + repo | Verify Gita Sanskrit accuracy | Implement `mhah-panchang` panchang display | Hello-world overlay (just shows "Hare Krishna" on top of any app — proves the pipe) |
| | | | Push initial scaffold to GitHub | Push initial module to GitHub |

**Exit criterion:** Both tracks have something running. Emergent preview shows the app with a mocked Diagnostics screen. Claude Code can show an overlay on a real device.

---

### Week 2 — UI builds + Real native scaffolding

| Vibhor | Aman | Nilay | Emergent | Nilay (Claude Code) |
|---|---|---|---|---|
| Scripture reader, lock list, overlay design | Finalize all overlay copy | Verify content | Build onboarding flow, deity picker, scripture reader | Implement `getInstalledApps()` (real) |
| Permission explainer screens | Curate festival dates | Sanskrit accuracy | Build prayer screen, deity theming, Verse-of-Day | Implement `setBlockedApps()` + foreground-app detection |
| | | | Wire content sync (MongoDB) | Implement `PrefsStore.kt` (matches §5 schema) |

**Exit criterion:** Emergent app is feature-complete except blocking. Claude Code can detect foreground app changes and log them.

---

### Week 3 — The hard week: Real blocking engine + Real integration

| Vibhor | Aman | Nilay | Emergent | Nilay (Claude Code) |
|---|---|---|---|---|
| Error/empty/loading states | Test app on phone #1 with mocked blocker | Test app on phone #2 | Build lock list screen (real `getInstalledApps()` call), permissions explainer screens, Gentle/Strict toggle | `OverlayService.kt` + overlay XML matching Figma |
| App icon + splash | | | Wire real `SadhanaBlocker` import behind a feature flag | `BootReceiver.kt`, `HealthCheckWorker.kt` |
| | | | Diagnostics screen consumes real events | OEM-specific battery optimization handling (Xiaomi, OnePlus, Samsung) |

**Exit criterion:** First end-to-end working build. Open Instagram → overlay appears → tap "Om" → unlock. Tested on 2 OEMs.

---

### Week 4 — Resilience + Play Store prep

| Vibhor | Aman | Nilay | Emergent | Nilay (Claude Code) |
|---|---|---|---|---|
| Design QA pass | Play Store listing draft, AccessibilityService declaration, privacy policy, data safety | Full E2E test new-user flow | Notifications, festival reminders, daily check-in polish, offline support | Edge cases: rapid app switching, back button, screen rotation, low memory |
| Play Store screenshots | Production MongoDB setup | OEM coverage (Xiaomi, Samsung, OnePlus, Realme) | ProGuard rules for the native module (Kotlin classes must NOT be obfuscated) | ProGuard rules for AccessibilityService classes |

**Exit criterion:** Release-candidate APK signed and ready.

---

### Week 5 — Submit + Iterate

| Vibhor | Aman | Nilay | Emergent | Nilay (Claude Code) |
|---|---|---|---|---|
| Fix QA-flagged design mismatches | Generate signing key (back up!), submit to Play Store closed testing, respond to Google review | Recruit 10–20 testers, content rating questionnaire, Hindu content expert review | Bug fixes from Nilay's QA list | Bug fixes from Nilay's QA list |

**Exit criterion:** App submitted to Play Store closed testing.

---

## 10. Integration Strategy (How both APKs become one)

Emergent and Claude Code both push to `main` of the same repo. The integration model is:

1. **Branch protection:** `main` is protected. All work happens on feature branches.
   - Track A branches: `feat/a-*` (e.g., `feat/a-scripture-reader`)
   - Track B branches: `feat/b-*` (e.g., `feat/b-overlay-service`)
2. **No file overlap by convention:**
   - Track A never edits `/frontend/modules/sadhana-blocker/android/**`
   - Track B never edits `/frontend/app/**` or `/frontend/src/**`
   - Both can edit `docs/NATIVE_MODULE_CONTRACT.md` and `src/SadhanaBlocker.types.ts` but only via PR (the other side reviews).
3. **CI on every PR:**
   - Track A PRs: lint TS, run `expo prebuild --no-install`, build APK via EAS.
   - Track B PRs: `./gradlew :sadhana-blocker:assembleRelease`, run instrumented test on `example-android`.
4. **Weekly integration build:** On Fridays, EAS Build produces a signed APK from `main` HEAD. Aman and Nilay test it on real phones over the weekend.

---

## 11. The Prompt to Hand to Nilay (Claude Code)

> Nilay copies this verbatim into Claude Code (Cursor / Claude desktop / CLI) after cloning the repo.

```
You are Claude Code working with Nilay on the SadhanaLock project — a Hindu prayer app
that blocks distracting apps until the user prays.

YOUR SCOPE: The native Android blocker engine ONLY. You will work exclusively
inside `/frontend/modules/sadhana-blocker/android/`. You will NEVER touch
`/frontend/app/`, `/frontend/src/`, or `/backend/`.

YOUR DELIVERABLE: A working Expo Module (Kotlin) that fulfills the contract
in `/frontend/modules/sadhana-blocker/src/index.ts` and
`/docs/NATIVE_MODULE_CONTRACT.md`.

READ FIRST, IN ORDER:
1. /PLAN.md  (master plan; read §3 "Claude Code builds", §4 contract, §5 schema, §7 never-block rules)
2. /docs/NATIVE_MODULE_CONTRACT.md  (the API you must implement)
3. /docs/REVIEW-ENG.md  (Android gotchas, OEM behaviour, package recs)
4. /frontend/modules/sadhana-blocker/src/SadhanaBlocker.types.ts  (TS contract)
5. /frontend/modules/sadhana-blocker/src/SadhanaBlocker.mock.ts  (the behaviour you must match in Kotlin)

YOUR WORKFLOW:
- Branch naming: feat/b-<short-description>
- Commit message prefix: [blocker]
- Each commit must compile (`./gradlew assembleDebug`)
- Open PR when feature is done; assign Aman for review
- Match the contract EXACTLY. Do not add methods. Do not rename methods. If
  you believe the contract is wrong, open an issue tagged `contract-change`
  and wait for review before changing it.

BUILD ORDER (recommended):
1. Hello-world overlay (any app trigger → show "Hare Krishna" on screen)
2. PermissionHelper.kt (Accessibility + Overlay permission flows)
3. AppListHelper.kt (real getInstalledApps)
4. PrefsStore.kt (shared state per §5)
5. SadhanaAccessibilityService.kt (foreground detection + interception)
6. OverlayService.kt + overlay_prayer.xml (the prayer UI, matches Figma in /design/overlay/)
7. BootReceiver.kt + HealthCheckWorker.kt (resilience)
8. SadhanaBlockerModule.kt (Expo bridge — last, since other pieces feed it)
9. OEM-specific handling (Xiaomi MIUI, OnePlus, Samsung)
10. ProGuard rules

TEST DEVICE TARGETS:
- Pixel (stock Android, baseline)
- Xiaomi/Redmi (MIUI — most hostile to AccessibilityService)
- Samsung (One UI)
- OnePlus (OxygenOS)

ASK FOR HELP WHEN:
- A required permission seems blocked by Android policy
- An OEM behaves differently than the others
- The contract appears to need a new method/event (open `contract-change` issue)

DO NOT:
- Touch Expo / React Native files
- Change the package name
- Use deprecated APIs (DeviceAdminReceiver, etc.)
- Add Firebase or any tracking
- Block your work waiting for the Expo side — you have a complete example-android target
```

---

## 12. GitHub Setup Instructions (One-time, do today)

1. **Aman creates the repo** on GitHub: `sadhana-lock` (private to start).
2. **From Emergent**, click **"Save to GitHub"** in the Emergent UI. Point it at the new repo. This pushes the current `/app/` contents (which become `/frontend/` and `/backend/` after the Week-1 restructure).
3. **Locally**, Aman/Claude Code clones: `git clone git@github.com:<org>/sadhana-lock.git`
4. Add branch protection on `main` (require 1 PR review, require CI green).
5. Invite Vibhor (read), Nilay (write), Claude Code operator (write).
6. Set up two GitHub Actions:
   - `.github/workflows/expo-ci.yml` — runs on Track A PRs.
   - `.github/workflows/android-ci.yml` — runs on Track B PRs.
7. Vibhor pushes initial Figma exports to `/design/`.
8. Hand Claude Code the prompt from §11 and the repo URL.

---

## 13. Critical Risks & Mitigations

| Risk | Severity | Mitigation | Owner |
|---|---|---|---|
| Google rejects AccessibilityService declaration | CRITICAL | Strong, honest declaration in Play Console; show the user-facing prayer flow in the demo video; submit Week 5 with buffer | Aman |
| Android 15/16 further restricts AccessibilityService | HIGH | Claude Code implements UsageStatsManager fallback in same module | Nilay (Claude Code) |
| OEM kills background service (Xiaomi most aggressive) | HIGH | HealthCheckWorker + per-OEM autostart deep links + clear user instructions | Nilay (Claude Code) |
| Mock and real module diverge | MEDIUM | Contract owns the types; PR review on any contract change; mock has unit tests matching real behaviour | Both |
| EAS Build can't find the local module | MEDIUM | Module declared in `app.json` `expo.plugins`; tested in Week-1 CI | Emergent |
| Devanagari rendering broken on overlay (Kotlin XML) | MEDIUM | Use the same Noto Sans Devanagari font bundled in module assets; test in Week 3 | Nilay (Claude Code) |
| ProGuard obfuscates AccessibilityService class (Play removes it) | MEDIUM | Explicit `-keep` rules in `proguard-rules.pro`; Track B owns these rules | Nilay (Claude Code) |
| Content inaccuracy (wrong mantra/deity attribute) | MEDIUM | Nilay verifies, gets pandit review before launch | Nilay |
| Vibhor's overlay design lands late | LOW | Claude Code ships v1 overlay with placeholder layout (Week 1 hello-world); swaps to final XML when design lands | Nilay (Claude Code) |
| Night Lock alarm deferred by Doze mode | HIGH | Use `setExactAndAllowWhileIdle()` for AlarmManager; test on Xiaomi/Samsung | Nilay (Claude Code) |
| Recent Apps swipe bypasses overlay | HIGH | Use `TYPE_ACCESSIBILITY_OVERLAY` flag + `FLAG_NOT_FOCUSABLE`; test bypass vectors | Nilay (Claude Code) |
| User blocks launcher or SadhanaLock itself | MEDIUM | Hard-coded protected-apps denylist in `AppListHelper.kt`; see `docs/BLOCKING_LOGIC.md` §7 | Nilay (Claude Code) |
| SharedPreferences cross-process invisible | HIGH | Module-first IPC (resolved); SharedPreferences for crash recovery only; see `docs/BLOCKING_LOGIC.md` §1 | Nilay (Claude Code) |

---

## 14. Open Questions (resolve in Week 1 kickoff)

1. **Primary language at launch:** ~~Hindi + English?~~ **RESOLVED: Hindi, English, and Hinglish.** Language selection in onboarding flow. All three available from V1.
2. **Auth model V1:** ~~Fully anonymous or Google Auth from Day 1?~~ **RESOLVED: Google Sign-In via Emergent built-in auth.** Emergent handles the full auth flow. Track A owns this.
3. **Monetization V1:** ~~Free forever, or "Plus" tier?~~ **RESOLVED: Free for now.** No monetization in V1.
4. **Content storage V1:** Bundled JSON only, or Mongo sync? Default: bundled + optional Mongo sync for future content updates without app release.
5. **Crashes/analytics:** ~~Sentry, Firebase Crashlytics, or none V1?~~ **RESOLVED: Firebase Crashlytics from Day 1** (Crashlytics only, no other Firebase services).
6. **Pandit/expert reviewer:** ~~Who is the final authority on Sanskrit accuracy?~~ **RESOLVED: Not required for V1.** Content accuracy handled by the team internally.
7. **Blocker engine design decisions:** ~~8 open items in docs/BLOCKING_LOGIC.md.~~ **RESOLVED: All 8 decisions made on 2026-05-28.** See docs/BLOCKING_LOGIC.md section 12 for the full resolution list. Track B can start immediately.

---

## 15. Definition of Done (V1.0)

The app ships when ALL of the following are true:

- [ ] Onboarding works (language → deity → permission explainers)
- [ ] User can browse Bhagavad Gita (700 verses), at least 4 Chalisas, at least 4 Stotrams
- [ ] Verse-of-the-Day rotates daily and is shown on Home
- [ ] User can pick from 7 deities; theming adapts
- [ ] Daily Panchang (tithi, day) renders correctly; festival list shows next 30 days
- [ ] Daily check-in records and persists
- [ ] User can pick apps to block from the lock list (real installed apps)
- [ ] Opening a blocked app shows the deity-themed prayer overlay
- [ ] Tapping "Om" dismisses the overlay; user can use the app for the unlock window
- [ ] Gentle vs Strict mode behaves as specified
- [ ] Night Lock activates at scheduled time — blocked apps are hard-locked (no prayer bypass)
- [ ] Morning Prayer Gate — first prayer of the day unlocks all apps
- [ ] UsageStatsManager fallback works if Accessibility permission unavailable
- [ ] Firebase Crashlytics reports crashes from both Expo and Kotlin layers
- [ ] Service survives a reboot
- [ ] Service survives Xiaomi MIUI kill (most hostile OEM)
- [ ] Battery-optimization-ignored guidance is shown to user
- [ ] Devanagari renders correctly in both Expo screens and the Kotlin overlay
- [ ] Notifications fire for major festivals
- [ ] Offline mode works (no network → app fully functional from bundled JSON)
- [ ] APK is signed, ProGuard'd, and under 30 MB
- [ ] Play Store listing complete; AccessibilityService declaration submitted
- [ ] 10 closed-test users have used the app for 7 days without critical bugs
- [ ] Content accuracy verified by the team (no external pandit required)

---

## 16. Next Steps (TODAY)

1. **Read this entire document** — Aman, Vibhor, Nilay.
2. **Confirm scope split** — anything we missed?
3. **~~Create the GitHub repo~~** (DONE — `amans-coder/sanatan-lock`).
3b. **Generate Play Store signing key** and back up securely. Do NOT defer to Week 5.
3c. **Rename `app.json`** from "frontend" to "SadhanaLock" / "sadhanalock" before first EAS build.
4. **Resolve §14 open questions** in a 30-min call.
5. **Emergent kickoff:** I will scaffold the Expo app, the mocked native module, the contract types, the design tokens, and the docs structure — and push to GitHub.
6. **Track B kickoff:** Nilay pastes the §11 prompt into Claude Code with the repo URL. Nilay begins from `/frontend/modules/sadhana-blocker/`.
7. **Daily 15-min sync** for the next 5 weeks.

---

> "Om Sarve Bhavantu Sukhinah" — May all be happy. Let's ship.
