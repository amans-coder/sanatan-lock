import { NativeModule, requireNativeModule } from 'expo';

import type {
  BlockMode,
  BlockerState,
  InstalledApp,
  PrayerContentForOverlay,
  SadhanaBlockerModuleEvents,
} from './SadhanaBlocker.types';

declare class SadhanaBlockerModule extends NativeModule<SadhanaBlockerModuleEvents> {
  // --- Permissions ---
  requestAccessibilityPermission(): Promise<void>;
  isAccessibilityEnabled(): Promise<boolean>;
  requestOverlayPermission(): Promise<void>;
  isOverlayPermissionGranted(): Promise<boolean>;
  requestIgnoreBatteryOptimization(): Promise<void>;
  isBatteryOptimizationIgnored(): Promise<boolean>;

  // --- App discovery ---
  getInstalledApps(): Promise<InstalledApp[]>;
  getAppIcon(packageName: string): Promise<string | null>; // BLOCKING_LOGIC §3

  // --- Configuration ---
  setBlockedApps(packages: string[]): Promise<void>;
  getBlockedApps(): Promise<string[]>;
  setBlockingMode(mode: BlockMode): Promise<void>;
  getBlockingMode(): Promise<BlockMode>;

  // --- Content handoff (Expo writes, Kotlin reads) ---
  setOverlayContent(content: PrayerContentForOverlay): Promise<void>;
  setSelectedDeity(deityId: string): Promise<void>;

  // --- Morning gate (decision #8/#16) ---
  // Track A calls this from the prayer screen on successful in-app prayer. Overlay
  // completion does NOT count — only this clears the morning gate for the day.
  markMorningPrayerDone(): Promise<void>;

  // --- Lifecycle ---
  startBlockerService(): Promise<void>;
  stopBlockerService(): Promise<void>;
  getBlockerState(): Promise<BlockerState>;

  // --- Manual trigger (testing) ---
  debugShowOverlay(): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<SadhanaBlockerModule>('SadhanaBlocker');
