package app.purrhunt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PurrColors {
    val Night = Color(0xFF10151F)
    val NightSurface = Color(0xFF19212E)
    val NightSurfaceHigh = Color(0xFF232D3D)
    val Amber = Color(0xFFF4C95D)
    val AmberDeep = Color(0xFFB98A22)
    val Sky = Color(0xFF7FB2F0)
    val Moss = Color(0xFF8DBF8B)
    val Cream = Color(0xFFF3EBDD)
    val Muted = Color(0xFF9AA6B8)
    val Danger = Color(0xFFE57B6E)
    val Outline = Color(0xFF3A4658)
}

private val DarkScheme = darkColorScheme(
    primary = PurrColors.Amber,
    onPrimary = Color(0xFF2B1E00),
    primaryContainer = Color(0xFF4A3A10),
    onPrimaryContainer = PurrColors.Amber,
    secondary = PurrColors.Sky,
    onSecondary = Color(0xFF0B2340),
    secondaryContainer = Color(0xFF1F3554),
    onSecondaryContainer = Color(0xFFD3E4FF),
    tertiary = PurrColors.Moss,
    background = PurrColors.Night,
    onBackground = PurrColors.Cream,
    surface = PurrColors.Night,
    onSurface = PurrColors.Cream,
    surfaceVariant = PurrColors.NightSurfaceHigh,
    onSurfaceVariant = PurrColors.Muted,
    surfaceContainer = PurrColors.NightSurface,
    surfaceContainerHigh = PurrColors.NightSurfaceHigh,
    surfaceContainerLow = Color(0xFF151C27),
    error = PurrColors.Danger,
    outline = Color(0xFF3A4658),
)

private val PurrTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    )
}

/** The app is dark by default: it is played in a dim room and dark menus don't dazzle the cat. */
@Composable
fun PurrHuntTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = PurrTypography, content = content)
}
