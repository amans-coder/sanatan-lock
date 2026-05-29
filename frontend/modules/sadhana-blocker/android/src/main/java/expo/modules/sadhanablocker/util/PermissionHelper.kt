package expo.modules.sadhanablocker.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import expo.modules.sadhanablocker.service.SadhanaAccessibilityService

/**
 * Permission flows (PLAN §4.2 permissions block, BLOCKING_LOGIC §5).
 * Pure helpers — they open the right system screen or report a boolean. The retry /
 * deep-link escalation policy (§5 table) lives in the JS onboarding layer; we only
 * expose the primitives it needs.
 */
object PermissionHelper {

    // ---- Accessibility ----

    /** True iff our AccessibilityService is currently enabled by the user. */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${SadhanaAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        while (splitter.hasNext()) {
            val component = splitter.next()
            // Match on the flattened component OR the short form, since OEMs vary.
            if (component.equals(expected, ignoreCase = true) ||
                component.endsWith(SadhanaAccessibilityService::class.java.name)
            ) {
                return true
            }
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---- Overlay (SYSTEM_ALERT_WINDOW) ----

    fun isOverlayGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun requestOverlay(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---- Battery optimization ----

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressWarnings("BatteryLife")
    fun requestIgnoreBatteryOptimization(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ---- Usage access (for the UsageStatsManager fallback, §4) ----

    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
