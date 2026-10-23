package app.uamo.ynotes.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import app.uamo.ynotes.ui.theme.GlassBorder
import app.uamo.ynotes.utils.MarkdownLiveVisualTransformation
import app.uamo.ynotes.utils.MediaManager
import app.uamo.ynotes.utils.SoundManager
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
    var isExiting by remember { mutableStateOf(false) }
    var isWidgetSpecial by remember { mutableStateOf(editingNote?.isWidgetSpecial ?: false) }
    var expiresAt by remember { mutableStateOf(editingNote?.expiresAt) }

    // Media state
    var mediaFileNames by remember {
        mutableStateOf(
            editingNote?.mediaFiles?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }
    val mediaBitmaps = remember { mutableStateMapOf<String, Bitmap>() }

    var showColorPicker by remember { mutableStateOf(false) }
    var showBookMenu by remember { mutableStateOf(false) }
    var showWidgetMenu by remember { mutableStateOf(false) }

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

    // Debounced save: cancels the previous pending save and waits 500ms of silence
    fun scheduleSave() {
        if (isDeleted || isExiting) return
        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(500)
            if (!isDeleted && (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank() || mediaFileNames.isNotEmpty())) {
                onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden, mediaFilesString(), expiresAt, isWidgetSpecial)
            }
        }
    }

    // Immediate save for buttons (pin, color, book, back, check)
    fun saveNow() {
        if (isDeleted) return
        debounceJob?.cancel()
        if (titleText.trim().isNotBlank() || bodyText.trim().isNotBlank() || mediaFileNames.isNotEmpty()) {
            onSave(stableNoteId, titleText.trim(), bodyText.trim(), noteColor, isPinned, bookId, isBodyHidden, mediaFilesString(), expiresAt, isWidgetSpecial)
        }
    }

    fun saveAndExitOnce() {
        if (isExiting) return
        isExiting = true
        saveNow()
        onNavigateBack()
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
        saveAndExitOnce()
    }

    Scaffold(
        modifier = Modifier.sharedElementTransition("note-$stableNoteId"),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        SoundManager.playTap()
                        saveAndExitOnce()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        SoundManager.playTap()
                        isPinned = !isPinned
                        saveNow()
                    }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Fijar",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isBooksEnabled && books.isNotEmpty()) {
                        Box {
                            IconButton(onClick = {
                                SoundManager.playTap()
                                showBookMenu = true
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Libro",
                                    tint = if (bookId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showBookMenu,
                                onDismissRequest = { showBookMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sin libro") },
                                    onClick = {
                                        SoundManager.playTap()
                                        bookId = null
                                        showBookMenu = false
                                        saveNow()
                                    }
                                )
                                books.forEach { book ->
                                    DropdownMenuItem(
                                        text = { Text(book.name) },
                                        onClick = {
                                            SoundManager.playTap()
                                            bookId = book.id
                                            showBookMenu = false
                                            saveNow()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = {
                            SoundManager.playTap()
                            showWidgetMenu = true
                        }) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Widget",
                                tint = if (isWidgetSpecial) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showWidgetMenu,
                            onDismissRequest = { showWidgetMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isWidgetSpecial) "Desactivar Widget" else "Fijar en Widget") },
                                onClick = {
                                    SoundManager.playTap()
                                    isWidgetSpecial = !isWidgetSpecial
                                    if (!isWidgetSpecial) {
                                        expiresAt = null
                                    }
                                    showWidgetMenu = false
                                    saveNow()
                                }
                            )
                            if (isWidgetSpecial) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("No expira", fontWeight = if (expiresAt == null) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        SoundManager.playTap()
                                        expiresAt = null
                                        showWidgetMenu = false
                                        saveNow()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Expira en 1 hora") },
                                    onClick = {
                                        SoundManager.playTap()
                                        expiresAt = System.currentTimeMillis() + 3600000L
                                        showWidgetMenu = false
                                        saveNow()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Expira en 1 día") },
                                    onClick = {
                                        SoundManager.playTap()
                                        expiresAt = System.currentTimeMillis() + 86400000L
                                        showWidgetMenu = false
                                        saveNow()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Expira en 1 semana") },
                                    onClick = {
                                        SoundManager.playTap()
                                        expiresAt = System.currentTimeMillis() + 604800000L
                                        showWidgetMenu = false
                                        saveNow()
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        SoundManager.playTap()
                        showColorPicker = !showColorPicker
                    }) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Color",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        SoundManager.playTap()
                        launchImagePicker()
                    }) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Adjuntar imagen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        SoundManager.playTap()
                        isBodyHidden = !isBodyHidden
                        saveNow()
                    }) {
                        Icon(
                            if (isBodyHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Ocultar descripción",
                            tint = if (isBodyHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (editingNote != null) {
                        IconButton(onClick = {
                            if (!isExiting) {
                                isExiting = true
                                isDeleted = true
                                debounceJob?.cancel()
                                onDelete(editingNote.id)
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar Nota",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = {
                        SoundManager.playTap()
                        saveAndExitOnce()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar Nota")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .imePadding()
        ) {
            if (showColorPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
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
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(displayColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else GlassBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    SoundManager.playTap()
                                    noteColor = colorValue
                                    showColorPicker = false
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

            // Media thumbnails strip
            if (mediaFileNames.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mediaFileNames) { fileName ->
                        val bitmap = mediaBitmaps[fileName]
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
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
                                    .size(36.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.6f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Eliminar imagen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

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
                            text = "Escribe tu nota...",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        )
                    }
                    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
                    val visualTransformation = remember(onBackgroundColor, bodyTextFieldValue.selection.start, hideMarkdownSyntax) {
                        MarkdownLiveVisualTransformation(onBackgroundColor, bodyTextFieldValue.selection.start, hideMarkdownSyntax)
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

            // Compact Markdown Formatting Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            SoundManager.playTap()
                            performUndo()
                        },
                        enabled = canUndo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Deshacer",
                            modifier = Modifier.size(18.dp),
                            tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }

                    IconButton(
                        onClick = {
                            SoundManager.playTap()
                            performRedo()
                        },
                        enabled = canRedo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Rehacer",
                            modifier = Modifier.size(18.dp),
                            tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(20.dp)
                            .padding(horizontal = 2.dp),
                        color = GlassBorder
                    )

                    ToolbarItemButton(
                        onClick = { wrapOrInsert("**", "**") },
                        contentDescription = "Negrita"
                    ) {
                        Text(
                            text = "B",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    ToolbarItemButton(
                        onClick = { wrapOrInsert("*", "*") },
                        contentDescription = "Cursiva"
                    ) {
                        Text(
                            text = "I",
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    ToolbarItemButton(
                        onClick = { wrapOrInsert("~~", "~~") },
                        contentDescription = "Tachado"
                    ) {
                        Text(
                            text = "S",
                            textDecoration = TextDecoration.LineThrough,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(20.dp)
                            .padding(horizontal = 2.dp),
                        color = GlassBorder
                    )

                    ToolbarItemButton(
                        onClick = { insertAtLineStart("# ") },
                        contentDescription = "Título H1"
                    ) {
                        Text(
                            text = "H1",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    ToolbarItemButton(
                        onClick = { insertAtLineStart("## ") },
                        contentDescription = "Título H2"
                    ) {
                        Text(
                            text = "H2",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    ToolbarItemButton(
                        onClick = { insertAtLineStart("### ") },
                        contentDescription = "Título H3"
                    ) {
                        Text(
                            text = "H3",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(20.dp)
                            .padding(horizontal = 2.dp),
                        color = GlassBorder
                    )

                    ToolbarItemButton(
                        onClick = { insertAtLineStart("- ") },
                        contentDescription = "Lista con viñetas"
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    ToolbarItemButton(
                        onClick = { insertAtLineStart("- [ ] ") },
                        contentDescription = "Lista de tareas"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    ToolbarItemButton(
                        onClick = { insertAtLineStart("> ") },
                        contentDescription = "Cita"
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    ToolbarItemButton(
                        onClick = { wrapOrInsert("`", "`") },
                        contentDescription = "Código"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(20.dp)
                            .padding(horizontal = 2.dp),
                        color = GlassBorder
                    )

                    ToolbarItemButton(
                        onClick = { hideMarkdownSyntax = !hideMarkdownSyntax },
                        contentDescription = if (hideMarkdownSyntax) "Sintaxis oculta" else "Sintaxis visible",
                        containerColor = if (hideMarkdownSyntax) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                    ) {
                        Icon(
                            imageVector = if (hideMarkdownSyntax) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (hideMarkdownSyntax) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarItemButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = {
            SoundManager.playTap()
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
