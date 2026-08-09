package app.uamo.ynotes.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: ImageBitmap
)

fun getInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    
    return resolveInfos.mapNotNull { resolveInfo ->
        val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
        val name = resolveInfo.loadLabel(pm).toString()
        val drawable = resolveInfo.loadIcon(pm)
        
        AppInfo(
            packageName = packageName,
            name = name,
            icon = drawableToImageBitmap(drawable)
        )
    }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
}

fun getAppInfoForPackage(context: Context, packageName: String): AppInfo? {
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        val name = pm.getApplicationLabel(appInfo).toString()
        val drawable = pm.getApplicationIcon(appInfo)
        AppInfo(
            packageName = packageName,
            name = name,
            icon = drawableToImageBitmap(drawable)
        )
    } catch (e: Exception) {
        null
    }
}

private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
    val targetSize = 96
    val rawBitmap: Bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null && drawable.bitmap.config != Bitmap.Config.HARDWARE) {
        drawable.bitmap
    } else {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetSize
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetSize
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp
    }

    val scaledBitmap = if (rawBitmap.width > targetSize || rawBitmap.height > targetSize) {
        Bitmap.createScaledBitmap(rawBitmap, targetSize, targetSize, true)
    } else {
        rawBitmap
    }

    return scaledBitmap.asImageBitmap()
}
