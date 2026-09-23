package app.uamo.ynotes.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import app.uamo.ynotes.ui.theme.AppThemeType
import app.uamo.ynotes.ui.theme.LocalAppTheme
import app.uamo.ynotes.utils.MediaManager
import app.uamo.ynotes.utils.parseMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A specialized card that renders notes inside notebook zones as authentic lined paper sheets
 * with binder punch holes, red vertical margin, subtle ruled notebook lines, and paper texture.
 */
@Composable
fun NotebookSheetCard(
    note: NoteEntity,
    onClick: (NoteEntity) -> Unit,
    onLongPress: ((NoteEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalAppTheme.current
    val isDark = isSystemInDarkTheme() || currentTheme == AppThemeType.AMOLED
    val context = LocalContext.current

    // Media thumbnail loading
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

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "sheetScale"
    )

    // Paper styling
    val paperBgColor = if (isDark) Color(0xFF22211F) else Color(0xFFFDFBF7)
    val paperBorderColor = if (isDark) Color(0xFF383633) else Color(0xFFE8E4DC)
    val marginLineColor = if (isDark) Color(0xFFFF8A80).copy(alpha = 0.28f) else Color(0xFFE57373).copy(alpha = 0.45f)
    val ruledLineColor = if (isDark) Color(0xFFE2E8F0).copy(alpha = 0.07f) else Color(0xFF90CAF9).copy(alpha = 0.24f)
    val punchHoleColor = if (isDark) Color(0xFF161514) else Color(0xFFDDD8CE)

    val shape = RoundedCornerShape(
        topStart = 4.dp,
        bottomStart = 4.dp,
        topEnd = 16.dp,
        bottomEnd = 16.dp
    )

    // Formatted date string
    val formattedDate = remember(note.updatedAt) {
        val noteCal = Calendar.getInstance().apply { timeInMillis = note.updatedAt }
        val nowCal = Calendar.getInstance()
        when {
            noteCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            noteCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
            }
            noteCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("d MMM", Locale("es", "ES")).format(Date(note.updatedAt))
            }
            else -> {
                SimpleDateFormat("d MMM yyyy", Locale("es", "ES")).format(Date(note.updatedAt))
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isDark) 2.dp else 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .pointerInput(note.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick(note) },
                    onLongPress = { onLongPress?.invoke(note) }
                )
            },
        shape = shape,
        color = paperBgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, paperBorderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val marginX = 26.dp.toPx()
                    val lineSpacing = 22.dp.toPx()

                    // 1. Draw horizontal ruled notebook lines (renglones)
                    var y = 38.dp.toPx()
                    while (y < size.height - 10.dp.toPx()) {
                        drawLine(
                            color = ruledLineColor,
                            start = Offset(marginX, y),
                            end = Offset(size.width - 4.dp.toPx(), y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += lineSpacing
                    }

                    // 2. Draw classic red vertical margin line
                    drawLine(
                        color = marginLineColor,
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // 3. Draw 3 binder ring punch holes along the spine edge
                    val holeRadius = 3.5.dp.toPx()
                    val holeX = 11.dp.toPx()
                    val holePositions = listOf(0.18f, 0.50f, 0.82f)
                    holePositions.forEach { fraction ->
                        val holeY = size.height * fraction
                        drawCircle(
                            color = punchHoleColor,
                            radius = holeRadius,
                            center = Offset(holeX, holeY)
                        )
                    }
                }
                .padding(start = 32.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: Pin clip badge & date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.isPinned) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Fijada",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Fijada",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title (styled as heading on notebook sheet)
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 21.sp
                        ),
                        color = if (isDark) Color(0xFFEDEDED) else Color(0xFF1E293B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Media thumbnail (styled like a photo clipped/taped to the notebook page)
                if (firstBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, paperBorderColor, RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            bitmap = firstBitmap!!.asImageBitmap(),
                            contentDescription = "Imagen adjunta",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Body content with clean Markdown preview (no raw symbols)
                if (note.body.isNotBlank()) {
                    val bodyColor = if (isDark) Color(0xFFC7C5C0) else Color(0xFF475569)
                    Text(
                        text = parseMarkdown(note.body, bodyColor),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        ),
                        color = bodyColor,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
