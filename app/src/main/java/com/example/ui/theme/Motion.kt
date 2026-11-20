package com.example.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Provides whether the user has reduced motion enabled at the system level
 * (ANIMATOR_DURATION_SCALE == 0f). All meaningful UI animations should consult
 * this value and snap immediately when true.
 */
val LocalReducedMotionEnabled = compositionLocalOf { false }

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

object RedeusMotion {
    const val DurationShort = 150
    const val DurationMedium = 250
    const val DurationLong = 350

    fun <T> spec(
        reducedMotion: Boolean,
        durationMillis: Int = DurationMedium
    ): FiniteAnimationSpec<T> = if (reducedMotion) snap() else tween(durationMillis = durationMillis)
}
