package expo.modules.sadhanablocker.engine

import expo.modules.sadhanablocker.BlockerEventSink

/**
 * Process-wide rendezvous between the JS bridge and whichever engine is live.
 *
 * Module-first IPC (BLOCKING_LOGIC §1): SadhanaBlockerModule reaches the running engine
 * through [activeEngine] for immediate effect, and pushes events back to JS through
 * [eventSink]. Exactly one engine is active at a time — either the AccessibilityService's
 * or the UsageStats fallback's — whichever detection path is in use (§4).
 */
object BlockerRuntime {

    @Volatile
    var activeEngine: BlockerEngine? = null
        private set

    /** Set by SadhanaBlockerModule; the live engine emits events through it. */
    @Volatile
    var eventSink: BlockerEventSink? = null

    fun register(engine: BlockerEngine) {
        activeEngine = engine
        eventSink?.onServiceStateChanged(true)
    }

    fun unregister(engine: BlockerEngine) {
        if (activeEngine === engine) {
            activeEngine = null
            eventSink?.onServiceStateChanged(false)
        }
    }

    fun isRunning(): Boolean = activeEngine != null
}
