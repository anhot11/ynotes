package app.uamo.ynotes.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteDatabase
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.ui.theme.AppThemeType
import app.uamo.ynotes.ui.theme.YNotesTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WidgetContentMode(val title: String, val description: String, val icon: ImageVector) {
    ALL("Todas las notas", "Tus notas públicas recientes", Icons.Default.Description),
    PINNED("Notas fijadas", "Acceso prioritario a notas con chincheta", Icons.Default.PushPin),
    CHECKLIST("Lista de tareas", "Notas con casillas interactivas marcables", Icons.Default.Checklist),
    BOOK("Un cuaderno", "Notas pertenecientes a un cuaderno específico", Icons.AutoMirrored.Filled.MenuBook),
    SINGLE_NOTE("Nota individual", "Una única nota destacada en grande", Icons.Default.Note),
    SAFE_ZONE("Zona Segura (Bóveda)", "Bóveda protegida con huella dactilar", Icons.Default.Lock)
}

enum class WidgetThemeOption(val title: String, val icon: ImageVector) {
    SYSTEM("Material You", Icons.Default.AutoAwesome),
    AMOLED("Negro AMOLED", Icons.Default.DarkMode),
    LIGHT("Modo Claro", Icons.Default.LightMode)
}

class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.activity.enableEdgeToEdge()

        // Set default cancelled in case the user closes without saving
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val sharedPrefs = getSharedPreferences("yNotesPrefs", Context.MODE_PRIVATE)
            val themeTypeIndex = sharedPrefs.getInt("APP_THEME", 0)
            val currentTheme = AppThemeType.entries.getOrElse(themeTypeIndex) { AppThemeType.AMOLED }

            YNotesTheme(themeType = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigurationContent(
                        onSave = { mode, targetId, theme ->
                            saveConfiguration(mode, targetId, theme)
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    private fun saveConfiguration(
        mode: WidgetContentMode,
        targetId: String?,
        theme: WidgetThemeOption
    ) {
        val widgetPrefs = getSharedPreferences("yNotesWidgetPrefs", Context.MODE_PRIVATE)
        widgetPrefs.edit()
            .putString("widget_mode_$appWidgetId", mode.name)
            .putString("widget_target_id_$appWidgetId", targetId ?: "")
            .putString("widget_theme_$appWidgetId", theme.name)
            .apply()

        // Push immediate widget update
        val context = applicationContext
        val id = appWidgetId
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val glanceManager = GlanceAppWidgetManager(context)
                val glanceId = glanceManager.getGlanceIdBy(id)
                YNotesWidget().update(context, glanceId)
            } catch (_: Exception) {
                YNotesWidget().updateAll(context)
            }
        }

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigurationContent(
    onSave: (WidgetContentMode, String?, WidgetThemeOption) -> Unit,
    onCancel: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedMode by remember { mutableStateOf(WidgetContentMode.ALL) }
    var selectedTheme by remember { mutableStateOf(WidgetThemeOption.AMOLED) }
    var selectedTargetId by remember { mutableStateOf<String?>(null) }

    var availableBooks by remember { mutableStateOf<List<BookEntity>>(emptyList()) }
    var availableNotes by remember { mutableStateOf<List<NoteEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = NoteDatabase.getDatabase(context).noteDao()
            availableBooks = db.getAllBooks().firstOrNull() ?: emptyList()
            availableNotes = db.getPublicNotes().firstOrNull() ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Configurar Widget",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancelar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val target = when (selectedMode) {
                                WidgetContentMode.BOOK -> selectedTargetId ?: availableBooks.firstOrNull()?.id
                                WidgetContentMode.SINGLE_NOTE -> selectedTargetId ?: availableNotes.firstOrNull()?.id
                                else -> null
                            }
                            onSave(selectedMode, target, selectedTheme)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aplicar Widget")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "CONTENIDO A MOSTRAR",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(WidgetContentMode.entries) { mode ->
                val isSelected = selectedMode == mode
                Surface(
                    onClick = {
                        selectedMode = mode
                        if (mode == WidgetContentMode.BOOK && selectedTargetId == null) {
                            selectedTargetId = availableBooks.firstOrNull()?.id
                        } else if (mode == WidgetContentMode.SINGLE_NOTE && selectedTargetId == null) {
                            selectedTargetId = availableNotes.firstOrNull()?.id
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                imageVector = mode.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedMode = mode }
                        )
                    }
                }
            }

            // Target selector if BOOK
            if (selectedMode == WidgetContentMode.BOOK) {
                item {
                    Text(
                        text = "SELECCIONA EL CUADERNO",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                if (availableBooks.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Aún no has creado cuadernos. Se mostrarán todas las notas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(availableBooks, key = { it.id }) { book ->
                        val isBookSelected = selectedTargetId == book.id || (selectedTargetId == null && book == availableBooks.first())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isBookSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedTargetId = book.id }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = book.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.weight(1f)
                            )
                            if (isBookSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Target selector if SINGLE_NOTE
            if (selectedMode == WidgetContentMode.SINGLE_NOTE) {
                item {
                    Text(
                        text = "SELECCIONA LA NOTA",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                if (availableNotes.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "No hay notas disponibles para seleccionar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(availableNotes.take(15), key = { it.id }) { note ->
                        val isNoteSelected = selectedTargetId == note.id || (selectedTargetId == null && note == availableNotes.first())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNoteSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedTargetId = note.id }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title.ifEmpty { "Sin título" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (note.body.isNotEmpty()) {
                                    Text(
                                        text = note.body.replace("\n", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (isNoteSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Theme selector
            item {
                Text(
                    text = "ESTILO VISUAL",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WidgetThemeOption.entries.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        Surface(
                            onClick = { selectedTheme = theme },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = theme.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = theme.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
