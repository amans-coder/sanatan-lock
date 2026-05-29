package expo.modules.sadhanablocker.model

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Hard-coded denylist (BLOCKING_LOGIC §7). The user CANNOT override this — there is no
 * Settings toggle. We never let the user lock these out, or they could brick their phone.
 */
object ProtectedPackages {

    // Static, OEM-spanning set. NOTE: "self" is intentionally NOT hard-coded to
    // com.sadhanalock.app — we derive it from the running context at check time so the
    // self-protection is correct no matter how Track A finally sets the package name.
    private val STATIC = setOf(
        "com.android.systemui",                       // system UI / nav bar
        "com.android.launcher",                       // generic launcher
        "com.android.launcher3",                      // AOSP launcher
        "com.google.android.apps.nexuslauncher",      // Pixel launcher
        "com.sec.android.app.launcher",               // Samsung One UI Home
        "com.miui.home",                              // Xiaomi MIUI Home
        "net.oneplus.launcher",                       // OnePlus launcher
        "com.android.dialer",                         // phone
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.contacts",                       // contacts
        "com.android.settings",                       // settings
        "com.android.emergency",                      // emergency info
        "com.android.phone",                          // telephony stack
        "com.android.server.telecom"
    )

    /**
     * @return true if [packageName] must never be blockable: it is the static set, is the
     *   app itself, or resolves as the current HOME (launcher) on this device.
     */
    fun isProtected(context: Context, packageName: String): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == context.packageName) return true      // self — derived, not hard-coded
        if (packageName in STATIC) return true
        return packageName in homePackages(context)
    }

    /** All packages that can act as HOME / launcher on this device (varies by OEM). */
    fun homePackages(context: Context): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val flags = PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_ALL
        return runCatching {
            context.packageManager.queryIntentActivities(intent, flags)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet())
    }
}
