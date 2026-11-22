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

    @Test
    fun `SadaAppIcon exposes all 4 distinct launcher aliases with Sonic Flip as default`() {
        val icons = com.example.ui.SadaAppIcon.entries
        assertEquals(4, icons.size)
        assertEquals(com.example.ui.SadaAppIcon.SONIC_FLIP, com.example.ui.SadaAppIcon.fromId(null))
        assertEquals(com.example.ui.SadaAppIcon.SONIC_FLIP, com.example.ui.SadaAppIcon.fromId("unknown_id"))
        assertEquals(com.example.ui.SadaAppIcon.ECHO_DECK, com.example.ui.SadaAppIcon.fromId("echo_deck"))
        assertEquals(com.example.ui.SadaAppIcon.RESONANCE, com.example.ui.SadaAppIcon.fromId("resonance"))
        assertEquals(com.example.ui.SadaAppIcon.WAVE_S, com.example.ui.SadaAppIcon.fromId("wave_s"))
        assertEquals(4, icons.map { it.aliasClassName }.toSet().size)
        assertEquals(4, icons.map { it.id }.toSet().size)
    }

    @Test
    fun `SadaBrandThemePresets provide all 8 curated palettes with Sada Teal + Coral default`() {
        val presets = com.example.ui.theme.SadaBrandThemePresets
        assertEquals(8, presets.size)
        assertEquals("sada_teal", presets.first().id)

        val defaultLight = com.example.ui.theme.getDynamicColorScheme("sada_teal", darkTheme = false)
        val defaultDark = com.example.ui.theme.getDynamicColorScheme("sada_teal", darkTheme = true)
        assertEquals(com.example.ui.theme.SadaTealLightColorScheme.primary, defaultLight.primary)
        assertEquals(com.example.ui.theme.SadaTealDarkColorScheme.primary, defaultDark.primary)
        // Dark mode onPrimary must be dark (not white) on bright pastel teal primary
        assertTrue(defaultDark.onPrimary.red < 0.25f)

        // Every single preset resolves cleanly in both light and dark modes
        presets.forEach { preset ->
            val light = com.example.ui.theme.getDynamicColorScheme(preset.id, darkTheme = false)
            val dark = com.example.ui.theme.getDynamicColorScheme(preset.id, darkTheme = true)
            assertTrue("Preset ${preset.id} light primary != background", light.primary != light.background)
            assertTrue("Preset ${preset.id} dark primary != background", dark.primary != dark.background)
        }
    }

    @Test
    fun `TtsVoiceSelector penalizes robotic echoey synthesizers far below offline Google Neural voices`() {
        val googleNeuralScore = com.example.ui.TtsVoiceSelector.scoreVoiceForAccent(
            name = "en-gb-x-gba-local",
            localeLanguage = "en",
            localeCountry = "GB",
            isNetworkRequired = false,
            quality = 400,
            targetAccent = "UK"
        )
        val espeakScore = com.example.ui.TtsVoiceSelector.scoreVoiceForAccent(
            name = "en-gb-espeak-local",
            localeLanguage = "en",
            localeCountry = "GB",
            isNetworkRequired = false,
            quality = 400,
            targetAccent = "UK"
        )
        val picoScore = com.example.ui.TtsVoiceSelector.scoreVoiceForAccent(
            name = "en-gb-pico-compact",
            localeLanguage = "en",
            localeCountry = "GB",
            isNetworkRequired = false,
            quality = 300,
            targetAccent = "UK"
        )
        assertTrue("Google Neural ($googleNeuralScore) must outrank eSpeak ($espeakScore) by >= 800", googleNeuralScore - espeakScore >= 800)
        assertTrue("Google Neural ($googleNeuralScore) must outrank Pico ($picoScore) by >= 800", googleNeuralScore - picoScore >= 800)
    }

    @Test
    fun `IeltsPracticePromptBuilder builds 3-part IELTS Speaking prompt with learner identity and target words`() {
        val words = listOf(
            WordEntity(word = "aquatic", partOfSpeech = "adj", definition = "Living in or near water", arabicMeaning = "مائي"),
            WordEntity(word = "resilient", partOfSpeech = "adj", definition = "Able to recover quickly", arabicMeaning = "مرن")
        )
        val prompt = com.example.domain.IeltsPracticePromptBuilder.buildSpeakingPrompt(
            words = words,
            userName = "Ahmed",
            cefrLevel = "B2",
            targetBand = "7.5",
            scope = com.example.domain.PracticeWordScope.TODAY
        )
        assertTrue("Prompt must include learner name", prompt.contains("Ahmed"))
        assertTrue("Prompt must include target band", prompt.contains("7.5"))
        assertTrue("Prompt must include CEFR level", prompt.contains("B2"))
        assertTrue("Prompt must include target words", prompt.contains("aquatic") && prompt.contains("resilient"))
        assertTrue("Prompt must include IELTS Parts 1, 2, 3", prompt.contains("Part 1") && prompt.contains("Part 2") && prompt.contains("Part 3"))
    }

    @Test
    fun `IeltsPracticePromptBuilder selects TODAY, THIS_WEEK, WEAK_WORDS and detects habit milestones`() {
        val today = java.time.LocalDate.of(2026, 11, 22)
        val zone = java.time.ZoneOffset.UTC
        val todayEpoch = today.toEpochDay()
        val todayMs = today.atStartOfDay(zone).plusHours(2).toInstant().toEpochMilli()
        val fourDaysAgoMs = today.minusDays(4).atStartOfDay(zone).plusHours(2).toInstant().toEpochMilli()
        val oldMs = today.minusDays(20).atStartOfDay(zone).toInstant().toEpochMilli()

        val deck = listOf(
            WordEntity(word = "todayWord", isBookmarked = true, lastReviewed = todayEpoch, timestamp = todayMs, lapses = 0, fsrsStability = 10.0),
            WordEntity(word = "weekWord", isBookmarked = true, lastReviewed = todayEpoch - 4, timestamp = fourDaysAgoMs, lapses = 1, fsrsStability = 6.0),
            WordEntity(word = "leechWord", isBookmarked = true, lastReviewed = todayEpoch - 15, timestamp = oldMs, isLeech = true, lapses = 5, fsrsStability = 1.2)
        )
        val logs = listOf(
            com.example.data.ReviewLogEntity(word = "todayWord", rating = "Good", timestamp = todayMs),
            com.example.data.ReviewLogEntity(word = "weekWord", rating = "Good", timestamp = fourDaysAgoMs)
        )

        val todayPicked = com.example.domain.IeltsPracticePromptBuilder.selectTargetWordsForPractice(
            bookmarkedWords = deck,
            reviewLogs = logs,
            scope = com.example.domain.PracticeWordScope.TODAY,
            today = today,
            zoneId = zone
        )
        assertEquals(listOf("todayWord"), todayPicked.map { it.word })

        val weekPicked = com.example.domain.IeltsPracticePromptBuilder.selectTargetWordsForPractice(
            bookmarkedWords = deck,
            reviewLogs = logs,
            scope = com.example.domain.PracticeWordScope.THIS_WEEK,
            today = today,
            zoneId = zone
        )
        assertEquals(setOf("todayWord", "weekWord"), weekPicked.map { it.word }.toSet())

        val weakPicked = com.example.domain.IeltsPracticePromptBuilder.selectTargetWordsForPractice(
            bookmarkedWords = deck,
            reviewLogs = logs,
            scope = com.example.domain.PracticeWordScope.WEAK_WORDS,
            today = today,
            zoneId = zone
        )
        assertEquals("leechWord", weakPicked.first().word)

        val studiedThisWeek = com.example.domain.IeltsPracticePromptBuilder.countDistinctWordsStudiedInWindow(
            bookmarkedWords = deck,
            reviewLogs = logs,
            days = 7,
            today = today,
            zoneId = zone
        )
        assertEquals(2, studiedThisWeek)

        assertEquals(7, com.example.domain.IeltsPracticePromptBuilder.detectStreakMilestone(7))
        assertEquals(null, com.example.domain.IeltsPracticePromptBuilder.detectStreakMilestone(5))
        val progress = com.example.domain.IeltsPracticePromptBuilder.calculateHabitProgress(
            dailyStudied = 10,
            dailyTarget = 10,
            weeklyStudied = 50,
            weeklyTarget = 70,
            currentStreakDays = 14
        )
        assertTrue(progress.dailyHit)
        assertFalse(progress.weeklyHit)
        assertEquals(14, progress.streakMilestoneDays)
    }

    @Test
    fun `WordRepository resolveGeminiModelChain migrates retired models and preserves valid custom model first`() {
        val retiredChain = com.example.data.WordRepository.resolveGeminiModelChain("gemini-2.5-flash")
        assertEquals("gemini-3.5-flash-lite", retiredChain.first())
        assertFalse("Retired model must be excluded from chain", retiredChain.contains("gemini-2.5-flash"))
        assertTrue("Chain must provide multiple fallback backups", retiredChain.size >= 4)

        val customChain = com.example.data.WordRepository.resolveGeminiModelChain("models/gemini-3.6-flash")
        assertEquals("gemini-3.6-flash", customChain.first())
        assertEquals("No duplicate models in failover chain", customChain.size, customChain.toSet().size)
    }
}
