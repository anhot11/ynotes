package app.uamo.ynotes.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.unit.ColorProvider
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.uamo.ynotes.MainActivity
import app.uamo.ynotes.R
import app.uamo.ynotes.data.NoteDatabase
import app.uamo.ynotes.data.NoteEntity
import kotlinx.coroutines.flow.firstOrNull

class YNotesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sharedPrefs = context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE)
        val areWidgetsEnabled = sharedPrefs.getBoolean("WIDGETS_ENABLED", true)
        val isUnlocked = sharedPrefs.getBoolean("widget_unlocked", false)
        val unlockExpiry = sharedPrefs.getLong("widget_unlock_expiry", 0)
        
        val currentlyUnlocked = areWidgetsEnabled && isUnlocked && System.currentTimeMillis() < unlockExpiry
        
        val showSafeZoneNotes = sharedPrefs.getBoolean("widget_show_safe_zone", false)

        val notes = if (currentlyUnlocked) {
            val db = NoteDatabase.getDatabase(context).noteDao()
            // Ensure expired notes are cleaned up when widget updates (even if app is closed)
            db.deleteExpiredNotes()
            
            if (showSafeZoneNotes) {
                db.getSecretNotes().firstOrNull() ?: emptyList()
            } else {
                db.getPublicNotes().firstOrNull() ?: emptyList()
            }
        } else {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    areWidgetsEnabled = areWidgetsEnabled,
                    isUnlocked = currentlyUnlocked,
                    notes = notes,
                    onLockClick = actionRunCallback<LockAction>(),
                    onUnlockClick = actionRunCallback<UnlockAction>()
                )
            }
        }
    }
}

@Composable
fun WidgetContent(
    areWidgetsEnabled: Boolean,
    isUnlocked: Boolean,
    notes: List<NoteEntity>,
    onLockClick: androidx.glance.action.Action,
    onUnlockClick: androidx.glance.action.Action
) {
    val context = LocalContext.current
    
    // Custom colors matching the app's AMOLED/Glass theme
    val surfaceColor = ColorProvider(Color(0xFF121212))
    val cardColor = ColorProvider(Color(0xFF1E1E1E))
    val textColor = ColorProvider(Color(0xFFF3F4F6))
    val subtextColor = ColorProvider(Color(0xFF9CA3AF))
    val accentColor = ColorProvider(Color(0xFF8B5CF6)) // PrimaryAccent

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "yNotes",
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                
                Image(
                    provider = ImageProvider(android.R.drawable.ic_secure),
                    contentDescription = if (isUnlocked) "Bloquear" else "Desbloquear",
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(if (isUnlocked) onLockClick else onUnlockClick)
                )
            }
            
            // Content
            if (!areWidgetsEnabled) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().clickable(onUnlockClick),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            provider = ImageProvider(android.R.drawable.ic_secure),
                            contentDescription = "Disabled",
                            modifier = GlanceModifier.size(42.dp).padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Widgets desactivados desde la Zona Segura",
                            style = TextStyle(color = subtextColor, fontSize = 12.sp)
                        )
                    }
                }
            } else if (!isUnlocked) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().clickable(onUnlockClick),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            provider = ImageProvider(android.R.drawable.ic_secure),
                            contentDescription = "Locked",
                            modifier = GlanceModifier.size(42.dp).padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Toca para desbloquear (10s)",
                            style = TextStyle(color = textColor, fontSize = 14.sp)
                        )
                    }
                }
            } else if (notes.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No hay notas",
                        style = TextStyle(color = subtextColor, fontSize = 14.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(notes) { note ->
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(cardColor)
                                .cornerRadius(12.dp)
                                .clickable {
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    context.startActivity(intent)
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                if (note.title.isNotEmpty()) {
                                    Text(
                                        text = note.title,
                                        style = TextStyle(
                                            color = textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        ),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = GlanceModifier.height(4.dp))
                                }
                                if (note.body.isNotEmpty()) {
                                    Text(
                                        text = note.body.replace("\n", " "),
                                        style = TextStyle(
                                            color = subtextColor,
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class UnlockAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, WidgetAuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}

class LockAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val sharedPrefs = context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("widget_unlocked", false)
            .putLong("widget_unlock_expiry", 0)
            .apply()
        
        YNotesWidget().update(context, glanceId)
    }
}
