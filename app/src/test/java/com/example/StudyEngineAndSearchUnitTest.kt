package com.example

import com.example.data.ArabicNormalizer
import com.example.data.OfflineBook
import com.example.data.OfflineCurriculumWord
import com.example.data.OfflineQueryMatcher
import com.example.data.OfflineUnit
import com.example.data.WordEntity
import com.example.domain.CardStudyBucket
import com.example.domain.CourseProgressCalculator
import com.example.domain.StudyQueuePlanner
import com.example.domain.UnitProgressStatus
import com.example.ui.PronunciationPriority
import com.example.ui.PronunciationStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM unit tests (test-first) for v1.3.0 core logic:
 *   - ArabicNormalizer & OfflineQueryMatcher (lemmatization + fuzzy Did-You-Mean)
 *   - StudyQueuePlanner (Anki 3-bucket priority, daily New cap, Again requeue, Leeches drill)
 *   - CourseProgressCalculator (book/unit progress, next unit, auto-feed deduplication)
 *   - PronunciationPriority (Natural Offline Neural -> Online Dictionary -> AI TTS)
 */
class StudyEngineAndSearchUnitTest {

    @Test
    fun `ArabicNormalizer strips tashkeel and unifies hamza, ta-marbuta, and alef-maksura`() {
        assertEquals(ArabicNormalizer.normalize("التّرك"), ArabicNormalizer.normalize("الترك"))
        assertEquals(ArabicNormalizer.normalize("يتخلّى"), ArabicNormalizer.normalize("يتخلى"))
        assertEquals(ArabicNormalizer.normalize("إختزال"), ArabicNormalizer.normalize("اختزال"))
        assertEquals(ArabicNormalizer.normalize("أكل"), ArabicNormalizer.normalize("اكل"))
        assertEquals(ArabicNormalizer.normalize("مدرسة"), ArabicNormalizer.normalize("مدرسه"))
        assertTrue(ArabicNormalizer.containsArabic("يوافق"))
        assertFalse(ArabicNormalizer.containsArabic("agree"))
    }

    @Test
    fun `OfflineQueryMatcher stems inflected and irregular English queries accurately`() {
        assertTrue(OfflineQueryMatcher.generateEnglishStemCandidates("abandoned").contains("abandon"))
        assertTrue(OfflineQueryMatcher.generateEnglishStemCandidates("studying").contains("study"))
        assertTrue(OfflineQueryMatcher.generateEnglishStemCandidates("arrived").contains("arrive"))
        assertTrue(OfflineQueryMatcher.generateEnglishStemCandidates("children").contains("child"))
        assertTrue(OfflineQueryMatcher.generateEnglishStemCandidates("went").contains("go"))
        assertTrue(OfflineQueryMatcher.generateEnglishStemCandidates("happier").contains("happy"))
        assertTrue(OfflineQueryMatcher.generateEnglishStemCandidates("leaves").contains("leaf"))
    }

    @Test
    fun `OfflineQueryMatcher ranks Damerau-Levenshtein fuzzy matches for typos`() {
        val candidates = listOf("abandon", "abundant", "accurate", "achieve", "accommodate", "zebra")
        val typo1 = OfflineQueryMatcher.rankFuzzyMatches("abndon", candidates)
        assertEquals("abandon", typo1.first())

        val typo2 = OfflineQueryMatcher.rankFuzzyMatches("accomodate", candidates)
        assertEquals("accommodate", typo2.first())

        val transposition = OfflineQueryMatcher.rankFuzzyMatches("achivee", candidates)
        assertEquals("achieve", transposition.first())
    }

    @Test
    fun `StudyQueuePlanner orders Learning then Due Review then capped New cards`() {
        val today = 20_000L
        val words = listOf(
            // 3 New cards (repetitions=0, lastReviewed=null)
            WordEntity(word = "new1", isBookmarked = true, dueDate = today, timestamp = 10L),
            WordEntity(word = "new2", isBookmarked = true, dueDate = today, timestamp = 20L),
            WordEntity(word = "new3", isBookmarked = true, dueDate = today, timestamp = 30L),
            // 2 Due review cards (revUrgent is older & lower stability)
            WordEntity(word = "revLater", isBookmarked = true, repetitions = 4, interval = 10, fsrsStability = 12.0, dueDate = today, lastReviewed = today - 10),
            WordEntity(word = "revUrgent", isBookmarked = true, repetitions = 2, interval = 3, fsrsStability = 2.5, dueDate = today - 2, lastReviewed = today - 5),
            // 1 Learning card
            WordEntity(word = "learn1", isBookmarked = true, repetitions = 1, interval = 0, lapses = 1, fsrsState = "RELEARNING", dueDate = today, lastReviewed = today),
            // 1 Future review card (not due yet)
            WordEntity(word = "future1", isBookmarked = true, repetitions = 5, interval = 25, dueDate = today + 10, lastReviewed = today - 2)
        )

        val summary = StudyQueuePlanner.summarizeToday(
            bookmarkedWords = words,
            todayEpochDay = today,
            newCardsDailyLimit = 2,
            newCardsIntroducedToday = 0
        )
        assertEquals(1, summary.learningCount)
        assertEquals(2, summary.reviewCount)
        assertEquals(2, summary.newCount) // Capped at 2 even though 3 are in deck!
        assertEquals(3, summary.totalNewRemainingInDeck)
        assertEquals(5, summary.totalToday)

        val queue = StudyQueuePlanner.buildDailyQueue(
            bookmarkedWords = words,
            todayEpochDay = today,
            newCardsDailyLimit = 2,
            newCardsIntroducedToday = 0
        )
        assertEquals(listOf("learn1", "revUrgent", "revLater", "new1", "new2"), queue.map { it.word })

        // Requeue Again card should place it 3 slots ahead (not immediately next, not dropped)
        val remaining = queue.drop(1)
        val requeued = StudyQueuePlanner.requeueAgainCard(remaining, queue.first(), spacing = 3)
        assertEquals("learn1", requeued[3].word)
        assertEquals(5, requeued.size)
    }

