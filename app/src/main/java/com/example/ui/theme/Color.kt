package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// Material Design 3 Tonal Palette â€” Seed: Scholarly Indigo (#3B5BA9)
// Tertiary Accent: Warm Ochre/Amber (#B36B00 light / #F2B84B dark)
// =============================================================================

// Light Scheme Tokens (Apple HIG Paper & Sapphire — High Contrast)
val IndigoLightPrimary = Color(0xFF007AFF)
val IndigoLightOnPrimary = Color(0xFFFFFFFF)
val IndigoLightPrimaryContainer = Color(0xFFE5F1FF)
val IndigoLightOnPrimaryContainer = Color(0xFF003A7A)

val IndigoLightSecondary = Color(0xFF5856D6)
val IndigoLightOnSecondary = Color(0xFFFFFFFF)
val IndigoLightSecondaryContainer = Color(0xFFECEBFF)
val IndigoLightOnSecondaryContainer = Color(0xFF282678)

val IndigoLightTertiary = Color(0xFFC97A00)
val IndigoLightOnTertiary = Color(0xFFFFFFFF)
val IndigoLightTertiaryContainer = Color(0xFFFFF1D6)
val IndigoLightOnTertiaryContainer = Color(0xFF4A2A00)

val IndigoLightError = Color(0xFFD70015)
val IndigoLightOnError = Color(0xFFFFFFFF)
val IndigoLightErrorContainer = Color(0xFFFFE5E7)
val IndigoLightOnErrorContainer = Color(0xFF690005)

val IndigoLightSurface = Color(0xFFF2F2F7)
val IndigoLightSurfaceDim = Color(0xFFE5E5EA)
val IndigoLightSurfaceBright = Color(0xFFFFFFFF)
val IndigoLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val IndigoLightSurfaceContainerLow = Color(0xFFFFFFFF)
val IndigoLightSurfaceContainer = Color(0xFFF2F2F7)
val IndigoLightSurfaceContainerHigh = Color(0xFFE5E5EA)
val IndigoLightSurfaceContainerHighest = Color(0xFFD1D1D6)
val IndigoLightOnSurface = Color(0xFF1C1C1E)
val IndigoLightOnSurfaceVariant = Color(0xFF636366)
val IndigoLightOutline = Color(0xFF8E8E93)
val IndigoLightOutlineVariant = Color(0xFFD1D1D6)
val IndigoLightInverseSurface = Color(0xFF1C1C1E)
val IndigoLightInverseOnSurface = Color(0xFFF2F2F7)
val IndigoLightInversePrimary = Color(0xFF0A84FF)
val IndigoLightScrim = Color(0xFF000000)

// Dark Scheme Tokens (Apple HIG True OLED Base #000000 + Elevated #1C1C1E)
val IndigoDarkPrimary = Color(0xFF0A84FF)
val IndigoDarkOnPrimary = Color(0xFFFFFFFF)
val IndigoDarkPrimaryContainer = Color(0xFF102A4A)
val IndigoDarkOnPrimaryContainer = Color(0xFF8CC5FF)

val IndigoDarkSecondary = Color(0xFF7D7AFF)
val IndigoDarkOnSecondary = Color(0xFFFFFFFF)
val IndigoDarkSecondaryContainer = Color(0xFF252538)
val IndigoDarkOnSecondaryContainer = Color(0xFFD7D6FF)

val IndigoDarkTertiary = Color(0xFFFF9F0A)
val IndigoDarkOnTertiary = Color(0xFF1C1200)
val IndigoDarkTertiaryContainer = Color(0xFF3A280A)
val IndigoDarkOnTertiaryContainer = Color(0xFFFFD89E)

val IndigoDarkError = Color(0xFFFF453A)
val IndigoDarkOnError = Color(0xFFFFFFFF)
val IndigoDarkErrorContainer = Color(0xFF3D1416)
val IndigoDarkOnErrorContainer = Color(0xFFFFB4AE)

