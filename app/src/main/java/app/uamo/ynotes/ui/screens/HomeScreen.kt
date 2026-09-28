package app.uamo.ynotes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import app.uamo.ynotes.ui.theme.AppThemeType
import app.uamo.ynotes.ui.theme.AuroraPrimary
import app.uamo.ynotes.ui.theme.LocalAppTheme
import app.uamo.ynotes.utils.SoundManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    var selectedFilter by remember { mutableStateOf(HomeFilter.ALL) }
    var selectedBookFilterId by remember { mutableStateOf<String?>(null) }
    var showFilterPills by remember { mutableStateOf(true) }
    var selectedNoteIds by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedNoteIds.isNotEmpty()

    val booksMap = remember(books) {
        books.associate { it.id to it.name }
    }

    LaunchedEffect(searchQuery) {
        if (safeZoneTriggerMode == 0 && safeZonePassword.isNotEmpty() && searchQuery == safeZonePassword) {
            onSearchQueryChange("")
            onRequestSafeZone()
        }
    }

    // Filter notes
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

    val pinnedNotes = remember(filteredByChip) { filteredByChip.filter { it.isPinned } }
    val unpinnedNotes = remember(filteredByChip) { filteredByChip.filter { !it.isPinned } }
    val currentTheme = LocalAppTheme.current
    val isDark = isSystemInDarkTheme() || currentTheme == AppThemeType.AMOLED

    // Dynamic greeting & date
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 6..12 -> "Buenos días"
            in 13..19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
    val currentDateStr = remember {
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        dateFormat.format(Date()).replaceFirstChar { it.uppercase() }
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
                                    selectedNoteIds = if (selectedNoteIds.size == filteredByChip.size) {
                                        emptySet()
                                    } else {
                                        filteredByChip.map { it.id }.toSet()
                                    }
                                }
                            ) {
                                Text(if (selectedNoteIds.size == filteredByChip.size) "Deseleccionar" else "Todas")
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
                ExtendedFloatingActionButton(
                    onClick = {
                        SoundManager.playTap()
                        onAddNote()
                    },
                    expanded = isFabExpanded,
                    icon = { Icon(Icons.Default.Edit, "Añadir Nota", modifier = Modifier.size(20.dp)) },
                    text = { Text("Nueva nota", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 🌟 HERO HEADER: Identity, Greeting & Glass Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "yNotes",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$greeting · $currentDateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Glass Header Action Buttons
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBooksEnabled) {
                            HeaderGlassButton(
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Cuadernos",
                                onClick = onBooksClick
                            )
                        }
                        HeaderGlassButton(
                            icon = Icons.Default.DeleteOutline,
                            contentDescription = "Papelera",
                            onClick = onTrashClick
                        )
                        HeaderGlassButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            onClick = onSettingsClick,
                            onLongPress = {
                                if (safeZoneTriggerMode == 2) {
                                    if (isBiometricEnabled) onRequestSafeZoneBiometric()
                                    else onRequestSafeZone()
                                }
                            }
                        )
                    }
                }

                // Stats capsule badge
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${notes.size} notas guardadas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            if (pinnedNotes.isNotEmpty()) {
                                Text(
                                    text = "  ·  ${notes.count { it.isPinned }} fijadas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 🔍 DEDICATED SLEEK SEARCH BAR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(22.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
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
                                text = "Buscar por título, contenido o palabra clave...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Limpiar búsqueda",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Toggle filter pills button
                    IconButton(
                        onClick = { showFilterPills = !showFilterPills },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = if (showFilterPills) "Ocultar filtros" else "Mostrar filtros",
                            tint = if (showFilterPills || selectedFilter != HomeFilter.ALL || selectedBookFilterId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Sort menu button
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Ordenar notas",
                                tint = if (sortOrder != SortOrder.DATE_MODIFIED_DESC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
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
                                            fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sortOrder == order) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
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
                }
            }

            // 🏷️ INTERACTIVE FILTER PILLS ROW (Collapsible to save vertical space)
            AnimatedVisibility(
                visible = showFilterPills || selectedFilter != HomeFilter.ALL || selectedBookFilterId != null,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(HomeFilter.entries) { filter ->
                        val isSelected = selectedFilter == filter && selectedBookFilterId == null
                        val count = when (filter) {
                            HomeFilter.ALL -> notes.size
                            HomeFilter.PINNED -> notes.count { it.isPinned }
                            HomeFilter.WITH_MEDIA -> notes.count { it.mediaFiles.isNotBlank() }
                            HomeFilter.WIDGET -> notes.count { it.isWidgetSpecial }
                        }

                        Surface(
                            onClick = {
                                selectedFilter = filter
                                selectedBookFilterId = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filter.icon} ${filter.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isBooksEnabled && books.isNotEmpty()) {
                        items(books, key = { it.id }) { book ->
                            val isSelected = selectedBookFilterId == book.id
                            val count = notes.count { it.bookId == book.id }

                            Surface(
                                onClick = {
                                    if (selectedBookFilterId == book.id) {
                                        selectedBookFilterId = null
                                        selectedFilter = HomeFilter.ALL
                                    } else {
                                        selectedBookFilterId = book.id
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📖 ${book.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 📋 MAIN NOTES STAGGERED GRID OR INSPIRING EMPTY STATE
            if (filteredByChip.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 48.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CustomIcons.NotebookEditOutline,
                            contentDescription = "Sin notas",
                            modifier = Modifier.size(46.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Sin resultados para \"$searchQuery\""
                        else if (selectedBookFilterId != null) "No hay notas en \"${booksMap[selectedBookFilterId] ?: "este cuaderno"}\""
                        else if (selectedFilter != HomeFilter.ALL) "No hay notas en ${selectedFilter.label}"
                        else "Tu espacio creativo está listo",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Prueba con otra palabra clave o revisa los filtros."
                        else "Captura pensamientos, listas y proyectos con privacidad total, Markdown fluido y cifrado local.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(AuroraPrimary)
                            .clickable { onAddNote() }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Escribir mi primera nota",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(minSize = 175.dp),
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 4.dp)
                            ) {
                                Text(
                                    "FIJADAS",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        "${pinnedNotes.size}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        items(pinnedNotes, key = { it.id }) { note ->
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

                    if (unpinnedNotes.isNotEmpty()) {
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 14.dp)
                                ) {
                                    Text(
                                        "OTRAS NOTAS",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            "${unpinnedNotes.size}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
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
    }

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

@Composable
private fun HeaderGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .size(38.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress?.invoke() }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
