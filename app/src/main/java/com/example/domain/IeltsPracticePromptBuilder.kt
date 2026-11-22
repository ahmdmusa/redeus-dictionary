package com.example.domain

import com.example.data.ReviewLogEntity
import com.example.data.WordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class PracticeWordScope(val label: String) {
    TODAY("Today's Words"),
    THIS_WEEK("This Week"),
    WEAK_WORDS("Weak & Due")
}

enum class ReadingStyle(val id: String, val label: String) {
    IELTS_ACADEMIC("ielts", "IELTS Reading"),
    SHORT_STORY("story", "Short Story");

    companion object {
        fun fromId(id: String?): ReadingStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: IELTS_ACADEMIC
        }
    }
}

data class HabitTargetProgress(
    val dailyStudied: Int,
    val dailyTarget: Int,
    val weeklyStudied: Int,
    val weeklyTarget: Int,
    val dailyHit: Boolean,
    val weeklyHit: Boolean,
    val streakMilestoneDays: Int?
) {
    val dailyFraction: Float
        get() = (dailyStudied.toFloat() / dailyTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    val weeklyFraction: Float
        get() = (weeklyStudied.toFloat() / weeklyTarget.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
}

/**
 * Pure domain engine powering Sada's Habit Targets, Milestones, and IELTS Speaking Prompt Generator.
 *
 * When a learner finishes their daily or weekly word target, Sada generates a structured,
 * ready-to-paste IELTS Speaking prompt for external AI voice/chat agents (ChatGPT Voice, Gemini Live, Claude)
 * so every memorized word is actively practiced in real spoken conversation.
 */
object IeltsPracticePromptBuilder {

    private val STREAK_MILESTONES = setOf(3, 7, 14, 21, 30, 50, 75, 100, 150, 200, 365)

    fun detectStreakMilestone(streakDays: Int): Int? {
        return if (streakDays in STREAK_MILESTONES) streakDays else null
    }

    fun calculateHabitProgress(
        dailyStudied: Int,
        dailyTarget: Int,
        weeklyStudied: Int,
        weeklyTarget: Int,
        currentStreakDays: Int
    ): HabitTargetProgress {
        val safeDailyTarget = dailyTarget.coerceAtLeast(1)
        val safeWeeklyTarget = weeklyTarget.coerceAtLeast(1)
        val safeDailyStudied = dailyStudied.coerceAtLeast(0)
        val safeWeeklyStudied = weeklyStudied.coerceAtLeast(0)
        return HabitTargetProgress(
            dailyStudied = safeDailyStudied,
            dailyTarget = safeDailyTarget,
            weeklyStudied = safeWeeklyStudied,
            weeklyTarget = safeWeeklyTarget,
            dailyHit = safeDailyStudied >= safeDailyTarget,
            weeklyHit = safeWeeklyStudied >= safeWeeklyTarget,
            streakMilestoneDays = detectStreakMilestone(currentStreakDays)
        )
    }

    /**
     * Selects the learner's target vocabulary words for IELTS Reading or Speaking practice.
     * - TODAY: words reviewed or added today (falls back to most recently studied/bookmarked words if 0).
     * - THIS_WEEK: distinct words reviewed or added in the last 7 days (falls back to recent deck words).
     * - WEAK_WORDS: leeches, high-lapse, or lowest-stability cards needing reinforcement.
     */
    fun selectTargetWordsForPractice(
        bookmarkedWords: List<WordEntity>,
        reviewLogs: List<ReviewLogEntity>,
        scope: PracticeWordScope,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        maxWords: Int = 12
    ): List<WordEntity> {
        if (bookmarkedWords.isEmpty()) return emptyList()
        val safeMax = maxWords.coerceIn(1, 30)
        val deckByLower = bookmarkedWords.associateBy { it.word.trim().lowercase() }
        val todayEpoch = today.toEpochDay()
        val weekStartEpoch = today.minusDays(6).toEpochDay()
        val todayStartMs = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val weekStartMs = today.minusDays(6).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val selected = LinkedHashMap<String, WordEntity>()

        when (scope) {
            PracticeWordScope.TODAY -> {
                // 1. Words reviewed today in review logs (most recent first)
                reviewLogs
                    .asSequence()
                    .filter { it.timestamp >= todayStartMs }
                    .sortedByDescending { it.timestamp }
                    .forEach { log ->
                        val key = log.word.trim().lowercase()
                        val entity = deckByLower[key]
                        if (entity != null && selected.size < safeMax) {
                            selected.putIfAbsent(key, entity)
                        }
                    }
                // 2. Words whose lastReviewed == todayEpoch or added today
                bookmarkedWords
                    .asSequence()
                    .filter { it.lastReviewed == todayEpoch || it.timestamp >= todayStartMs }
                    .sortedByDescending { it.timestamp }
                    .forEach { entity ->
                        val key = entity.word.trim().lowercase()
                        if (selected.size < safeMax) {
                            selected.putIfAbsent(key, entity)
                        }
                    }
            }

            PracticeWordScope.THIS_WEEK -> {
                // 1. Words reviewed in the last 7 days
                reviewLogs
                    .asSequence()
                    .filter { it.timestamp >= weekStartMs }
                    .sortedByDescending { it.timestamp }
                    .forEach { log ->
                        val key = log.word.trim().lowercase()
                        val entity = deckByLower[key]
                        if (entity != null && selected.size < safeMax) {
                            selected.putIfAbsent(key, entity)
                        }
                    }
                // 2. Words reviewed or added within the last 7 days
                bookmarkedWords
                    .asSequence()
                    .filter { (it.lastReviewed != null && it.lastReviewed >= weekStartEpoch) || it.timestamp >= weekStartMs }
                    .sortedByDescending { it.timestamp }
                    .forEach { entity ->
                        val key = entity.word.trim().lowercase()
                        if (selected.size < safeMax) {
                            selected.putIfAbsent(key, entity)
                        }
                    }
            }

            PracticeWordScope.WEAK_WORDS -> {
                bookmarkedWords
                    .sortedWith(
                        compareByDescending<WordEntity> { it.isLeech }
                            .thenByDescending { it.lapses }
                            .thenBy { it.fsrsStability ?: (it.interval.toDouble()) }
                    )
                    .take(safeMax)
                    .forEach { entity ->
                        selected.putIfAbsent(entity.word.trim().lowercase(), entity)
                    }
            }
        }

        // Graceful fallback so users always get a useful set even before today's first review
        if (selected.isEmpty()) {
            bookmarkedWords
                .sortedByDescending { it.lastReviewed ?: (it.timestamp / 86_400_000L) }
                .take(safeMax)
                .forEach { entity ->
                    selected.putIfAbsent(entity.word.trim().lowercase(), entity)
                }
        }

        return selected.values.toList()
    }

    /**
     * Counts distinct words actively studied (reviewed or introduced) in the last 7 days.
     */
    fun countDistinctWordsStudiedInWindow(
        bookmarkedWords: List<WordEntity>,
        reviewLogs: List<ReviewLogEntity>,
        days: Int = 7,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Int {
        val startDate = today.minusDays((days - 1).coerceAtLeast(0).toLong())
        val startEpoch = startDate.toEpochDay()
        val startMs = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val distinctWords = HashSet<String>()

        reviewLogs.forEach { log ->
            if (log.timestamp >= startMs) {
                val key = log.word.trim().lowercase()
                if (key.isNotEmpty()) distinctWords.add(key)
            }
        }
        bookmarkedWords.forEach { word ->
            val reviewedInWindow = word.lastReviewed != null && word.lastReviewed >= startEpoch
            val addedInWindow = word.timestamp >= startMs && word.repetitions > 0
            if (reviewedInWindow || addedInWindow) {
                distinctWords.add(word.word.trim().lowercase())
            }
        }
        return distinctWords.size
    }

    /**
     * Generates a structured, copy-ready IELTS Speaking Practice Prompt designed for
     * external AI voice/chat agents (ChatGPT Voice, Gemini Live, Claude, etc.).
     */
    fun buildSpeakingPrompt(
        words: List<WordEntity>,
        userName: String = "",
        cefrLevel: String = "B1",
        targetBand: String = "7.0",
        scope: PracticeWordScope = PracticeWordScope.TODAY
    ): String {
        val learnerLine = userName.trim().takeIf { it.isNotBlank() }?.let { "Learner name: $it\n" } ?: ""
        val cleanLevel = cefrLevel.trim().uppercase().ifBlank { "B1" }
        val cleanBand = targetBand.trim().ifBlank { "7.0" }
        val scopeLabel = when (scope) {
            PracticeWordScope.TODAY -> "today's study session"
            PracticeWordScope.THIS_WEEK -> "this week's study target"
            PracticeWordScope.WEAK_WORDS -> "words that need extra reinforcement"
        }

        val vocabLines = if (words.isEmpty()) {
            "- (Choose 6–8 high-utility IELTS Band $cleanBand vocabulary words suitable for my level)"
        } else {
            words.take(15).mapIndexed { index, entity ->
                val headword = entity.word.trim()
                val posPart = entity.partOfSpeech.trim().takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                val defPart = entity.definition.trim().take(90).takeIf { it.isNotBlank() && it != entity.arabicMeaning }
                val arPart = entity.arabicMeaning.trim().take(45).takeIf { it.isNotBlank() }
                val gloss = listOfNotNull(defPart, arPart).joinToString(" — ")
                if (gloss.isNotBlank()) {
                    "${index + 1}. $headword$posPart: $gloss"
                } else {
                    "${index + 1}. $headword$posPart"
                }
            }.joinToString("\n")
        }

        return buildString {
            appendLine("Act as a supportive, certified IELTS Speaking Examiner and English conversation coach.")
            append(learnerLine)
            appendLine("Current level: CEFR $cleanLevel | Target IELTS Speaking Band: $cleanBand")
            appendLine()
            appendLine("GOAL:")
            appendLine("I just finished $scopeLabel in Sada (my vocabulary app). Help me practice REAL spoken English in authentic IELTS Speaking format while naturally weaving ALL of my target words below into our conversation.")
            appendLine()
            appendLine("MY TARGET VOCABULARY WORDS:")
            appendLine(vocabLines)
            appendLine()
            appendLine("HOW TO RUN THIS SESSION:")
            appendLine("1. Pick ONE engaging, practical IELTS Speaking theme that naturally connects as many of these words as possible (e.g., technology & habits, education, society, travel, health, or work).")
            appendLine("2. Run a full interactive 3-part IELTS Speaking session — ask ONLY ONE question at a time and wait for my answer before moving on:")
            appendLine("   • Part 1 (2–3 quick warm-up questions): easy personal questions related to the theme.")
            appendLine("   • Part 2 (Long Turn Cue Card): give me a realistic IELTS Cue Card with 4 bullet prompts and suggest 3–4 target words I can weave into a 1–2 minute answer.")
            appendLine("   • Part 3 (2–3 follow-up discussion questions): deeper analytical questions to push my Band $cleanBand+ lexical range.")
            appendLine("3. After EACH of my answers:")
            appendLine("   • Gently correct any grammar or awkward phrasing and show a natural Band $cleanBand+ upgraded version of 1–2 sentences.")
            appendLine("   • Keep a live checklist of which target words I used naturally (✓) and subtly nudge me toward the remaining unused words in your next question.")
            appendLine("4. When we finish Part 3 (or when I say 'Finish'), give me a concise IELTS Speaking score breakdown across Fluency & Coherence, Lexical Resource, Grammatical Range & Accuracy, and Pronunciation tips, plus my 3 highest-impact improvements.")
            appendLine()
            append("Start now: briefly greet me, introduce the topic you chose for my words, and ask Part 1 Question 1.")
        }
    }
}
