package expo.modules.sadhanablocker.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import expo.modules.sadhanablocker.model.InstalledAppInfo
import expo.modules.sadhanablocker.model.ProtectedPackages
import java.io.ByteArrayOutputStream

/**
 * App discovery (PLAN §4.2 app discovery, BLOCKING_LOGIC §3 paginated icons).
 *
 * [getInstalledApps] returns a lightweight list WITHOUT icons so we never push 4–10 MB
 * across the bridge at once (no ANR on low-end devices). The UI lazy-loads each icon via
 * [getAppIcon] as rows scroll into view.
 */
object AppListHelper {

    private const val ICON_PX = 96 // downscale target; keeps each base64 payload small

    /**
     * Launchable apps the user may choose to lock, excluding the hard denylist
     * (BLOCKING_LOGIC §7) and apps with no launcher entry. `isSystem` is reported so the
     * UI can group/sort, but pre-installed launchable apps (e.g. YouTube) are still shown.
     */
    fun getInstalledApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return installed.asSequence()
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filterNot { ProtectedPackages.isProtected(context, it.packageName) }
            .map { appInfo ->
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    /** PNG base64 icon for a single package, or null if it can't be extracted. */
    fun getAppIcon(context: Context, packageName: String): String? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        drawableToBase64(drawable)
    }.getOrNull()

    private fun drawableToBase64(drawable: Drawable): String {
        val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            Bitmap.createScaledBitmap(drawable.bitmap, ICON_PX, ICON_PX, true)
        } else {
            val bmp = Bitmap.createBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