    @Test
    fun `StudyQueuePlanner respects already introduced new cards today and supports study-ahead bonus`() {
        val today = 20_000L
        val newDeck = (1..15).map { i ->
            WordEntity(word = "w$i", isBookmarked = true, dueDate = today, timestamp = i.toLong())
        }
        val normalQueue = StudyQueuePlanner.buildDailyQueue(
            bookmarkedWords = newDeck,
            todayEpochDay = today,
            newCardsDailyLimit = 10,
            newCardsIntroducedToday = 8
        )
        assertEquals(2, normalQueue.size)

        val bonusQueue = StudyQueuePlanner.buildDailyQueue(
            bookmarkedWords = newDeck,
            todayEpochDay = today,
            newCardsDailyLimit = 10,
            newCardsIntroducedToday = 10,
            extraNewBonus = 5
        )
        assertEquals(5, bonusQueue.size)
    }

    @Test
    fun `CourseProgressCalculator tracks unit states, next unit, and deduplicated auto-feed`() {
        val book = OfflineBook(1, "Essential Book 1", "Book 1", "A1", "essential", 2, 4)
        val unit1 = OfflineUnit("1-1", 1, 1, "Unit 1", 2)
        val unit2 = OfflineUnit("1-2", 1, 2, "Unit 2", 2)
        val unitMap = mapOf(1 to setOf("agree", "arrive"), 2 to setOf("clever", "cruel"))

        val deckMap = mapOf(
            "agree" to WordEntity(word = "agree", isBookmarked = true, distinctReviewDays = 3, interval = 25),
            "arrive" to WordEntity(word = "arrive", isBookmarked = true, distinctReviewDays = 1, interval = 3),
            "clever" to WordEntity(word = "clever", isBookmarked = true, distinctReviewDays = 0, interval = 0)
        )

        val bp = CourseProgressCalculator.calculateBookProgress(book, setOf("agree", "arrive", "clever", "cruel"), deckMap, unitMap)
        assertEquals(3, bp.inDeckCount)
        assertEquals(1, bp.masteredCount)
        assertEquals(75, bp.inDeckPercent)
        assertEquals(2, bp.currentUnitNum)

        val u1p = CourseProgressCalculator.calculateUnitProgress(unit1, unitMap[1]!!, deckMap)
        val u2p = CourseProgressCalculator.calculateUnitProgress(unit2, unitMap[2]!!, deckMap)
        assertEquals(UnitProgressStatus.COMPLETED, u1p.status)
        assertEquals(UnitProgressStatus.IN_PROGRESS, u2p.status)

        val unit2Words = listOf(
            OfflineCurriculumWord("1-2-1", "clever", "", "adj", "smart", "ذكي", "ex", 1, 2, "", 1, "A1", "essential"),
            OfflineCurriculumWord("1-2-2", "cruel", "", "adj", "mean", "قاس", "ex", 1, 2, "", 2, "A1", "essential")
        )
        val nextBatch = CourseProgressCalculator.selectNextAutoFeedWords(unit2Words, deckMap.keys, batchSize = 5)
        assertEquals(listOf("cruel"), nextBatch.map { it.word })
    }

    @Test
    fun `PronunciationPriority strictly prefers Natural Offline then Online Dictionary then AI TTS`() {
        val defaultReady = PronunciationPriority.resolveWordPlaybackOrder(
            pronunciationSource = "auto",
            hasSavedLocalAudio = false,
            isNaturalOfflineTtsReady = true
        )
        assertEquals(
            listOf(
                PronunciationStep.NATURAL_OFFLINE_TTS,
                PronunciationStep.ONLINE_DICTIONARY_AUDIO,
                PronunciationStep.AI_TTS
            ),
            defaultReady
        )

        val withSaved = PronunciationPriority.resolveWordPlaybackOrder(
            pronunciationSource = "auto",
            hasSavedLocalAudio = true,
            isNaturalOfflineTtsReady = true
        )
        assertEquals(PronunciationStep.SAVED_LOCAL_FILE, withSaved.first())
        assertEquals(PronunciationStep.NATURAL_OFFLINE_TTS, withSaved[1])

        val explicitAi = PronunciationPriority.resolveWordPlaybackOrder(
            pronunciationSource = "auto",
            hasSavedLocalAudio = true,
            isNaturalOfflineTtsReady = true,
            forceAiVoice = true
        )
        assertEquals(PronunciationStep.AI_TTS, explicitAi.first())

        val sentenceOrder = PronunciationPriority.resolveSentencePlaybackOrder(isNaturalOfflineTtsReady = true)
        assertEquals(PronunciationStep.NATURAL_OFFLINE_TTS, sentenceOrder.first())
    }
}
