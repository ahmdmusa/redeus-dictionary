package com.example.domain

import com.example.data.OfflineBook
import com.example.data.OfflineCurriculumWord
import com.example.data.OfflineUnit
import com.example.data.WordEntity
import java.util.Locale

enum class UnitProgressStatus {
    COMPLETED,   // All words in unit are in deck (or mastered)
    IN_PROGRESS, // Some words in unit are in deck
    NOT_STARTED  // Zero words in unit are in deck yet
}

data class BookProgressSummary(
    val book: OfflineBook,
    val inDeckCount: Int,
    val masteredCount: Int,
    val totalWords: Int,
    val currentUnitNum: Int
) {
    val inDeckPercent: Int
        get() = if (totalWords <= 0) 0 else ((inDeckCount * 100f) / totalWords.toFloat()).toInt().coerceIn(0, 100)
    val masteredPercent: Int
        get() = if (totalWords <= 0) 0 else ((masteredCount * 100f) / totalWords.toFloat()).toInt().coerceIn(0, 100)
}

data class UnitProgressSummary(
    val unit: OfflineUnit,
    val inDeckCount: Int,
    val masteredCount: Int,
    val totalWords: Int,
    val status: UnitProgressStatus
) {
    val progressFraction: Float
        get() = if (totalWords <= 0) 0f else (inDeckCount.toFloat() / totalWords.toFloat()).coerceIn(0f, 1f)
}

/**
 * Pure calculator for book/unit progress and Active Course Auto-Feed batches.
 */
object CourseProgressCalculator {

    fun isMastered(word: WordEntity): Boolean {
        return word.distinctReviewDays >= 3 && word.interval >= 21 && !word.isLeech
    }

    fun calculateBookProgress(
        book: OfflineBook,
        bookWordLowers: Set<String>,
        deckWordMap: Map<String, WordEntity>,
        unitWordLowersMap: Map<Int, Set<String>> = emptyMap()
    ): BookProgressSummary {
        val total = bookWordLowers.size.takeIf { it > 0 } ?: book.wordCount
        var inDeck = 0
        var mastered = 0
        for (wl in bookWordLowers) {
            val entity = deckWordMap[wl]
            if (entity != null) {
                inDeck++
                if (isMastered(entity)) mastered++
            }
        }
        val currentUnit = detectNextUnitNum(book.unitCount, unitWordLowersMap, deckWordMap)
        return BookProgressSummary(
            book = book,
            inDeckCount = inDeck,
            masteredCount = mastered,
            totalWords = total,
            currentUnitNum = currentUnit
        )
    }

    fun calculateUnitProgress(
        unit: OfflineUnit,
        unitWordLowers: Set<String>,
        deckWordMap: Map<String, WordEntity>
    ): UnitProgressSummary {
        val total = unitWordLowers.size.takeIf { it > 0 } ?: unit.wordCount
        var inDeck = 0
        var mastered = 0
        for (wl in unitWordLowers) {
            val entity = deckWordMap[wl]
            if (entity != null) {
                inDeck++
                if (isMastered(entity)) mastered++
            }
        }
        val status = when {
            total > 0 && inDeck >= total -> UnitProgressStatus.COMPLETED
            inDeck > 0 -> UnitProgressStatus.IN_PROGRESS
            else -> UnitProgressStatus.NOT_STARTED
        }
        return UnitProgressSummary(
            unit = unit,
            inDeckCount = inDeck,
            masteredCount = mastered,
            totalWords = total,
            status = status
        )
    }

    /**
     * Finds the earliest unit in the book that still has words not yet in the learner's deck.
     */
    fun detectNextUnitNum(
        unitCount: Int,
        unitWordLowersMap: Map<Int, Set<String>>,
        deckWordMap: Map<String, WordEntity>
    ): Int {
        if (unitWordLowersMap.isEmpty()) return 1
        val sortedUnits = unitWordLowersMap.keys.sorted()
        for (unitNum in sortedUnits) {
            val words = unitWordLowersMap[unitNum].orEmpty()
            if (words.any { !deckWordMap.containsKey(it) }) {
                return unitNum
            }
        }
        return sortedUnits.lastOrNull() ?: unitCount.coerceAtLeast(1)
    }

    /**
     * Selects the next [batchSize] curriculum words that are NOT yet in the learner's deck,
     * preserving unit & word order.
     */
    fun selectNextAutoFeedWords(
        orderedUnitWords: List<OfflineCurriculumWord>,
        existingDeckWordsLower: Set<String>,
        batchSize: Int
    ): List<OfflineCurriculumWord> {
        if (batchSize <= 0) return emptyList()
        val seen = HashSet<String>()
        val selected = mutableListOf<OfflineCurriculumWord>()
        for (cw in orderedUnitWords) {
            val key = cw.word.trim().lowercase(Locale.ROOT)
            if (key.isBlank() || key in existingDeckWordsLower || !seen.add(key)) continue
            selected.add(cw)
            if (selected.size >= batchSize) break
        }
        return selected
    }
}
