package app.uamo.ynotes.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.ui.theme.*
import app.uamo.ynotes.utils.MediaManager
import app.uamo.ynotes.utils.parseMarkdown
import app.uamo.ynotes.utils.sharedElementTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: (NoteEntity) -> Unit,
    onLongPress: ((NoteEntity) -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    bookName: String? = null,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalAppTheme.current
    val isDark = isSystemInDarkTheme() || currentTheme == AppThemeType.AMOLED
    val context = LocalContext.current

    // Load first media thumbnail if available
    val mediaFileNames = remember(note.mediaFiles) {
        note.mediaFiles.split("|").filter { it.isNotBlank() }
    }
    var firstBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(note.id, mediaFileNames.firstOrNull()) {
        if (mediaFileNames.isNotEmpty()) {
            firstBitmap = withContext(Dispatchers.IO) {
                MediaManager.loadMediaBitmap(context, note.id, mediaFileNames.first(), note.isSecret, maxSize = 300)
            }
        } else {
            firstBitmap = null
        }
    }

    // Interactive scale spring on press
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardScale"
    )

    // Animated selection borders
    val targetBorderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        currentTheme == AppThemeType.SAMSUNG -> Color.Transparent
        else -> getNoteCardBorderColor(note.color, isDark)
    }
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(200),
        label = "borderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.5.dp else 1.dp,
        animationSpec = tween(200),
        label = "borderWidth"
    )

    val cardShape = RoundedCornerShape(20.dp)
    val cardBrush = remember(note.color, isDark) {
        getNoteCardBrush(note.color, isDark)
    }

    // Relative date calculation
    val formattedDate = remember(note.updatedAt) {
        formatRelativeDate(note.updatedAt)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .sharedElementTransition("note-${note.id}")
            .scale(scale)
            .clip(cardShape)
            .background(cardBrush)
            .border(borderWidth, borderColor, cardShape)
            .pointerInput(isSelectionMode) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick(note) },
                    onLongPress = { onLongPress?.invoke(note) }
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 🖼️ Image preview thumbnail if attached
            if (firstBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    Image(
                        bitmap = firstBitmap!!.asImageBitmap(),
                        contentDescription = "Imagen adjunta",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Photo counter badge overlay
                    if (mediaFileNames.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Photo,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${mediaFileNames.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 📝 Card Body & Details
            Column(modifier = Modifier.padding(16.dp)) {
                // Top Tag Row: Notebook, Pinned, and Widget indicators
                val hasTopBadges = bookName != null || note.isPinned || note.isWidgetSpecial
                if (hasTopBadges) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (bookName != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = bookName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (note.isWidgetSpecial) {
                                Icon(
                                    Icons.Default.Widgets,
                                    contentDescription = "En Widget",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                            if (note.isPinned) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = "Fijada",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Title
                if (note.title.isNotEmpty()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Body Markdown snippet
                if (note.body.isNotEmpty() && !note.isBodyHidden) {
                    if (note.title.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
                    Text(
                        text = parseMarkdown(note.body, bodyColor),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp
                        ),
                        color = bodyColor,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Meta Row: Date, Word count, Vault indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.isSecret) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = "Nota cifrada",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Selection Checkmark Overlay
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(26.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Nota seleccionada",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(26.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}

/**
 * Returns formatted relative date (e.g. "Hoy, 14:20", "Ayer", "15 sep").
 */
private fun formatRelativeDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 60_000L) return "Ahora"
    if (diff < 3600_000L) return "Hace ${diff / 60_000L} min"

    val calendarNow = Calendar.getInstance()
    val calendarNote = Calendar.getInstance().apply { timeInMillis = timestamp }

    val isToday = calendarNow.get(Calendar.YEAR) == calendarNote.get(Calendar.YEAR) &&
            calendarNow.get(Calendar.DAY_OF_YEAR) == calendarNote.get(Calendar.DAY_OF_YEAR)

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    if (isToday) return "Hoy, ${timeFormat.format(Date(timestamp))}"

    val isYesterday = calendarNow.get(Calendar.YEAR) == calendarNote.get(Calendar.YEAR) &&
            calendarNow.get(Calendar.DAY_OF_YEAR) - calendarNote.get(Calendar.DAY_OF_YEAR) == 1
    if (isYesterday) return "Ayer, ${timeFormat.format(Date(timestamp))}"

    val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}
