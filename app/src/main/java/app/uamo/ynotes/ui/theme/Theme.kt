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
    primary = PrimaryAccentDark,
    secondary = SecondaryAccentDark,
    tertiary = TertiaryAccent,
    background = MidnightCharcoalDark,
    surface = CharcoalSurfaceDark,
    surfaceVariant = CharcoalSurfaceHighlightDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0x24FFFFFF),
    outlineVariant = Color(0x14FFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    tertiary = TertiaryAccent,
    background = WarmLinenLight,
    surface = WarmSurfaceLight,
    surfaceVariant = WarmSurfaceHighlightLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFE2E2DF),
    outlineVariant = Color(0xFFEEEEEC)
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
                        background = MidnightCharcoalDark,
                        surface = CharcoalSurfaceDark,
                        surfaceVariant = CharcoalSurfaceHighlightDark,
                        onSurface = TextPrimaryDark,
                        onSurfaceVariant = TextSecondaryDark,
                        outline = Color(0x24FFFFFF)
                    )
                } else {
                    dynamicLightColorScheme(context).copy(
                        background = WarmLinenLight,
                        surface = WarmSurfaceLight,
                        surfaceVariant = WarmSurfaceHighlightLight,
                        onSurface = TextPrimaryLight,
                        onSurfaceVariant = TextSecondaryLight
                    )
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
