package com.example

import android.app.Application
import android.content.Context
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
}
