package app.uamo.ynotes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
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
import kotlinx.coroutines.launch
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
import app.uamo.ynotes.ui.components.CustomIcons
import app.uamo.ynotes.ui.components.NoteCard
import app.uamo.ynotes.ui.theme.AuroraPrimary
import app.uamo.ynotes.utils.SoundManager

enum class HomeFilter(val label: String, val icon: String) {
    ALL("Todas", "✨"),
    PINNED("Fijadas", "📌"),
    WITH_MEDIA("Con fotos", "📷"),
    WIDGET("En Widget", "⏱️")
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

    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(HomeFilter.ALL) }
    var selectedBookFilterId by remember { mutableStateOf<String?>(null) }
    var selectedNoteIds by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedNoteIds.isNotEmpty()

    val booksMap = remember(books) {
        books.associate { it.id to it.name }
    }

    // SafeZone Mode 0: Password entered in the search bar
    LaunchedEffect(searchQuery) {
        if (safeZoneTriggerMode == 0 && safeZonePassword.isNotEmpty() && searchQuery.trim() == safeZonePassword.trim()) {
            onSearchQueryChange("")
            if (isBiometricEnabled) onRequestSafeZoneBiometric()
            else onRequestSafeZone()
        }
    }

    // Filter notes by category chip or book filter
    val filteredByChip = remember(notes, selectedFilter, selectedBookFilterId) {
        if (selectedBookFilterId != null) {
            notes.filter { it.bookId == selectedBookFilterId }
        } else {
            when (selectedFilter) {
                HomeFilter.ALL -> notes
                HomeFilter.PINNED -> notes.filter { it.isPinned }
                HomeFilter.WITH_MEDIA -> notes.filter { it.mediaFiles.isNotBlank() }
                HomeFilter.WIDGET -> notes.filter { it.isWidgetSpecial }
            }
        }
    }

    // Pinned notes float to the top naturally without disruptive full-line banners
    val displayNotes = remember(filteredByChip) {
        filteredByChip.sortedWith(
            compareByDescending<NoteEntity> { it.isPinned }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Contextual bottom action bar when notes are selected
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
                                    selectedNoteIds = if (selectedNoteIds.size == displayNotes.size) {
                                        emptySet()
                                    } else {
                                        displayNotes.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(if (selectedNoteIds.size == displayNotes.size) "Deseleccionar" else "Todas")
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
                    targetValue = if (isFabPressed) 0.94f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "fabScale"
                )

                Box(
                    modifier = Modifier
                        .padding(end = 4.dp, bottom = 4.dp)
                        .scale(fabScale)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AuroraPrimary)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isFabPressed = true
                                    tryAwaitRelease()
                                    isFabPressed = false
                                },
                                onTap = {
                                    SoundManager.playTap()
                                    onAddNote()
                                },
                                onLongPress = {
                                    if (safeZoneTriggerMode == 3) {
                                        SoundManager.playTap()
                                        if (isBiometricEnabled) onRequestSafeZoneBiometric()
                                        else onRequestSafeZone()
                                    }
                                }
                            )
                        }
                        .padding(horizontal = if (isFabExpanded) 22.dp else 16.dp, vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, "Añadir Nota", tint = Color.White, modifier = Modifier.size(20.dp))
                        AnimatedVisibility(
                            visible = isFabExpanded,
                            enter = expandHorizontally() + fadeIn(),
                            exit = shrinkHorizontally() + fadeOut()
                        ) {
                            Row {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Nueva nota",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ── TOP BAR: Sleek, Minimalist Dual Pills ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pill: Search Input
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(50.dp),
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
                            contentDescription = "Buscar notas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { SoundManager.playTap() },
                                        onLongPress = {
                                            SoundManager.playTap()
                                            if (safeZoneTriggerMode == 1) {
                                                if (isBiometricEnabled) onRequestSafeZoneBiometric()
                                                else onRequestSafeZone()
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Pill: Action Controls (Sort, Categories, Trash, Settings)
                Surface(
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 1. Sort Button
                        Box {
                            IconButton(onClick = {
                                SoundManager.playTap()
                                showSortMenu = true
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Ordenar",
                                    tint = if (sortOrder != SortOrder.DATE_MODIFIED_DESC)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                Text(
                                    text = "Ordenar por",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.label,
                                                color = if (sortOrder == order)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            SoundManager.playTap()
                                            onSortOrderChange(order)
                                            showSortMenu = false
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

                        // 2. Categories / Cuadernos Button
                        Box {
                            IconButton(onClick = {
                                SoundManager.playTap()
                                showCategoryMenu = true
                            }) {
                                Icon(
                                    imageVector = if (selectedFilter != HomeFilter.ALL || selectedBookFilterId != null)
                                        Icons.Default.FilterList
                                    else
                                        Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Categorías",
                                    tint = if (selectedFilter != HomeFilter.ALL || selectedBookFilterId != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false }
                            ) {
                                Text(
                                    text = "Categorías",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                                HomeFilter.entries.forEach { filter ->
                                    val isSelected = selectedFilter == filter && selectedBookFilterId == null
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${filter.icon}  ${filter.label}",
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                val count = when (filter) {
                                                    HomeFilter.ALL -> notes.size
                                                    HomeFilter.PINNED -> notes.count { it.isPinned }
                                                    HomeFilter.WITH_MEDIA -> notes.count { it.mediaFiles.isNotBlank() }
                                                    HomeFilter.WIDGET -> notes.count { it.isWidgetSpecial }
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    text = "$count",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        },
                                        onClick = {
                                            SoundManager.playTap()
                                            selectedFilter = filter
                                            selectedBookFilterId = null
                                            showCategoryMenu = false
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

                                if (isBooksEnabled) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    Text(
                                        text = "Cuadernos",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                    books.forEach { book ->
                                        val isSelected = selectedBookFilterId == book.id
                                        val count = notes.count { it.bookId == book.id }
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "📖  ${book.name}",
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Text(
                                                        text = "$count",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                SoundManager.playTap()
                                                selectedBookFilterId = if (selectedBookFilterId == book.id) null else book.id
                                                showCategoryMenu = false
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
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Administrar cuadernos",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        onClick = {
                                            SoundManager.playTap()
                                            showCategoryMenu = false
                                            onBooksClick()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Trash Button
                        IconButton(onClick = {
                            SoundManager.playTap()
                            onTrashClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Papelera",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 4. Settings Button (with Mode 2 SafeZone long-press)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            SoundManager.playTap()
                                            onSettingsClick()
                                        },
                                        onLongPress = {
                                            SoundManager.playTap()
                                            if (safeZoneTriggerMode == 2) {
                                                if (isBiometricEnabled) onRequestSafeZoneBiometric()
                                                else onRequestSafeZone()
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

            Spacer(modifier = Modifier.height(2.dp))

            // ── MAIN NOTES STAGGERED GRID (2-Column "cuadro" layout) ──
            if (displayNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Sin notas",
                        modifier = Modifier
                            .size(72.dp)
                            .padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "Sin resultados para \"$searchQuery\""
                        else if (selectedBookFilterId != null) "No hay notas en \"${booksMap[selectedBookFilterId] ?: "este cuaderno"}\""
                        else if (selectedFilter != HomeFilter.ALL) "No hay notas en ${selectedFilter.label}"
                        else "Todavía no hay notas. ¡Añade una!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
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
                        top = 6.dp,
                        bottom = if (isSelectionMode) 96.dp else 88.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(displayNotes, key = { it.id }) { note ->
                        val isSelected = selectedNoteIds.contains(note.id)
                        NoteCard(
                            note = note,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedNoteIds = if (isSelected) selectedNoteIds - note.id else selectedNoteIds + note.id
                                } else {
                                    onNoteClick(note)
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
                            isSelectionMode = isSelectionMode,
                            bookName = note.bookId?.let { booksMap[it] }
                        )
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
