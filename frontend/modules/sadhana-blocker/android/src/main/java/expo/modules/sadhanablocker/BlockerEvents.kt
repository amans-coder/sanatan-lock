package expo.modules.sadhanablocker

/**
 * Service → JS event channel (BLOCKING_LOGIC §1, "Kotlin sendEvent()"). The Expo module
 * registers itself as the sink; the AccessibilityService/overlay call these. Decoupled via
 * an interface so the engine has no compile dependency on Expo classes.
 */
interface BlockerEventSink {
    fun onOverlayShown(packageName: String, timestamp: Long)
    fun onPrayerCompleted(packageName: String, durationMs: Long, timestamp: Long)
    fun onServiceStateChanged(isRunning: Boolean)
}
