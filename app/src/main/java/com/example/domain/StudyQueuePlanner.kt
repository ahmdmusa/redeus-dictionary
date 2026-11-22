package com.example.domain

import com.example.data.WordEntity
import java.util.Locale
import kotlin.math.ceil

/**
 * Anki-style card study bucket derived cleanly from existing [WordEntity] fields
 * (zero Room schema migration needed).
 */
enum class CardStudyBucket {
    LEARNING, // Recently lapsed or in short-term learning/relearning steps
    REVIEW,   // Graduated card due for spaced-repetition review
    NEW       // Unstudied card in the learner's deck (repetitions == 0)
}

/**
 * Snapshot of today's achievable study session (respecting the daily New Cards cap).
 */
data class TodayQueueSummary(
    val newCount: Int,
    val learningCount: Int,
    val reviewCount: Int,
    val totalNewRemainingInDeck: Int,
    val newCardsIntroducedToday: Int,
    val newCardsDailyLimit: Int
) {
    val totalToday: Int get() = newCount + learningCount + reviewCount
    val estimatedMinutes: Int
        get() = if (totalToday == 0) 0 else ceil(totalToday * 0.45).toInt().coerceAtLeast(1)
}

enum class StudyRecallMode {
    STANDARD, // English Word + Audio -> Meaning
    REVERSE   // Arabic Meaning -> Recall English Word
}

data class LastReviewSnapshot(
    val previousWord: WordEntity,
    val insertedReviewLogId: Long?,
    val previousQueue: List<WordEntity>,
    val previousCompletedCount: Int,
    val previousAgainCount: Int,
    val previousAgainWords: List<WordEntity>,
    val wasNewIntroduction: Boolean
)

data class ActiveStudySession(
    val sessionId: Long,
    val deckTag: String,
    val deckTitle: String,
    val queue: List<WordEntity>,
    val completedCount: Int,
    val initialTotal: Int,
    val againCount: Int,
    val againWords: List<WordEntity>,
    val startedAtMs: Long,
    val recallMode: StudyRecallMode = StudyRecallMode.STANDARD,
    val stepIndex: Int = 0,
    val undoSnapshot: LastReviewSnapshot? = null
) {
    val isFinished: Boolean get() = queue.isEmpty()
    val currentCard: WordEntity? get() = queue.firstOrNull()
    val effectiveTotal: Int get() = (completedCount + queue.size).coerceAtLeast(initialTotal).coerceAtLeast(1)
    val progressFraction: Float get() = (completedCount.toFloat() / effectiveTotal.toFloat()).coerceIn(0f, 1f)
    val canUndo: Boolean get() = undoSnapshot != null

    fun toSummary(tomorrowReviews: Int, tomorrowNew: Int, nowMs: Long = System.currentTimeMillis()): SessionSummaryData {
        val totalRated = completedCount.coerceAtLeast(1)
        val accuracy = (((totalRated - againCount).coerceAtLeast(0).toFloat() / totalRated.toFloat()) * 100f).toInt()
        val durationSec = ((nowMs - startedAtMs) / 1000L).toInt().coerceAtLeast(1)
        val uniqueHardest = againWords.distinctBy { it.word.lowercase(Locale.ROOT) }
        return SessionSummaryData(
            deckTitle = deckTitle,
            cardsStudied = completedCount,
            accuracyPercent = accuracy.coerceIn(0, 100),
            durationSeconds = durationSec,
            hardestWords = uniqueHardest,
            tomorrowReviews = tomorrowReviews,
            tomorrowNew = tomorrowNew
        )
    }
}

data class SessionSummaryData(
    val deckTitle: String,
    val cardsStudied: Int,
    val accuracyPercent: Int,
    val durationSeconds: Int,
    val hardestWords: List<WordEntity>,
    val tomorrowReviews: Int,
    val tomorrowNew: Int
)

/**
 * Pure Anki/FSRS daily queue planner.
 * Strictly orders cards as: LEARNING -> DUE REVIEW (most overdue / lowest stability first) -> NEW (capped daily).
 */
object StudyQueuePlanner {

    fun classifyCard(word: WordEntity): CardStudyBucket {
        val state = word.fsrsState?.uppercase(Locale.ROOT) ?: ""
        if (state == "LEARNING" || state == "RELEARNING") {
            return CardStudyBucket.LEARNING
        }
        if (word.repetitions == 0 && word.lastReviewed == null && word.lapses == 0) {
            return CardStudyBucket.NEW
        }
        if (word.interval <= 0 || word.repetitions == 0) {
            return CardStudyBucket.LEARNING
        }
        return CardStudyBucket.REVIEW
    }

    fun matchesDeckFilter(word: WordEntity, deckFilter: String?): Boolean {
        if (deckFilter.isNullOrBlank() || deckFilter.equals("ALL", ignoreCase = true)) {
            return true
        }
        val target = deckFilter.trim().lowercase(Locale.ROOT)
        return word.tags.split(",")
            .map { it.trim().lowercase(Locale.ROOT) }
            .any { it == target }
    }

