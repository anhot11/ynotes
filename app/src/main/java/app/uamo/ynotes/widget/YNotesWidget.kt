package app.uamo.ynotes.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.uamo.ynotes.MainActivity
import app.uamo.ynotes.R
import app.uamo.ynotes.data.NoteDatabase
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.utils.CryptoManager
import kotlinx.coroutines.flow.firstOrNull

class YNotesWidget : GlanceAppWidget() {

    companion object {
        val SMALL_SQUARE = DpSize(100.dp, 100.dp)       // 2x2
        val HORIZONTAL_RECT = DpSize(240.dp, 100.dp)    // 4x2
        val BIG_SQUARE = DpSize(240.dp, 240.dp)         // 4x4
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL_SQUARE, HORIZONTAL_RECT, BIG_SQUARE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sharedPrefs = context.getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE)
        val widgetPrefs = context.getSharedPreferences("yNotesWidgetPrefs", Context.MODE_PRIVATE)

        val areWidgetsEnabled = sharedPrefs.getBoolean("WIDGETS_ENABLED", true)
        val isUnlocked = sharedPrefs.getBoolean("widget_unlocked", false)
        val unlockExpiry = sharedPrefs.getLong("widget_unlock_expiry", 0)
        val currentlyUnlocked = areWidgetsEnabled && isUnlocked && System.currentTimeMillis() < unlockExpiry

        // Resolve AppWidgetId
        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Exception) {
            -1
        }

        // Widget configuration
        val modeStr = widgetPrefs.getString("widget_mode_$appWidgetId", "ALL") ?: "ALL"
        val targetId = widgetPrefs.getString("widget_target_id_$appWidgetId", null)
        val themeStr = widgetPrefs.getString("widget_theme_$appWidgetId", "AMOLED") ?: "AMOLED"

        val isSafeZoneMode = modeStr == "SAFE_ZONE"

        // Load data from Room
        val db = NoteDatabase.getDatabase(context).noteDao()
        db.deleteExpiredNotes()

        var bookName: String? = null
        if (modeStr == "BOOK" && !targetId.isNullOrEmpty()) {
            val books = db.getAllBooks().firstOrNull() ?: emptyList()
            bookName = books.find { it.id == targetId }?.name
        }

        val notes: List<NoteEntity> = when {
            !areWidgetsEnabled -> emptyList()
            isSafeZoneMode -> {
                if (currentlyUnlocked) {
                    val secretNotes = db.getSecretNotes().firstOrNull() ?: emptyList()
                    CryptoManager.decryptBatch(secretNotes)
                } else {
                    emptyList()
                }
            }
            modeStr == "PINNED" -> {
                val all = db.getPublicNotes().firstOrNull() ?: emptyList()
                all.filter { it.isPinned }
            }
            modeStr == "BOOK" && !targetId.isNullOrEmpty() -> {
                db.getNotesByBook(targetId).firstOrNull() ?: emptyList()
            }
            modeStr == "SINGLE_NOTE" && !targetId.isNullOrEmpty() -> {
                val single = db.getNoteById(targetId)
                if (single != null) listOf(single) else (db.getPublicNotes().firstOrNull()?.take(1) ?: emptyList())
            }
            modeStr == "CHECKLIST" -> {
                val all = db.getPublicNotes().firstOrNull() ?: emptyList()
                val checklistNotes = all.filter { it.body.contains("[ ]") || it.body.contains("[x]") || it.body.contains("[X]") }
                checklistNotes.ifEmpty { all }
            }
            else -> {
                db.getPublicNotes().firstOrNull() ?: emptyList()
            }
        }

        provideContent {
            GlanceTheme {
                WidgetMainLayout(
                    modeStr = modeStr,
                    themeStr = themeStr,
                    bookName = bookName,
                    targetBookId = targetId,
                    areWidgetsEnabled = areWidgetsEnabled,
                    isUnlocked = currentlyUnlocked,
                    isSafeZoneMode = isSafeZoneMode,
                    notes = notes
                )
            }
        }
    }
}

