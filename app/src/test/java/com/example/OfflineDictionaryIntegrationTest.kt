package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.api.Candidate
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerateContentResponse
import com.example.api.GeminiApiService
import com.example.api.Part
import com.example.data.AppDatabase
import com.example.data.OfflineDictionaryDatabase
import com.example.data.WordEntity
import com.example.data.WordRepository
import com.example.ui.DictionaryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfflineDictionaryIntegrationTest {

    private val failingGeminiService = object : GeminiApiService {
        override suspend fun generateContent(apiKey: String, request: GenerateContentRequest): GenerateContentResponse {
            throw IllegalStateException("Network should not be called for offline words")
        }

        override suspend fun generateContentWithModel(
            model: String,
            apiKey: String,
            request: GenerateContentRequest
        ): GenerateContentResponse {
            throw IllegalStateException("Network should not be called for offline words")
        }
    }

    @Test
    fun `offline sqlite asset contains 6 Essential Books, 3 IELTS Books, 26 Topics, and 87K dictionary`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val offlineDb = OfflineDictionaryDatabase.getInstance(context)

        assertTrue("Offline dictionary DB should be available from assets", offlineDb.isAvailable())

        val books = offlineDb.getAllBooks()
        assertEquals(9, books.size)
        assertEquals(6, books.count { it.category == "essential" })
        assertEquals(3, books.count { it.category == "ielts" })
        assertEquals(9591, books.sumOf { it.wordCount })

        val barronBook = books.first { it.bookNum == 7 }
        assertTrue(barronBook.title.contains("Barron", ignoreCase = true))
        assertEquals(750, barronBook.wordCount)

        val listeningBook = books.first { it.bookNum == 8 }
        assertEquals(918, listeningBook.wordCount)

        val ieltsBankBook = books.first { it.bookNum == 9 }
        assertEquals(4323, ieltsBankBook.wordCount)

        val topics = offlineDb.getAllTopics()
        assertEquals(26, topics.size)

        val arGeneral = offlineDb.getGeneralArabicTranslation("serendipity")
        assertNotNull("Expected 87K dictionary entry for serendipity", arGeneral)
        assertTrue(arGeneral!!.isNotBlank())
    }

    @Test
    fun `searchAndFetchWord serves Essential Books, IELTS Books, and 87K Arabic dictionary offline`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = WordRepository(
                wordDao = db.wordDao(),
                apiService = failingGeminiService,
                context = context
            )

            // 1. Book 1 word ("agree")
            val agreeRes = repo.searchAndFetchWord("agree")
            assertTrue("Expected offline success for 'agree'", agreeRes.isSuccess)
            val agree = agreeRes.getOrThrow()
            assertEquals("agree", agree.word)
            assertEquals("يوافق", agree.arabicMeaning)
            assertTrue(agree.definition.contains("same opinion", ignoreCase = true))
            assertTrue(agree.tags.contains("Book 1"))
            assertTrue(agree.tags.contains("Essential 6 Books"))

            // 2. Barron's IELTS Book 7 word ("aquatic")
            val aquaticRes = repo.searchAndFetchWord("aquatic")
            assertTrue("Expected offline success for IELTS word 'aquatic'", aquaticRes.isSuccess)
            val aquatic = aquaticRes.getOrThrow()
            assertEquals("aquatic", aquatic.word)
            assertTrue(aquatic.arabicMeaning.isNotBlank())
            assertTrue(aquatic.tags.contains("IELTS"))
            assertTrue(aquatic.tags.contains("Barron's IELTS"))

            // 3. General 87K dictionary fallback (no API keys configured)
            val generalRes = repo.searchAndFetchWord("serendipity")
            assertTrue("Expected 87K offline dictionary fallback for 'serendipity'", generalRes.isSuccess)
            val serendipity = generalRes.getOrThrow()
            assertTrue(serendipity.arabicMeaning.isNotBlank())
            assertTrue(serendipity.tags.contains("Offline Dictionary"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `offline autocomplete supports both English prefix and Arabic meaning search`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val offlineDb = OfflineDictionaryDatabase.getInstance(context)

        val englishSuggestions = offlineDb.searchSuggestions("aqua", limit = 10)
        assertTrue(englishSuggestions.any { it.word.equals("aquatic", ignoreCase = true) })

        val arabicSuggestions = offlineDb.searchSuggestions("يوافق", limit = 10)
        assertTrue(arabicSuggestions.any { it.word.equals("agree", ignoreCase = true) })
    }

    @Test
    fun `addCurriculumWordsToDeck imports IELTS unit batch with Anki tags and preserves existing FSRS progress`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = WordRepository(
                wordDao = db.wordDao(),
                apiService = failingGeminiService,
                context = context
            )
            val unit1Words = repo.getOfflineWordsForUnit(bookNum = 7, unitNum = 1)
            assertTrue(unit1Words.size >= 10)

            val firstWord = unit1Words.first()
            // Pre-insert first word with existing FSRS study history
            db.wordDao().insertWord(
                WordEntity(
                    word = firstWord.word.lowercase(),
                    definition = firstWord.definitionEn,
                    isBookmarked = true,
                    repetitions = 5,
                    interval = 28,
                    distinctReviewDays = 4,
                    fsrsStability = 31.5,
                    fsrsDifficulty = 4.2,
                    tags = "CustomTag"
                )
            )

            // Batch add first 10 words from Barron's IELTS Unit 1
            val addedCount = repo.addCurriculumWordsToDeck(unit1Words.take(10))
            assertEquals("9 new words should be added (1st was already bookmarked)", 9, addedCount)

            val preserved = db.wordDao().getWord(firstWord.word.lowercase())
            assertNotNull(preserved)
            assertEquals("Existing FSRS repetitions must be preserved", 5, preserved!!.repetitions)
            assertEquals("Existing interval must be preserved", 28, preserved.interval)
            assertEquals(31.5, preserved.fsrsStability!!, 0.001)
            assertTrue(preserved.tags.contains("CustomTag"))
            assertTrue(preserved.tags.contains("IELTS"))
            assertTrue(preserved.tags.contains("Barron's IELTS"))
            assertTrue(preserved.tags.contains("Barron's IELTS - U1"))
            assertTrue(preserved.arabicMeaning.isNotBlank())
        } finally {
            db.close()
        }
    }

    @Test
    fun `forceAiEnrich adds synonyms and mnemonic while preserving bookmark, FSRS stats, and Arabic meaning`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val mockGemini = object : GeminiApiService {
            override suspend fun generateContent(apiKey: String, request: GenerateContentRequest) = buildResponse()
            override suspend fun generateContentWithModel(model: String, apiKey: String, request: GenerateContentRequest) = buildResponse()
            private fun buildResponse() = GenerateContentResponse(
                candidates = listOf(
                    Candidate(
                        content = Content(
                            parts = listOf(
                                Part(
                                    text = """
                                        {
                                          "word": "aquatic",
                                          "phonetic": "/əˈkwæt.ɪk/",
                                          "partOfSpeech": "adjective",
                                          "definition": "Relating to water; living or growing in water.",
                                          "synonyms": ["marine", "watery", "oceanic"],
                                          "antonyms": ["terrestrial"],
                                          "example": "Dolphins are intelligent aquatic mammals.",
                                          "mnemonic": "Aqua means water — aquatic lives in aqua!",
                                          "imagePrompt": ""
                                        }
                                    """.trimIndent()
                                )
                            )
                        )
                    )
                )
            )
        }
        try {
            val repo = WordRepository(
                wordDao = db.wordDao(),
                apiService = mockGemini,
                context = context
            )
            // First fetch offline
            val offline = repo.searchAndFetchWord("aquatic").getOrThrow()
            // Bookmark & simulate FSRS progress
            db.wordDao().insertWord(
                offline.copy(
                    isBookmarked = true,
                    repetitions = 3,
                    interval = 12,
                    fsrsStability = 14.0
                )
            )

            // Enrich with AI
            val enriched = repo.searchAndFetchWord(
                word = "aquatic",
                geminiApiKeys = listOf("test-valid-key"),
                aiVisualizationsEnabled = false,
                forceAiEnrich = true
            ).getOrThrow()

            assertTrue(enriched.isBookmarked)
            assertEquals(3, enriched.repetitions)
            assertEquals(12, enriched.interval)
            assertEquals(14.0, enriched.fsrsStability!!, 0.001)
            assertTrue(enriched.synonyms.contains("marine"))
            assertTrue(enriched.mnemonic.contains("Aqua"))
            assertTrue(enriched.arabicMeaning.isNotBlank())
            assertTrue(enriched.tags.contains("IELTS"))
        } finally {
            db.close()
        }
    }

    @Test
    fun `previewReviewIntervals returns Anki interval labels for all 4 FSRS ratings`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = WordRepository(
                wordDao = db.wordDao(),
                apiService = failingGeminiService,
                context = app
            )
            val vm = DictionaryViewModel(app, repo)
            val card = WordEntity(word = "aquatic", definition = "living in water", isBookmarked = true)
            val previews = vm.previewReviewIntervals(card)
            assertEquals(4, previews.size)
            listOf("Again", "Hard", "Good", "Easy").forEach { key ->
                val value = previews[key]
                assertNotNull("Expected interval preview for $key", value)
                assertFalse("Interval preview for $key should not be blank", value.isNullOrBlank())
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `TtsVoiceSelector prefers Google Offline Neural UK local voice over network and US voices`() {
        val ukLocalNeural = com.example.ui.TtsVoiceSelector.scoreVoiceForAccent(
            name = "en-gb-x-gba-local",
            localeLanguage = "en",
            localeCountry = "GB",
            isNetworkRequired = false,
            quality = android.speech.tts.Voice.QUALITY_HIGH,
            targetAccent = "UK"
        )
        val ukNetworkVoice = com.example.ui.TtsVoiceSelector.scoreVoiceForAccent(
            name = "en-gb-x-gba-network",
            localeLanguage = "en",
            localeCountry = "GB",
            isNetworkRequired = true,
            quality = android.speech.tts.Voice.QUALITY_VERY_HIGH,
            targetAccent = "UK"
        )
        val usLocalVoice = com.example.ui.TtsVoiceSelector.scoreVoiceForAccent(
            name = "en-us-x-tpf-local",
            localeLanguage = "en",
            localeCountry = "US",
            isNetworkRequired = false,
            quality = android.speech.tts.Voice.QUALITY_HIGH,
            targetAccent = "UK"
        )
        val arabicVoice = com.example.ui.TtsVoiceSelector.scoreVoiceForAccent(
            name = "ar-xa-x-arz-local",
            localeLanguage = "ar",
            localeCountry = "EG",
            isNetworkRequired = false,
            quality = android.speech.tts.Voice.QUALITY_HIGH,
            targetAccent = "UK"
        )

        assertTrue("Offline UK Neural must beat network UK voice", ukLocalNeural > ukNetworkVoice)
        assertTrue("Offline UK Neural must beat US voice when UK accent selected", ukLocalNeural > usLocalVoice)
        assertEquals("Non-English voices should be excluded", -1, arabicVoice)
        assertEquals(
            "Google UK Neural Female 1 (Offline)",
            com.example.ui.TtsVoiceSelector.formatFriendlyVoiceName("en-gb-x-gba-local", "GB", true)
        )
    }

    @Test
    fun `selectedAccent defaults to UK and persists US switch across ViewModel instances`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = WordRepository(
                wordDao = db.wordDao(),
                apiService = failingGeminiService,
                context = app
            )
            val vm1 = DictionaryViewModel(app, repo)
            assertEquals("UK", vm1.selectedAccent.value)
            vm1.setSelectedAccent("US")
            assertEquals("US", vm1.selectedAccent.value)

            val vm2 = DictionaryViewModel(app, repo)
            assertEquals("US", vm2.selectedAccent.value)
            vm2.setSelectedAccent("UK")
        } finally {
            db.close()
        }
    }

    @Test
    fun `v2 offline database has zero template definitions or examples, zero broken HTML, and 100 percent Arabic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val offlineDb = OfflineDictionaryDatabase.getInstance(context)
        assertTrue(offlineDb.isAvailable())

        val arabicRegex = Regex("[\u0600-\u06FF]")
        var totalChecked = 0
        for (book in offlineDb.getAllBooks()) {
            val units = offlineDb.getUnitsForBook(book.bookNum)
            for (unit in units) {
                val words = offlineDb.getWordsForUnit(book.bookNum, unit.unitNum)
                for (w in words) {
                    totalChecked++
                    assertTrue(
                        "Empty/too-short definition for Book ${book.bookNum} '${w.word}': '${w.definitionEn}'",
                        w.definitionEn.trim().length >= 8
                    )
                    assertFalse(
                        "Template definition found for '${w.word}': '${w.definitionEn}'",
                        w.definitionEn.contains("Key IELTS listening vocabulary", ignoreCase = true)
                    )
                    assertTrue(
                        "Empty/too-short example for Book ${book.bookNum} '${w.word}': '${w.exampleEn}'",
                        w.exampleEn.trim().length >= 10
                    )
                    assertFalse(
                        "Template example found for '${w.word}': '${w.exampleEn}'",
                        w.exampleEn.contains("frequently tested in IELTS", ignoreCase = true) ||
                            w.exampleEn.contains("essential academic term for IELTS mastery", ignoreCase = true)
                    )
                    assertTrue(
                        "Missing Arabic characters for '${w.word}': '${w.translationAr}'",
                        arabicRegex.containsMatchIn(w.translationAr)
                    )
                    assertFalse(
                        "Broken HTML/markup in '${w.word}'",
                        w.definitionEn.contains("<") || w.definitionEn.contains(">") || w.exampleEn.contains("<")
                    )
                }
            }
        }
        assertEquals(9591, totalChecked)
    }

    @Test
    fun `smart offline search supports diacritic-free Arabic, English stems, fuzzy Did-You-Mean, and Word of the Day`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val offlineDb = OfflineDictionaryDatabase.getInstance(context)

        // Arabic without tashkeel/hamza finds words whose DB translation has tashkeel (e.g. "يتخلّى" / "التّرك")
        val arNoTashkeel = offlineDb.searchSuggestions("يتخلى", limit = 10)
        assertTrue("Expected 'abandon' for normalized Arabic 'يتخلى'", arNoTashkeel.any { it.word.equals("abandon", ignoreCase = true) })

        val arAlTark = offlineDb.searchSuggestions("الترك", limit = 10)
        assertTrue("Expected match for normalized Arabic 'الترك'", arAlTark.isNotEmpty())

        // English inflected stem finds base curriculum word
        val stemHits = offlineDb.searchSuggestions("abandoned", limit = 10)
        assertTrue("Expected stem 'abandon' when searching 'abandoned'", stemHits.any { it.word.equals("abandon", ignoreCase = true) })

        // Fuzzy Did-You-Mean catches 1-char typo
        val fuzzyHits = offlineDb.findFuzzySuggestions("abndon", limit = 5)
        assertTrue("Expected fuzzy 'abandon' for typo 'abndon'", fuzzyHits.any { it.word.equals("abandon", ignoreCase = true) })

        // Deterministic Word of the Day + CEFR browse
        val wotd1 = offlineDb.getWordOfTheDay(20_500L)
        val wotd2 = offlineDb.getWordOfTheDay(20_500L)
        assertNotNull(wotd1)
        assertEquals(wotd1!!.word, wotd2!!.word)

        val a1Words = offlineDb.getWordsByCefr("A1", limit = 15)
        assertTrue("Expected A1 curriculum words", a1Words.size >= 10)
    }

    @Test
    fun `LocalWordImageStore serves bundled WebP images offline and persists new downloaded assets permanently`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = com.example.data.LocalWordImageStore.getInstance(context)

        val bundledCount = store.getBundledImageCount()
        assertTrue("Expected ~3,586 bundled Essential Book WebP images, got $bundledCount", bundledCount >= 3500)
        assertTrue("Expected bundled offline photo for 'agree'", store.hasBundledImage("agree"))
        assertEquals(com.example.data.LocalImageOrigin.BUNDLED_OFFLINE, store.getLocalImageOrigin("agree"))
        val agreeBytes = store.getBundledImageBytes("agree")
        assertNotNull(agreeBytes)
        assertTrue("Bundled WebP bytes should be valid size", agreeBytes!!.size > 500)

        // Simulate saving a newly downloaded photo permanently for a word with no bundled image
        val testWord = "custom_offline_asset_test"
        store.deleteSavedPermanentImage(testWord)
        assertEquals(com.example.data.LocalImageOrigin.NONE, store.getLocalImageOrigin(testWord))

        val savedFile = store.saveImageBytesPermanently(testWord, agreeBytes)
        assertNotNull(savedFile)
        assertTrue(savedFile!!.exists())
        assertTrue(store.hasSavedPermanentImage(testWord))
        assertEquals(com.example.data.LocalImageOrigin.SAVED_PERMANENT, store.getLocalImageOrigin(testWord))

        // Clean up test artifact
        store.deleteSavedPermanentImage(testWord)
    }

    @Test
    fun `unified study session rate and undo restores exact Room word and review log state`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = WordRepository(db.wordDao(), failingGeminiService, context = app)
            val today = java.time.LocalDate.now().toEpochDay()
            val w1 = WordEntity(word = "agree", definition = "To agree", arabicMeaning = "يوافق", isBookmarked = true, dueDate = today)
            val w2 = WordEntity(word = "arrive", definition = "To arrive", arabicMeaning = "يصل", isBookmarked = true, dueDate = today)
            db.wordDao().insertWord(w1)
            db.wordDao().insertWord(w2)

            val vm = DictionaryViewModel(app, repo)
            vm.startUnifiedStudySession()
            var tries = 0
            while (vm.activeStudySession.value == null && tries < 60) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(15)
                tries++
            }
            val initial = vm.activeStudySession.value
            assertNotNull("Expected active study session to start", initial)
            assertEquals(2, initial!!.queue.size)
            val firstWordName = initial.currentCard!!.word

            // Rate Good
            vm.rateSessionCard("Good")
            tries = 0
            while ((vm.activeStudySession.value?.completedCount ?: 0) < 1 && tries < 60) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(15)
                tries++
            }
            assertEquals(1, vm.activeStudySession.value!!.completedCount)
            assertTrue(vm.activeStudySession.value!!.canUndo)
            val logsAfterRate = db.wordDao().getReviewLogsForWordList(firstWordName)
            assertEquals(1, logsAfterRate.size)

            // Undo!
            vm.undoLastSessionRating()
            tries = 0
            while ((vm.activeStudySession.value?.completedCount ?: 1) != 0 && tries < 60) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(15)
                tries++
            }
            assertEquals(0, vm.activeStudySession.value!!.completedCount)
            assertEquals(firstWordName, vm.activeStudySession.value!!.currentCard!!.word)
            val restoredWord = db.wordDao().getWord(firstWordName)!!
            assertEquals("Repetitions must revert to 0 after Undo", 0, restoredWord.repetitions)
            val logsAfterUndo = db.wordDao().getReviewLogsForWordList(firstWordName)
            assertEquals("Inserted review log must be removed after Undo", 0, logsAfterUndo.size)
        } finally {
            db.close()
        }
    }
}

