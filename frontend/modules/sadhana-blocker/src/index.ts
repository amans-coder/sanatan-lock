// Public JS surface for the SadhanaLock blocker engine.
// Track A consumes ONLY what is exported here. Keep in lockstep with
// SadhanaBlocker.mock.ts (Track A) — any signature change must update both.

import type { EventSubscription } from 'expo';

import SadhanaBlockerModule from './SadhanaBlockerModule';
import type {
  BlockMode,
  BlockerState,
  InstalledApp,
  OverlayShownEvent,
  PrayerCompletedEvent,
  PrayerContentForOverlay,
  ServiceStateChangedEvent,
} from './SadhanaBlocker.types';

export * from './SadhanaBlocker.types';

// --- Permissions ---
export function requestAccessibilityPermission(): Promise<void> {
  return SadhanaBlockerModule.requestAccessibilityPermission();
}
export function isAccessibilityEnabled(): Promise<boolean> {
  return SadhanaBlockerModule.isAccessibilityEnabled();
}
export function requestOverlayPermission(): Promise<void> {
  return SadhanaBlockerModule.requestOverlayPermission();
}
export function isOverlayPermissionGranted(): Promise<boolean> {
  return SadhanaBlockerModule.isOverlayPermissionGranted();
}
export function requestIgnoreBatteryOptimization(): Promise<void> {
  return SadhanaBlockerModule.requestIgnoreBatteryOptimization();
}
export function isBatteryOptimizationIgnored(): Promise<boolean> {
  return SadhanaBlockerModule.isBatteryOptimizationIgnored();
}

// --- App discovery ---
export function getInstalledApps(): Promise<InstalledApp[]> {
  return SadhanaBlockerModule.getInstalledApps();
}
export function getAppIcon(packageName: string): Promise<string | null> {
  return SadhanaBlockerModule.getAppIcon(packageName);
}

// --- Configuration ---
export function setBlockedApps(packages: string[]): Promise<void> {
  return SadhanaBlockerModule.setBlockedApps(packages);
}
export function getBlockedApps(): Promise<string[]> {
  return SadhanaBlockerModule.getBlockedApps();
}
export function setBlockingMode(mode: BlockMode): Promise<void> {
  return SadhanaBlockerModule.setBlockingMode(mode);
}
export function getBlockingMode(): Promise<BlockMode> {
  return SadhanaBlockerModule.getBlockingMode();
}

// --- Content handoff ---
export function setOverlayContent(content: PrayerContentForOverlay): Promise<void> {
  return SadhanaBlockerModule.setOverlayContent(content);
}
export function setSelectedDeity(deityId: string): Promise<void> {
  return SadhanaBlockerModule.setSelectedDeity(deityId);
}

// --- Morning gate (decision #8/#16) ---
export function markMorningPrayerDone(): Promise<void> {
  return SadhanaBlockerModule.markMorningPrayerDone();
}

// --- Lifecycle ---
export function startBlockerService(): Promise<void> {
  return SadhanaBlockerModule.startBlockerService();
}
export function stopBlockerService(): Promise<void> {
  return SadhanaBlockerModule.stopBlockerService();
}
export function getBlockerState(): Promise<BlockerState> {
  return SadhanaBlockerModule.getBlockerState();
}

// --- Manual trigger (testing) ---
export function debugShowOverlay(): Promise<void> {
  return SadhanaBlockerModule.debugShowOverlay();
}

// --- Events (Kotlin → TS) ---
export function addOverlayShownListener(
  listener: (event: OverlayShownEvent) => void
): EventSubscription {
  return SadhanaBlockerModule.addListener('onOverlayShown', listener);
}
export function addPrayerCompletedListener(
  listener: (event: PrayerCompletedEvent) => void
): EventSubscription {
  return SadhanaBlockerModule.addListener('onPrayerCompleted', listener);
}
export function addServiceStateChangedListener(
  listener: (event: ServiceStateChangedEvent) => void
): EventSubscription {
  return SadhanaBlockerModule.addListener('onServiceStateChanged', listener);
}
