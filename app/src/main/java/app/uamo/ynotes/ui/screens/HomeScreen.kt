package app.uamo.ynotes.ui.screens

import android.os.SystemClock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.data.SortOrder
import app.uamo.ynotes.ui.components.NoteCard
import app.uamo.ynotes.ui.theme.AppThemeType
import app.uamo.ynotes.ui.theme.AuroraPrimary
import app.uamo.ynotes.ui.theme.LocalAppTheme
import app.uamo.ynotes.utils.SoundManager
import kotlinx.coroutines.launch

enum class HomeFilter(val label: String) {
    ALL("Todas"),
    PINNED("Fijadas"),
    WITH_MEDIA("Con fotos"),
    WIDGET("En Widget")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    notes: List<NoteEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    safeZonePassword: String,
    safeZoneTriggerMode: Int,
    isBiometricEnabled: Boolean,
    isBooksEnabled: Boolean,
    books: List<BookEntity> = emptyList(),
    onRequestSafeZone: () -> Unit,
    onRequestSafeZoneBiometric: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onBooksClick: () -> Unit,
    onTrashClick: () -> Unit,
    onDeleteNotes: ((List<String>) -> Unit)? = null,
    onRestoreNote: ((String) -> Unit)? = null,
    onToggleWidgetSpecial: ((String) -> Unit)? = null,
    onSetNotesWidgetSpecial: ((List<String>, Boolean) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyStaggeredGridState()
    val isFabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }
    var longPressedNote by remember { mutableStateOf<NoteEntity?>(null) }