val IndigoDarkSurface = Color(0xFF000000)
val IndigoDarkSurfaceDim = Color(0xFF000000)
val IndigoDarkSurfaceBright = Color(0xFF2C2C2E)
val IndigoDarkSurfaceContainerLowest = Color(0xFF000000)
val IndigoDarkSurfaceContainerLow = Color(0xFF1C1C1E)
val IndigoDarkSurfaceContainer = Color(0xFF242426)
val IndigoDarkSurfaceContainerHigh = Color(0xFF2C2C2E)
val IndigoDarkSurfaceContainerHighest = Color(0xFF3A3A3C)
val IndigoDarkOnSurface = Color(0xFFF5F5F7)
val IndigoDarkOnSurfaceVariant = Color(0xFF98989D)
val IndigoDarkOutline = Color(0xFF636366)
val IndigoDarkOutlineVariant = Color(0xFF38383A)
val IndigoDarkInverseSurface = Color(0xFFF5F5F7)
val IndigoDarkInverseOnSurface = Color(0xFF1C1C1E)
val IndigoDarkInversePrimary = Color(0xFF007AFF)
val IndigoDarkScrim = Color(0xFF000000)

val RedeusLightColorScheme: ColorScheme = lightColorScheme(
    primary = IndigoLightPrimary,
    onPrimary = IndigoLightOnPrimary,
    primaryContainer = IndigoLightPrimaryContainer,
    onPrimaryContainer = IndigoLightOnPrimaryContainer,
    secondary = IndigoLightSecondary,
    onSecondary = IndigoLightOnSecondary,
    secondaryContainer = IndigoLightSecondaryContainer,
    onSecondaryContainer = IndigoLightOnSecondaryContainer,
    tertiary = IndigoLightTertiary,
    onTertiary = IndigoLightOnTertiary,
    tertiaryContainer = IndigoLightTertiaryContainer,
    onTertiaryContainer = IndigoLightOnTertiaryContainer,
    error = IndigoLightError,
    onError = IndigoLightOnError,
    errorContainer = IndigoLightErrorContainer,
    onErrorContainer = IndigoLightOnErrorContainer,
    background = IndigoLightSurface,
    onBackground = IndigoLightOnSurface,
    surface = IndigoLightSurface,
    onSurface = IndigoLightOnSurface,
    surfaceVariant = IndigoLightSurfaceContainerHighest,
    onSurfaceVariant = IndigoLightOnSurfaceVariant,
    surfaceDim = IndigoLightSurfaceDim,
    surfaceBright = IndigoLightSurfaceBright,
    surfaceContainerLowest = IndigoLightSurfaceContainerLowest,
    surfaceContainerLow = IndigoLightSurfaceContainerLow,
    surfaceContainer = IndigoLightSurfaceContainer,
    surfaceContainerHigh = IndigoLightSurfaceContainerHigh,
    surfaceContainerHighest = IndigoLightSurfaceContainerHighest,
    outline = IndigoLightOutline,
    outlineVariant = IndigoLightOutlineVariant,
    inverseSurface = IndigoLightInverseSurface,
    inverseOnSurface = IndigoLightInverseOnSurface,
    inversePrimary = IndigoLightInversePrimary,
    scrim = IndigoLightScrim
)

