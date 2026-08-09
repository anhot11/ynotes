package app.uamo.ynotes.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = YNotesWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_SCREEN_OFF || intent.action == Intent.ACTION_USER_PRESENT) {
            val sharedPrefs = context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putBoolean("widget_unlocked", false)
                .putLong("widget_unlock_expiry", 0)
                .apply()

            CoroutineScope(Dispatchers.IO).launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}