@Composable
fun WidgetMainLayout(
    modeStr: String,
    themeStr: String,
    bookName: String?,
    targetBookId: String?,
    areWidgetsEnabled: Boolean,
    isUnlocked: Boolean,
    isSafeZoneMode: Boolean,
    notes: List<NoteEntity>
) {
    val size = LocalSize.current

    // Palette setup based on theme
    val (surfaceColor, cardColor, textColor, subtextColor, accentColor, borderColor) = when (themeStr) {
        "LIGHT" -> WidgetPalette(
            surface = ColorProvider(Color(0xFFF3F4F6)),
            card = ColorProvider(Color(0xFFFFFFFF)),
            text = ColorProvider(Color(0xFF111827)),
            subtext = ColorProvider(Color(0xFF6B7280)),
            accent = ColorProvider(Color(0xFF7C3AED)),
            border = ColorProvider(Color(0xFFE5E7EB))
        )
        "SYSTEM" -> WidgetPalette(
            surface = GlanceTheme.colors.surface,
            card = GlanceTheme.colors.surfaceVariant,
            text = GlanceTheme.colors.onSurface,
            subtext = GlanceTheme.colors.onSurfaceVariant,
            accent = GlanceTheme.colors.primary,
            border = GlanceTheme.colors.outline
        )
        else -> WidgetPalette( // AMOLED default
            surface = ColorProvider(Color(0xFF000000)),
            card = ColorProvider(Color(0xFF141414)),
            text = ColorProvider(Color(0xFFF9FAFB)),
            subtext = ColorProvider(Color(0xFF9CA3AF)),
            accent = ColorProvider(Color(0xFF8B5CF6)),
            border = ColorProvider(Color(0xFF262626))
        )
    }

    val titleText = when (modeStr) {
        "SAFE_ZONE" -> "Bóveda Segura"
        "BOOK" -> bookName ?: "Cuaderno"
        "CHECKLIST" -> "Tareas"
        "PINNED" -> "Fijadas"
        "SINGLE_NOTE" -> notes.firstOrNull()?.title?.ifEmpty { "Nota" } ?: "Nota"
        else -> "yNotes"
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(18.dp)
            .padding(10.dp)
    ) {
        when {
            size.width < 180.dp && size.height < 180.dp -> {
                // 2x2 Small Layout
                SmallWidgetView(
                    title = titleText,
                    notes = notes,
                    isSafeZone = isSafeZoneMode,
                    isUnlocked = isUnlocked,
                    areWidgetsEnabled = areWidgetsEnabled,
                    cardColor = cardColor,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor,
                    targetBookId = targetBookId
                )
            }
            size.height < 160.dp -> {
                // 4x2 Horizontal Layout
                MediumWidgetView(
                    title = titleText,
                    notes = notes,
                    isSafeZone = isSafeZoneMode,
                    isUnlocked = isUnlocked,
                    areWidgetsEnabled = areWidgetsEnabled,
                    cardColor = cardColor,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor,
                    targetBookId = targetBookId
                )
            }
            else -> {
                // 4x4 Full Responsive Layout
                LargeWidgetView(
                    title = titleText,
                    notes = notes,
                    isSafeZone = isSafeZoneMode,
                    isUnlocked = isUnlocked,
                    areWidgetsEnabled = areWidgetsEnabled,
                    cardColor = cardColor,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    accentColor = accentColor,
                    targetBookId = targetBookId
                )
            }
        }
    }
}

data class WidgetPalette(
    val surface: ColorProvider,
    val card: ColorProvider,
    val text: ColorProvider,
    val subtext: ColorProvider,
    val accent: ColorProvider,
    val border: ColorProvider
)

