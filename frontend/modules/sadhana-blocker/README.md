# sadhana-blocker (Track B — Native Android Blocker Engine)

The Kotlin Expo Module that detects the foreground app and shows a prayer overlay until the
user prays. Owner: **Nilay (Claude Code)**. Scope: this directory only — never
`frontend/app/`, `frontend/src/`, or `backend/`.

Implements the contract in [`src/SadhanaBlocker.types.ts`](src/SadhanaBlocker.types.ts) /
[`/docs/NATIVE_MODULE_CONTRACT.md`](../../../docs/NATIVE_MODULE_CONTRACT.md). Design
decisions: [`/docs/BLOCKING_LOGIC.md`](../../../docs/BLOCKING_LOGIC.md).

## Architecture

```
JS (Track A)
  │  requireNativeModule('SadhanaBlocker')
  ▼
SadhanaBlockerModule.kt ──set*()──▶ PrefsStore (crash-recovery backup)
  │                      └─────────▶ BlockerRuntime.activeEngine.loadConfig()  (live, §1)
  ▲  events                                   │
  └── BlockerEventSink ◀── BlockerRuntime ◀── BlockerEngine  (the §2 state machine)
                                              ▲          │ shows
                          ┌───────────────────┘          ▼
            SadhanaAccessibilityService          OverlayController
            (primary, TYPE_ACCESSIBILITY_OVERLAY)  (gentle/strict, debounce, call-dismiss)
                          OR
            UsageStatsForegroundService           OverlayController
            (fallback §4, TYPE_APPLICATION_OVERLAY)
```

One `BlockerEngine` is live at a time, registered in `BlockerRuntime`. Both detection paths
drive the **same** engine, so behaviour is identical regardless of how the foreground app is
detected.

### File map

| Area | Files |
|---|---|
| Bridge | `SadhanaBlockerModule.kt`, `BlockerEvents.kt` |
| Engine | `engine/BlockerEngine.kt`, `engine/BlockerRuntime.kt` |
| Detection | `service/SadhanaAccessibilityService.kt`, `work/UsageStatsForegroundService.kt` |
| Overlay | `overlay/OverlayController.kt`, `res/layout/overlay_prayer.xml` |
| Resilience | `service/BootReceiver.kt`, `work/HealthCheckWorker.kt`, `schedule/ScheduleManager.kt` |
| Data | `store/PrefsStore.kt` (13-key schema §8), `model/*` |
| Discovery/Perms | `util/AppListHelper.kt`, `util/PermissionHelper.kt`, `util/NotificationHelper.kt` |

## ⚠️ Build gates

1. ✅ **`android.package` set** — Aman landed `com.sadhanalock.app` on main (2026-05-29).
   We do NOT hard-code the package anywhere; self-protection derives from `context.packageName`.
2. ⏳ **JDK 17** — `./gradlew assembleDebug` needs it. Install: `brew install --cask zulu@17`
   (or Temurin 17). Last remaining gate on the dev machine.
3. **Managed workflow** — no `android/` dir. `npx expo prebuild -p android` generates it.

### Bring-up once gates clear
```bash
cd frontend
npm install
npx expo prebuild -p android          # generates android/, autolinks this module
npx expo run:android                   # or: cd android && ./gradlew assembleDebug
```
Local Expo modules in `modules/` are autolinked automatically; `expo-module.config.json`
registers the Kotlin module class.

## ✅ Resolved contract item

The **Morning Gate** (decision #8) requires praying *in the app*, not on the overlay. The
JS → native signal `markMorningPrayerDone()` was **approved** (decision #16, PLAN §4.2,
2026-05-29) and is now implemented end-to-end: Track A calls it from `/prayer/[deity]` on
prayer completion → native sets `morning_prayer_done_today` (commit) and tears down any live
morning-gate overlay; `ScheduleManager` resets it nightly. `getAppIcon()` (decision #17) is
the other approved addition.

## Manual test checklist (on device, after bring-up)

- [ ] Enable Accessibility + Overlay perms via onboarding; `getBlockerState()` reflects them.
- [ ] Lock Instagram (gentle) → open it → overlay appears over it and over Recent Apps (§6).
- [ ] Tap Om → overlay dismisses, app usable for cooldown minutes; re-open within cooldown → no overlay.
- [ ] Strict mode → Om disabled for 10s, then tappable (decision #7). **Confirm on a chatty
      app (e.g. WhatsApp) that fires many window events — the 10s timer must NOT keep
      resetting** (guarded in `BlockerEngine.onForeground`; unverifiable without a device).
- [ ] Double-tap Om → single completion (500ms debounce, §11 #5).
- [ ] Incoming call while overlay up → overlay auto-dismisses (§6).
- [ ] Reboot → re-open locked app → still blocked (BootReceiver + auto-rebind).
- [ ] Try to lock the launcher / dialer / SadhanaLock itself → not offered / not persisted (§7).
- [ ] Night lock window → locked app shows hard block, no Om.
- [ ] OEM kill (Xiaomi: force stop) → HealthCheckWorker notification within ~15 min.
