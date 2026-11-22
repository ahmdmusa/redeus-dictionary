package com.example.ui

import android.speech.tts.Voice

data class OfflineTtsVoiceOption(
    val name: String,
    val displayName: String,
    val localeTag: String,
    val country: String,
    val isLocal: Boolean,
    val isHighQuality: Boolean,
    val qualityScore: Int
)

object TtsVoiceSelector {

    private val preferredUkGoogleNeuralIds = listOf(
        "en-gb-x-gba-local",
        "en-gb-x-gbc-local",
        "en-gb-x-gbg-local",
        "en-gb-x-rjs-local",
        "en-gb-x-gbd-local",
        "en-gb-x-fis-local"
    )

    private val preferredUsGoogleNeuralIds = listOf(
        "en-us-x-tpf-local",
        "en-us-x-sfg-local",
        "en-us-x-iob-local",
        "en-us-x-iom-local",
        "en-us-x-tpd-local",
        "en-us-x-sfg-network"
    )

    fun scoreVoiceForAccent(
        name: String,
        localeLanguage: String,
        localeCountry: String,
        isNetworkRequired: Boolean,
        quality: Int,
        targetAccent: String
    ): Int {
        if (!localeLanguage.equals("en", ignoreCase = true)) return -1

        val lowerName = name.lowercase()
        val normalizedCountry = localeCountry.uppercase()
        val wantUk = targetAccent.equals("UK", ignoreCase = true)

        var score = 0

        // Strongly prioritize true offline local voices
        if (!isNetworkRequired) {
            score += 1000
        }

        // Accent matching
        if (wantUk && normalizedCountry == "GB") {
            score += 500
        } else if (!wantUk && normalizedCountry == "US") {
            score += 500
        } else if (normalizedCountry == "GB" || normalizedCountry == "US") {
            score += 140
        } else {
            score += 60
        }

        // Named premium / neural voices (e.g. Microsoft Sonia via MultiTTS, Samsung HD/Neural)
        if (lowerName.contains("sonia") ||
            lowerName.contains("neural") ||
            lowerName.contains("natural") ||
            lowerName.contains("studio")
        ) {
            score += 420
        } else if (lowerName.contains("hd") ||
            lowerName.contains("enhanced") ||
            lowerName.contains("premium") ||
            lowerName.contains("wavenet")
        ) {
            score += 280
        }

        // Google Speech Services Offline Neural local packs (UK & US)
        val ukIdx = preferredUkGoogleNeuralIds.indexOf(lowerName)
        if (ukIdx >= 0) {
            score += (360 - ukIdx * 12)
        }
        val usIdx = preferredUsGoogleNeuralIds.indexOf(lowerName)
        if (usIdx >= 0) {
            score += (360 - usIdx * 12)
        }

        // General local suffix bonus
        if (lowerName.endsWith("-local") || lowerName.contains("local")) {
            score += 150
        }

        // Android TTS quality tier
        when {
            quality >= Voice.QUALITY_VERY_HIGH -> score += 140
            quality >= Voice.QUALITY_HIGH -> score += 100
            quality >= Voice.QUALITY_NORMAL -> score += 40
        }

        return score
    }

    fun formatFriendlyVoiceName(
        name: String,
        localeCountry: String,
        isLocal: Boolean
    ): String {
        val lower = name.lowercase()
        val regionLabel = when (localeCountry.uppercase()) {
            "GB" -> "UK British"
            "US" -> "US American"
            "AU" -> "Australian"
            "CA" -> "Canadian"
            else -> "English"
        }
        val modeTag = if (isLocal) "Offline" else "Online"

        return when {
            lower.contains("sonia") -> "Sonia Neural ($regionLabel • $modeTag)"
            lower.startsWith("en-gb-x-") -> {
                val variant = lower.removePrefix("en-gb-x-").substringBefore("-").uppercase()
                val style = when (variant) {
                    "GBA" -> "Female 1"
                    "GBC" -> "Female 2"
                    "GBG" -> "Female 3"
                    "GBD" -> "Male 1"
                    "RJS" -> "Male 2"
                    "FIS" -> "Natural"
                    else -> variant
                }
                "Google UK Neural $style ($modeTag)"
            }
            lower.startsWith("en-us-x-") -> {
                val variant = lower.removePrefix("en-us-x-").substringBefore("-").uppercase()
                val style = when (variant) {
                    "TPF" -> "Female 1"
                    "SFG" -> "Female 2"
                    "IOB" -> "Female 3"
                    "IOM" -> "Male 1"
                    "TPD" -> "Male 2"
                    else -> variant
                }
                "Google US Neural $style ($modeTag)"
            }
            lower.contains("samsung") || lower.contains("smt") -> "Samsung $regionLabel ($modeTag)"
            else -> {
                val shortCode = name.takeLast(18)
                "$regionLabel ($shortCode • $modeTag)"
            }
        }
    }

    fun rankOfflineEnglishVoices(
        voices: Collection<Voice>?,
        targetAccent: String
    ): List<OfflineTtsVoiceOption> {
        if (voices.isNullOrEmpty()) return emptyList()
        return voices.mapNotNull { voice ->
            val locale = voice.locale ?: return@mapNotNull null
            val score = scoreVoiceForAccent(
                name = voice.name,
                localeLanguage = locale.language,
                localeCountry = locale.country,
                isNetworkRequired = voice.isNetworkConnectionRequired,
                quality = voice.quality,
                targetAccent = targetAccent
            )
            if (score < 0) return@mapNotNull null
            val isLocal = !voice.isNetworkConnectionRequired
            OfflineTtsVoiceOption(
                name = voice.name,
                displayName = formatFriendlyVoiceName(voice.name, locale.country, isLocal),
                localeTag = locale.toLanguageTag(),
                country = locale.country.uppercase(),
                isLocal = isLocal,
                isHighQuality = voice.quality >= Voice.QUALITY_HIGH || score >= 1450,
                qualityScore = score
            )
        }.sortedByDescending { it.qualityScore }
    }
}
