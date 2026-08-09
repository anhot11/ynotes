package app.uamo.ynotes.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream

/**
 * Manages a disk cache of hidden app info (name, package, icon) so the
 * SafeZone shortcut row can render instantly without scanning all installed apps.
 * Icons are stored as individual .png files in filesDir/app_icons to avoid Base64 bloating.
 */
object AppCacheManager {

    private const val CACHE_FILE = "hidden_apps_cache.json"
    private const val ICONS_DIR = "app_icons"
    private val gson = Gson()
    private val iconRamCache = LruCache<String, ImageBitmap>(32)

    /** Lightweight serializable DTO — only package name and app name */
    private data class CachedApp(
        val packageName: String,
        val name: String
    )

    /** Save the current list of hidden apps to disk cache */
    fun saveHiddenApps(context: Context, apps: List<AppInfo>) {
        val iconsDir = getIconsDir(context)
        if (!iconsDir.exists()) iconsDir.mkdirs()

        val cached = apps.map { app ->
            val iconFile = File(iconsDir, "${app.packageName}.png")
            try {
                val bmp = app.icon.asAndroidBitmap()
                val safeBmp = if (bmp.config == Bitmap.Config.HARDWARE) {
                    bmp.copy(Bitmap.Config.ARGB_8888, false) ?: bmp
                } else {
                    bmp
                }
                FileOutputStream(iconFile).use { fos ->
                    safeBmp.compress(Bitmap.CompressFormat.PNG, 90, fos)
                }
                iconRamCache.put(app.packageName, app.icon)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            CachedApp(
                packageName = app.packageName,
                name = app.name
            )
        }
        val json = gson.toJson(cached)
        getCacheFile(context).writeText(json)
    }

    /** Load cached hidden apps — returns empty list if no cache exists */
    fun loadHiddenApps(context: Context): List<AppInfo> {
        val file = getCacheFile(context)
        if (!file.exists()) return emptyList()

        return try {
            val iconsDir = getIconsDir(context)
            val json = file.readText()
            val type = object : TypeToken<List<CachedApp>>() {}.type
            val cached: List<CachedApp> = gson.fromJson(json, type) ?: return emptyList()
            cached.mapNotNull { c ->
                var icon = iconRamCache.get(c.packageName)
                if (icon == null) {
                    val iconFile = File(iconsDir, "${c.packageName}.png")
                    if (iconFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(iconFile.absolutePath)
                        if (bitmap != null) {
                            icon = bitmap.asImageBitmap()
                            iconRamCache.put(c.packageName, icon)
                        }
                    }
                }
                if (icon == null) {
                    val fallbackApp = getAppInfoForPackage(context, c.packageName)
                    if (fallbackApp != null) {
                        icon = fallbackApp.icon
                        iconRamCache.put(c.packageName, icon)
                    }
                }
                if (icon == null) return@mapNotNull null

                AppInfo(
                    packageName = c.packageName,
                    name = c.name,
                    icon = icon
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Clear the cache file and deleted icon files */
    fun clearCache(context: Context) {
        val file = getCacheFile(context)
        if (file.exists()) file.delete()
        val iconsDir = getIconsDir(context)
        if (iconsDir.exists()) iconsDir.deleteRecursively()
        iconRamCache.evictAll()
    }

    private fun getCacheFile(context: Context): File {
        return File(context.filesDir, CACHE_FILE)
    }

    private fun getIconsDir(context: Context): File {
        return File(context.filesDir, ICONS_DIR)
    }
}
