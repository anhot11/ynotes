package app.uamo.ynotes.ui.screens

import androidx.compose.foundation.background
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.data.HiddenAppEntity
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.data.SortOrder
import app.uamo.ynotes.data.applySortOrder
import app.uamo.ynotes.ui.components.CustomIcons
import app.uamo.ynotes.ui.components.NoteCard
import app.uamo.ynotes.ui.theme.LocalAppTheme
import app.uamo.ynotes.ui.theme.AppThemeType
import app.uamo.ynotes.utils.AppInfo
import app.uamo.ynotes.utils.getInstalledApps
import app.uamo.ynotes.utils.rememberAppIcon
import app.uamo.ynotes.utils.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZoneScreen(
    notes: List<NoteEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    isAppHidingEnabled: Int, // 0=disabled, 1=normal(apps tab), 2=reverse(apps main)
    isBooksEnabled: Boolean,
    hiddenApps: List<HiddenAppEntity> = emptyList(),
    onToggleHiddenApp: (packageName: String, name: String) -> Unit = { _, _ -> },
    onRemoveHiddenApp: (packageName: String) -> Unit = {},
    onDeactivateSafeZone: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onSettingsClick: () -> Unit,
    onBooksClick: () -> Unit,
    onTrashClick: () -> Unit
) {
    val context = LocalContext.current
    
    var showSortMenu by remember { mutableStateOf(false) }
    var isSecurityBannerExpanded by remember { mutableStateOf(false) }
    
    // All system apps — only loaded when user opens the picker dialog
    var allInstalledApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var pickerSearchQuery by remember { mutableStateOf("") }
    var isAppsListExpanded by remember { mutableStateOf(true) }
    var isNotesExpanded by remember { mutableStateOf(true) }

    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val unpinnedNotes = remember(notes) { notes.filter { !it.isPinned } }

    // Only load ALL system apps when the app picker dialog is opened
    LaunchedEffect(showAppPicker) {
        if (showAppPicker && allInstalledApps.isEmpty()) {
            isLoadingApps = true
            allInstalledApps = withContext(Dispatchers.IO) {
                getInstalledApps(context)
            }
            isLoadingApps = false
        }
    }

    if (showAppPicker) {
        val filteredInstalledApps = remember(allInstalledApps, pickerSearchQuery) {
            if (pickerSearchQuery.isBlank()) allInstalledApps
            else allInstalledApps.filter { it.name.contains(pickerSearchQuery, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = {
                showAppPicker = false
                pickerSearchQuery = ""
            },
            title = { Text("Ocultar Aplicaciones") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pickerSearchQuery,
                        onValueChange = { pickerSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("Buscar aplicación...", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (pickerSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { pickerSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isLoadingApps) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Cargando aplicaciones...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (filteredInstalledApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No se encontraron aplicaciones",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                            items(filteredInstalledApps, key = { it.packageName }) { app ->
                                val isSelected = hiddenApps.any { it.packageName == app.packageName }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            SoundManager.playSelect()
                                            onToggleHiddenApp(app.packageName, app.name)
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = app.name,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(app.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            SoundManager.playSelect()
                                            onToggleHiddenApp(app.packageName, app.name)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAppPicker = false
                    pickerSearchQuery = ""
                }) {
                    Text("Cerrar")
                }
            }
        )
    }

    val currentTheme = LocalAppTheme.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            val fabShape = when (currentTheme) {
                AppThemeType.GOOGLE -> RoundedCornerShape(16.dp)
                AppThemeType.SAMSUNG -> CircleShape
                else -> RoundedCornerShape(16.dp)
            }
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(Icons.Default.Lock, "Añadir Secreto") },
                text = { Text("Nuevo secreto") },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = fabShape
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Cyber Vault Security Banner - Compact & Collapsible
            Surface(
                onClick = { isSecurityBannerExpanded = !isSecurityBannerExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.07f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Bóveda Segura AES-256-GCM",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Icon(
                            imageVector = if (isSecurityBannerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isSecurityBannerExpanded) "Contraer" else "Ver detalles",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSecurityBannerExpanded,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            Text(
                                "Cifrado autenticado por hardware (AndroidKeyStore), protección activa contra capturas y purga de memoria.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search bar + toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
                            contentDescription = "Buscar secreto",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar en bóveda",
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
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.error),
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
                                        MaterialTheme.colorScheme.error
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
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = order.label,
                                                color = if (sortOrder == order)
                                                    MaterialTheme.colorScheme.error
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
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
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
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDeactivateSafeZone) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Cerrar Zona Segura",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════
            // MODE 0: Disabled — only show notes
            // ═══════════════════════════════════════
            if (isAppHidingEnabled == 0) {
                NotesContent(
                    filteredNotes = notes,
                    pinnedNotes = pinnedNotes,
                    unpinnedNotes = unpinnedNotes,
                    searchQuery = searchQuery,
                    onNoteClick = onNoteClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ═══════════════════════════════════════
            // MODE 1: Normal — apps as collapsible tab on top, notes below
            // ═══════════════════════════════════════
            if (isAppHidingEnabled == 1) {
                if (hiddenApps.isNotEmpty()) {
                    // Collapsible apps header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAppsListExpanded = !isAppsListExpanded }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aplicaciones Ocultas (${hiddenApps.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isAppsListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Desplegar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isAppsListExpanded) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(hiddenApps, key = { it.packageName }) { app ->
                                AppIconItem(
                                    app = app,
                                    iconSize = 48,
                                    context = context,
                                    onRemove = { onRemoveHiddenApp(app.packageName) }
                                )
                            }
                            item {
                                AddAppButton(iconSize = 48, onClick = { showAppPicker = true })
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                } else {
                    // No apps added yet — show add button
                    TextButton(
                        onClick = { showAppPicker = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir Apps Ocultas")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                NotesContent(
                    filteredNotes = notes,
                    pinnedNotes = pinnedNotes,
                    unpinnedNotes = unpinnedNotes,
                    searchQuery = searchQuery,
                    onNoteClick = onNoteClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ═══════════════════════════════════════
            // MODE 2: Reverse — apps as main grid, notes as collapsible tab
            // ═══════════════════════════════════════
            if (isAppHidingEnabled == 2) {
                // Collapsible notes header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isNotesExpanded = !isNotesExpanded }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Notas Secretas (${notes.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isNotesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Desplegar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isNotesExpanded) {
                    if (notes.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = CustomIcons.ShieldLockOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "No hay notas secretas guardadas aún." else "Sin resultados en la bóveda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(notes, key = { it.id }) { note ->
                                Card(
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clickable { onNoteClick(note) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (note.color == 0L) MaterialTheme.colorScheme.surfaceVariant
                                                         else androidx.compose.ui.graphics.Color(note.color)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (note.isPinned) {
                                            Icon(
                                                Icons.Default.PushPin,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        Text(
                                            text = note.title.ifEmpty { "Sin título" },
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        if (note.body.isNotEmpty() && !note.isBodyHidden) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            Text(
                                                text = app.uamo.ynotes.utils.parseMarkdown(note.body, bodyColor),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = bodyColor,
                                                maxLines = 2,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }

                // Apps as main grid with bigger icons
                if (hiddenApps.isEmpty()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "Sin apps ocultas",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { showAppPicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir Apps")
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(hiddenApps, key = { it.packageName }) { app ->
                            AppIconItem(
                                app = app,
                                iconSize = 60,
                                context = context,
                                onRemove = { onRemoveHiddenApp(app.packageName) }
                            )
                        }
                        item {
                            AddAppButton(iconSize = 60, onClick = { showAppPicker = true })
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────
// Reusable components
// ──────────────────────────────────────

@Composable
private fun AppIconItem(
    app: HiddenAppEntity,
    iconSize: Int,
    context: android.content.Context,
    onRemove: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val iconBitmap = rememberAppIcon(packageName = app.packageName, context = context)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            SoundManager.playSelect()
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            }
                        },
                        onLongPress = {
                            SoundManager.playSelect()
                            showMenu = true
                        }
                    )
                }
                .padding(4.dp)
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(iconSize.dp)
                        .graphicsLayer {
                            shadowElevation = 8.dp.toPx()
                            shape = RoundedCornerShape(if (iconSize > 50) 16.dp else 12.dp)
                            clip = true
                        }
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(if (iconSize > 50) 16.dp else 12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(iconSize.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = app.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (app.name.isNotBlank()) app.name else app.packageName.substringAfterLast('.'),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier = Modifier.width((iconSize + 28).dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Abrir") },
                leadingIcon = {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                onClick = {
                    showMenu = false
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Quitar de la bóveda", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    showMenu = false
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun AddAppButton(iconSize: Int, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(if (iconSize > 50) 16.dp else 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(iconSize.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Añadir app",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding((iconSize / 4).dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Añadir",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun NotesContent(
    filteredNotes: List<NoteEntity>,
    pinnedNotes: List<NoteEntity>,
    unpinnedNotes: List<NoteEntity>,
    searchQuery: String,
    onNoteClick: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (filteredNotes.isEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = "Bóveda vacía",
                modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )
            Text(
                text = if (searchQuery.isBlank()) "Bóveda vacía. ¡Añade tu primer secreto!" else "Sin resultados para \"$searchQuery\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 175.dp),
            modifier = modifier,
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
