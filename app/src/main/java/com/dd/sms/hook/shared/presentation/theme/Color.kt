package com.dd.sms.hook.shared.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** The palette: Material 3 tonal roles from the brand blue. Screens read them through MaterialTheme. */
internal object Palette {
    // Light roles
    val primaryLight: Color = Color(0xFF2F5BC4)
    val primaryContainerLight: Color = Color(0xFFDAE2FF)
    val onPrimaryContainerLight: Color = Color(0xFF001848)
    val secondaryLight: Color = Color(0xFF575E71)
    val secondaryContainerLight: Color = Color(0xFFDCE2F9)
    val onSecondaryContainerLight: Color = Color(0xFF141B2C)
    val tertiaryLight: Color = Color(0xFF006A62)
    val tertiaryContainerLight: Color = Color(0xFF9EF2E5)
    val onTertiaryContainerLight: Color = Color(0xFF00201D)
    val errorLight: Color = Color(0xFFBA1A1A)
    val errorContainerLight: Color = Color(0xFFFFDAD6)
    val onErrorContainerLight: Color = Color(0xFF410002)
    val surfaceLight: Color = Color(0xFFFAF9FF)
    val onSurfaceLight: Color = Color(0xFF1A1B21)
    val surfaceVariantLight: Color = Color(0xFFE1E2EC)
    val onSurfaceVariantLight: Color = Color(0xFF44464F)
    val outlineLight: Color = Color(0xFF757780)
    val outlineVariantLight: Color = Color(0xFFC5C6D0)
    val containerLowestLight: Color = Color(0xFFFFFFFF)
    val containerLowLight: Color = Color(0xFFF4F3FA)
    val containerLight: Color = Color(0xFFEEEDF4)
    val containerHighLight: Color = Color(0xFFE8E7EF)
    val containerHighestLight: Color = Color(0xFFE3E2E9)
    val inverseSurfaceLight: Color = Color(0xFF2F3036)
    val inverseOnSurfaceLight: Color = Color(0xFFF1F0F7)

    // Dark roles
    val primaryDark: Color = Color(0xFFB2C5FF)
    val onPrimaryDark: Color = Color(0xFF002B73)
    val primaryContainerDark: Color = Color(0xFF17418F)
    val onPrimaryContainerDark: Color = Color(0xFFDAE2FF)
    val secondaryDark: Color = Color(0xFFC0C6DC)
    val onSecondaryDark: Color = Color(0xFF2A3042)
    val secondaryContainerDark: Color = Color(0xFF404659)
    val onSecondaryContainerDark: Color = Color(0xFFDCE2F9)
    val tertiaryDark: Color = Color(0xFF82D5C9)
    val onTertiaryDark: Color = Color(0xFF003733)
    val tertiaryContainerDark: Color = Color(0xFF005049)
    val onTertiaryContainerDark: Color = Color(0xFF9EF2E5)
    val errorDark: Color = Color(0xFFFFB4AB)
    val onErrorDark: Color = Color(0xFF690005)
    val errorContainerDark: Color = Color(0xFF93000A)
    val onErrorContainerDark: Color = Color(0xFFFFDAD6)
    val surfaceDark: Color = Color(0xFF121318)
    val onSurfaceDark: Color = Color(0xFFE3E2E9)
    val surfaceVariantDark: Color = Color(0xFF44464F)
    val onSurfaceVariantDark: Color = Color(0xFFC5C6D0)
    val outlineDark: Color = Color(0xFF8F9099)
    val outlineVariantDark: Color = Color(0xFF44464F)
    val containerLowestDark: Color = Color(0xFF0D0E13)
    val containerLowDark: Color = Color(0xFF1A1B21)
    val containerDark: Color = Color(0xFF1E1F25)
    val containerHighDark: Color = Color(0xFF292A2F)
    val containerHighestDark: Color = Color(0xFF34343A)

