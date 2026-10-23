package app.uamo.ynotes.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.uamo.ynotes.data.BookEntity
import app.uamo.ynotes.ui.theme.GlassBorder
import app.uamo.ynotes.utils.SoundManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    books: List<BookEntity>,
    onAddBook: (name: String, color: Long) -> Unit,
    onDeleteBook: (String) -> Unit,
    onBookClick: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newBookName by remember { mutableStateOf("") }
    var newBookColor by remember { mutableStateOf(NoteColors[0]) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    fun runSingleNav(action: () -> Unit) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime > 400L) {
            lastClickTime = now
            action()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = { Text("Libros") },
                navigationIcon = {
                    IconButton(onClick = {
                        runSingleNav {
                            SoundManager.playTap()
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    SoundManager.playTap()
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, "Nuevo Libro")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (books.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay libros. ¡Crea uno!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    runSingleNav {
                                        SoundManager.playTap()
                                        onBookClick(book.id)
                                    }
                                },
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.cardColors(
                                containerColor = if (book.color == 0L) MaterialTheme.colorScheme.surfaceVariant else Color(book.color)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = book.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    SoundManager.playDelete()
                                    onDeleteBook(book.id)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Nuevo Libro") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newBookName,
                            onValueChange = { newBookName = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Color", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NoteColors.take(5).forEach { colorValue ->
                                val isSelected = newBookColor == colorValue
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (colorValue == 0L) MaterialTheme.colorScheme.surfaceVariant else Color(colorValue))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else GlassBorder,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            SoundManager.playTap()
                                            newBookColor = colorValue
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newBookName.isNotBlank()) {
                            SoundManager.playTap()
                            onAddBook(newBookName.trim(), newBookColor)
                            showAddDialog = false
                            newBookName = ""
                            newBookColor = NoteColors[0]
                        }
                    }) {
                        Text("Crear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        SoundManager.playTap()
                        showAddDialog = false
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
