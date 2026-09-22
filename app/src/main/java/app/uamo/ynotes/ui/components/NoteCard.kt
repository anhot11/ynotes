package app.uamo.ynotes.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.uamo.ynotes.data.NoteEntity
import app.uamo.ynotes.utils.MediaManager
import app.uamo.ynotes.utils.parseMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.uamo.ynotes.utils.sharedElementTransition

import app.uamo.ynotes.ui.theme.AuroraPrimary
import app.uamo.ynotes.ui.theme.GlassBorder
import app.uamo.ynotes.ui.theme.LocalAppTheme
import app.uamo.ynotes.ui.theme.AppThemeType

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: (NoteEntity) -> Unit,
    onLongPress: ((NoteEntity) -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalAppTheme.current
    val cardColor = if (note.color == 0L) MaterialTheme.colorScheme.surfaceVariant else Color(note.color)
    val context = LocalContext.current

    // Load first media thumbnail if available
    val mediaFileNames = remember(note.mediaFiles) {
        note.mediaFiles.split("|").filter { it.isNotBlank() }
    }
    var firstBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(note.id, mediaFileNames.firstOrNull()) {
        if (mediaFileNames.isNotEmpty()) {
            firstBitmap = withContext(Dispatchers.IO) {
                MediaManager.loadMediaBitmap(context, note.id, mediaFileNames.first(), note.isSecret, maxSize = 256)
            }
        } else {
            firstBitmap = null
        }
    }

    // Animated selection border color
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            currentTheme == AppThemeType.SAMSUNG -> Color.Transparent
            else -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(200),
        label = "border"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.5.dp else if (currentTheme == AppThemeType.SAMSUNG) 0.dp else 1.dp,
        animationSpec = tween(200),
        label = "borderWidth"
    )

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val baseModifier = modifier
        .fillMaxWidth()
        .sharedElementTransition("note-${note.id}")
        .scale(scale)
        .clip(RoundedCornerShape(if (currentTheme == AppThemeType.GOOGLE) 16.dp else 28.dp))
        .border(borderWidth, borderColor, RoundedCornerShape(if (currentTheme == AppThemeType.GOOGLE) 16.dp else 28.dp))
        .pointerInput(isSelectionMode) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    onClick(note)
                },
                onLongPress = {
                    onLongPress?.invoke(note)
                }
            )
        }

    val finalModifier = if (note.isPinned && !isSelected) {
        if (currentTheme == AppThemeType.AMOLED) {
            baseModifier.background(AuroraPrimary)
        } else {
            baseModifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        }
    } else if (isSelected) {
        baseModifier.background(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        )
    } else {
        baseModifier
    }

    Card(
        modifier = finalModifier,
        shape = RoundedCornerShape(if (currentTheme == AppThemeType.GOOGLE) 16.dp else 28.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color.Transparent
                note.isPinned && currentTheme == AppThemeType.AMOLED -> Color.Transparent
                else -> cardColor
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 8.dp)
    ) {
        Box {
            Column {
                // Image preview at top of card
                if (firstBitmap != null) {
                    Image(
                        bitmap = firstBitmap!!.asImageBitmap(),
                        contentDescription = "Imagen adjunta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (note.title.isNotEmpty()) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (note.isPinned && !isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        if (note.isPinned && !isSelected) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Fijado",
                                modifier = Modifier.size(16.dp).padding(start = 4.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (note.body.isNotEmpty() && !note.isBodyHidden) {
                        if (note.title.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                        val bodyTextColor = if (note.isPinned && !isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                        Text(
                            text = parseMarkdown(note.body, bodyTextColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = bodyTextColor,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (mediaFileNames.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Nota con imágenes adjuntas",
                                modifier = Modifier.size(14.dp),
                                tint = (if (note.isPinned && !isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Icon(
                            imageVector = if (note.isSecret) Icons.Default.VpnKey else Icons.Default.Description,
                            contentDescription = if (note.isSecret) "Nota secreta cifrada" else "Nota pública",
                            modifier = Modifier.size(16.dp),
                            tint = (if (note.isSecret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Selection checkmark overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(24.dp)
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
                // Empty circle indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(24.dp)
                        .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                )
            }
        }
    }
}
