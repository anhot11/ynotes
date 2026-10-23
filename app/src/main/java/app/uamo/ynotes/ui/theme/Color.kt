package app.uamo.ynotes.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Palette (v1.0.3 AMOLED & Glass Edition)
val PrimaryAccent = Color(0xFF8B5CF6) // Vibrant Violet
val SecondaryAccent = Color(0xFF06B6D4) // Cyan
val TertiaryAccent = Color(0xFFEC4899) // Pink

// Dark / AMOLED Surfaces & Glassmorphism
val AmoledBlack = Color(0xFF000000)
val AmoledDeepBlack = Color(0xFF000000)
val GlassSurface = Color(0x15FFFFFF) // 15% White for translucent cards & pills
val GlassSurfaceHighlight = Color(0x25FFFFFF) // 25% White for highlighted surfaces
val GlassBorder = Color(0x33FFFFFF) // 20% White for subtle borders
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)

// Aurora Gradients for elements
val AuroraPrimary = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
val AuroraSecondary = Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)))

// Google Theme Colors
val GoogleLightBackground = Color(0xFFF8F9FA)
val GoogleLightSurface = Color(0xFFFFFFFF)
val GoogleDarkBackground = Color(0xFF202124)
val GoogleDarkSurface = Color(0xFF202124)
val GooglePrimary = Color(0xFF1A73E8)
val GoogleBorderLight = Color(0xFFE0E0E0)
val GoogleBorderDark = Color(0xFF5F6368)

// Samsung Theme Colors
val SamsungLightBackground = Color(0xFFF2F2F7)
val SamsungLightSurface = Color(0xFFFFFFFF)
val SamsungDarkBackground = Color(0xFF000000)
val SamsungDarkSurface = Color(0xFF1C1C1E)
val SamsungPrimary = Color(0xFFFF9F0A)

// Classic Dark Note Colors (0L = Default GlassSurface)
val NoteColors = listOf(
    0L,          // Default
    0xFF4A1C1C,  // Dark Red
    0xFF4A321C,  // Dark Orange
    0xFF4A471C,  // Dark Yellow
    0xFF1F4A1C,  // Dark Green
    0xFF1C4A4A,  // Dark Teal
    0xFF1C2D4A,  // Dark Blue
    0xFF351C4A,  // Dark Purple
    0xFF4A1C3A,  // Dark Pink
    0xFF33261E,  // Dark Brown
    0xFF2B2D2F   // Dark Gray
)

fun getNoteCardBrush(colorValue: Long, isDark: Boolean): Brush {
    val c = if (colorValue == 0L) {
        if (isDark) GlassSurface else Color.White
    } else {
        Color(colorValue)
    }
    return Brush.linearGradient(listOf(c, c))
}

fun getNoteCardBorderColor(colorValue: Long, isDark: Boolean): Color {
    return if (isDark) {
        if (colorValue == 0L) GlassBorder else Color(colorValue).copy(alpha = 0.5f)
    } else {
        if (colorValue == 0L) Color(0x1F000000) else Color(colorValue).copy(alpha = 0.3f)
    }
}
