// SadhanaLock — Native Module Contract (Source of Truth)
// Mirrors PLAN.md §4.1. Track B (Kotlin) implements this exactly.
// The ONLY sanctioned addition beyond PLAN §4.2 is `getAppIcon()`, pre-blessed by
// docs/BLOCKING_LOGIC.md §3 (paginated icon loading) and PROGRESS Phase 2.

export type BlockMode = 'gentle' | 'strict';

export interface InstalledApp {
  packageName: string; // e.g. "com.instagram.android"
  appName: string; // e.g. "Instagram"
  iconBase64: string | null; // always null from getInstalledApps(); fetch via getAppIcon()
  isSystem: boolean;
}

export interface BlockerState {
  isAccessibilityEnabled: boolean;
  isOverlayPermissionGranted: boolean;
  isBatteryOptimizationIgnored: boolean;
  blockedPackages: string[];
  mode: BlockMode;
  selectedDeity: string; // e.g. "krishna"
}

export interface PrayerContentForOverlay {
  deity: string;
  mantraDevanagari: string;
  mantraTransliteration: string;
  mantraMeaning: string;
  bgColorHex: string; // deity-themed
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

export interface ServiceStateChangedEvent {
  isRunning: boolean;
}

// Event map consumed by the Expo EventEmitter (Kotlin → TS).
export type SadhanaBlockerModuleEvents = {
  onOverlayShown: (event: OverlayShownEvent) => void;
  onPrayerCompleted: (event: PrayerCompletedEvent) => void;
  onServiceStateChanged: (event: ServiceStateChangedEvent) => void;
};
