package com.example.ui

/**
 * Explicit pronunciation execution steps ordered by user priority:
 *   Default ("auto"): Saved Local File -> 1) Natural Offline Neural TTS -> 2) Online Dictionary Audio -> 3) AI TTS
 *   "device":         Saved Local File -> Natural Offline Neural TTS only
 *   "online":         Saved Local File -> Online Dictionary Audio -> AI TTS (never Device TTS)
 */
enum class PronunciationStep {
    SAVED_LOCAL_FILE,
    NATURAL_OFFLINE_TTS,
    ONLINE_DICTIONARY_AUDIO,
    AI_TTS
}

object PronunciationPriority {

    fun resolveWordPlaybackOrder(
        pronunciationSource: String,
        hasSavedLocalAudio: Boolean,
        isNaturalOfflineTtsReady: Boolean,
        forceAiVoice: Boolean = false
    ): List<PronunciationStep> {
        if (forceAiVoice) {
            return listOf(PronunciationStep.AI_TTS, PronunciationStep.NATURAL_OFFLINE_TTS)
        }

        val steps = mutableListOf<PronunciationStep>()
        if (hasSavedLocalAudio) {
            steps.add(PronunciationStep.SAVED_LOCAL_FILE)
        }

        when (pronunciationSource.trim().lowercase()) {
            "device" -> {
                steps.add(PronunciationStep.NATURAL_OFFLINE_TTS)
            }
            "online" -> {
                steps.add(PronunciationStep.ONLINE_DICTIONARY_AUDIO)
                steps.add(PronunciationStep.AI_TTS)
            }
            else -> { // "auto" — Strict Priority: Natural Offline Neural -> Online Dictionary -> AI TTS
                if (isNaturalOfflineTtsReady) {
                    steps.add(PronunciationStep.NATURAL_OFFLINE_TTS)
                }
                steps.add(PronunciationStep.ONLINE_DICTIONARY_AUDIO)
                steps.add(PronunciationStep.AI_TTS)
                if (!isNaturalOfflineTtsReady) {
                    steps.add(PronunciationStep.NATURAL_OFFLINE_TTS)
                }
            }
        }
        return steps
    }

    fun resolveSentencePlaybackOrder(
        isNaturalOfflineTtsReady: Boolean,
        forceAiVoice: Boolean = false
    ): List<PronunciationStep> {
        if (forceAiVoice) {
            return listOf(PronunciationStep.AI_TTS, PronunciationStep.NATURAL_OFFLINE_TTS)
        }
        return if (isNaturalOfflineTtsReady) {
            listOf(PronunciationStep.NATURAL_OFFLINE_TTS, PronunciationStep.AI_TTS)
        } else {
            listOf(PronunciationStep.AI_TTS, PronunciationStep.NATURAL_OFFLINE_TTS)
        }
    }
}
