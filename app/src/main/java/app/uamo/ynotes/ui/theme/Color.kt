package app.uamo.ynotes.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// 2026 Core Palette - Warm, Alive & Editorial
val PrimaryAccent = Color(0xFF5B45E0) // Warm Brand Indigo (Creative & Human)
val PrimaryAccentDark = Color(0xFF816EF7) // Luminous Electric Lavender
val SecondaryAccent = Color(0xFFE06345) // Warm Terracotta / Coral
val SecondaryAccentDark = Color(0xFFF87171) // Luminous Soft Coral
val TertiaryAccent = Color(0xFFF59E0B) // Solar Warm Amber

// Warm Paper (Light) & Midnight Charcoal (Dark) Surfaces
val WarmLinenLight = Color(0xFFFBFBFA) // Cream linen background
val WarmSurfaceLight = Color(0xFFFFFFFF)
val WarmSurfaceHighlightLight = Color(0xFFF3F2EE) // Soft parchment
val TextPrimaryLight = Color(0xFF1A1A1E) // Humanistic deep graphite
val TextSecondaryLight = Color(0xFF64666E) // Neutral slate

val MidnightCharcoalDark = Color(0xFF0E1013) // Abyssal charcoal
val CharcoalSurfaceDark = Color(0xFF17191E) // Layered dark surface
val CharcoalSurfaceHighlightDark = Color(0xFF21242C) // Elevated dark surface
val TextPrimaryDark = Color(0xFFF4F5F7) // Soft ice gray
val TextSecondaryDark = Color(0xFF9CA3AF) // Fog gray

// Backwards-compatible aliases
val AmoledBlack = MidnightCharcoalDark
val AmoledDeepBlack = Color(0xFF000000)
val GlassSurface = CharcoalSurfaceDark
val GlassSurfaceHighlight = CharcoalSurfaceHighlightDark
val GlassBorder = Color(0x22FFFFFF)
val TextPrimary = TextPrimaryDark
val TextSecondary = TextSecondaryDark

// Aurora Gradients for elements
val AuroraPrimary = Brush.linearGradient(listOf(Color(0xFF5B45E0), Color(0xFF816EF7), Color(0xFFE06345)))
val AuroraSecondary = Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF5B45E0)))
val AuroraAmber = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFE06345)))
val AuroraEmerald = Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF0284C7)))

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

// Modern Aesthetic Note Color Codes (0L = Default)
val NoteColors = listOf(
    0L,          // 0: Default (Obsidian Slate)
    0xFF7C3AED,  // 1: Amethyst Violet
    0xFF0284C7,  // 2: Cyber Sky
    0xFF059669,  // 3: Emerald Jade
    0xFFD97706,  // 4: Golden Amber
    0xFFE11D48,  // 5: Rose Ruby
    0xFF2563EB,  // 6: Royal Cobalt
    0xFF78350F,  // 7: Warm Espresso
    0xFFC026D3,  // 8: Neon Fuchsia
    0xFF65A30D,  // 9: Lime Kiwi
    0xFF4F46E5,  // 10: Deep Indigo
    0xFF475569   // 11: Steel Slate
)

/**
 * Returns a rich, tailored Brush gradient for a given note color code,
 * ensuring high contrast and modern aesthetics in both Dark and Light modes.
 */
fun getNoteCardBrush(colorValue: Long, isDark: Boolean): Brush {
    if (isDark) {
        return when (colorValue) {
            0L -> Brush.linearGradient(listOf(Color(0xFF181A20), Color(0xFF131418)))
            0xFF7C3AED -> Brush.linearGradient(listOf(Color(0xFF2B1C44), Color(0xFF1E1332)))
            0xFF0284C7 -> Brush.linearGradient(listOf(Color(0xFF132B3B), Color(0xFF0E1F2C)))
            0xFF059669 -> Brush.linearGradient(listOf(Color(0xFF133224), Color(0xFF0E241A)))
            0xFFD97706 -> Brush.linearGradient(listOf(Color(0xFF382512), Color(0xFF2A1B0D)))
            0xFFE11D48 -> Brush.linearGradient(listOf(Color(0xFF391421), Color(0xFF2A0E18)))
            0xFF2563EB -> Brush.linearGradient(listOf(Color(0xFF162342), Color(0xFF101930)))
            0xFF78350F -> Brush.linearGradient(listOf(Color(0xFF2D1E16), Color(0xFF20150F)))
            0xFFC026D3 -> Brush.linearGradient(listOf(Color(0xFF351633), Color(0xFF260F25)))
            0xFF65A30D -> Brush.linearGradient(listOf(Color(0xFF232D12), Color(0xFF19200C)))
            0xFF4F46E5 -> Brush.linearGradient(listOf(Color(0xFF1F1C42), Color(0xFF161330)))
            0xFF475569 -> Brush.linearGradient(listOf(Color(0xFF21252D), Color(0xFF181B21)))
            else -> Brush.linearGradient(listOf(Color(colorValue).copy(alpha = 0.45f), Color(colorValue).copy(alpha = 0.25f)))
        }
    } else {
        return when (colorValue) {
            0L -> Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFAF9F6)))
            0xFF7C3AED -> Brush.linearGradient(listOf(Color(0xFFF7F3FF), Color(0xFFEDE5FE)))
            0xFF0284C7 -> Brush.linearGradient(listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE)))
            0xFF059669 -> Brush.linearGradient(listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5)))
            0xFFD97706 -> Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)))
            0xFFE11D48 -> Brush.linearGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6)))
            0xFF2563EB -> Brush.linearGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)))
            0xFF78350F -> Brush.linearGradient(listOf(Color(0xFFFAF5F0), Color(0xFFF3EBE1)))
            0xFFC026D3 -> Brush.linearGradient(listOf(Color(0xFFFDF4FF), Color(0xFFFAE8FF)))
            0xFF65A30D -> Brush.linearGradient(listOf(Color(0xFFF7FEE7), Color(0xFFECFCCB)))
            0xFF4F46E5 -> Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF)))
            0xFF475569 -> Brush.linearGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
            else -> Brush.linearGradient(listOf(Color(colorValue).copy(alpha = 0.15f), Color(colorValue).copy(alpha = 0.08f)))
        }
    }
}

/**
 * Returns a subtle, elegant border color for cards matching their tone.
 */
fun getNoteCardBorderColor(colorValue: Long, isDark: Boolean): Color {
    return if (isDark) {
        if (colorValue == 0L) Color(0x18FFFFFF)
        else Color(colorValue).copy(alpha = 0.35f)
    } else {
        if (colorValue == 0L) Color(0x0F000000)
        else Color(colorValue).copy(alpha = 0.22f)
    }
}
