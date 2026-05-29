package expo.modules.sadhanablocker.model

/** Blocking strictness. Mirrors TS `BlockMode`. */
enum class BlockMode(val wire: String) {
    GENTLE("gentle"),
    STRICT("strict");

    companion object {
        fun from(value: String?): BlockMode =
            entries.firstOrNull { it.wire == value } ?: GENTLE
    }
}

/** Foreground-app detection strategy (BLOCKING_LOGIC §4). */
enum class DetectionMethod(val wire: String) {
    ACCESSIBILITY("accessibility"),
    USAGE_STATS("usage_stats");

    companion object {
        fun from(value: String?): DetectionMethod =
            entries.firstOrNull { it.wire == value } ?: ACCESSIBILITY
    }
}

/**
 * Overlay state machine (BLOCKING_LOGIC §2).
 *
 *  IDLE → CHECK_TIME → { NIGHT_LOCK | MORNING_GATE | PRAYER_SHOWN } → COOLDOWN → IDLE
 */
enum class OverlayState {
    IDLE,
    NIGHT_LOCK,
    MORNING_GATE,
    PRAYER_SHOWN,
    COOLDOWN
}

/** Prayer content the overlay renders. Mirrors TS `PrayerContentForOverlay`. */
data class OverlayContent(
    val deity: String,
    val mantraDevanagari: String,
    val mantraTransliteration: String,
    val mantraMeaning: String,
    val bgColorHex: String,
    val deityImageBase64: String
)

/** Lightweight app row (no icon — see getAppIcon). Mirrors TS `InstalledApp`. */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean
)