    // Success/failure pair validated for colour-vision deficiency on both surfaces (teal vs orange, not green vs red).
    val teal: Color = Color(0xFF00897B)
    val tealDark: Color = Color(0xFF26A69A)
    val orange: Color = Color(0xFFE8710A)
    val orangeDark: Color = Color(0xFFD2691E)
}

internal val LightColors: ColorScheme = lightColorScheme(
    primary = Palette.primaryLight,
    onPrimary = Color.White,
    primaryContainer = Palette.primaryContainerLight,
    onPrimaryContainer = Palette.onPrimaryContainerLight,
    secondary = Palette.secondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Palette.secondaryContainerLight,
    onSecondaryContainer = Palette.onSecondaryContainerLight,
    tertiary = Palette.tertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Palette.tertiaryContainerLight,
    onTertiaryContainer = Palette.onTertiaryContainerLight,
    error = Palette.errorLight,
    onError = Color.White,
    errorContainer = Palette.errorContainerLight,
    onErrorContainer = Palette.onErrorContainerLight,
    background = Palette.surfaceLight,
    onBackground = Palette.onSurfaceLight,
    surface = Palette.surfaceLight,
    onSurface = Palette.onSurfaceLight,
    surfaceVariant = Palette.surfaceVariantLight,
    onSurfaceVariant = Palette.onSurfaceVariantLight,
    outline = Palette.outlineLight,
    outlineVariant = Palette.outlineVariantLight,
    inverseSurface = Palette.inverseSurfaceLight,
    inverseOnSurface = Palette.inverseOnSurfaceLight,
    inversePrimary = Palette.primaryDark,
    surfaceContainerLowest = Palette.containerLowestLight,
    surfaceContainerLow = Palette.containerLowLight,
    surfaceContainer = Palette.containerLight,
    surfaceContainerHigh = Palette.containerHighLight,
    surfaceContainerHighest = Palette.containerHighestLight,
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Palette.primaryDark,
    onPrimary = Palette.onPrimaryDark,
    primaryContainer = Palette.primaryContainerDark,
    onPrimaryContainer = Palette.onPrimaryContainerDark,
    secondary = Palette.secondaryDark,
    onSecondary = Palette.onSecondaryDark,
    secondaryContainer = Palette.secondaryContainerDark,
    onSecondaryContainer = Palette.onSecondaryContainerDark,
    tertiary = Palette.tertiaryDark,
    onTertiary = Palette.onTertiaryDark,
    tertiaryContainer = Palette.tertiaryContainerDark,
    onTertiaryContainer = Palette.onTertiaryContainerDark,
    error = Palette.errorDark,
    onError = Palette.onErrorDark,
    errorContainer = Palette.errorContainerDark,
    onErrorContainer = Palette.onErrorContainerDark,
    background = Palette.surfaceDark,
    onBackground = Palette.onSurfaceDark,
    surface = Palette.surfaceDark,
    onSurface = Palette.onSurfaceDark,
    surfaceVariant = Palette.surfaceVariantDark,
    onSurfaceVariant = Palette.onSurfaceVariantDark,
    outline = Palette.outlineDark,
    outlineVariant = Palette.outlineVariantDark,
    inverseSurface = Palette.onSurfaceDark,
    inverseOnSurface = Palette.inverseSurfaceLight,
    inversePrimary = Palette.primaryLight,
    surfaceContainerLowest = Palette.containerLowestDark,
    surfaceContainerLow = Palette.containerLowDark,
    surfaceContainer = Palette.containerDark,
    surfaceContainerHigh = Palette.containerHighDark,
    surfaceContainerHighest = Palette.containerHighestDark,
)

/** Semantic colours Material has no role for. */
data class StatusColors(val success: Color, val failure: Color)

internal val LightStatusColors: StatusColors = StatusColors(Palette.teal, Palette.orange)
internal val DarkStatusColors: StatusColors = StatusColors(Palette.tealDark, Palette.orangeDark)