    fun summarizeToday(
        bookmarkedWords: List<WordEntity>,
        todayEpochDay: Long,
        newCardsDailyLimit: Int,
        newCardsIntroducedToday: Int,
        deckFilter: String? = null,
        maxReviewsPerDay: Int = Int.MAX_VALUE
    ): TodayQueueSummary {
        val filtered = bookmarkedWords.filter { matchesDeckFilter(it, deckFilter) }
        val learning = mutableListOf<WordEntity>()
        val dueReviews = mutableListOf<WordEntity>()
        val newCards = mutableListOf<WordEntity>()

        for (card in filtered) {
            when (classifyCard(card)) {
                CardStudyBucket.LEARNING -> {
                    if (card.dueDate <= todayEpochDay) learning.add(card)
                }
                CardStudyBucket.REVIEW -> {
                    if (card.dueDate <= todayEpochDay) dueReviews.add(card)
                }
                CardStudyBucket.NEW -> {
                    if (card.dueDate <= todayEpochDay) newCards.add(card)
                }
            }
        }

        val safeLimit = newCardsDailyLimit.coerceAtLeast(0)
        val remainingNewAllowance = (safeLimit - newCardsIntroducedToday.coerceAtLeast(0)).coerceAtLeast(0)
        val todayNewCount = minOf(newCards.size, remainingNewAllowance)
        val todayReviewCount = minOf(dueReviews.size, maxReviewsPerDay.coerceAtLeast(0))

        return TodayQueueSummary(
            newCount = todayNewCount,
            learningCount = learning.size,
            reviewCount = todayReviewCount,
            totalNewRemainingInDeck = newCards.size,
            newCardsIntroducedToday = newCardsIntroducedToday.coerceAtLeast(0),
            newCardsDailyLimit = safeLimit
        )
    }

    fun buildDailyQueue(
        bookmarkedWords: List<WordEntity>,
        todayEpochDay: Long,
        newCardsDailyLimit: Int,
        newCardsIntroducedToday: Int,
        deckFilter: String? = null,
        extraNewBonus: Int = 0,
        maxReviewsPerDay: Int = Int.MAX_VALUE
    ): List<WordEntity> {
        val filtered = bookmarkedWords.filter { matchesDeckFilter(it, deckFilter) }

        val learning = mutableListOf<WordEntity>()
        val dueReviews = mutableListOf<WordEntity>()
        val newCards = mutableListOf<WordEntity>()

        for (card in filtered) {
            if (card.dueDate > todayEpochDay) continue
            when (classifyCard(card)) {
                CardStudyBucket.LEARNING -> learning.add(card)
                CardStudyBucket.REVIEW -> dueReviews.add(card)
                CardStudyBucket.NEW -> newCards.add(card)
            }
        }

        // Order Learning: most recently lapsed/oldest first
        val sortedLearning = learning.sortedWith(
            compareBy<WordEntity> { it.dueDate }
                .thenByDescending { it.lapses }
                .thenBy { it.timestamp }
        )

        // Order Due Reviews: most overdue first, then lowest FSRS stability (hardest)
        val sortedReviews = dueReviews.sortedWith(
            compareBy<WordEntity> { it.dueDate }
                .thenBy { it.fsrsStability ?: it.interval.toDouble() }
                .thenByDescending { it.lapses }
        ).take(maxReviewsPerDay.coerceAtLeast(0))

        // Order New Cards: FIFO by insertion timestamp so units stay in curriculum order
        val remainingNewAllowance = (newCardsDailyLimit.coerceAtLeast(0) - newCardsIntroducedToday.coerceAtLeast(0))
            .coerceAtLeast(0) + extraNewBonus.coerceAtLeast(0)
        val sortedNew = newCards
            .sortedBy { it.timestamp }
            .take(remainingNewAllowance)

        return sortedLearning + sortedReviews + sortedNew
    }

    /**
     * Builds a focused Leech / Weak-Words drill queue (leeches first, then highest lapses).
     */
    fun buildLeechesDrillQueue(bookmarkedWords: List<WordEntity>, limit: Int = 20): List<WordEntity> {
        return bookmarkedWords
            .filter { it.isLeech || it.lapses >= 2 }
            .sortedWith(
                compareByDescending<WordEntity> { it.isLeech }
                    .thenByDescending { it.lapses }
                    .thenBy { it.fsrsStability ?: Double.MAX_VALUE }
            )
            .take(limit)
    }

    /**
     * Re-inserts an 'Again'-rated card cleanly 3 positions ahead (or at the tail if queue is short)
     * so the learner sees it again soon without immediate back-to-back repetition.
     */
    fun requeueAgainCard(remainingQueue: List<WordEntity>, updatedCard: WordEntity, spacing: Int = 3): List<WordEntity> {
        val next = remainingQueue.toMutableList()
        val insertIndex = minOf(spacing.coerceAtLeast(1), next.size)
        next.add(insertIndex, updatedCard)
        return next
    }

    /**
     * Estimates how many reviews will be due tomorrow (for Session Summary forecast).
     */
    fun countTomorrowDueReviews(bookmarkedWords: List<WordEntity>, todayEpochDay: Long): Int {
        val tomorrow = todayEpochDay + 1L
        return bookmarkedWords.count { card ->
            classifyCard(card) != CardStudyBucket.NEW && card.dueDate <= tomorrow
        }
    }
}
