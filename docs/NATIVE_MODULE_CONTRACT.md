# SadhanaLock — Native Module Contract

> **Source of truth for the TS ↔ Kotlin boundary.** Mirrors PLAN.md §4. Both tracks must
> respect it. Any change requires a PR that updates the TS types, the Kotlin module, AND the
> Track-A mock (`SadhanaBlocker.mock.ts`) together.
>
> Canonical TS lives at `frontend/modules/sadhana-blocker/src/SadhanaBlocker.types.ts`.

## Module name
`requireNativeModule('SadhanaBlocker')` ↔ Kotlin `Name("SadhanaBlocker")`.

## Methods (TS → Kotlin)

| Method | Returns | Notes |
|---|---|---|
| `requestAccessibilityPermission()` | `void` | Opens system Accessibility settings |
| `isAccessibilityEnabled()` | `boolean` | Is our service enabled |
| `requestOverlayPermission()` | `void` | Opens "Display over other apps" |
| `isOverlayPermissionGranted()` | `boolean` | `Settings.canDrawOverlays` |
| `requestIgnoreBatteryOptimization()` | `void` | Opens battery-exemption dialog |
| `isBatteryOptimizationIgnored()` | `boolean` | |
| `getInstalledApps()` | `InstalledApp[]` | **No icons** — `iconBase64` is always `null` (§3) |
| `getAppIcon(packageName)` | `string \| null` | Paginated PNG base64 (§3). **Only addition beyond PLAN §4.2** |
| `setBlockedApps(packages)` | `void` | Denylisted packages are silently dropped (§7) |
| `getBlockedApps()` | `string[]` | |
| `setBlockingMode(mode)` | `void` | `'gentle' \| 'strict'` |
| `getBlockingMode()` | `BlockMode` | |
| `setOverlayContent(content)` | `void` | `PrayerContentForOverlay` |
| `setSelectedDeity(deityId)` | `void` | |
| `markMorningPrayerDone()` | `void` | JS calls on in-app prayer completion; clears the morning gate (decision #16). Overlay completion does NOT count |
| `startBlockerService()` | `void` | Master enable; arms health check + daily reset |
| `stopBlockerService()` | `void` | User pause; dismisses overlay |
| `getBlockerState()` | `BlockerState` | |
| `debugShowOverlay()` | `void` | Throws `E_NO_ENGINE` if no detection path is live |

## Events (Kotlin → TS)

| Event | Payload |
|---|---|
| `onOverlayShown` | `{ packageName, timestamp }` |
| `onPrayerCompleted` | `{ packageName, durationMs, timestamp }` |
| `onServiceStateChanged` | `{ isRunning }` |

## Contract changes (approved 2026-05-29)

| # | Addition | Status |
|---|---|---|
| 16 | `markMorningPrayerDone(): Promise<void>` | ✅ Approved + implemented (native). Track A wires the prayer screen + mock. |
| 17 | `getAppIcon(packageName): Promise<string \| null>` | ✅ Approved + implemented (native). |
