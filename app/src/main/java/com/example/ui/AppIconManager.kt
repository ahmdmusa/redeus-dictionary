package com.example.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.example.R

/**
 * User-selectable Sada launcher icons backed by Android <activity-alias> components.
 * Default is SONIC_FLIP (Concept C — Sada Teal + Coral).
 */
enum class SadaAppIcon(
    val id: String,
    val title: String,
    val subtitle: String,
    val aliasClassName: String,
    @param:DrawableRes val foregroundDrawableRes: Int
) {
    SONIC_FLIP(
        id = "sonic_flip",
        title = "Sonic Flip",
        subtitle = "Default · 3D Flip",
        aliasClassName = "com.example.LauncherSonicFlip",
        foregroundDrawableRes = R.drawable.ic_sada_fg_sonic_flip
    ),
    ECHO_DECK(
        id = "echo_deck",
        title = "Echo Deck",
        subtitle = "Stacked Cards",
        aliasClassName = "com.example.LauncherEchoDeck",
        foregroundDrawableRes = R.drawable.ic_sada_fg_echo_deck
    ),
    RESONANCE(
        id = "resonance",
        title = "Resonance",
        subtitle = "Editorial Folio",
        aliasClassName = "com.example.LauncherResonance",
        foregroundDrawableRes = R.drawable.ic_sada_fg_resonance
    ),
    WAVE_S(
        id = "wave_s",
        title = "Wave S",
        subtitle = "Monogram",
        aliasClassName = "com.example.LauncherWaveS",
        foregroundDrawableRes = R.drawable.ic_sada_fg_wave_s
    );

    companion object {
        fun fromId(id: String?): SadaAppIcon {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SONIC_FLIP
        }
    }
}

object AppIconManager {

    /**
     * Switches the active Android launcher <activity-alias> safely without killing the app.
     */
    fun applyLauncherIcon(context: Context, icon: SadaAppIcon): Boolean {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            // Enable the newly selected alias first so the app always has an active launcher entry
            val targetComp = ComponentName(pkg, icon.aliasClassName)
            pm.setComponentEnabledSetting(
                targetComp,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            // Disable all other launcher aliases
            SadaAppIcon.entries
                .filter { it != icon }
                .forEach { other ->
                    pm.setComponentEnabledSetting(
                        ComponentName(pkg, other.aliasClassName),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            true
        } catch (_: Throwable) {
            false
        }
    }
}
