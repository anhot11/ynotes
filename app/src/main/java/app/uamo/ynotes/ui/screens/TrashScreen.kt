package app.uamo.ynotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.ui.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    deletedNotes: List<NoteEntity>,
    onRestore: (String) -> Unit,
    onDeletePermanently: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = { Text("Papelera") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (deletedNotes.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Vaciar Papelera", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (deletedNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("La papelera está vacía", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(deletedNotes, key = { it.id }) { note ->
                        NoteCard(note = note, onClick = { selectedNote = it })
                    }
                }
            }
        }

        if (showEmptyTrashDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyTrashDialog = false },
                title = { Text("Vaciar papelera") },
                text = { Text("¿Estás seguro de que quieres eliminar todas las notas permanentemente? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        onEmptyTrash()
                        showEmptyTrashDialog = false
                    }) {
                        Text("Vaciar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        selectedNote?.let { note ->
            AlertDialog(
                onDismissRequest = { selectedNote = null },
                title = { Text("Opciones de nota") },
                text = { Text("¿Qué deseas hacer con esta nota eliminada?") },
                confirmButton = {
                    TextButton(onClick = {
                        onRestore(note.id)
                        selectedNote = null
                    }) {
                        Text("Restaurar")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            onDeletePermanently(note.id)
                            selectedNote = null
                        }) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { selectedNote = null }) {
                            Text("Cancelar")
                        }
                    }
                }
            )
        }
    }
}