// ──────────────────────────────────────────────
// 2x2 SMALL LAYOUT
// ──────────────────────────────────────────────
@Composable
private fun SmallWidgetView(
    title: String,
    notes: List<NoteEntity>,
    isSafeZone: Boolean,
    isUnlocked: Boolean,
    areWidgetsEnabled: Boolean,
    cardColor: ColorProvider,
    textColor: ColorProvider,
    subtextColor: ColorProvider,
    accentColor: ColorProvider,
    targetBookId: String?
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = "Nueva nota",
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable(
                        actionRunCallback<NewNoteAction>(
                            actionParametersOf(
                                NewNoteAction.BookIdKey to (targetBookId ?: ""),
                                NewNoteAction.IsSecretKey to isSafeZone
                            )
                        )
                    )
            )
        }

        // Body
        if (!areWidgetsEnabled) {
            CenterMessage("Desactivado", subtextColor)
        } else if (isSafeZone && !isUnlocked) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionRunCallback<UnlockAction>()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_lock),
                        contentDescription = "Bloqueado",
                        modifier = GlanceModifier.size(24.dp).padding(bottom = 4.dp)
                    )
                    Text("Toca para abrir", style = TextStyle(color = textColor, fontSize = 11.sp))
                }
            }
        } else if (notes.isEmpty()) {
            CenterMessage("Sin notas", subtextColor)
        } else {
            val note = notes.first()
            val noteCardColor = if (note.color != 0L) ColorProvider(Color(note.color)) else cardColor
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(noteCardColor)
                    .cornerRadius(12.dp)
                    .clickable(
                        actionRunCallback<OpenNoteAction>(
                            actionParametersOf(
                                OpenNoteAction.NoteIdKey to note.id,
                                OpenNoteAction.IsSecretKey to note.isSecret
                            )
                        )
                    )
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = note.title.ifEmpty { "Sin título" },
                        style = TextStyle(color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    Text(
                        text = note.body.replace("\n", " "),
                        style = TextStyle(color = subtextColor, fontSize = 11.sp),
                        maxLines = 2
                    )
                    if (notes.size > 1) {
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "+${notes.size - 1} más",
                            style = TextStyle(color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// 4x2 MEDIUM LAYOUT
// ──────────────────────────────────────────────
@Composable
private fun MediumWidgetView(
    title: String,
    notes: List<NoteEntity>,
    isSafeZone: Boolean,
    isUnlocked: Boolean,
    areWidgetsEnabled: Boolean,
    cardColor: ColorProvider,
    textColor: ColorProvider,
    subtextColor: ColorProvider,
    accentColor: ColorProvider,
    targetBookId: String?
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(color = accentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            if (isSafeZone) {
                Image(
                    provider = ImageProvider(if (isUnlocked) R.drawable.ic_widget_unlock else R.drawable.ic_widget_lock),
                    contentDescription = if (isUnlocked) "Bloquear" else "Desbloquear",
                    modifier = GlanceModifier
                        .size(20.dp)
                        .clickable(if (isUnlocked) actionRunCallback<LockAction>() else actionRunCallback<UnlockAction>())
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
            }
            Image(
                provider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = "Nueva nota",
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable(
                        actionRunCallback<NewNoteAction>(
                            actionParametersOf(
                                NewNoteAction.BookIdKey to (targetBookId ?: ""),
                                NewNoteAction.IsSecretKey to isSafeZone
                            )
                        )
                    )
            )
        }

        // Body
        if (!areWidgetsEnabled) {
            CenterMessage("Widgets desactivados en la app", subtextColor)
        } else if (isSafeZone && !isUnlocked) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionRunCallback<UnlockAction>()),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_lock),
                        contentDescription = "Bloqueado",
                        modifier = GlanceModifier.size(28.dp).padding(end = 8.dp)
                    )
                    Text(
                        "Zona Segura protegida · Toca para desbloquear",
                        style = TextStyle(color = textColor, fontSize = 12.sp)
                    )
                }
            }
        } else if (notes.isEmpty()) {
            CenterMessage("No hay notas disponibles", subtextColor)
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(notes) { note ->
                    NoteCardItem(
                        note = note,
                        cardColor = cardColor,
                        textColor = textColor,
                        subtextColor = subtextColor
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// 4x4 LARGE LAYOUT
// ──────────────────────────────────────────────
@Composable
private fun LargeWidgetView(
    title: String,
    notes: List<NoteEntity>,
    isSafeZone: Boolean,
    isUnlocked: Boolean,
    areWidgetsEnabled: Boolean,
    cardColor: ColorProvider,
    textColor: ColorProvider,
    subtextColor: ColorProvider,
    accentColor: ColorProvider,
    targetBookId: String?
) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = title,
                    style = TextStyle(color = accentColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                )
                if (notes.isNotEmpty()) {
                    Text(
                        text = "${notes.size} nota${if (notes.size > 1) "s" else ""}",
                        style = TextStyle(color = subtextColor, fontSize = 11.sp)
                    )
                }
            }

            if (isSafeZone) {
                Image(
                    provider = ImageProvider(if (isUnlocked) R.drawable.ic_widget_unlock else R.drawable.ic_widget_lock),
                    contentDescription = if (isUnlocked) "Bloquear" else "Desbloquear",
                    modifier = GlanceModifier
                        .size(22.dp)
                        .clickable(if (isUnlocked) actionRunCallback<LockAction>() else actionRunCallback<UnlockAction>())
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
            }

            Image(
                provider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = "Nueva nota",
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(
                        actionRunCallback<NewNoteAction>(
                            actionParametersOf(
                                NewNoteAction.BookIdKey to (targetBookId ?: ""),
                                NewNoteAction.IsSecretKey to isSafeZone
                            )
                        )
                    )
            )
        }

        // Body
        if (!areWidgetsEnabled) {
            CenterMessage("Widgets desactivados en la app", subtextColor)
        } else if (isSafeZone && !isUnlocked) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionRunCallback<UnlockAction>()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_lock),
                        contentDescription = "Bloqueado",
                        modifier = GlanceModifier.size(40.dp).padding(bottom = 10.dp)
                    )
                    Text(
                        "Bóveda Segura Cifrada",
                        style = TextStyle(color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        "Toca para autenticar con biometría",
                        style = TextStyle(color = subtextColor, fontSize = 12.sp)
                    )
                }
            }
        } else if (notes.isEmpty()) {
            CenterMessage("No hay notas disponibles", subtextColor)
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(notes) { note ->
                    NoteCardItem(
                        note = note,
                        cardColor = cardColor,
                        textColor = textColor,
                        subtextColor = subtextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCardItem(
    note: NoteEntity,
    cardColor: ColorProvider,
    textColor: ColorProvider,
    subtextColor: ColorProvider
) {
    val noteCardColor = if (note.color != 0L) ColorProvider(Color(note.color)) else cardColor
    val hasChecklist = note.body.contains("[ ]") || note.body.contains("[x]") || note.body.contains("[X]")

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(noteCardColor)
            .cornerRadius(12.dp)
            .padding(10.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            // Note Title row
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(
                        actionRunCallback<OpenNoteAction>(
                            actionParametersOf(
                                OpenNoteAction.NoteIdKey to note.id,
                                OpenNoteAction.IsSecretKey to note.isSecret
                            )
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.isPinned) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_pin),
                        contentDescription = "Fijada",
                        modifier = GlanceModifier.size(13.dp).padding(end = 4.dp)
                    )
                }
                Text(
                    text = note.title.ifEmpty { "Sin título" },
                    style = TextStyle(
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            // Checklist or preview
            if (hasChecklist) {
                val lines = note.body.lines()
                var shownCount = 0
                for ((index, line) in lines.withIndex()) {
                    if (shownCount >= 3) break
                    val isChecked = line.contains("[x]") || line.contains("[X]")
                    val isUnchecked = line.contains("[ ]")

                    if (isChecked || isUnchecked) {
                        val cleanText = line
                            .replace("- [x]", "")
                            .replace("- [X]", "")
                            .replace("- [ ]", "")
                            .replace("[x]", "")
                            .replace("[X]", "")
                            .replace("[ ]", "")
                            .trim()

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (isChecked) R.drawable.ic_widget_check_box
                                    else R.drawable.ic_widget_check_box_outline
                                ),
                                contentDescription = if (isChecked) "Completada" else "Pendiente",
                                modifier = GlanceModifier
                                    .size(18.dp)
                                    .clickable(
                                        actionRunCallback<ToggleChecklistAction>(
                                            actionParametersOf(
                                                ToggleChecklistAction.NoteIdKey to note.id,
                                                ToggleChecklistAction.LineIndexKey to index
                                            )
                                        )
                                    )
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = cleanText,
                                style = TextStyle(
                                    color = if (isChecked) subtextColor else textColor,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .clickable(
                                        actionRunCallback<OpenNoteAction>(
                                            actionParametersOf(
                                                OpenNoteAction.NoteIdKey to note.id,
                                                OpenNoteAction.IsSecretKey to note.isSecret
                                            )
                                        )
                                    )
                            )
                        }
                        shownCount++
                    }
                }
            } else if (note.body.isNotEmpty() && !note.isBodyHidden) {
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = note.body.replace("\n", " "),
                    style = TextStyle(color = subtextColor, fontSize = 12.sp),
                    maxLines = 2,
                    modifier = GlanceModifier.clickable(
                        actionRunCallback<OpenNoteAction>(
                            actionParametersOf(
                                OpenNoteAction.NoteIdKey to note.id,
                                OpenNoteAction.IsSecretKey to note.isSecret
                            )
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String, color: ColorProvider) {
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = TextStyle(color = color, fontSize = 13.sp))
    }
}

// ──────────────────────────────────────────────
// ACTIONS
// ──────────────────────────────────────────────

class OpenNoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val noteId = parameters[NoteIdKey] ?: return
        val isSecret = parameters[IsSecretKey] ?: false
        val route = if (isSecret) "editor/secret/$noteId" else "editor/public/$noteId"

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_route", route)
        }
        context.startActivity(intent)
    }

    companion object {
        val NoteIdKey = ActionParameters.Key<String>("note_id")
        val IsSecretKey = ActionParameters.Key<Boolean>("is_secret")
    }
}

class NewNoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val bookId = parameters[BookIdKey]
        val isSecret = parameters[IsSecretKey] ?: false

        val route = when {
            isSecret -> "editor/secret/new"
            !bookId.isNullOrEmpty() -> "editor/public/new?bookId=$bookId"
            else -> "editor/public/new"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_route", route)
        }
        context.startActivity(intent)
    }

    companion object {
        val BookIdKey = ActionParameters.Key<String>("book_id")
        val IsSecretKey = ActionParameters.Key<Boolean>("is_secret")
    }
}

class ToggleChecklistAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val noteId = parameters[NoteIdKey] ?: return
        val lineIndex = parameters[LineIndexKey] ?: return

        val db = NoteDatabase.getDatabase(context).noteDao()
        val note = db.getNoteById(noteId) ?: return

        val lines = note.body.lines().toMutableList()
        if (lineIndex in lines.indices) {
            val line = lines[lineIndex]
            val updated = when {
                line.contains("[ ]") -> line.replaceFirst("[ ]", "[x]")
                line.contains("[x]") -> line.replaceFirst("[x]", "[ ]")
                line.contains("[X]") -> line.replaceFirst("[X]", "[ ]")
                else -> line
            }
            lines[lineIndex] = updated
            val updatedNote = note.copy(body = lines.joinToString("\n"), updatedAt = System.currentTimeMillis())
            db.insertNote(updatedNote)
            YNotesWidget().update(context, glanceId)
        }
    }

    companion object {
        val NoteIdKey = ActionParameters.Key<String>("note_id")
        val LineIndexKey = ActionParameters.Key<Int>("line_index")
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
