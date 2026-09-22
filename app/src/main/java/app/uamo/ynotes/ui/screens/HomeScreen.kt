package app.uamo.ynotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.data.SortOrder
import app.uamo.ynotes.data.applySortOrder
import app.uamo.ynotes.ui.components.NoteCard
import app.uamo.ynotes.ui.theme.LocalAppTheme
import app.uamo.ynotes.ui.theme.AppThemeType

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
    onRequestSafeZone: () -> Unit,
    onRequestSafeZoneBiometric: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onBooksClick: () -> Unit,
    onTrashClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (safeZoneTriggerMode == 0 && safeZonePassword.isNotEmpty() && searchQuery == safeZonePassword) {
            onSearchQueryChange("")
            onRequestSafeZone()
        }
    }


    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val unpinnedNotes = remember(notes) { notes.filter { !it.isPinned } }
    val currentTheme = LocalAppTheme.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            var isFabPressed by remember { mutableStateOf(false) }
            val fabScale by animateFloatAsState(
                targetValue = if (isFabPressed) 0.95f else 1f,
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
                    .padding(end = 8.dp, bottom = 8.dp)
                    .scale(fabScale)
                    .background(
                        brush = if (currentTheme == AppThemeType.AMOLED) app.uamo.ynotes.ui.theme.AuroraPrimary else SolidColor(MaterialTheme.colorScheme.primary),
                        shape = fabShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isFabPressed = true
                                tryAwaitRelease()
                                isFabPressed = false
                            },
                            onTap = { onAddNote() },
                            onLongPress = {
                                if (safeZoneTriggerMode == 3) {
                                    if (isBiometricEnabled) onRequestSafeZoneBiometric()
                                    else onRequestSafeZone()
                                }
                            }
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, "Añadir Nota", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nueva nota", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            modifier = Modifier.size(24.dp).pointerInput(Unit) {
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Sort button
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
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
                                    modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                                            onSortOrderChange(order)
                                            showSortMenu = false
                                        },
                                        leadingIcon = if (sortOrder == order) ({
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = androidx.compose.ui.Modifier.size(18.dp)
                                            )
                                        }) else null
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onTrashClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Papelera",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isBooksEnabled) {
                            IconButton(onClick = onBooksClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Libros",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onSettingsClick() },
                                        onLongPress = {
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

            Spacer(modifier = Modifier.height(4.dp))

            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Sin notas",
                        modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) "Todavía no hay notas. ¡Añade una!" else "Sin resultados para \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                            NoteCard(note = note, onClick = onNoteClick)
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
                            NoteCard(note = note, onClick = onNoteClick)
                        }
                    }
                }
            }
        }
    }
}
