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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.ui.theme.*
import app.uamo.ynotes.utils.MediaManager
import app.uamo.ynotes.utils.sharedElementTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

val NoteColors = app.uamo.ynotes.ui.theme.NoteColors

data class EditorTextSnapshot(
    val title: String,
    val bodyValue: TextFieldValue
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    editingNote: NoteEntity?,
    initialBookId: String? = null,
    isSecret: Boolean,
    isBooksEnabled: Boolean,
    books: List<BookEntity>,
    onSave: (id: String?, title: String, body: String, color: Long, isPinned: Boolean, bookId: String?, isBodyHidden: Boolean, mediaFiles: String, expiresAt: Long?, isWidgetSpecial: Boolean) -> Unit,
    onDelete: (id: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val stableNoteId = remember { editingNote?.id ?: UUID.randomUUID().toString() }

    var titleText by remember { mutableStateOf(editingNote?.title ?: "") }
    var bodyTextFieldValue by remember { mutableStateOf(TextFieldValue(editingNote?.body ?: "")) }
    val bodyText = bodyTextFieldValue.text
    var hideMarkdownSyntax by remember { mutableStateOf(true) }
    var noteColor by remember { mutableStateOf(editingNote?.color ?: 0L) }
    var isPinned by remember { mutableStateOf(editingNote?.isPinned ?: false) }
    var bookId by remember { mutableStateOf(editingNote?.bookId ?: initialBookId) }
    var isBodyHidden by remember { mutableStateOf(editingNote?.isBodyHidden ?: false) }
    var isDeleted by remember { mutableStateOf(false) }
    var isWidgetSpecial by remember { mutableStateOf(editingNote?.isWidgetSpecial ?: false) }
    var expiresAt by remember { mutableStateOf(editingNote?.expiresAt) }

    // Focus mode & options sheet state
    var isFocusMode by remember { mutableStateOf(false) }
    var showPropertiesSheet by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme() || LocalAppTheme.current == AppThemeType.AMOLED

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

    // ──────────────────────────────────────────────
    // IN-MEMORY UNDO / REDO SESSION STATE
    // ──────────────────────────────────────────────
    val undoStack = remember { mutableStateListOf<EditorTextSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorTextSnapshot>() }
    var pendingSnapshot by remember { mutableStateOf<EditorTextSnapshot?>(null) }
    var lastEditTimestamp by remember { mutableLongStateOf(0L) }
    var isUndoRedoActive by remember { mutableStateOf(false) }

    fun commitPreEditSnapshot() {
        if (isUndoRedoActive) return
        pendingSnapshot?.let {
            if (undoStack.isEmpty() || undoStack.last() != it) {
                undoStack.add(it)
                if (undoStack.size > 50) undoStack.removeAt(0)
            }
        }
        val current = EditorTextSnapshot(titleText, bodyTextFieldValue)
        if (undoStack.isEmpty() || undoStack.last() != current) {
            undoStack.add(current)
            if (undoStack.size > 50) undoStack.removeAt(0)
        }
        pendingSnapshot = null
        redoStack.clear()
    }

    fun onBodyChange(newValue: TextFieldValue) {
        if (isUndoRedoActive) {
            bodyTextFieldValue = newValue
            return
        }
        if (newValue.text != bodyTextFieldValue.text) {
            val now = System.currentTimeMillis()
            if (pendingSnapshot == null) {
                pendingSnapshot = EditorTextSnapshot(titleText, bodyTextFieldValue)
            }
            if (now - lastEditTimestamp > 800L || newValue.text.endsWith(" ") || newValue.text.endsWith("\n") || Math.abs(newValue.text.length - bodyTextFieldValue.text.length) > 1) {
                pendingSnapshot?.let {
                    if (undoStack.isEmpty() || undoStack.last() != it) {
                        undoStack.add(it)
                        if (undoStack.size > 50) undoStack.removeAt(0)
                    }
                }
                pendingSnapshot = EditorTextSnapshot(titleText, newValue)
                redoStack.clear()
            }
            lastEditTimestamp = now
            bodyTextFieldValue = newValue
            scheduleSave()
        } else {
            bodyTextFieldValue = newValue
        }
    }

    fun onTitleChange(newTitle: String) {
        if (isUndoRedoActive) {
            titleText = newTitle
            return
        }
        if (newTitle != titleText) {
            val now = System.currentTimeMillis()
            if (pendingSnapshot == null) {
                pendingSnapshot = EditorTextSnapshot(titleText, bodyTextFieldValue)
            }
            if (now - lastEditTimestamp > 800L || newTitle.endsWith(" ")) {
                pendingSnapshot?.let {
                    if (undoStack.isEmpty() || undoStack.last() != it) {
                        undoStack.add(it)
                        if (undoStack.size > 50) undoStack.removeAt(0)
                    }
                }
                pendingSnapshot = EditorTextSnapshot(newTitle, bodyTextFieldValue)
                redoStack.clear()
            }
            lastEditTimestamp = now
            titleText = newTitle
            scheduleSave()
        }
    }

    val canUndo = undoStack.isNotEmpty() || (pendingSnapshot != null && (pendingSnapshot?.title != titleText || pendingSnapshot?.bodyValue?.text != bodyTextFieldValue.text))
    val canRedo = redoStack.isNotEmpty()

    fun performUndo() {
        if (pendingSnapshot != null && (titleText != pendingSnapshot?.title || bodyTextFieldValue.text != pendingSnapshot?.bodyValue?.text)) {
            if (undoStack.isEmpty() || undoStack.last() != pendingSnapshot) {
                undoStack.add(pendingSnapshot!!)
            }
            pendingSnapshot = null
        }
        if (undoStack.isNotEmpty()) {
            isUndoRedoActive = true
            val current = EditorTextSnapshot(titleText, bodyTextFieldValue)
            redoStack.add(current)
            val previous = undoStack.removeAt(undoStack.lastIndex)
            titleText = previous.title
            bodyTextFieldValue = previous.bodyValue
            scheduleSave()
            isUndoRedoActive = false
        }
    }

    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            isUndoRedoActive = true
            val current = EditorTextSnapshot(titleText, bodyTextFieldValue)
            undoStack.add(current)
            val next = redoStack.removeAt(redoStack.lastIndex)
            titleText = next.title
            bodyTextFieldValue = next.bodyValue
            scheduleSave()
            isUndoRedoActive = false
        }
    }

    fun wrapOrInsert(prefix: String, suffix: String) {
        if (isDeleted) return
        commitPreEditSnapshot()
        val text = bodyTextFieldValue.text
        val selection = bodyTextFieldValue.selection
        val start = minOf(selection.start, selection.end).coerceIn(0, text.length)
        val end = maxOf(selection.start, selection.end).coerceIn(0, text.length)

        if (start != end) {
            val selected = text.substring(start, end)
            val newText = text.substring(0, start) + prefix + selected + suffix + text.substring(end)
            bodyTextFieldValue = TextFieldValue(newText, TextRange(start + prefix.length, end + prefix.length))
        } else {
            val newText = text.substring(0, start) + prefix + suffix + text.substring(start)
            bodyTextFieldValue = TextFieldValue(newText, TextRange(start + prefix.length))
        }
        scheduleSave()
    }

    fun insertAtLineStart(prefix: String) {
        if (isDeleted) return
        commitPreEditSnapshot()
        val text = bodyTextFieldValue.text
        val selection = bodyTextFieldValue.selection
        val cursor = selection.start.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        bodyTextFieldValue = TextFieldValue(newText, TextRange(cursor + prefix.length))
        scheduleSave()
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
                    if (isBooksEnabled && books.isNotEmpty()) {
                        val currentBook = books.find { it.id == bookId }
                        Surface(
                            onClick = { showPropertiesSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentBook != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (currentBook != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = if (currentBook != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentBook?.let { "Cuaderno: ${it.name}" } ?: "Sin cuaderno (Toca para asignar)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (currentBook != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
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
                            onValueChange = { onTitleChange(it) },
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
                                text = "Escribe tu nota... (Soporta Markdown: **negrita**, *cursiva*, # títulos, etc.)",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            )
                        }
                        val onBackgroundColor = MaterialTheme.colorScheme.onBackground
                        val visualTransformation = remember(onBackgroundColor, bodyTextFieldValue.selection.start, hideMarkdownSyntax) {
                            app.uamo.ynotes.utils.MarkdownLiveVisualTransformation(onBackgroundColor, bodyTextFieldValue.selection.start, hideMarkdownSyntax)
                        }
                        BasicTextField(
                            value = bodyTextFieldValue,
                            onValueChange = { onBodyChange(it) },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = onBackgroundColor,
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.fillMaxSize(),
                            cursorBrush = SolidColor(cursorColor),
                            visualTransformation = visualTransformation
                        )
                    }
                }

                // ⚡ QUICK MARKDOWN FORMATTING TOOLBAR
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Undo ("Atrás") button - session only
                        AnimatedVisibility(
                            visible = canUndo,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Surface(
                                onClick = { performUndo() },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Deshacer (Volver atrás)",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Atrás",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Redo ("Alante") button - session only
                        AnimatedVisibility(
                            visible = canRedo,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Surface(
                                onClick = { performRedo() },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Redo,
                                        contentDescription = "Rehacer (Volver alante)",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Alante",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (canUndo || canRedo) {
                            VerticalDivider(
                                modifier = Modifier
                                    .height(20.dp)
                                    .padding(horizontal = 2.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        // Quick Toggle Button for syntax
                        Surface(
                            onClick = { hideMarkdownSyntax = !hideMarkdownSyntax },
                            shape = RoundedCornerShape(10.dp),
                            color = if (hideMarkdownSyntax) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (hideMarkdownSyntax) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hideMarkdownSyntax) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (hideMarkdownSyntax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (hideMarkdownSyntax) "Oculto" else "Visible",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hideMarkdownSyntax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Formatted markdown example chips
                        MarkdownToolbarChip(onClick = { wrapOrInsert("**", "**") }) {
                            Text(
                                text = "Negrita",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        MarkdownToolbarChip(onClick = { wrapOrInsert("*", "*") }) {
                            Text(
                                text = "Cursiva",
                                fontStyle = FontStyle.Italic,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        MarkdownToolbarChip(onClick = { insertAtLineStart("# ") }) {
                            Text(
                                text = "H1 Título",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        MarkdownToolbarChip(onClick = { insertAtLineStart("## ") }) {
                            Text(
                                text = "H2 Subtítulo",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        MarkdownToolbarChip(onClick = { insertAtLineStart("- ") }) {
                            Text(
                                text = "• Lista",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        MarkdownToolbarChip(onClick = { insertAtLineStart("- [ ] ") }) {
                            Text(
                                text = "☑ Tarea",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        MarkdownToolbarChip(onClick = { insertAtLineStart("> ") }) {
                            Text(
                                text = "❝ Cita",
                                fontStyle = FontStyle.Italic,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        MarkdownToolbarChip(onClick = { wrapOrInsert("`", "`") }) {
                            Text(
                                text = "Código",
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        MarkdownToolbarChip(onClick = { wrapOrInsert("~~", "~~") }) {
                            Text(
                                text = "Tachado",
                                textDecoration = TextDecoration.LineThrough,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
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

                // 1. Color Palette with modern gradients
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
                            val swatchBrush = getNoteCardBrush(colorValue, isDark)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(swatchBrush)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else getNoteCardBorderColor(colorValue, isDark),
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
                                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
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
                        // Focus Mode / Fullscreen
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Modo Enfoque (Pantalla completa)") },
                            supportingContent = { Text("Oculta barras y elementos para escribir sin distracciones") },
                            leadingContent = {
                                Icon(Icons.Default.Fullscreen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.clickable {
                                showPropertiesSheet = false
                                isFocusMode = true
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

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

@Composable
private fun MarkdownToolbarChip(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 32.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
