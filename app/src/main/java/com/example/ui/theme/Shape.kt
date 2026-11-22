package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material Design 3 standard shape scale (4 / 8 / 12 / 16 / 28 dp).
 * - extraSmall (4dp): badges, linear progress indicators
 * - small (8dp): chips, text fields, compact tags
 * - medium (12dp): standard cards, list containers, dialogs
 * - large (16dp): prominent content cards, reading passages, flashcards
 * - extraLarge (28dp): search bars, modal sheets, expressive hero containers
 */
val RedeusShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
