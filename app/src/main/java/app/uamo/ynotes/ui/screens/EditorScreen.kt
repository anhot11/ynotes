package app.uamo.ynotes.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.utils.MediaManager
import app.uamo.ynotes.utils.sharedElementTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

val NoteColors = listOf(
    0L, // Default (uses surfaceVariant)
    0xFF4A1C1C, // Dark Red
    0xFF4A1C3B, // Dark Pink
    0xFF3B1C4A, // Dark Purple
    0xFF2C1C4A, // Dark Deep Purple
    0xFF1C2A4A, // Dark Indigo
    0xFF1C3A4A, // Dark Blue
    0xFF1C4A47, // Dark Cyan
    0xFF1C4A3B, // Dark Teal
    0xFF1C4A22, // Dark Green
    0xFF2F4A1C, // Dark Light Green
    0xFF4A471C, // Dark Lime
    0xFF4A421C, // Dark Yellow
    0xFF4A351C, // Dark Amber
    0xFF4A2B1C, // Dark Orange
    0xFF4A221C  // Dark Deep Orange
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    editingNote: NoteEntity?,
    isSecret: Boolean,
    isBooksEnabled: Boolean,
    books: List<BookEntity>,
    onSave: (id: String?, title: String, body: String, color: Long, isPinned: Boolean, bookId: String?, isBodyHidden: Boolean, mediaFiles: String, expiresAt: Long?, isWidgetSpecial: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val stableNoteId = remember { editingNote?.id ?: UUID.randomUUID().toString() }

    var titleText by remember { mutableStateOf(editingNote?.title ?: "") }
    var bodyText by remember { mutableStateOf(editingNote?.body ?: "") }
    var noteColor by remember { mutableStateOf(editingNote?.color ?: 0L) }
    var isPinned by remember { mutableStateOf(editingNote?.isPinned ?: false) }
    var bookId by remember { mutableStateOf(editingNote?.bookId) }
    var isBodyHidden by remember { mutableStateOf(editingNote?.isBodyHidden ?: false) }
    var isDeleted by remember { mutableStateOf(false) }
    var isWidgetSpecial by remember { mutableStateOf(editingNote?.isWidgetSpecial ?: false) }
    var expiresAt by remember { mutableStateOf(editingNote?.expiresAt) }

    // Focus mode & options sheet state
    var isFocusMode by remember { mutableStateOf(false) }
    var showPropertiesSheet by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Media state
    var mediaFileNames by remember {
        mutableStateOf(
            editingNote?.mediaFiles?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }
    val mediaBitmaps = remember { mutableStateMapOf<String, Bitmap>() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val cursorColor = if (isSecret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val backgroundColor = if (noteColor == 0L) MaterialTheme.colorScheme.background else Color(noteColor)

    fun mediaFilesString(): String = mediaFileNames.joinToString("|")

    // Load existing media thumbnails
    LaunchedEffect(mediaFileNames) {
        mediaFileNames.forEach { fileName ->
            if (!mediaBitmaps.containsKey(fileName)) {
                withContext(Dispatchers.IO) {
                    MediaManager.loadMediaBitmap(context, stableNoteId, fileName, isSecret)
                }?.let { bitmap ->
                    mediaBitmaps[fileName] = bitmap
                }
            }
        }
    }

    // Modern Android Photo Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        try {
            if (uris.isNotEmpty()) {
                coroutineScope.launch {
                    val newNames = mutableListOf<String>()
                    uris.forEach { uri ->
                        withContext(Dispatchers.IO) {
                            try {
                                MediaManager.saveMedia(context, stableNoteId, uri, isSecret)
                            } catch (t: Throwable) {
                                t.printStackTrace()
                                null
                            }
                        }?.let { name ->
                            newNames.add(name)
                            withContext(Dispatchers.IO) {
                                try {
                                    MediaManager.loadMediaBitmap(context, stableNoteId, name, isSecret)
                                } catch (t: Throwable) {
                                    t.printStackTrace()
                                    null
                                }
                            }?.let { bitmap ->
                                mediaBitmaps[name] = bitmap
                            }
                        }
                    }
                    if (newNames.isNotEmpty()) {
                        mediaFileNames = mediaFileNames + newNames
                        if (!isDeleted && (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank() || mediaFileNames.isNotEmpty())) {
                            onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden, mediaFilesString(), expiresAt, isWidgetSpecial)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    // Fallback launcher for Android 11 / devices where PickMultipleVisualMedia fails
    val fallbackImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        try {
            if (uris.isNotEmpty()) {
                coroutineScope.launch {
                    val newNames = mutableListOf<String>()
                    uris.forEach { uri ->
                        withContext(Dispatchers.IO) {
                            try {
                                MediaManager.saveMedia(context, stableNoteId, uri, isSecret)
                            } catch (t: Throwable) {
                                t.printStackTrace()
                                null
                            }
                        }?.let { name ->
                            newNames.add(name)
                            withContext(Dispatchers.IO) {
                                try {
                                    MediaManager.loadMediaBitmap(context, stableNoteId, name, isSecret)
                                } catch (t: Throwable) {
                                    t.printStackTrace()
                                    null
                                }
                            }?.let { bitmap ->
                                mediaBitmaps[name] = bitmap
                            }
                        }
                    }
                    if (newNames.isNotEmpty()) {
                        mediaFileNames = mediaFileNames + newNames
                        if (!isDeleted && (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank() || mediaFileNames.isNotEmpty())) {
                            onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden, mediaFilesString(), expiresAt, isWidgetSpecial)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun launchImagePicker() {
        try {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                fallbackImagePickerLauncher.launch("image/*")
            }
        } catch (t: Throwable) {
            try {
                fallbackImagePickerLauncher.launch("image/*")
            } catch (t2: Throwable) {
                t2.printStackTrace()
            }
        }
    }

    // Debounced save
    fun scheduleSave() {
        if (isDeleted) return
        isSaving = true
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(500)
            if (!isDeleted && (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank() || mediaFileNames.isNotEmpty())) {
                onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden, mediaFilesString(), expiresAt, isWidgetSpecial)
            }
            isSaving = false
        }
    }

    // Immediate save
    fun saveNow() {
        if (isDeleted) return
        debounceJob?.cancel()
        isSaving = false
        if (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank() || mediaFileNames.isNotEmpty()) {
            onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden, mediaFilesString(), expiresAt, isWidgetSpecial)
        }
    }

    BackHandler {
        if (isFocusMode) {
            isFocusMode = false
        } else {
            saveNow()
            onNavigateBack()
        }
    }

    // Word and character count
    val wordCount = remember(bodyText) {
        if (bodyText.isBlank()) 0 else bodyText.trim().split("\\s+".toRegex()).size
    }
    val charCount = bodyText.length

    Scaffold(
        modifier = Modifier.sharedElementTransition("note-$stableNoteId"),
        containerColor = backgroundColor,
        topBar = {
            AnimatedVisibility(
                visible = !isFocusMode,
                enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    title = {
                        AnimatedContent(targetState = isSaving, label = "SaveStatus") { saving ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (saving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Guardando...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Guardado",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            saveNow()
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                    actions = {
                        // Focus Mode toggle
                        IconButton(onClick = { isFocusMode = true }) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Modo Enfoque",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Pin button
                        IconButton(onClick = {
                            isPinned = !isPinned
                            saveNow()
                        }) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isPinned) "Desfijar" else "Fijar",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Options BottomSheet button
                        IconButton(onClick = { showPropertiesSheet = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones de la nota",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Save & exit checkmark
                        IconButton(onClick = {
                            saveNow()
                            onNavigateBack()
                        }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Guardar Nota",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(if (isFocusMode) PaddingValues(0.dp) else innerPadding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (isFocusMode) 40.dp else 0.dp)
            ) {
                // Media thumbnails strip
                if (mediaFileNames.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mediaFileNames) { fileName ->
                            val bitmap = mediaBitmaps[fileName]
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Imagen adjunta",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            MediaManager.deleteMediaFile(context, stableNoteId, fileName, isSecret)
                                        }
                                        mediaBitmaps.remove(fileName)
                                        mediaFileNames = mediaFileNames - fileName
                                        saveNow()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(32.dp)
                                        .padding(4.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.65f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Eliminar imagen",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Text fields
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .weight(1f)
                ) {
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (titleText.isEmpty()) {
                            Text(
                                text = "Título",
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        }
                        BasicTextField(
                            value = titleText,
                            onValueChange = {
                                titleText = it
                                scheduleSave()
                            },
                            textStyle = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(cursorColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (bodyText.isEmpty()) {
                            Text(
                                text = "Escribe tu nota...",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        }
                        val onBackgroundColor = MaterialTheme.colorScheme.onBackground
                        val visualTransformation = remember(onBackgroundColor) {
                            app.uamo.ynotes.utils.MarkdownVisualTransformation(onBackgroundColor)
                        }
                        BasicTextField(
                            value = bodyText,
                            onValueChange = {
                                bodyText = it
                                scheduleSave()
                            },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = onBackgroundColor
                            ),
                            modifier = Modifier.fillMaxSize(),
                            cursorBrush = SolidColor(cursorColor),
                            visualTransformation = visualTransformation
                        )
                    }
                }

                // Stats footer (Word and character counter)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$wordCount palabras  ·  $charCount caracteres",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    if (isWidgetSpecial) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "En Widget",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Focus Mode Floating Exit Pill
            AnimatedVisibility(
                visible = isFocusMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Surface(
                    onClick = { isFocusMode = false },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FullscreenExit,
                            contentDescription = "Salir de enfoque",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Salir de enfoque",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Modal BottomSheet for Note Options & Actions
    if (showPropertiesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPropertiesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Opciones de la nota",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // 1. Color Palette
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Color de nota",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NoteColors.forEach { colorValue ->
                            val isSelected = noteColor == colorValue
                            val displayColor = if (colorValue == 0L)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                Color(colorValue)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(displayColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else app.uamo.ynotes.ui.theme.GlassBorder,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        noteColor = colorValue
                                        saveNow()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Color seleccionado",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Notebook / Folder (if enabled)
                if (isBooksEnabled && books.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Libro / Cuaderno",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = bookId == null,
                                onClick = {
                                    bookId = null
                                    saveNow()
                                },
                                label = { Text("Sin libro") },
                                leadingIcon = if (bookId == null) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            books.forEach { book ->
                                FilterChip(
                                    selected = bookId == book.id,
                                    onClick = {
                                        bookId = book.id
                                        saveNow()
                                    },
                                    label = { Text(book.name) },
                                    leadingIcon = if (bookId == book.id) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // 3. Actions & Settings
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        // Attach Media
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Adjuntar imagen") },
                            supportingContent = {
                                Text(
                                    if (mediaFileNames.isEmpty()) "Añade fotos desde tu galería"
                                    else "${mediaFileNames.size} imagen(es) adjunta(s)"
                                )
                            },
                            leadingContent = {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable {
                                showPropertiesSheet = false
                                launchImagePicker()
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Hide preview switch
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Ocultar previsualización") },
                            supportingContent = { Text("Oculta el texto de la nota en la lista de notas") },
                            leadingContent = {
                                Icon(
                                    if (isBodyHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = isBodyHidden,
                                    onCheckedChange = {
                                        isBodyHidden = it
                                        saveNow()
                                    }
                                )
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Widget special switch
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Fijar en Widget") },
                            supportingContent = { Text("Muestra esta nota de forma destacada en la pantalla de inicio") },
                            leadingContent = {
                                Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                Switch(
                                    checked = isWidgetSpecial,
                                    onCheckedChange = {
                                        isWidgetSpecial = it
                                        if (!it) expiresAt = null
                                        saveNow()
                                    }
                                )
                            }
                        )

                        // Widget expiration chips
                        if (isWidgetSpecial) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Tiempo de expiración en widget:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isNever = expiresAt == null
                                    FilterChip(
                                        selected = isNever,
                                        onClick = { expiresAt = null; saveNow() },
                                        label = { Text("Siempre") }
                                    )
                                    FilterChip(
                                        selected = expiresAt != null && (expiresAt!! - System.currentTimeMillis()) in 1..3600000L,
                                        onClick = { expiresAt = System.currentTimeMillis() + 3600000L; saveNow() },
                                        label = { Text("1 hora") }
                                    )
                                    FilterChip(
                                        selected = expiresAt != null && (expiresAt!! - System.currentTimeMillis()) in 3600001L..86400000L,
                                        onClick = { expiresAt = System.currentTimeMillis() + 86400000L; saveNow() },
                                        label = { Text("24 horas") }
                                    )
                                    FilterChip(
                                        selected = expiresAt != null && (expiresAt!! - System.currentTimeMillis()) > 86400000L,
                                        onClick = { expiresAt = System.currentTimeMillis() + 604800000L; saveNow() },
                                        label = { Text("1 semana") }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Delete Note (if editing existing note)
                if (editingNote != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text("Eliminar nota", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            },
                            supportingContent = {
                                Text("Mover esta nota a la papelera", color = MaterialTheme.colorScheme.onErrorContainer)
                            },
                            leadingContent = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            modifier = Modifier.clickable {
                                isDeleted = true
                                debounceJob?.cancel()
                                onDelete(editingNote.id)
                                showPropertiesSheet = false
                                onNavigateBack()
                            }
                        )
                    }
                }
            }
        }
    }
}
