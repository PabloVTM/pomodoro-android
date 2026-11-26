package com.pva.pomodoro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.pva.pomodoro.model.Phase

/** Palette PabloSystems : sauge profonde, ivoire chaud, vermillon vif. */
val DeepSage = Color(0xFF4B5A50)
val WarmIvory = Color(0xFFF1EEE3)
val Vermillion = Color(0xFFF20B0B)

private val InkLight = Color(0xFF2B332D)
private val InkDark = Color(0xFF111512)
private val SurfaceDark = Color(0xFF1A201C)
private val SurfaceVariantDark = Color(0xFF2A332D)
private val SageLight = Color(0xFFA9C0B2)
private val VermillionLight = Color(0xFFFF6B5E)

private val LightColors = lightColorScheme(
    primary = DeepSage,
    onPrimary = WarmIvory,
    primaryContainer = Color(0xFFDDE5DE),
    onPrimaryContainer = InkLight,
    secondary = Vermillion,
    onSecondary = WarmIvory,
    secondaryContainer = Color(0xFFDCE3DC),
    onSecondaryContainer = InkLight,
    tertiary = Color(0xFF33413A),
    onTertiary = WarmIvory,
    tertiaryContainer = Color(0xFFD3DED5),
    onTertiaryContainer = InkLight,
    background = WarmIvory,
    onBackground = InkLight,
    surface = Color(0xFFF7F4E9),
    onSurface = InkLight,
    surfaceVariant = Color(0xFFE3DFD1),
    onSurfaceVariant = Color(0xFF5D675F),
    surfaceDim = Color(0xFFDCD7C8),
    surfaceBright = Color(0xFFFDFAF0),
    surfaceContainerLowest = Color(0xFFFFFDF4),
    surfaceContainerLow = Color(0xFFF7F4E9),
    surfaceContainer = Color(0xFFF1EDE1),
    surfaceContainerHigh = Color(0xFFEBE7DA),
    surfaceContainerHighest = Color(0xFFE5E1D4),
    inverseSurface = Color(0xFF2B332D),
    inverseOnSurface = WarmIvory,
    inversePrimary = SageLight,
    outline = Color(0xFF8A948C),
    outlineVariant = Color(0xFFC9C3B5),
    error = Vermillion,
    onError = WarmIvory,
)

private val DarkColors = darkColorScheme(
    primary = SageLight,
    onPrimary = Color(0xFF101713),
    primaryContainer = DeepSage,
    onPrimaryContainer = WarmIvory,
    secondary = VermillionLight,
    onSecondary = InkDark,
    secondaryContainer = Color(0xFF33413A),
    onSecondaryContainer = Color(0xFFD6E4D9),
    tertiary = Color(0xFFC9DCCF),
    onTertiary = InkDark,
    tertiaryContainer = Color(0xFF3D4B42),
    onTertiaryContainer = WarmIvory,
    background = InkDark,
    onBackground = WarmIvory,
    surface = SurfaceDark,
    onSurface = WarmIvory,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB7C2BA),
    surfaceDim = InkDark,
    surfaceBright = Color(0xFF343E38),
    surfaceContainerLowest = Color(0xFF0C0F0D),
    surfaceContainerLow = SurfaceDark,
    surfaceContainer = Color(0xFF1F2621),
    surfaceContainerHigh = Color(0xFF2A332D),
    surfaceContainerHighest = Color(0xFF343E38),
    inverseSurface = WarmIvory,
    inverseOnSurface = InkDark,
    inversePrimary = DeepSage,
    outline = Color(0xFF5D6B61),
    outlineVariant = Color(0xFF3E4A42),
    error = VermillionLight,
    onError = InkDark,
)

private val Mono = FontFamily.Monospace

private val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Mono),
        displayMedium = base.displayMedium.copy(fontFamily = Mono),
        displaySmall = base.displaySmall.copy(fontFamily = Mono),
        headlineLarge = base.headlineLarge.copy(fontFamily = Mono),
        headlineMedium = base.headlineMedium.copy(fontFamily = Mono),
        headlineSmall = base.headlineSmall.copy(fontFamily = Mono),
        titleLarge = base.titleLarge.copy(fontFamily = Mono),
        titleMedium = base.titleMedium.copy(fontFamily = Mono),
        titleSmall = base.titleSmall.copy(fontFamily = Mono),
        bodyLarge = base.bodyLarge.copy(fontFamily = Mono),
        bodyMedium = base.bodyMedium.copy(fontFamily = Mono),
        bodySmall = base.bodySmall.copy(fontFamily = Mono),
        labelLarge = base.labelLarge.copy(fontFamily = Mono),
        labelMedium = base.labelMedium.copy(fontFamily = Mono),
        labelSmall = base.labelSmall.copy(fontFamily = Mono),
    )
}

@Composable
fun PomodoroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

/** Couleur d'accent associée à chaque phase. */
data class PhaseColors(val accent: Color, val onAccent: Color)

@Composable
fun phaseColors(phase: Phase): PhaseColors {
    val dark = isSystemInDarkTheme()
    return when (phase) {
        Phase.WORK -> PhaseColors(
            accent = if (dark) VermillionLight else Vermillion,
            onAccent = if (dark) InkDark else WarmIvory,
        )
        Phase.SHORT_BREAK -> PhaseColors(
            accent = if (dark) SageLight else DeepSage,
            onAccent = if (dark) InkDark else WarmIvory,
        )
        Phase.LONG_BREAK -> PhaseColors(
            accent = if (dark) Color(0xFFC9DCCF) else Color(0xFF33413A),
            onAccent = if (dark) InkDark else WarmIvory,
        )
    }
}