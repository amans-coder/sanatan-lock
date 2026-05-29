package expo.modules.sadhanablocker.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import expo.modules.sadhanablocker.R
import expo.modules.sadhanablocker.model.BlockMode
import expo.modules.sadhanablocker.model.OverlayContent

/**
 * Owns the prayer overlay window (PLAN §3.6, BLOCKING_LOGIC §2 / §6).
 *
 * Window type is configurable:
 *  - TYPE_ACCESSIBILITY_OVERLAY (default): renders above everything incl. the Recent Apps
 *    switcher (§6 swipe-bypass mitigation). MUST be added from an AccessibilityService
 *    context — used by SadhanaAccessibilityService.
 *  - TYPE_APPLICATION_OVERLAY: used by the UsageStatsManager fallback path (§4), where no
 *    accessibility service is available; relies on the SYSTEM_ALERT_WINDOW grant.
 *
 * Responsibilities:
 *  - gentle vs strict gating of the Om button (decision #7)
 *  - 500 ms double-tap debounce (§11 #5)
 *  - auto-dismiss on incoming call (§6)
 *  - portrait lock (§6)
 */
class OverlayController(
    private val context: Context,
    private val overlayType: Int =
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootView: View? = null
    private var lastOmTapAt = 0L

    // Pending strict-mode reveal so we can cancel it on dismiss.
    private var strictReveal: Runnable? = null

    // Telephony auto-dismiss
    private val telephony =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var telephonyCallback: TelephonyCallback? = null
    @Suppress("DEPRECATION")
    private var legacyPhoneListener: android.telephony.PhoneStateListener? = null

    val isShowing: Boolean get() = rootView != null

    // ---------------------------------------------------------------------------------
    // Prayer overlay (PRAYER_SHOWN)
    // ---------------------------------------------------------------------------------

    /**
     * @param onCompleted invoked on the main thread when the user finishes the prayer
     *   (after debounce + strict wait). Caller records stats, cooldown, and emits the event.
     */
    fun showPrayer(content: OverlayContent?, mode: BlockMode, onCompleted: () -> Unit) {
        mainHandler.post {
            val view = ensureView()
            applyContent(view, content)
            view.findViewById<TextView>(R.id.om_button).visibility = View.VISIBLE
            val strictHint = view.findViewById<TextView>(R.id.strict_hint)
            val om = view.findViewById<TextView>(R.id.om_button)

            if (mode == BlockMode.STRICT) {
                // Forced 10 s stillness, THEN the Om becomes tappable (decision #7).
                om.isEnabled = false
                om.alpha = 0.35f
                strictHint.visibility = View.VISIBLE
                strictReveal = Runnable {
                    om.isEnabled = true
                    om.alpha = 1f
                    strictHint.visibility = View.GONE
                }.also { mainHandler.postDelayed(it, STRICT_WAIT_MS) }
            } else {
                om.isEnabled = true
                om.alpha = 1f
                strictHint.visibility = View.GONE
            }

            om.setOnClickListener {
                if (!om.isEnabled) return@setOnClickListener
                val now = SystemClock.elapsedRealtime()
                if (now - lastOmTapAt < DEBOUNCE_MS) return@setOnClickListener // §11 #5
                lastOmTapAt = now
                onCompleted()
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Night-lock overlay (hard block, no Om — decision §2)
    // ---------------------------------------------------------------------------------

    fun showNightLock() = showMessageOnly(
        title = context.getString(R.string.overlay_night_lock_title),
        body = context.getString(R.string.overlay_night_lock_text)
    )

    // ---------------------------------------------------------------------------------
    // Morning-gate overlay (must pray in-app to unlock — decision #8)
    // ---------------------------------------------------------------------------------

    fun showMorningGate() = showMessageOnly(
        title = context.getString(R.string.overlay_default_mantra_dev),
        body = "Begin your day with prayer. Open SadhanaLock and complete the morning sadhana to unlock your apps."
    )

    private fun showMessageOnly(title: String, body: String) {
        mainHandler.post {
            val view = ensureView()
            view.findViewById<ImageView>(R.id.deity_image).visibility = View.GONE
            view.findViewById<TextView>(R.id.mantra_devanagari).text = title
            view.findViewById<TextView>(R.id.mantra_translit).visibility = View.GONE
            view.findViewById<TextView>(R.id.mantra_meaning).text = body
            view.findViewById<TextView>(R.id.strict_hint).visibility = View.GONE
            view.findViewById<TextView>(R.id.om_button).visibility = View.GONE
        }
    }

    // ---------------------------------------------------------------------------------
    // Dismiss
    // ---------------------------------------------------------------------------------

    fun dismiss() {
        mainHandler.post {
            strictReveal?.let { mainHandler.removeCallbacks(it) }
            strictReveal = null
            stopPhoneStateWatch()
            rootView?.let { v ->
                runCatching { windowManager.removeView(v) }
            }
            rootView = null
        }
    }

    // ---------------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------------

    @SuppressLint("InflateParams")
    private fun ensureView(): View {
        rootView?.let { return it }
        val view = LayoutInflater.from(context).inflate(R.layout.overlay_prayer, null)
        // addView can throw if overlay permission was revoked mid-session (§11 #2);
        // the service catches and surfaces a notification.
        windowManager.addView(view, buildLayoutParams())
        rootView = view
        startPhoneStateWatch()
        return view
    }

    private fun applyContent(view: View, content: OverlayContent?) {
        view.findViewById<TextView>(R.id.mantra_translit).visibility = View.VISIBLE
        if (content == null) return

        runCatching { Color.parseColor(content.bgColorHex) }
            .onSuccess { view.findViewById<View>(R.id.overlay_root).setBackgroundColor(it) }

        view.findViewById<TextView>(R.id.mantra_devanagari).text = content.mantraDevanagari
        view.findViewById<TextView>(R.id.mantra_translit).text = content.mantraTransliteration
        view.findViewById<TextView>(R.id.mantra_meaning).text = content.mantraMeaning

        val image = view.findViewById<ImageView>(R.id.deity_image)
        val bytes = runCatching { Base64.decode(content.deityImageBase64, Base64.DEFAULT) }.getOrNull()
        if (bytes != null && bytes.isNotEmpty()) {
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) {
                image.setImageDrawable(BitmapDrawable(context.resources, bmp))
                image.visibility = View.VISIBLE
            } else image.visibility = View.GONE
        } else {
            image.visibility = View.GONE
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            // Cover & consume touches to the app beneath, but don't grab the keyboard.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Portrait lock so a rotation can't briefly reveal the app beneath (§6).
            screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // ---- Incoming-call auto-dismiss (§6) ----

    private fun startPhoneStateWatch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    if (state != TelephonyManager.CALL_STATE_IDLE) dismiss()
                }
            }
            telephonyCallback = cb
            runCatching { telephony.registerTelephonyCallback(context.mainExecutor, cb) }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : android.telephony.PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (state != TelephonyManager.CALL_STATE_IDLE) dismiss()
                }
            }
            legacyPhoneListener = listener
            @Suppress("DEPRECATION")
            runCatching {
                telephony.listen(listener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
    }

    private fun stopPhoneStateWatch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephony.unregisterTelephonyCallback(it) }
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION")
            legacyPhoneListener?.let {
                telephony.listen(it, android.telephony.PhoneStateListener.LISTEN_NONE)
            }
            legacyPhoneListener = null
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 500L       // §11 #5
        private const val STRICT_WAIT_MS = 10_000L // decision #7
    }
}
