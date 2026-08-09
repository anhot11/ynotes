package app.uamo.ynotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.ui.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookNotesScreen(
    book: BookEntity,
    notes: List<NoteEntity>,
    onNoteClick: (NoteEntity) -> Unit,
    onAddNote: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val unpinnedNotes = remember(notes) { notes.filter { !it.isPinned } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = { Text(book.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                icon = { Icon(Icons.Default.Edit, "Añadir Nota") },
                text = { Text("Nueva nota") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
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
                        text = "Este libro está vacío.",
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
                            NoteCard(note = note, onClick = { onNoteClick(note) })
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
