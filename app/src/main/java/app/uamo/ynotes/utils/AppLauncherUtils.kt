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
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        val bmp = drawable.bitmap
        if (bmp.config != Bitmap.Config.HARDWARE) {
            return bmp.asImageBitmap()
        }
    }
    
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 100
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 100
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    
    return bitmap.asImageBitmap()
}
