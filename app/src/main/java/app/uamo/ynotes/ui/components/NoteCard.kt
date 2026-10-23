package app.uamo.ynotes.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.ui.theme.AppThemeType
import app.uamo.ynotes.ui.theme.AuroraPrimary
import app.uamo.ynotes.ui.theme.LocalAppTheme
import app.uamo.ynotes.utils.SoundManager
import app.uamo.ynotes.utils.parseMarkdownForPreview
import app.uamo.ynotes.utils.sharedElementTransition

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
    val cardColor = if (note.color == 0L) MaterialTheme.colorScheme.surfaceVariant else Color(note.color)

    var isPressed by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentNote by rememberUpdatedState(note)

    val borderStroke = when {
        isSelected -> BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
        currentTheme == AppThemeType.SAMSUNG -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }

    val isAmoledPinned = note.isPinned && !isSelected && currentTheme == AppThemeType.AMOLED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .sharedElementTransition("note-${note.id}")
            .scale(scale)
            .pointerInput(isSelectionMode) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        val now = SystemClock.elapsedRealtime()
                        if (isSelectionMode || now - lastClickTime > 400L) {
                            lastClickTime = now
                            SoundManager.playTap()
                            currentOnClick(currentNote)
                        }
                    },
                    onLongPress = {
                        SoundManager.playTap()
                        currentOnLongPress?.invoke(currentNote)
                    }
                )
            },
        shape = RoundedCornerShape(if (currentTheme == AppThemeType.GOOGLE) 16.dp else 28.dp),
        border = borderStroke,
        colors = CardDefaults.cardColors(
            containerColor = if (currentTheme == AppThemeType.GOOGLE && note.color == 0L) Color.Transparent else cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isAmoledPinned) Modifier.background(AuroraPrimary) else Modifier
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                if (note.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Fijada",
                        tint = if (isAmoledPinned) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(bottom = 4.dp)
                    )
                }
                if (note.title.isNotEmpty()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isAmoledPinned) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (note.body.isNotEmpty() && !note.isBodyHidden) {
                    if (note.title.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val bodyColor = if (isAmoledPinned) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                    val parsedBody = remember(note.body, bodyColor) {
                        parseMarkdownForPreview(note.body, bodyColor)
                    }
                    Text(
                        text = parsedBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                val hasMedia = remember(note.mediaFiles) {
                    note.mediaFiles.isNotBlank() && note.mediaFiles.split("|").any { it.isNotBlank() }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasMedia) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Tiene imágenes",
                            modifier = Modifier.size(16.dp),
                            tint = (if (isAmoledPinned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Icon(
                        imageVector = if (note.isSecret) Icons.Default.VpnKey else Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = (if (note.isSecret) MaterialTheme.colorScheme.error else if (isAmoledPinned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.5f)
                    )
                }
            }

            // Selection Checkmark Overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Nota seleccionada",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(24.dp)
                        .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                )
            }
        }
    }
}
