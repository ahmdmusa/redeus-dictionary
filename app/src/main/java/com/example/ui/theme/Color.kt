package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// =============================================================================
// Material Design 3 Tonal Palette — Seed: Scholarly Indigo (#3B5BA9)
// Tertiary Accent: Warm Ochre/Amber (#B36B00 light / #F2B84B dark)
// =============================================================================

// Light Scheme Tokens (Scholarly Indigo + Warm Ochre)
val IndigoLightPrimary = Color(0xFF3B5BA9)
val IndigoLightOnPrimary = Color(0xFFFFFFFF)
val IndigoLightPrimaryContainer = Color(0xFFDAE2FF)
val IndigoLightOnPrimaryContainer = Color(0xFF001848)

val IndigoLightSecondary = Color(0xFF575E71)
val IndigoLightOnSecondary = Color(0xFFFFFFFF)
val IndigoLightSecondaryContainer = Color(0xFFDCE2F9)
val IndigoLightOnSecondaryContainer = Color(0xFF141B2C)

val IndigoLightTertiary = Color(0xFF8B5000)
val IndigoLightOnTertiary = Color(0xFFFFFFFF)
val IndigoLightTertiaryContainer = Color(0xFFFFDCBE)
val IndigoLightOnTertiaryContainer = Color(0xFF2D1600)

val IndigoLightError = Color(0xFFBA1A1A)
val IndigoLightOnError = Color(0xFFFFFFFF)
val IndigoLightErrorContainer = Color(0xFFFFDAD6)
val IndigoLightOnErrorContainer = Color(0xFF410002)

val IndigoLightSurface = Color(0xFFFAF8FF)
val IndigoLightSurfaceDim = Color(0xFFDAD9E0)
val IndigoLightSurfaceBright = Color(0xFFFAF8FF)
val IndigoLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val IndigoLightSurfaceContainerLow = Color(0xFFF4F3FA)
val IndigoLightSurfaceContainer = Color(0xFFEEEDF4)
val IndigoLightSurfaceContainerHigh = Color(0xFFE8E7EF)
val IndigoLightSurfaceContainerHighest = Color(0xFFE2E2E9)
val IndigoLightOnSurface = Color(0xFF1A1B20)
val IndigoLightOnSurfaceVariant = Color(0xFF45464F)
val IndigoLightOutline = Color(0xFF757780)
val IndigoLightOutlineVariant = Color(0xFFC5C6D0)
val IndigoLightInverseSurface = Color(0xFF2F3036)
val IndigoLightInverseOnSurface = Color(0xFFF1F0F7)
val IndigoLightInversePrimary = Color(0xFFB2C5FF)
val IndigoLightScrim = Color(0xFF000000)

// Dark Scheme Tokens (Scholarly Indigo + Warm Ochre)
val IndigoDarkPrimary = Color(0xFFB2C5FF)
val IndigoDarkOnPrimary = Color(0xFF002B74)
val IndigoDarkPrimaryContainer = Color(0xFF21428F)
val IndigoDarkOnPrimaryContainer = Color(0xFFDAE2FF)

val IndigoDarkSecondary = Color(0xFFC0C6DC)
val IndigoDarkOnSecondary = Color(0xFF293042)
val IndigoDarkSecondaryContainer = Color(0xFF404659)
val IndigoDarkOnSecondaryContainer = Color(0xFFDCE2F9)

val IndigoDarkTertiary = Color(0xFFF2B84B)
val IndigoDarkOnTertiary = Color(0xFF452600)
val IndigoDarkTertiaryContainer = Color(0xFF653900)
val IndigoDarkOnTertiaryContainer = Color(0xFFFFDCBE)

val IndigoDarkError = Color(0xFFFFB4AB)
val IndigoDarkOnError = Color(0xFF690005)
val IndigoDarkErrorContainer = Color(0xFF93000A)
val IndigoDarkOnErrorContainer = Color(0xFFFFDAD6)

val IndigoDarkSurface = Color(0xFF121318)
val IndigoDarkSurfaceDim = Color(0xFF121318)
val IndigoDarkSurfaceBright = Color(0xFF38393F)
val IndigoDarkSurfaceContainerLowest = Color(0xFF0D0E13)
val IndigoDarkSurfaceContainerLow = Color(0xFF1A1B20)
val IndigoDarkSurfaceContainer = Color(0xFF1E1F25)
val IndigoDarkSurfaceContainerHigh = Color(0xFF292A2F)
val IndigoDarkSurfaceContainerHighest = Color(0xFF34343A)
val IndigoDarkOnSurface = Color(0xFFE2E2E9)
val IndigoDarkOnSurfaceVariant = Color(0xFFC5C6D0)
val IndigoDarkOutline = Color(0xFF8F909A)
val IndigoDarkOutlineVariant = Color(0xFF45464F)
val IndigoDarkInverseSurface = Color(0xFFE2E2E9)
val IndigoDarkInverseOnSurface = Color(0xFF2F3036)
val IndigoDarkInversePrimary = Color(0xFF3B5BA9)
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