    var showSortAndFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(HomeFilter.ALL) }
    var selectedNoteIds by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedNoteIds.isNotEmpty()
    val currentTheme = LocalAppTheme.current

    // Single-click debounce timestamp for all navigation actions from HomeScreen
    var lastNavClickTime by remember { mutableLongStateOf(0L) }
    fun runSingleNav(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastNavClickTime > 400L) {
            lastNavClickTime = now
            action()
        }
    }

    val currentSafeZoneTriggerMode by rememberUpdatedState(safeZoneTriggerMode)
    val currentIsBiometricEnabled by rememberUpdatedState(isBiometricEnabled)
    val currentOnRequestSafeZone by rememberUpdatedState(onRequestSafeZone)
    val currentOnRequestSafeZoneBiometric by rememberUpdatedState(onRequestSafeZoneBiometric)
    val currentOnAddNote by rememberUpdatedState(onAddNote)
    val currentOnSettingsClick by rememberUpdatedState(onSettingsClick)

    // SafeZone Mode 0: Password entered in the search bar
    LaunchedEffect(searchQuery, safeZoneTriggerMode, safeZonePassword, isBiometricEnabled) {
        if (safeZoneTriggerMode == 0 && safeZonePassword.isNotEmpty() && searchQuery.trim() == safeZonePassword.trim()) {
            onSearchQueryChange("")
            if (isBiometricEnabled) onRequestSafeZoneBiometric()
            else onRequestSafeZone()
        }
    }

    // Filter notes by category
    val filteredNotes = remember(notes, selectedFilter) {
        when (selectedFilter) {
            HomeFilter.ALL -> notes
            HomeFilter.PINNED -> notes.filter { it.isPinned }
            HomeFilter.WITH_MEDIA -> notes.filter { it.mediaFiles.isNotBlank() }
            HomeFilter.WIDGET -> notes.filter { it.isWidgetSpecial }
        }
    }

    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.isPinned } }
    val unpinnedNotes = remember(filteredNotes) { filteredNotes.filter { !it.isPinned } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                SoundManager.playTap()
                                selectedNoteIds = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar selección")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${selectedNoteIds.size} seleccionada${if (selectedNoteIds.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    SoundManager.playTap()
                                    selectedNoteIds = if (selectedNoteIds.size == filteredNotes.size) {
                                        emptySet()
                                    } else {
                                        filteredNotes.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(if (selectedNoteIds.size == filteredNotes.size) "Deseleccionar" else "Todas")
                            }

                            if (onSetNotesWidgetSpecial != null) {
                                val allInWidget = selectedNoteIds.all { id -> notes.find { it.id == id }?.isWidgetSpecial == true }
                                IconButton(
                                    onClick = {
                                        SoundManager.playTap()
                                        onSetNotesWidgetSpecial(selectedNoteIds.toList(), !allInWidget)
                                        val count = selectedNoteIds.size
                                        val msg = if (allInWidget) "$count quitadas del widget" else "$count añadidas al widget"
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (allInWidget) Icons.Default.Widgets else Icons.Outlined.Widgets,
                                        contentDescription = "Fijar en Widget",
                                        tint = if (allInWidget) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (onDeleteNotes != null) {
                                IconButton(
                                    onClick = {
                                        val idsToDelete = selectedNoteIds.toList()
                                        onDeleteNotes(idsToDelete)
                                        selectedNoteIds = emptySet()
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "${idsToDelete.size} nota${if (idsToDelete.size > 1) "s" else ""} a la papelera",
                                                actionLabel = "Deshacer",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                idsToDelete.forEach { id -> onRestoreNote?.invoke(id) }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Mover a papelera",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                var isFabPressed by remember { mutableStateOf(false) }
                val fabScale by animateFloatAsState(
                    targetValue = when {
                        isFabPressed -> 0.90f
                        !isFabExpanded -> 0.92f
                        else -> 1f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "fabScale"
                )

                val fabShape = when (currentTheme) {
                    AppThemeType.GOOGLE -> RoundedCornerShape(16.dp)
                    AppThemeType.SAMSUNG -> CircleShape
                    else -> RoundedCornerShape(20.dp)
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(fabScale)
                        .clip(fabShape)
                        .then(
                            if (currentTheme == AppThemeType.AMOLED) Modifier.background(AuroraPrimary)
                            else Modifier.background(MaterialTheme.colorScheme.primary)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isFabPressed = true
                                    tryAwaitRelease()
                                    isFabPressed = false
                                },
                                onTap = {
                                    runSingleNav {
                                        SoundManager.playTap()
                                        currentOnAddNote()
                                    }
                                },
                                onLongPress = {
                                    if (currentSafeZoneTriggerMode == 3) {
                                        SoundManager.playTap()
                                        if (currentIsBiometricEnabled) currentOnRequestSafeZoneBiometric()
                                        else currentOnRequestSafeZone()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Añadir Nota",
                        tint = if (currentTheme == AppThemeType.AMOLED) Color.White else MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ── TOP BAR: Minimalist Dual Pills (v1.0.3 layout) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pill: Search Bar
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = if (currentTheme == AppThemeType.GOOGLE) RoundedCornerShape(16.dp) else RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { SoundManager.playTap() },
                                        onLongPress = {
                                            if (currentSafeZoneTriggerMode == 1) {
                                                SoundManager.playTap()
                                                if (currentIsBiometricEnabled) currentOnRequestSafeZoneBiometric()
                                                else currentOnRequestSafeZone()
                                            }
                                        }
                                    )
                                }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar notas",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    SoundManager.playTap()
                                    onSearchQueryChange("")
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Pill: Action Buttons
                val topIconSize = if (isBooksEnabled) 40.dp else 46.dp
                Surface(
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = if (isBooksEnabled) 4.dp else 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Sort & Categories Button
                        Box {
                            val isFilterOrSortActive = sortOrder != SortOrder.DATE_MODIFIED_DESC || selectedFilter != HomeFilter.ALL
                            IconButton(
                                onClick = {
                                    SoundManager.playTap()
                                    showSortAndFilterMenu = true
                                },
                                modifier = Modifier.size(topIconSize)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Ordenar y Categorías",
                                    tint = if (isFilterOrSortActive)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showSortAndFilterMenu,
                                onDismissRequest = { showSortAndFilterMenu = false }
                            ) {
                                Text(
                                    text = "Categorías",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                HomeFilter.entries.forEach { filter ->
                                    val isSelected = selectedFilter == filter
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = filter.label,
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            SoundManager.playTap()
                                            selectedFilter = filter
                                            showSortAndFilterMenu = false
                                        },
                                        leadingIcon = if (isSelected) ({
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }) else null
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                Text(
                                    text = "Ordenar por",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.label,
                                                color = if (sortOrder == order)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            SoundManager.playTap()
                                            onSortOrderChange(order)
                                            showSortAndFilterMenu = false
                                        },
                                        leadingIcon = if (sortOrder == order) ({
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }) else null
                                    )
                                }
                            }
                        }

                        // 2. Trash Button (solid Delete icon matching v1.0.3 screenshot)
                        IconButton(
                            onClick = {
                                runSingleNav {
                                    SoundManager.playTap()
                                    onTrashClick()
                                }
                            },
                            modifier = Modifier.size(topIconSize)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Papelera",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 3. Books Button (when enabled in Settings, directly opens BooksScreen)
                        if (isBooksEnabled) {
                            IconButton(
                                onClick = {
                                    runSingleNav {
                                        SoundManager.playTap()
                                        onBooksClick()
                                    }
                                },
                                modifier = Modifier.size(topIconSize)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Libros",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 4. Settings Button (with Mode 2 SafeZone long-press)
                        Box(
                            modifier = Modifier
                                .size(topIconSize)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            runSingleNav {
                                                SoundManager.playTap()
                                                currentOnSettingsClick()
                                            }
                                        },
                                        onLongPress = {
                                            if (currentSafeZoneTriggerMode == 2) {
                                                SoundManager.playTap()
                                                if (currentIsBiometricEnabled) currentOnRequestSafeZoneBiometric()
                                                else currentOnRequestSafeZone()
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── MAIN NOTES STAGGERED GRID (v1.0.3 2-Column layout) ──
            if (filteredNotes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Sin notas",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "Sin resultados para \"$searchQuery\""
                        else if (selectedFilter != HomeFilter.ALL) "No hay notas en ${selectedFilter.label}"
                        else "Todavía no hay notas. ¡Añade una!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = if (isSelectionMode) 96.dp else 88.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    if (pinnedNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                "FIJADAS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp)
                            )
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
                            val isSelected = selectedNoteIds.contains(note.id)
                            NoteCard(
                                note = note,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedNoteIds = if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id
                                    } else {
                                        runSingleNav { onNoteClick(note) }
                                    }
                                },
                                onLongPress = {
                                    if (isSelectionMode) {
                                        selectedNoteIds = if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id
                                    } else {
                                        longPressedNote = note
                                    }
                                },
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode
                            )
                        }
                    }

                    if (unpinnedNotes.isNotEmpty()) {
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    "OTRAS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 16.dp)
                                )
                            }
                        }
                        items(unpinnedNotes, key = { it.id }) { note ->
                            val isSelected = selectedNoteIds.contains(note.id)
                            NoteCard(
                                note = note,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedNoteIds = if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id
                                    } else {
                                        runSingleNav { onNoteClick(note) }
                                    }
                                },
                                onLongPress = {
                                    if (isSelectionMode) {
                                        selectedNoteIds = if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id
                                    } else {
                                        longPressedNote = note
                                    }
                                },
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode
                            )
                        }
                    }
                }
            }
        }
    }

    // Long press quick action sheet on a note
    if (longPressedNote != null) {
        val note = longPressedNote!!
        ModalBottomSheet(
            onDismissRequest = { longPressedNote = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = note.title.ifEmpty { "Nota sin título" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Action: Añadir/Quitar de Widget
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            SoundManager.playTap()
                            onToggleWidgetSpecial?.invoke(note.id)
                            val msg = if (note.isWidgetSpecial) "Quitada del widget" else "Fijada en widget de pantalla"
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                            longPressedNote = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (note.isWidgetSpecial) Icons.Default.Widgets else Icons.Outlined.Widgets,
                        contentDescription = null,
                        tint = if (note.isWidgetSpecial) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (note.isWidgetSpecial) "Quitar del widget" else "Fijar en widget de pantalla",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Action: Seleccionar varias notas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            SoundManager.playTap()
                            selectedNoteIds = setOf(note.id)
                            longPressedNote = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Seleccionar varias notas", style = MaterialTheme.typography.bodyLarge)
                }

                // Action: Mover a papelera con Deshacer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            SoundManager.playTap()
                            onDeleteNotes?.invoke(listOf(note.id))
                            longPressedNote = null
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Nota movida a la papelera",
                                    actionLabel = "Deshacer",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    onRestoreNote?.invoke(note.id)
                                }
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Mover a papelera",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
