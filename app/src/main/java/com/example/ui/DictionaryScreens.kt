package com.example.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil.ImageLoader
import com.example.data.WordEntity
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Compatibility forwarding entry point preserved for GreetingScreenshotTest.
 */
@Composable
fun HeroBannerSection() = com.example.ui.screens.search.HeroBannerSection()

fun filterWordList(
    list: List<WordEntity>,
    searchQuery: String,
    statusFilter: WordStatusFilter,
    posFilter: String,
    tagFilter: String
): List<WordEntity> {
    return list.filter { word ->
        val query = searchQuery.trim().lowercase()
        val normArQuery = com.example.data.ArabicNormalizer.normalize(query)
        val matchesQuery = query.isEmpty() ||
            word.word.lowercase().contains(query) ||
            word.definition.lowercase().contains(query) ||
            word.tags.lowercase().contains(query) ||
            (normArQuery.isNotEmpty() && com.example.data.ArabicNormalizer.normalize(word.arabicMeaning).contains(normArQuery))

        val matchesStatus = when (statusFilter) {
            WordStatusFilter.ALL -> true
            WordStatusFilter.NEW -> word.repetitions == 0 && word.distinctReviewDays == 0
            WordStatusFilter.LEARNING -> (word.repetitions > 0 || word.distinctReviewDays > 0) &&
                !(word.distinctReviewDays >= 3 && word.interval >= 6)
            WordStatusFilter.MASTERED -> word.distinctReviewDays >= 3 && word.interval >= 6
            WordStatusFilter.LEECH -> word.isLeech
        }

        val matchesPos = when (posFilter.uppercase()) {
            "ALL" -> true
            "NOUN" -> word.partOfSpeech.lowercase().startsWith("n")
            "VERB" -> word.partOfSpeech.lowercase().startsWith("v")
            "ADJECTIVE" -> word.partOfSpeech.lowercase().startsWith("adj")
            "ADVERB" -> word.partOfSpeech.lowercase().startsWith("adv")
            "OTHER" -> !word.partOfSpeech.lowercase().let {
                it.startsWith("n") || it.startsWith("v") || it.startsWith("adj") || it.startsWith("adv")
            }
            else -> true
        }

        val matchesTag = tagFilter.isBlank() || tagFilter.equals("ALL", ignoreCase = true) ||
            word.tags.split(",").map { it.trim().lowercase() }.contains(tagFilter.trim().lowercase())

        matchesQuery && matchesStatus && matchesPos && matchesTag
    }
}

fun getAIImageUrl(word: String, prompt: String): String {
    if (prompt.startsWith("http://") || prompt.startsWith("https://")) {
        return prompt
    }
    val cleanPrompt = prompt.ifEmpty {
        "A beautiful creative symbolic digital art vector illustration of the concept of $word"
    }
    val encodedPrompt = try {
        URLEncoder.encode(cleanPrompt, "UTF-8")
    } catch (_: Exception) {
        word
    }
    return "https://image.pollinations.ai/p/$encodedPrompt?width=600&height=400&nologo=true"
}

@Composable
fun rememberAppImageLoader(context: Context): ImageLoader {
    return remember(context) {
        com.example.data.LocalWordImageStore.getSharedImageLoader(context)
    }
}

fun getPhoneticForAccent(phonetic: String, accent: String): String {
    if (phonetic.contains("(US)") || phonetic.contains("(UK)")) {
        val chunks = Regex("(/[^/]+/)(?:\\s*\\((US|UK)\\))?").findAll(phonetic).toList()
        if (chunks.isNotEmpty()) {
            val matchingChunk = chunks.find { it.groups[2]?.value == accent }
            if (matchingChunk != null) {
                return matchingChunk.groups[1]?.value?.trim() ?: phonetic
            }
            val genericChunk = chunks.find { it.groups[2] == null }
            if (genericChunk != null) {
                return genericChunk.groups[1]?.value?.trim() ?: phonetic
            }
            return chunks.first().groups[1]?.value?.trim() ?: phonetic
        }
    }
    return phonetic.replace(" (US)", "").replace(" (UK)", "").trim()
}

fun formatPartOfSpeechAbbreviation(pos: String?): String {
    if (pos.isNullOrBlank()) return ""
    val trimmed = pos.trim().lowercase()
    return when (trimmed) {
        "noun", "n", "n." -> "n."
        "verb", "v", "v.", "vb", "vb." -> "v."
        "adjective", "adj", "adj." -> "adj."
        "adverb", "adv", "adv." -> "adv."
        "preposition", "prep", "prep." -> "prep."
        "pronoun", "pron", "pron." -> "pron."
        "conjunction", "conj", "conj." -> "conj."
        "interjection", "interj", "interj." -> "interj."
        "phrase", "phr", "phr." -> "phr."
        "abbreviation", "abbr", "abbr." -> "abbr."
        "idiom", "idm", "idm." -> "idm."
        "article", "art", "art." -> "art."
        "determiner", "det", "det." -> "det."
        "particle", "part", "part." -> "part."
        "numeral", "num", "num." -> "num."
        else -> {
            val clean = trimmed.replace(".", "").trim()
            if (clean.isEmpty()) "" else "${clean.take(4)}."
        }
    }
}