// =============================================================================
// Deprecated Neo-Brutalist Compatibility Aliases
// Kept temporarily so un-migrated screens compile until retired in Phase 8.
// =============================================================================

@Deprecated("Use MaterialTheme.colorScheme.surfaceContainerLow")
val NeoCream = Color(0xFFF0EAE0)
@Deprecated("Use MaterialTheme.colorScheme.surfaceContainerLowest")
val NeoWhite = Color(0xFFFFFFFF)
@Deprecated("Use MaterialTheme.colorScheme.onSurface")
val NeoBlack = Color(0xFF000000)
@Deprecated("Use MaterialTheme.colorScheme.onSurfaceVariant")
val NeoNearBlack = Color(0xFF18181B)
@Deprecated("Use MaterialTheme.colorScheme.surface")
val NeoDarkBg = Color(0xFF18181B)
@Deprecated("Use MaterialTheme.colorScheme.surfaceContainer")
val NeoDarkSurface = Color(0xFF27272A)

@Deprecated("Use MaterialTheme.colorScheme.primary or tertiaryContainer")
val NeoYellow = Color(0xFFF2B705)
@Deprecated("Use MaterialTheme.colorScheme.tertiaryContainer")
val NeoYellowLight = Color(0xFFFDE68A)
@Deprecated("Use MaterialTheme.colorScheme.error")
val NeoRed = Color(0xFFD93B2B)
@Deprecated("Use MaterialTheme.colorScheme.errorContainer")
val NeoRedLight = Color(0xFFFCA5A5)

@Deprecated("Use MaterialTheme.colorScheme.secondaryContainer")
val NeoCyan = Color(0xFF38BDF8)
@Deprecated("Use MaterialTheme.colorScheme.primaryContainer")
val NeoGreen = Color(0xFF10B981)
@Deprecated("Use MaterialTheme.colorScheme.tertiaryContainer")
val NeoPurple = Color(0xFFA855F7)
@Deprecated("Use MaterialTheme.colorScheme.tertiaryContainer")
val NeoPink = Color(0xFFFF6584)
@Deprecated("Use MaterialTheme.colorScheme.tertiaryContainer")
val NeoOrange = Color(0xFFFB923C)
@Deprecated("Use MaterialTheme.colorScheme.surfaceContainerHigh")
val NeoGray = Color(0xFFE2E8F0)
@Deprecated("Use MaterialTheme.colorScheme.surfaceContainer")
val NeoMutedBg = Color(0xFFE5DECE)

@Deprecated("Use MaterialTheme.colorScheme.primary")
val BentoPrimary = Color(0xFF3B5BA9)
@Deprecated("Use MaterialTheme.colorScheme.primaryContainer")
val BentoPrimaryContainer = Color(0xFFDAE2FF)
@Deprecated("Use MaterialTheme.colorScheme.onPrimaryContainer")
val BentoOnPrimaryContainer = Color(0xFF001848)
@Deprecated("Use MaterialTheme.colorScheme.secondary")
val BentoSecondary = Color(0xFF575E71)
@Deprecated("Use MaterialTheme.colorScheme.secondaryContainer")
val BentoSecondaryContainer = Color(0xFFDCE2F9)
@Deprecated("Use MaterialTheme.colorScheme.onSecondaryContainer")
val BentoOnSecondaryContainer = Color(0xFF141B2C)
@Deprecated("Use MaterialTheme.colorScheme.surface")
val BentoLightBg = IndigoLightSurface
@Deprecated("Use MaterialTheme.colorScheme.surfaceContainerLowest")
val BentoLightSurface = IndigoLightSurfaceContainerLowest
@Deprecated("Use MaterialTheme.colorScheme.surface")
val BentoDarkBg = IndigoDarkSurface
@Deprecated("Use MaterialTheme.colorScheme.surfaceContainer")
val BentoDarkSurface = IndigoDarkSurfaceContainer
@Deprecated("Use MaterialTheme.colorScheme.outlineVariant")
val BentoBorderLight = IndigoLightOutlineVariant
@Deprecated("Use MaterialTheme.colorScheme.outline")
val BentoBorderAlt = IndigoLightOutline
@Deprecated("Use MaterialTheme.colorScheme.tertiaryContainer")
val BentoBadgeBg = IndigoLightTertiaryContainer
@Deprecated("Use MaterialTheme.colorScheme.tertiary")
val BentoHighlightGold = IndigoLightTertiary
@Deprecated("Use MaterialTheme.colorScheme.surfaceContainerHigh")
val BentoPlaceholderBg = IndigoDarkSurfaceContainerHigh