val RedeusDarkColorScheme: ColorScheme = darkColorScheme(
    primary = IndigoDarkPrimary,
    onPrimary = IndigoDarkOnPrimary,
    primaryContainer = IndigoDarkPrimaryContainer,
    onPrimaryContainer = IndigoDarkOnPrimaryContainer,
    secondary = IndigoDarkSecondary,
    onSecondary = IndigoDarkOnSecondary,
    secondaryContainer = IndigoDarkSecondaryContainer,
    onSecondaryContainer = IndigoDarkOnSecondaryContainer,
    tertiary = IndigoDarkTertiary,
    onTertiary = IndigoDarkOnTertiary,
    tertiaryContainer = IndigoDarkTertiaryContainer,
    onTertiaryContainer = IndigoDarkOnTertiaryContainer,
    error = IndigoDarkError,
    onError = IndigoDarkOnError,
    errorContainer = IndigoDarkErrorContainer,
    onErrorContainer = IndigoDarkOnErrorContainer,
    background = IndigoDarkSurface,
    onBackground = IndigoDarkOnSurface,
    surface = IndigoDarkSurface,
    onSurface = IndigoDarkOnSurface,
    surfaceVariant = IndigoDarkSurfaceContainerHighest,
    onSurfaceVariant = IndigoDarkOnSurfaceVariant,
    surfaceDim = IndigoDarkSurfaceDim,
    surfaceBright = IndigoDarkSurfaceBright,
    surfaceContainerLowest = IndigoDarkSurfaceContainerLowest,
    surfaceContainerLow = IndigoDarkSurfaceContainerLow,
    surfaceContainer = IndigoDarkSurfaceContainer,
    surfaceContainerHigh = IndigoDarkSurfaceContainerHigh,
    surfaceContainerHighest = IndigoDarkSurfaceContainerHighest,
    outline = IndigoDarkOutline,
    outlineVariant = IndigoDarkOutlineVariant,
    inverseSurface = IndigoDarkInverseSurface,
    inverseOnSurface = IndigoDarkInverseOnSurface,
    inversePrimary = IndigoDarkInversePrimary,
    scrim = IndigoDarkScrim
)

// =============================================================================
// Theme Preset Palettes (Light + Dark M3 mappings for user themeColor setting)
// =============================================================================

internal fun presetColorScheme(themeColor: String, darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) RedeusDarkColorScheme else RedeusLightColorScheme
    return when (themeColor.lowercase()) {
        "blue" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFF9FCAFF),
                onPrimary = Color(0xFF003258),
                primaryContainer = Color(0xFF00497D),
                onPrimaryContainer = Color(0xFFD1E4FF)
            )
        } else {
            base.copy(
                primary = Color(0xFF0061A4),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD1E4FF),
                onPrimaryContainer = Color(0xFF001D36)
            )
        }
        "green" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFF8CD6B1),
                onPrimary = Color(0xFF003824),
                primaryContainer = Color(0xFF005136),
                onPrimaryContainer = Color(0xFFA8F2CC)
            )
        } else {
            base.copy(
                primary = Color(0xFF1D6B4A),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFA8F2CC),
                onPrimaryContainer = Color(0xFF002113)
            )
        }
        "orange" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFFFFB68D),
                onPrimary = Color(0xFF532200),
                primaryContainer = Color(0xFF763300),
                onPrimaryContainer = Color(0xFFFFDBC8)
            )
        } else {
            base.copy(
                primary = Color(0xFF9A4500),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDBC8),
                onPrimaryContainer = Color(0xFF321200)
            )
        }
        "pink" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFFFFB1C4),
                onPrimary = Color(0xFF650028),
                primaryContainer = Color(0xFF8E0A3D),
                onPrimaryContainer = Color(0xFFFFD9E1)
            )
        } else {
            base.copy(
                primary = Color(0xFFB32656),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFD9E1),
                onPrimaryContainer = Color(0xFF3F0016)
            )
        }
        "teal" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFF80D5CE),
                onPrimary = Color(0xFF003734),
                primaryContainer = Color(0xFF00504C),
                onPrimaryContainer = Color(0xFF9DF2EA)
            )
        } else {
            base.copy(
                primary = Color(0xFF006A65),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFF9DF2EA),
                onPrimaryContainer = Color(0xFF00201E)
            )
        }
        "red" -> if (darkTheme) {
            base.copy(
                primary = Color(0xFFFFB4AB),
                onPrimary = Color(0xFF690005),
                primaryContainer = Color(0xFF93000A),
                onPrimaryContainer = Color(0xFFFFDAD6)
            )
        } else {
            base.copy(
                primary = Color(0xFFB3261E),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFDAD6),
                onPrimaryContainer = Color(0xFF410002)
            )
        }
        else -> base // Default: Scholarly Indigo (#3B5BA9) + Warm Ochre tertiary
    }
}

