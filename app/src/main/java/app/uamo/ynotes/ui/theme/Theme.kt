package app.uamo.ynotes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    tertiary = TertiaryAccent,
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceVariant = GlassSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = Color(0x1FFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    tertiary = TertiaryAccent,
    background = Color(0xFFF9FAFB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF3F4F6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB)
)

enum class AppThemeType {
    AMOLED,
    GOOGLE,
    SAMSUNG
}

val LocalAppTheme = compositionLocalOf { AppThemeType.AMOLED }

private val GoogleLightColorScheme = lightColorScheme(
    primary = GooglePrimary,
    background = GoogleLightBackground,
    surface = GoogleLightSurface,
    surfaceVariant = GoogleLightSurface,
    onSurface = Color.Black,
    onSurfaceVariant = Color.DarkGray,
    outline = GoogleBorderLight
)

private val GoogleDarkColorScheme = darkColorScheme(
    primary = GooglePrimary,
    background = GoogleDarkBackground,
    surface = GoogleDarkSurface,
    surfaceVariant = GoogleDarkSurface,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray,
    outline = GoogleBorderDark
)

private val SamsungLightColorScheme = lightColorScheme(
    primary = SamsungPrimary,
    background = SamsungLightBackground,
    surface = SamsungLightSurface,
    surfaceVariant = SamsungLightSurface,
    onSurface = Color.Black,
    onSurfaceVariant = Color.DarkGray,
    outline = Color.Transparent
)

private val SamsungDarkColorScheme = darkColorScheme(
    primary = SamsungPrimary,
    background = SamsungDarkBackground,
    surface = SamsungDarkSurface,
    surfaceVariant = SamsungDarkSurface,
    onSurface = Color.White,
    onSurfaceVariant = Color.LightGray,
    outline = Color.Transparent
)

@Composable
fun YNotesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeType: AppThemeType = AppThemeType.AMOLED,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        AppThemeType.GOOGLE -> if (darkTheme) GoogleDarkColorScheme else GoogleLightColorScheme
        AppThemeType.SAMSUNG -> if (darkTheme) SamsungDarkColorScheme else SamsungLightColorScheme
        AppThemeType.AMOLED -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) {
                    dynamicDarkColorScheme(context).copy(
                        background = AmoledBlack,
                        surface = AmoledBlack,
                        surfaceVariant = GlassSurface,
                        onSurface = TextPrimary,
                        onSurfaceVariant = TextSecondary,
                        outline = GlassBorder
                    )
                } else {
                    dynamicLightColorScheme(context)
                }
            } else if (darkTheme) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
    }

    CompositionLocalProvider(LocalAppTheme provides themeType) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
