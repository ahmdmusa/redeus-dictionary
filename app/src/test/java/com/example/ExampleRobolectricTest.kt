package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.api.GeminiApiService
import com.example.data.AppDatabase
import com.example.data.ReviewLogEntity
import com.example.data.WordEntity
import com.example.data.WordRepository
import com.example.ui.DictionaryViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ui.DailyReadingUiState
import com.example.ui.ReadingPhase
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Redeus Dictionary", appName)
  }

  @Test
  fun `verify image_source preference default and update`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    // Default should be "ai"
    val defaultSource = sharedPrefs.getString("image_source", "ai") ?: "ai"
    assertEquals("ai", defaultSource)

    // Update to "web"
    sharedPrefs.edit().putString("image_source", "web").apply()
    val updatedSource = sharedPrefs.getString("image_source", "ai") ?: "ai"
    assertEquals("web", updatedSource)
  }

  @Test
  fun `verify reading_level preference default and persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Default reading_level should be 1
    val defaultLevel = sharedPrefs.getInt("reading_level", 1)
    assertEquals(1, defaultLevel)

    // Update reading_level to 3
    sharedPrefs.edit().putInt("reading_level", 3).apply()
    val updatedLevel = sharedPrefs.getInt("reading_level", 1)
    assertEquals(3, updatedLevel)
  }

  @Test
  fun `verify daily_reading_date epoch day caching`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val todayEpoch = java.time.LocalDate.now().toEpochDay()
    sharedPrefs.edit().putLong("daily_reading_date", todayEpoch).apply()

    val cachedDate = sharedPrefs.getLong("daily_reading_date", -1L)
    assertEquals(todayEpoch, cachedDate)
  }

  @Test
  fun `verify user_english_level preference and CEFR mapping`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val defaultLevel = sharedPrefs.getString("user_english_level", "A2") ?: "A2"
    assertEquals("A2", defaultLevel)

    sharedPrefs.edit().putString("user_english_level", "B2").apply()
    val updatedLevel = sharedPrefs.getString("user_english_level", "A2") ?: "A2"
    assertEquals("B2", updatedLevel)
  }

  @Test
  fun `verify reading words last used tracking persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val json = org.json.JSONObject()
    val now = System.currentTimeMillis()
    json.put("ephemeral", now)
    json.put("ubiquitous", now)
    sharedPrefs.edit().putString("reading_words_last_used", json.toString()).apply()

    val retrievedJsonStr = sharedPrefs.getString("reading_words_last_used", null)
    val parsedJson = org.json.JSONObject(retrievedJsonStr!!)
    assertEquals(now, parsedJson.getLong("ephemeral"))
    assertEquals(now, parsedJson.getLong("ubiquitous"))
  }

  @Test
  fun `verify smoothed adaptive reading level progression logic`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Initial state: Level 2
    var currentLevel = 2
    sharedPrefs.edit().putInt("reading_level", currentLevel).remove("prev_reading_outcome").apply()

    // Session 1: High score (5/5 = 1.0 >= 0.8)
    // First session should NOT change level, but record outcome as HIGH
    var prevOutcome = sharedPrefs.getString("prev_reading_outcome", null)
    var sessionRatio = 5f / 5f
    var currentOutcome = if (sessionRatio >= 0.8f) "HIGH" else if (sessionRatio <= 0.4f) "LOW" else "NEUTRAL"
    var newLevel = when {
      prevOutcome == "HIGH" && currentOutcome == "HIGH" -> (currentLevel + 1).coerceAtMost(5)
      prevOutcome == "LOW" && currentOutcome == "LOW" -> (currentLevel - 1).coerceAtLeast(1)
      else -> currentLevel
    }
    assertEquals(2, newLevel) // Unchanged after 1 session
    sharedPrefs.edit().putString("prev_reading_outcome", currentOutcome).apply()

    // Session 2: High score again (4/5 = 0.8 >= 0.8)
    // Second consecutive HIGH session MUST increment level to 3
    prevOutcome = sharedPrefs.getString("prev_reading_outcome", null)
    sessionRatio = 4f / 5f
    currentOutcome = if (sessionRatio >= 0.8f) "HIGH" else if (sessionRatio <= 0.4f) "LOW" else "NEUTRAL"
    newLevel = when {
      prevOutcome == "HIGH" && currentOutcome == "HIGH" -> (currentLevel + 1).coerceAtMost(5)
      prevOutcome == "LOW" && currentOutcome == "LOW" -> (currentLevel - 1).coerceAtLeast(1)
      else -> currentLevel
    }
    assertEquals(3, newLevel) // Incremented after 2 consecutive sessions
  }

  @Test
  fun `verify WordEntity lastUsedInReadingDate field`() {
    val now = System.currentTimeMillis()
    val word = WordEntity(
      word = "serendipity",
      phonetic = "/ˌsɛr.ənˈdɪp.ə.ti/",
      partOfSpeech = "noun",
      definition = "Finding valuable things not sought for",
      synonyms = "chance, fluke",
      antonyms = "misfortune",
      example = "It was pure serendipity.",
      mnemonic = "Serena dips into tea happily",
      imagePrompt = "A cozy cup of tea",
      lastUsedInReadingDate = now
    )
    assertEquals(now, word.lastUsedInReadingDate)
  }

  @Test
  fun `verify OpenRouterChatRequest model defaults to openrouter free`() {
    val req = com.example.api.OpenRouterChatRequest(
      messages = listOf(
        com.example.api.OpenRouterMessage(role = "user", content = "Hello world")
      )
    )
    assertEquals("openrouter/free", req.model)
    assertEquals("user", req.messages.first().role)
    assertEquals("Hello world", req.messages.first().content)
  }

  @Test
  fun `verify OpenRouterChatResponse extraction`() {
    val moshi = com.squareup.moshi.Moshi.Builder()
      .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
      .build()
    val adapter = moshi.adapter(com.example.api.OpenRouterChatResponse::class.java)

    val json = """
      {
        "choices": [
          {
            "message": {
              "role": "assistant",
              "content": "{\"word\":\"test\",\"definition\":\"a test definition\"}"
            }
          }
        ]
      }
    """.trimIndent()

    val parsed = adapter.fromJson(json)
    val content = parsed?.choices?.firstOrNull()?.message?.content
    assertEquals("{\"word\":\"test\",\"definition\":\"a test definition\"}", content)
  }

  @Test
  fun `verify AI usage tracking records gemini and openrouter calls with daily reset`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Simulate yesterday's calls
    val yesterday = java.time.LocalDate.now().toEpochDay() - 1
    prefs.edit()
      .putLong("usage_stats_date", yesterday)
      .putInt("gemini_calls_today", 5)
      .putInt("openrouter_calls_today", 3)
      .putString("last_provider_used", "openrouter")
      .commit()

    val database = AppDatabase.getDatabase(context)
    val repository = WordRepository(
      wordDao = database.wordDao(),
      apiService = GeminiApiService.create(),
      context = context
    )

    // First call today from Gemini should reset counters and increment gemini
    repository.recordAiProviderUsage("gemini")

    val today = java.time.LocalDate.now().toEpochDay()
    assertEquals(today, prefs.getLong("usage_stats_date", -1L))
    assertEquals(1, prefs.getInt("gemini_calls_today", -1))
    assertEquals(0, prefs.getInt("openrouter_calls_today", -1))
    assertEquals("gemini", prefs.getString("last_provider_used", null))

    // Second call today from OpenRouter fallback
    repository.recordAiProviderUsage("openrouter")
    assertEquals(1, prefs.getInt("gemini_calls_today", -1))
    assertEquals(1, prefs.getInt("openrouter_calls_today", -1))
    assertEquals("openrouter", prefs.getString("last_provider_used", null))

    // Third call today from Gemini
    repository.recordAiProviderUsage("gemini")
    assertEquals(2, prefs.getInt("gemini_calls_today", -1))
    assertEquals(1, prefs.getInt("openrouter_calls_today", -1))
    assertEquals("gemini", prefs.getString("last_provider_used", null))

    // Verify DictionaryViewModel.getUsageStats()
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DictionaryViewModel(app, repository)
    val stats = viewModel.getUsageStats()
    assertEquals(2, stats.geminiCallsToday)
    assertEquals(1, stats.openRouterCallsToday)
    assertEquals("gemini", stats.lastProviderUsed)
  }

  @Test
  fun `verify DictionaryViewModel getUsageStats returns zero when date is outdated`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val oldDate = java.time.LocalDate.now().toEpochDay() - 2
    prefs.edit()
      .putLong("usage_stats_date", oldDate)
      .putInt("gemini_calls_today", 10)
      .putInt("openrouter_calls_today", 4)
      .putString("last_provider_used", "openrouter")
      .commit()

    val database = AppDatabase.getDatabase(context)
    val repository = WordRepository(
      wordDao = database.wordDao(),
      apiService = GeminiApiService.create(),
      context = context
    )

    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DictionaryViewModel(app, repository)
    val stats = viewModel.getUsageStats()
    assertEquals(0, stats.geminiCallsToday)
    assertEquals(0, stats.openRouterCallsToday)
  }

  @Test
  fun `verify gemini manual lock persistence and auto reset`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val database = AppDatabase.getDatabase(context)
    val repository = WordRepository(
      wordDao = database.wordDao(),
      apiService = GeminiApiService.create(),
      context = context
    )
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = DictionaryViewModel(app, repository)

    val today = java.time.LocalDate.now().toEpochDay()

    // Initially false
    assertEquals(false, viewModel.getGeminiLocked())
    assertEquals(false, repository.isGeminiManuallyLocked())

    // Set locked to true
    viewModel.setGeminiLocked(true)
    assertEquals(true, viewModel.getGeminiLocked())
    assertEquals(true, repository.isGeminiManuallyLocked())
    assertEquals(today, prefs.getLong("gemini_locked_date", -1L))
    assertEquals(true, prefs.getBoolean("gemini_manually_locked", false))

    // Set locked to false
    viewModel.setGeminiLocked(false)
    assertEquals(false, viewModel.getGeminiLocked())
    assertEquals(false, repository.isGeminiManuallyLocked())

    // Set locked to true, then simulate date change to yesterday
    viewModel.setGeminiLocked(true)
    assertEquals(true, viewModel.getGeminiLocked())
    prefs.edit().putLong("gemini_locked_date", today - 1).commit()

    // Auto-reset when checked on new day
    assertEquals(false, repository.isGeminiManuallyLocked())
    assertEquals(false, viewModel.getGeminiLocked())
    assertEquals(false, prefs.getBoolean("gemini_manually_locked", true))
    assertEquals(today, prefs.getLong("gemini_locked_date", -1L))
  }

  @Test
  fun `verify extractPexelsKeywords extracts concrete visual keywords and ignores stop words`() {
    // 1. Concrete scene describing veterinarian
    val prompt1 = "A veterinarian gently bandaging the paw of an injured golden retriever on a stainless steel exam table."
    val result1 = WordRepository.extractPexelsKeywords(prompt1)
    assertEquals("veterinarian bandaging paw", result1)

    // 2. Prompt containing photography and scene filler words
    val prompt2 = "A close-up realistic photograph showing a red apple on a rustic wooden table."
    val result2 = WordRepository.extractPexelsKeywords(prompt2)
    assertEquals("red apple rustic", result2)

    // 3. Short prompt with 2-3 keywords
    val prompt3 = "Happy children playing outdoors."
    val result3 = WordRepository.extractPexelsKeywords(prompt3)
    assertEquals("happy children playing", result3)

    // 4. Blank prompt returns empty string (never raw word or example)
    val resultBlank = WordRepository.extractPexelsKeywords("")
    assertEquals("", resultBlank)

    // 5. Prompt with only stop words returns empty string (never raw word or example)
    val promptStopWords = "In and of the with it for on"
    val resultStop = WordRepository.extractPexelsKeywords(promptStopWords)
    assertEquals("", resultStop)
  }

  @Test
  fun `verify Pexels search query is built ONLY from imagePrompt and never from example or raw word`() {
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(context)
    var capturedQuery: String? = null

    val fakePexelsService = object : com.example.api.PexelsApiService {
      override suspend fun searchPhotos(
        apiKey: String,
        query: String,
        perPage: Int,
        page: Int
      ): com.example.api.PexelsSearchResponse {
        capturedQuery = query
        return com.example.api.PexelsSearchResponse(
          page = page,
          perPage = 1,
          photos = listOf(
            com.example.api.PexelsPhoto(
              id = 999L,
              src = com.example.api.PexelsPhotoSrc(
                medium = "https://images.pexels.com/photos/999/test.jpg"
              )
            )
          )
        )
      }
    }

    val repository = WordRepository(db.wordDao(), GeminiApiService.create(), pexelsApiService = fakePexelsService, context = context)
    val viewModel = DictionaryViewModel(context, repository)

    val word = WordEntity(
      word = "subpoena",
      phonetic = "/səˈpiː.nə/",
      partOfSpeech = "noun",
      definition = "A writ ordering a person to attend a court.",
      synonyms = "summons, writ, warrant",
      antonyms = "",
      example = "She spent hours finding deleted texts from his phone to present to the judge.",
      mnemonic = "Subpoena: sounds like sub-penalty.",
      imagePrompt = "A legal officer in a suit delivering an official stamped courtroom document."
    )
    db.wordDao().insertWord(word)

    viewModel.cycleNextPhoto(word)
    waitForPhotoCycle(viewModel)

    assertNotNull(capturedQuery)
    // 1. Must NOT contain words from example sentence (e.g. "finding", "deleted", "texts", "phone")
    assertFalse(capturedQuery!!.contains("finding"))
    assertFalse(capturedQuery!!.contains("deleted"))
    assertFalse(capturedQuery!!.contains("texts"))
    assertFalse(capturedQuery!!.contains("phone"))
    // 2. Must NOT be the raw word alone
    assertFalse(capturedQuery!!.equals("subpoena", ignoreCase = true))
    // 3. Must be derived ONLY from imagePrompt
    assertTrue(
      capturedQuery!!.contains("legal") ||
      capturedQuery!!.contains("officer") ||
      capturedQuery!!.contains("suit") ||
      capturedQuery!!.contains("courtroom") ||
      capturedQuery!!.contains("stamped")
    )
    }
  }

  private fun waitForPhotoCycle(viewModel: DictionaryViewModel) {
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    var attempts = 0
    while (viewModel.cyclingPhotoWord.value != null && attempts < 100) {
      Thread.sleep(20)
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      attempts++
    }
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
  }

  @Test
  fun `verify wordPhotoPageMap tracks pagination per word and increments`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(context)
    val fakePexelsService = object : com.example.api.PexelsApiService {
      var requestedPage = 1
      override suspend fun searchPhotos(
        apiKey: String,
        query: String,
        perPage: Int,
        page: Int
      ): com.example.api.PexelsSearchResponse {
        requestedPage = page
        return com.example.api.PexelsSearchResponse(
          page = page,
          perPage = 1,
          photos = listOf(
            com.example.api.PexelsPhoto(
              id = page.toLong(),
              src = com.example.api.PexelsPhotoSrc(
                medium = "https://images.pexels.com/photos/123/medium_$page.jpg"
              )
            )
          )
        )
      }
    }
    val repository = WordRepository(db.wordDao(), GeminiApiService.create(), pexelsApiService = fakePexelsService)
    val viewModel = DictionaryViewModel(context, repository)

    val word = WordEntity(
      word = "solitude",
      phonetic = "/ˈsɒl.ɪ.tʃuːd/",
      partOfSpeech = "noun",
      definition = "The state of being alone",
      synonyms = "peace, calm",
      antonyms = "company",
      example = "He enjoyed the solitude of the forest",
      mnemonic = "Solo attitude",
      imagePrompt = "A quiet forest path at dawn"
    )
    db.wordDao().insertWord(word)

    // Initial page starts at default 1
    assertEquals(1, viewModel.getWordPhotoPage("solitude"))

    // First cycle -> page 2
    viewModel.cycleNextPhoto(word)
    waitForPhotoCycle(viewModel)
    assertEquals(2, viewModel.getWordPhotoPage("solitude"))

    // Check DB updated
    val updatedWord = db.wordDao().getWord("solitude")
    assertEquals("https://images.pexels.com/photos/123/medium_2.jpg", updatedWord?.imagePrompt)

    // Second cycle -> page 3
    viewModel.cycleNextPhoto(word)
    waitForPhotoCycle(viewModel)
    assertEquals(3, viewModel.getWordPhotoPage("solitude"))
    val updatedWord2 = db.wordDao().getWord("solitude")
    assertEquals("https://images.pexels.com/photos/123/medium_3.jpg", updatedWord2?.imagePrompt)
  }

  @Test
  fun `verify cycleNextPhoto wraps to page 1 and emits wrap message when no more results`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(context)
    val fakePexelsService = object : com.example.api.PexelsApiService {
      override suspend fun searchPhotos(
        apiKey: String,
        query: String,
        perPage: Int,
        page: Int
      ): com.example.api.PexelsSearchResponse {
        return if (page == 1) {
          com.example.api.PexelsSearchResponse(
            page = 1,
            perPage = 1,
            photos = listOf(
              com.example.api.PexelsPhoto(
                id = 1L,
                src = com.example.api.PexelsPhotoSrc(
                  medium = "https://images.pexels.com/photos/123/first_photo.jpg"
                )
              )
            )
          )
        } else {
          // Page > 1 has no photos
          com.example.api.PexelsSearchResponse(
            page = page,
            perPage = 1,
            photos = emptyList()
          )
        }
      }
    }
    val repository = WordRepository(db.wordDao(), GeminiApiService.create(), pexelsApiService = fakePexelsService, context = context)
    val viewModel = DictionaryViewModel(context, repository)

    val word = WordEntity(
      word = "epiphany",
      phonetic = "/ɪˈpɪf.ən.i/",
      partOfSpeech = "noun",
      definition = "A moment of sudden revelation",
      synonyms = "revelation, insight",
      antonyms = "confusion",
      example = "He had an epiphany",
      mnemonic = "Epic funny moment",
      imagePrompt = "A glowing lightbulb of sudden revelation and insight"
    )
    db.wordDao().insertWord(word)

    // Cycle when next page (2) has no results -> wraps to page 1
    viewModel.cycleNextPhoto(word)
    waitForPhotoCycle(viewModel)

    assertEquals(1, viewModel.getWordPhotoPage("epiphany"))
    val updatedWord = db.wordDao().getWord("epiphany")
    assertEquals("https://images.pexels.com/photos/123/first_photo.jpg", updatedWord?.imagePrompt)
    assertEquals("Back to the first result for 'epiphany'", viewModel.snackbarMessage.replayCache.lastOrNull())
  }

  @Test
  fun `verify cycleNextPhoto error does not clear existing image`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(context)
    val fakePexelsService = object : com.example.api.PexelsApiService {
      override suspend fun searchPhotos(
        apiKey: String,
        query: String,
        perPage: Int,
        page: Int
      ): com.example.api.PexelsSearchResponse {
        throw java.io.IOException("Network connection lost")
      }
    }
    val repository = WordRepository(db.wordDao(), GeminiApiService.create(), pexelsApiService = fakePexelsService, context = context)
    val viewModel = DictionaryViewModel(context, repository)

    // Pre-cache the Gemini-generated visual scene prompt for resilience
    context.getSharedPreferences("word_image_prompts", Context.MODE_PRIVATE).edit()
      .putString("ai_prompt_resilience", "A strong green sprout pushing through heavy rocks")
      .apply()

    val originalPrompt = "https://images.pexels.com/photos/keep_this.jpg"
    val word = WordEntity(
      word = "resilience",
      phonetic = "/rɪˈzɪl.jəns/",
      partOfSpeech = "noun",
      definition = "The capacity to recover quickly",
      synonyms = "toughness",
      antonyms = "fragility",
      example = "Great emotional resilience",
      mnemonic = "Re-silience silence",
      imagePrompt = originalPrompt
    )
    db.wordDao().insertWord(word)

    viewModel.cycleNextPhoto(word)
    waitForPhotoCycle(viewModel)

    val updatedWord = db.wordDao().getWord("resilience")
    // Existing photo prompt must NOT be cleared or overwritten
    assertEquals(originalPrompt, updatedWord?.imagePrompt)
    assertEquals("Failed to load new photo. Please check your connection.", viewModel.snackbarMessage.replayCache.lastOrNull())
  }

  @Test
  fun `verify pexels_query_source preference default and updates`() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(context)
    val repository = WordRepository(db.wordDao(), GeminiApiService.create(), context = context)
    val viewModel = DictionaryViewModel(context, repository)

    // Default must be "image_prompt"
    assertEquals("image_prompt", viewModel.getPexelsQuerySource())
    assertEquals("image_prompt", viewModel.pexelsQuerySource.value)

    // Update to "word"
    viewModel.setPexelsQuerySource("word")
    assertEquals("word", viewModel.getPexelsQuerySource())
    assertEquals("word", viewModel.pexelsQuerySource.value)
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    assertEquals("word", sharedPrefs.getString("pexels_query_source", null))

    // Update to "definition"
    viewModel.setPexelsQuerySource("definition")
    assertEquals("definition", viewModel.getPexelsQuerySource())
    assertEquals("definition", viewModel.pexelsQuerySource.value)
    assertEquals("definition", sharedPrefs.getString("pexels_query_source", null))

    // Update to "image_prompt"
    viewModel.setPexelsQuerySource("image_prompt")
    assertEquals("image_prompt", viewModel.getPexelsQuerySource())
    assertEquals("image_prompt", viewModel.pexelsQuerySource.value)
    assertEquals("image_prompt", sharedPrefs.getString("pexels_query_source", null))
  }

  @Test
  fun `verify pexels query branching on pexels_query_source setting`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(context)
    var capturedQuery: String? = null
    val fakePexelsService = object : com.example.api.PexelsApiService {
      override suspend fun searchPhotos(
        apiKey: String,
        query: String,
        perPage: Int,
        page: Int
      ): com.example.api.PexelsSearchResponse {
        capturedQuery = query
        return com.example.api.PexelsSearchResponse(
          page = page,
          perPage = perPage,
          photos = listOf(
            com.example.api.PexelsPhoto(
              id = 1L,
              src = com.example.api.PexelsPhotoSrc(
                medium = "https://images.pexels.com/photos/123/medium.jpg"
              )
            )
          )
        )
      }
    }
    val repository = WordRepository(db.wordDao(), GeminiApiService.create(), pexelsApiService = fakePexelsService, context = context)
    val viewModel = DictionaryViewModel(context, repository)

    val word = WordEntity(
      word = "benevolent",
      phonetic = "/bəˈnev.əl.ənt/",
      partOfSpeech = "adjective",
      definition = "Well meaning and kindly character",
      synonyms = "kind, caring",
      antonyms = "malevolent",
      example = "The benevolent king gave food to the villagers after the harvest",
      mnemonic = "Bene means good",
      imagePrompt = "An elderly warm volunteer serving warm soup to people"
    )
    db.wordDao().insertWord(word)

    // 1. When query source is "word" -> query is the raw word directly
    viewModel.setPexelsQuerySource("word")
    viewModel.cycleNextPhoto(word)
    waitForPhotoCycle(viewModel)
    assertEquals("benevolent", capturedQuery)
    assertFalse(capturedQuery!!.contains("villagers"))
    assertFalse(capturedQuery!!.contains("harvest"))

    // 2. When query source is "definition" -> query extracts keywords from definition
    viewModel.setPexelsQuerySource("definition")
    viewModel.cycleNextPhoto(word)
    waitForPhotoCycle(viewModel)
    assertNotNull(capturedQuery)
    assertTrue(capturedQuery!!.contains("meaning") || capturedQuery!!.contains("kindly") || capturedQuery!!.contains("character"))
    assertFalse(capturedQuery!!.contains("villagers"))
    assertFalse(capturedQuery!!.contains("harvest"))
    assertFalse(capturedQuery!!.contains("elderly"))

    // 3. When query source is "image_prompt" -> query extracts keywords from imagePrompt
    viewModel.setPexelsQuerySource("image_prompt")
    viewModel.cycleNextPhoto(word)
    waitForPhotoCycle(viewModel)
    assertNotNull(capturedQuery)
    assertTrue(capturedQuery!!.contains("elderly") || capturedQuery!!.contains("volunteer") || capturedQuery!!.contains("serving") || capturedQuery!!.contains("soup"))
    assertFalse(capturedQuery!!.contains("villagers"))
    assertFalse(capturedQuery!!.contains("harvest"))
    assertFalse(capturedQuery!!.contains("meaning"))
  }

  @Test
  fun `verify Daily Reading Grammar Check flow and separate scoring`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    val repository = WordRepository(db.wordDao(), GeminiApiService.create(), context = context)

    val readingQuestions = listOf(
      com.example.api.ReadingQuestion(
        question = "What is the main idea of the passage?",
        options = listOf("Idea A", "Idea B", "Idea C", "Idea D"),
        correctAnswer = "Idea A",
        explanation = "The passage discusses Idea A.",
        type = "comprehension"
      )
    )
    val grammarQuestions = listOf(
      com.example.api.ReadingQuestion(
        question = "Which tense is used in the sentence?",
        options = listOf("Past simple", "Present perfect", "Future", "Past continuous"),
        correctAnswer = "Past simple",
        explanation = "The verb ended in -ed indicating past simple tense.",
        type = "grammar"
      ),
      com.example.api.ReadingQuestion(
        question = "Choose the correct preposition for the phrase.",
        options = listOf("in", "on", "at", "by"),
        correctAnswer = "in",
        explanation = "We use 'in' before months and years.",
        type = "grammar"
      )
    )

    val response = com.example.api.ReadingPassageResponse(
      passage = "Yesterday, they walked in the park.",
      questions = readingQuestions,
      grammarQuestions = grammarQuestions
    )

    // Verify Moshi adapter serialization/deserialization retains grammarQuestions and type
    val json = repository.getReadingJsonAdapter().toJson(response)
    val parsed = repository.getReadingJsonAdapter().fromJson(json)
    assertNotNull(parsed)
    assertEquals(1, parsed!!.questions.size)
    assertEquals(2, parsed.grammarQuestions.size)
    assertEquals("grammar", parsed.grammarQuestions[0].type)
    assertEquals("Past simple", parsed.grammarQuestions[0].correctAnswer)

    // Test ViewModel flow
    val viewModel = DictionaryViewModel(context, repository)
    // Seed active state
    val activeState = DailyReadingUiState.Active(
      passage = parsed.passage,
      questions = parsed.questions,
      currentQuestionIndex = 0,
      grammarQuestions = parsed.grammarQuestions,
      currentPhase = ReadingPhase.READING,
      readingLevel = 2,
      cefrLevel = "B1"
    )

    // Access or simulate through reflection / internal state by driving ViewModel methods
    // Answer the single reading question correctly
    // We can simulate the state directly or test the transition logic
    val stateField = DictionaryViewModel::class.java.getDeclaredField("_dailyReadingUiState")
    stateField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val flow = stateField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<DailyReadingUiState>
    flow.value = activeState

    // 1. Select reading option
    viewModel.selectReadingOption("Idea A")
    val afterReadingSelect = flow.value as DailyReadingUiState.Active
    assertEquals(true, afterReadingSelect.isCorrect)
    assertEquals(1, afterReadingSelect.score)

    // 2. Next reading question -> transitions to GRAMMAR_INTRO because grammarQuestions is non-empty
    viewModel.nextReadingQuestion()
    var waitAttempts = 0
    while ((flow.value as? DailyReadingUiState.Active)?.currentPhase == ReadingPhase.READING && waitAttempts < 100) {
      Thread.sleep(20)
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      waitAttempts++
    }
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    val introState = flow.value as DailyReadingUiState.Active
    assertEquals(ReadingPhase.GRAMMAR_INTRO, introState.currentPhase)
    assertEquals(1, introState.score)

    // 3. Start Grammar Check -> transitions to GRAMMAR phase at question index 0
    viewModel.startGrammarCheck()
    val grammarActiveState = flow.value as DailyReadingUiState.Active
    assertEquals(ReadingPhase.GRAMMAR, grammarActiveState.currentPhase)
    assertEquals(0, grammarActiveState.currentGrammarIndex)
    assertEquals(0, grammarActiveState.grammarScore)

    // 4. Answer first grammar question correctly
    viewModel.selectGrammarOption("Past simple")
    val afterG1 = flow.value as DailyReadingUiState.Active
    assertEquals(true, afterG1.isGrammarCorrect)
    assertEquals(1, afterG1.grammarScore)

    // 5. Next grammar question -> index 1
    viewModel.nextGrammarQuestion()
    val atG2 = flow.value as DailyReadingUiState.Active
    assertEquals(1, atG2.currentGrammarIndex)
    assertNull(atG2.selectedGrammarOption)

    // 6. Answer second grammar question incorrectly
    viewModel.selectGrammarOption("on")
    val afterG2 = flow.value as DailyReadingUiState.Active
    assertEquals(false, afterG2.isGrammarCorrect)
    assertEquals(1, afterG2.grammarScore)

    // 7. Finish grammar check -> transitions to Finished state with separate scores
    viewModel.nextGrammarQuestion()
    val finishedState = flow.value as DailyReadingUiState.Finished
    assertEquals(1, finishedState.score) // Reading score: 1/1
    assertEquals(1, finishedState.total)
    assertEquals(1, finishedState.grammarScore) // Grammar score: 1/2
    assertEquals(2, finishedState.grammarTotal)

    // 8. Restart resets both reading and grammar state
    viewModel.restartCurrentPassageQuestions()
    val restartedState = flow.value as DailyReadingUiState.Active
    assertEquals(ReadingPhase.READING, restartedState.currentPhase)
    assertEquals(0, restartedState.score)
    assertEquals(0, restartedState.grammarScore)
    assertEquals(0, restartedState.currentQuestionIndex)
    assertEquals(0, restartedState.currentGrammarIndex)
  }

  @Test
  fun `verify selectAudioUrl prefers UK when accent is UK and normalizes URLs`() {
    val urls = listOf(
      "https://api.dictionaryapi.dev/media/pronunciations/en/hello-au.mp3",
      "//ssl.gstatic.com/dictionary/static/sounds/20200429/hello--_gb_1.mp3",
      "https://api.dictionaryapi.dev/media/pronunciations/en/hello-us.mp3",
      ""
    )

    // With preferUk = true, should select the gb/uk URL and prepend https:
    val selectedUk = com.example.api.selectAudioUrl(urls, preferUk = true)
    assertEquals("https://ssl.gstatic.com/dictionary/static/sounds/20200429/hello--_gb_1.mp3", selectedUk)

    // If no UK url is available, fall back to US or any other
    val urlsNoUk = listOf(
      "https://api.dictionaryapi.dev/media/pronunciations/en/hello-au.mp3",
      "https://api.dictionaryapi.dev/media/pronunciations/en/hello-us.mp3"
    )
    val fallbackToUs = com.example.api.selectAudioUrl(urlsNoUk, preferUk = true)
    assertEquals("https://api.dictionaryapi.dev/media/pronunciations/en/hello-us.mp3", fallbackToUs)
  }

  @Test
  fun `verify selectAudioUrl prefers US when accent is US and handles empty lists`() {
    val urls = listOf(
      "https://api.dictionaryapi.dev/media/pronunciations/en/hello-uk.mp3",
      "https://api.dictionaryapi.dev/media/pronunciations/en/hello-us.mp3",
      "https://api.dictionaryapi.dev/media/pronunciations/en/hello-au.mp3"
    )

    val selectedUs = com.example.api.selectAudioUrl(urls, preferUk = false)
    assertEquals("https://api.dictionaryapi.dev/media/pronunciations/en/hello-us.mp3", selectedUs)

    // Falls back to ANY non-empty audio (e.g. AU or unaccented) when neither preferred nor secondary match
    val urlsOnlyAu = listOf(
      "https://api.dictionaryapi.dev/media/pronunciations/en/hello-au.mp3"
    )
    val fallbackToAu = com.example.api.selectAudioUrl(urlsOnlyAu, preferUk = false)
    assertEquals("https://api.dictionaryapi.dev/media/pronunciations/en/hello-au.mp3", fallbackToAu)

    // Test protocol-relative URLs starting with //
    val protocolRelativeUrls = listOf(
      "",
      "//ssl.gstatic.com/dictionary/static/sounds/20200429/hello--_gb_1.mp3"
    )
    val selectedGb = com.example.api.selectAudioUrl(protocolRelativeUrls, preferUk = true)
    assertEquals("https://ssl.gstatic.com/dictionary/static/sounds/20200429/hello--_gb_1.mp3", selectedGb)

    assertEquals("https://example.com/audio.mp3", com.example.api.normalizeAudioUrl("//example.com/audio.mp3"))
    assertNull(com.example.api.normalizeAudioUrl(""))
    assertNull(com.example.api.normalizeAudioUrl("   "))
    assertNull(com.example.api.normalizeAudioUrl(null))

    // Empty or blank list returns null
    assertNull(com.example.api.selectAudioUrl(emptyList(), preferUk = false))
    assertNull(com.example.api.selectAudioUrl(listOf("", "  "), preferUk = true))
  }

  @Test
  fun `verify DictionaryApiEntry Moshi deserialization`() {
    val json = """
      [
        {
          "word": "hello",
          "phonetic": "həˈləʊ",
          "phonetics": [
            {
              "text": "həˈləʊ",
              "audio": "//ssl.gstatic.com/dictionary/static/sounds/20200429/hello--_gb_1.mp3"
            },
            {
              "text": "həˈloʊ",
              "audio": "https://api.dictionaryapi.dev/media/pronunciations/en/hello-us.mp3"
            },
            {
              "text": "empty",
              "audio": ""
            }
          ]
        }
      ]
    """.trimIndent()

    val moshi = com.squareup.moshi.Moshi.Builder()
      .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
      .build()
    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.api.DictionaryApiEntry::class.java)
    val adapter = moshi.adapter<List<com.example.api.DictionaryApiEntry>>(type)
    val entries = adapter.fromJson(json)

    assertNotNull(entries)
    assertEquals(1, entries!!.size)
    assertEquals("hello", entries[0].word)
    assertEquals(3, entries[0].phonetics!!.size)
    assertEquals("https://api.dictionaryapi.dev/media/pronunciations/en/hello-us.mp3", entries[0].phonetics!![1].audio)
  }

  @Test
  fun `verify pronunciation_source preference default and persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Test default
    val defaultSource = sharedPrefs.getString("pronunciation_source", "auto") ?: "auto"
    assertEquals("auto", defaultSource)

    // Update to online
    sharedPrefs.edit().putString("pronunciation_source", "online").apply()
    assertEquals("online", sharedPrefs.getString("pronunciation_source", "auto"))

    // Update to device
    sharedPrefs.edit().putString("pronunciation_source", "device").apply()
    assertEquals("device", sharedPrefs.getString("pronunciation_source", "auto"))
  }

  @Test
  fun `verify DictionaryViewModel pronunciationSource state and setter`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(application)
    val repository = WordRepository(database.wordDao(), GeminiApiService.create(), context = application)
    val viewModel = DictionaryViewModel(application, repository)

    // Default should be auto
    assertEquals("auto", viewModel.pronunciationSource.value)

    // Test switching to online
    viewModel.setPronunciationSource("online")
    assertEquals("online", viewModel.pronunciationSource.value)

    val sharedPrefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    assertEquals("online", sharedPrefs.getString("pronunciation_source", null))

    // Test switching to device
    viewModel.setPronunciationSource("device")
    assertEquals("device", viewModel.pronunciationSource.value)
    assertEquals("device", sharedPrefs.getString("pronunciation_source", null))

    // Test switching back to auto
    viewModel.setPronunciationSource("auto")
    assertEquals("auto", viewModel.pronunciationSource.value)
    assertEquals("auto", sharedPrefs.getString("pronunciation_source", null))
  }

  @Test
  fun `verify GEMINI_TTS_MODELS configuration list`() {
    val models = DictionaryViewModel.GEMINI_TTS_MODELS
    assertEquals(3, models.size)
    assertEquals("gemini-3.1-flash-tts-preview", models[0])
    assertEquals("gemini-2.5-flash-preview-tts", models[1])
    assertEquals("gemini-2.5-pro-preview-tts", models[2])
  }

  @Test
  fun `verify isInvalidApiKey rejects blanks and MY_ placeholders`() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(application)
    val repository = WordRepository(database.wordDao(), GeminiApiService.create(), context = application)
    val viewModel = DictionaryViewModel(application, repository)

    assertTrue(viewModel.isInvalidApiKey(null))
    assertTrue(viewModel.isInvalidApiKey(""))
    assertTrue(viewModel.isInvalidApiKey("   "))
    assertTrue(viewModel.isInvalidApiKey("MY_GEMINI_TTS_API_KEY"))
    assertTrue(viewModel.isInvalidApiKey("MY_GEMINI_API_KEY"))
    assertTrue(viewModel.isInvalidApiKey("MY_CUSTOM_SECRET"))
    assertFalse(viewModel.isInvalidApiKey("AIzaSyActualValidApiKey12345"))
  }

  @Test
  fun `verify fetchGeminiTtsAudio model fallback on 429 and successful audio decode`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(application)
    val repository = WordRepository(database.wordDao(), GeminiApiService.create(), context = application)

    val requestedUrls = mutableListOf<String>()
    val requestedHeaders = mutableListOf<String?>()

    val dummyAudioBase64 = android.util.Base64.encodeToString("FAKE_AUDIO_DATA".toByteArray(), android.util.Base64.NO_WRAP)
    val successResponseBody = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "inlineData": {
                    "mimeType": "audio/mp3",
                    "data": "$dummyAudioBase64"
                  }
                }
              ]
            }
          }
        ]
      }
    """.trimIndent()

    val mockClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        val req = chain.request()
        val url = req.url.toString()
        requestedUrls.add(url)
        requestedHeaders.add(req.header("x-goog-api-key"))

        if (url.contains("gemini-3.1-flash-tts-preview")) {
          // Simulate 429 rate limit on first model
          okhttp3.Response.Builder()
            .request(req)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(429)
            .message("Too Many Requests")
            .body("{\"error\": {\"code\": 429, \"message\": \"Rate limit exceeded\"}}".toResponseBody("application/json".toMediaType()))
            .build()
        } else if (url.contains("gemini-2.5-flash-preview-tts")) {
          // Second model succeeds
          okhttp3.Response.Builder()
            .request(req)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(successResponseBody.toResponseBody("application/json".toMediaType()))
            .build()
        } else {
          okhttp3.Response.Builder()
            .request(req)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(500)
            .message("Server Error")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
        }
      }
      .build()

    val viewModel = DictionaryViewModel(application, repository, mockClient)
    viewModel.apiKeyOverride = "test-api-key-123"

    val audioBytes = viewModel.fetchGeminiTtsAudio("serendipity")
    assertNotNull(audioBytes)
    assertEquals("FAKE_AUDIO_DATA", String(audioBytes!!))

    // Verify both first model (which returned 429) and second model (which succeeded) were called in order
    assertEquals(2, requestedUrls.size)
    assertTrue(requestedUrls[0].contains("gemini-3.1-flash-tts-preview"))
    assertTrue(requestedUrls[1].contains("gemini-2.5-flash-preview-tts"))

    // Verify in-memory cache: second call shouldn't make network requests
    val cachedBytes = viewModel.fetchGeminiTtsAudio("serendipity")
    assertNotNull(cachedBytes)
    assertEquals("FAKE_AUDIO_DATA", String(cachedBytes!!))
    assertEquals(2, requestedUrls.size) // No new requests

    // Verify speak() doesn't crash in device, online, or auto mode
    viewModel.setPronunciationSource("device")
    viewModel.speak("serendipity")

    viewModel.setPronunciationSource("online")
    viewModel.speak("serendipity")

    viewModel.setPronunciationSource("auto")
    viewModel.speak("serendipity")
  }

  @Test
  fun `verify WordDao updateAudioPath and audio file persistence`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(application)
    val wordDao = database.wordDao()
    val repository = WordRepository(wordDao, GeminiApiService.create(), context = application)

    val word = WordEntity(
      word = "persistword",
      phonetic = "/pərˈsɪst/",
      partOfSpeech = "verb",
      definition = "To continue firmly",
      synonyms = "endure, continue",
      antonyms = "quit, stop",
      example = "She persisted in her studies.",
      mnemonic = "Persist: per-sister who never gives up",
      imagePrompt = "A person climbing a steep cliff",
      isBookmarked = false,
      audioPath = null
    )
    wordDao.insertWord(word)

    val fetched = wordDao.getWord("persistword")
    assertNotNull(fetched)
    assertNull(fetched!!.audioPath)

    // Test updateAudioPath
    val testPath = "${application.filesDir}/pronunciations/persistword.wav"
    wordDao.updateAudioPath("persistword", testPath)
    val updated = wordDao.getWord("persistword")
    assertNotNull(updated)
    assertEquals(testPath, updated!!.audioPath)

    // Test ViewModel audio saving and playback
    val viewModel = DictionaryViewModel(application, repository)
    val dummyAudio = "RIFF....WAVEfmt ....dataFAKEAUDIO".toByteArray()
    val savedPath = viewModel.saveAudioFileForWord("persistword", dummyAudio)
    assertNotNull(savedPath)
    val file = java.io.File(savedPath!!)
    assertTrue(file.exists())
    assertTrue(file.length() > 0)

    // Speak should find saved file and not crash
    viewModel.speak("persistword")

    // Test cleanup on deleteFromHistory
    viewModel.deleteFromHistory(updated)
    assertFalse(file.exists())
  }

  @Test
  fun `verify Dual Gemini API Keys persistence switching and 429 automatic retry`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(application)
    val wordDao = database.wordDao()
    val repository = WordRepository(wordDao, GeminiApiService.create(), context = application)

    val requestedKeys = mutableListOf<String?>()
    val dummyAudioBase64 = android.util.Base64.encodeToString("RETRY_AUDIO_SUCCESS".toByteArray(), android.util.Base64.NO_WRAP)
    val successBody = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "inlineData": {
                    "mimeType": "audio/mp3",
                    "data": "$dummyAudioBase64"
                  }
                }
              ]
            }
          }
        ]
      }
    """.trimIndent()

    val mockClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        val req = chain.request()
        val apiKey = req.header("x-goog-api-key")
        requestedKeys.add(apiKey)

        if (apiKey == "first-primary-key") {
          // Simulate 429 rate limit on first key
          okhttp3.Response.Builder()
            .request(req)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(429)
            .message("Too Many Requests")
            .body("{\"error\": {\"code\": 429, \"message\": \"Rate limit exceeded\"}}".toResponseBody("application/json".toMediaType()))
            .build()
        } else if (apiKey == "second-fallback-key") {
          // Fallback key succeeds
          okhttp3.Response.Builder()
            .request(req)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(successBody.toResponseBody("application/json".toMediaType()))
            .build()
        } else {
          okhttp3.Response.Builder()
            .request(req)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(500)
            .message("Internal Error")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
        }
      }
      .build()

    val viewModel = DictionaryViewModel(application, repository, mockClient)
    viewModel.resetTtsThrottle()

    // Set keys
    viewModel.setGeminiApiKey1("first-primary-key")
    viewModel.setGeminiApiKey2("second-fallback-key")
    viewModel.setActiveKeyIndex(0)

    assertEquals("first-primary-key", viewModel.geminiApiKey1.value)
    assertEquals("second-fallback-key", viewModel.geminiApiKey2.value)
    assertEquals(listOf("first-primary-key", "second-fallback-key"), viewModel.geminiApiKeys.value)
    assertEquals(0, viewModel.activeKeyIndex.value)
    assertEquals("first-primary-key", viewModel.getGeminiTtsApiKey())

    // Switch active key to 1 (Key 2)
    viewModel.setActiveKeyIndex(1)
    assertEquals(1, viewModel.activeKeyIndex.value)
    assertEquals("second-fallback-key", viewModel.getGeminiTtsApiKey())

    // Switch back to 0 (Key 1)
    viewModel.setActiveKeyIndex(0)
    assertEquals(0, viewModel.activeKeyIndex.value)
    assertEquals("first-primary-key", viewModel.getGeminiTtsApiKey())

    // Trigger TTS fetch: first key hits 429 across models, so it retries with second key
    val audio = viewModel.fetchGeminiTtsAudio("resilience")
    assertNotNull(audio)
    assertEquals("RETRY_AUDIO_SUCCESS", String(audio!!))

    // Verify key 1 was attempted and then key 2 was attempted
    assertTrue(requestedKeys.contains("first-primary-key"))
    assertTrue(requestedKeys.contains("second-fallback-key"))

    // Verify lastPronunciationSource was recorded as "Gemini Key 2" (due to 429 retry on Key 2)
    assertEquals("Gemini Key 2", viewModel.lastPronunciationSource.value)

    // Test SharedPreferences persistence
    val sharedPrefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val savedListJson = sharedPrefs.getString("gemini_api_keys_list", null)
    assertNotNull(savedListJson)
    assertTrue(savedListJson!!.contains("first-primary-key"))
    assertTrue(savedListJson.contains("second-fallback-key"))
    assertEquals(0, sharedPrefs.getInt("gemini_active_key_index", -1))
    assertEquals("Gemini Key 2", sharedPrefs.getString("last_pronunciation_source", null))
  }

  @Test
  fun `verify Global 30-Second Throttle on TTS requests and online mode message`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(application)
    val wordDao = database.wordDao()
    val repository = WordRepository(wordDao, GeminiApiService.create(), context = application)

    var networkCallsCount = 0
    val dummyAudioBase64 = android.util.Base64.encodeToString("THROTTLE_AUDIO".toByteArray(), android.util.Base64.NO_WRAP)
    val successBody = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "inlineData": {
                    "mimeType": "audio/mp3",
                    "data": "$dummyAudioBase64"
                  }
                }
              ]
            }
          }
        ]
      }
    """.trimIndent()

    val mockClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        networkCallsCount++
        val req = chain.request()
        okhttp3.Response.Builder()
          .request(req)
          .protocol(okhttp3.Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body(successBody.toResponseBody("application/json".toMediaType()))
          .build()
      }
      .build()

    val viewModel = DictionaryViewModel(application, repository, mockClient)
    viewModel.setGeminiApiKey1("test-key-1")
    viewModel.setActiveKeyIndex(0)
    viewModel.resetTtsThrottle()

    // First call: makes network request with Key 1
    val firstAudio = viewModel.fetchGeminiTtsAudio("wordone")
    assertNotNull(firstAudio)
    assertEquals(1, networkCallsCount)
    assertTrue(viewModel.isTtsNetworkThrottled())
    assertEquals("Gemini Key 1", viewModel.lastPronunciationSource.value)

    // Second call for a DIFFERENT word within 30s: should be throttled and return null (no network call!)
    val secondAudio = viewModel.fetchGeminiTtsAudio("wordtwo")
    assertNull(secondAudio)
    assertEquals(1, networkCallsCount) // Network call was skipped!

    // But cached word (wordone) should NOT be throttled
    val cachedAudio = viewModel.fetchGeminiTtsAudio("wordone")
    assertNotNull(cachedAudio)
    assertEquals(1, networkCallsCount) // Still no new network call, hit cache

    // speak() with online mode during throttle:
    // Should NOT call device TTS, should show cooldown message
    viewModel.setPronunciationSource("online")
    viewModel.speak("wordthree")
    var attempts = 0
    while (viewModel.snackbarMessage.replayCache.isEmpty() && attempts < 50) {
      Thread.sleep(20)
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      attempts++
    }
    
    // Check snackbar emitted cooldown message
    val emittedMessage = viewModel.snackbarMessage.replayCache.lastOrNull()
    assertNotNull(emittedMessage)
    assertTrue(emittedMessage!!.contains("Please wait") && emittedMessage.contains("seconds before pronouncing a new word"))

    // speak() with auto mode during throttle:
    // Should fall back to device TTS and update lastPronunciationSource
    viewModel.setPronunciationSource("auto")
    viewModel.speak("wordfour")
    attempts = 0
    while (viewModel.lastPronunciationSource.value != "Device TTS" && attempts < 50) {
      Thread.sleep(20)
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      attempts++
    }
    assertEquals("Device TTS", viewModel.lastPronunciationSource.value)
  }

  @Test
  fun `verify dynamic Gemini keys addition, editing, deletion and active key fallback`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(application)
    val wordDao = database.wordDao()
    val repository = WordRepository(wordDao, GeminiApiService.create(), context = application)

    val viewModel = DictionaryViewModel(application, repository)

    // Clear initial keys
    viewModel.setGeminiApiKeys(emptyList())
    assertEquals(emptyList<String>(), viewModel.geminiApiKeys.value)
    assertEquals(0, viewModel.activeKeyIndex.value)

    // Add keys dynamically
    viewModel.addGeminiApiKey("key-alpha")
    viewModel.addGeminiApiKey("key-beta")
    viewModel.addGeminiApiKey("key-gamma")

    assertEquals(listOf("key-alpha", "key-beta", "key-gamma"), viewModel.geminiApiKeys.value)
    assertEquals(0, viewModel.activeKeyIndex.value)
    assertEquals("key-alpha", viewModel.getGeminiTtsApiKey())

    // Update key at index 1
    viewModel.updateGeminiApiKey(1, "key-beta-updated")
    assertEquals(listOf("key-alpha", "key-beta-updated", "key-gamma"), viewModel.geminiApiKeys.value)

    // Set active key to index 2 ("key-gamma", Key 3)
    viewModel.setActiveKeyIndex(2)
    assertEquals(2, viewModel.activeKeyIndex.value)
    assertEquals("key-gamma", viewModel.getGeminiTtsApiKey())

    // Delete the active key (index 2): should fall back to index 0
    viewModel.removeGeminiApiKey(2)
    assertEquals(listOf("key-alpha", "key-beta-updated"), viewModel.geminiApiKeys.value)
    assertEquals(0, viewModel.activeKeyIndex.value)
    assertEquals("key-alpha", viewModel.getGeminiTtsApiKey())

    // Set active to 1 ("key-beta-updated"), then delete index 0 ("key-alpha"):
    // active index should adjust to 0 to stay on "key-beta-updated"
    viewModel.setActiveKeyIndex(1)
    assertEquals(1, viewModel.activeKeyIndex.value)
    viewModel.removeGeminiApiKey(0)
    assertEquals(listOf("key-beta-updated"), viewModel.geminiApiKeys.value)
    assertEquals(0, viewModel.activeKeyIndex.value)
    assertEquals("key-beta-updated", viewModel.getGeminiTtsApiKey())

    // Remove remaining key: empty list, active index defaults to 0
    viewModel.removeGeminiApiKey(0)
    assertEquals(emptyList<String>(), viewModel.geminiApiKeys.value)
    assertEquals(0, viewModel.activeKeyIndex.value)
  }

  @Test
  fun `verify SharedPreferences migration from old gemini_api_key_1 and gemini_api_key_2 to JSON list`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val sharedPrefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Clear new list and set old legacy keys
    sharedPrefs.edit()
      .remove("gemini_api_keys_list")
      .putString("gemini_api_key_1", "migrated-first-key")
      .putString("gemini_api_key_2", "migrated-second-key")
      .putInt("gemini_active_key_index", 2)
      .apply()

    val database = AppDatabase.getDatabase(application)
    val wordDao = database.wordDao()
    val repository = WordRepository(wordDao, GeminiApiService.create(), context = application)

    // Instantiate ViewModel -> init runs migration
    val viewModel = DictionaryViewModel(application, repository)

    assertEquals(listOf("migrated-first-key", "migrated-second-key"), viewModel.geminiApiKeys.value)
    // Old active key 2 (1-based) migrated to 0-based index 1
    assertEquals(1, viewModel.activeKeyIndex.value)
    assertEquals("migrated-second-key", viewModel.getGeminiTtsApiKey())

    // Verify persisted JSON array
    val savedJson = sharedPrefs.getString("gemini_api_keys_list", null)
    assertNotNull(savedJson)
    val jsonArray = org.json.JSONArray(savedJson!!)
    assertEquals(2, jsonArray.length())
    assertEquals("migrated-first-key", jsonArray.getString(0))
    assertEquals("migrated-second-key", jsonArray.getString(1))

    // Old preference entries are left in place
    assertEquals("migrated-first-key", sharedPrefs.getString("gemini_api_key_1", null))
    assertEquals("migrated-second-key", sharedPrefs.getString("gemini_api_key_2", null))
  }

  @Test
  fun `verify multi-key wrap-around 429 failover starting from active key`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(application)
    val wordDao = database.wordDao()
    val repository = WordRepository(wordDao, GeminiApiService.create(), context = application)

    val requestedKeysInOrder = mutableListOf<String>()
    val dummyAudioBase64 = android.util.Base64.encodeToString("WRAP_SUCCESS".toByteArray(), android.util.Base64.NO_WRAP)
    val successBody = """
      {
        "candidates": [
          {
            "content": {
              "parts": [
                {
                  "inlineData": {
                    "mimeType": "audio/wav",
                    "data": "$dummyAudioBase64"
                  }
                }
              ]
            }
          }
        ]
      }
    """.trimIndent()

    val mockClient = okhttp3.OkHttpClient.Builder()
      .addInterceptor { chain ->
        val req = chain.request()
        val apiKey = req.header("x-goog-api-key") ?: ""
        requestedKeysInOrder.add(apiKey)

        if (apiKey == "key-B" || apiKey == "key-C") {
          // B and C hit 429
          okhttp3.Response.Builder()
            .request(req)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(429)
            .message("Too Many Requests")
            .body("{\"error\": {\"code\": 429, \"message\": \"Rate limit exceeded\"}}".toResponseBody("application/json".toMediaType()))
            .build()
        } else if (apiKey == "key-A") {
          // A succeeds on wrap-around
          okhttp3.Response.Builder()
            .request(req)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(successBody.toResponseBody("application/json".toMediaType()))
            .build()
        } else {
          okhttp3.Response.Builder()
            .request(req)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(500)
            .message("Internal Error")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
        }
      }
      .build()

    val viewModel = DictionaryViewModel(application, repository, mockClient)
    viewModel.resetTtsThrottle()

    // 3 keys: [key-A, key-B, key-C]
    viewModel.setGeminiApiKeys(listOf("key-A", "key-B", "key-C"))
    // Set active key to index 1 ("key-B")
    viewModel.setActiveKeyIndex(1)
    assertEquals(1, viewModel.activeKeyIndex.value)

    val audio = viewModel.fetchGeminiTtsAudio("dynamicfailover")
    assertNotNull(audio)
    assertEquals("WRAP_SUCCESS", String(audio!!))

    // Keys should be attempted starting from active (key-B), wrapping around: key-B -> key-C -> key-A
    val distinctKeysAttempted = requestedKeysInOrder.distinct()
    assertEquals(listOf("key-B", "key-C", "key-A"), distinctKeysAttempted)

    // Key-A is at index 0 (1-based position "Gemini Key 1")
    assertEquals("Gemini Key 1", viewModel.lastPronunciationSource.value)
  }

  @Test
  fun `verify detailed SRS statistics calculations including retention, rating distribution, ease buckets, and 14-day forecast`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val sharedPrefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean("is_seeded_v2", true).commit()

    val database = AppDatabase.getDatabase(application)
    val wordDao = database.wordDao()
    val repository = WordRepository(wordDao, GeminiApiService.create(), context = application)

    // Clear existing logs and words
    withContext(Dispatchers.IO) {
      database.clearAllTables()
    }

    val viewModel = DictionaryViewModel(application, repository)

    val today = java.time.LocalDate.now()
    val zoneId = java.time.ZoneId.systemDefault()
    val todayEpochMilli = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val todayEpochDay = today.toEpochDay()

    // Insert sample review logs:
    // 3 logs today: 1 Good, 1 Easy, 1 Again
    wordDao.insertReviewLog(ReviewLogEntity(word = "ephemeral", rating = "Good", timestamp = todayEpochMilli + 1000))
    wordDao.insertReviewLog(ReviewLogEntity(word = "resilience", rating = "Easy", timestamp = todayEpochMilli + 2000))
    wordDao.insertReviewLog(ReviewLogEntity(word = "ubiquitous", rating = "Again", timestamp = todayEpochMilli + 3000))

    // 2 logs 10 days ago (within 30 days): 1 Hard, 1 Good
    val tenDaysAgoMilli = today.minusDays(10).atStartOfDay(zoneId).toInstant().toEpochMilli()
    wordDao.insertReviewLog(ReviewLogEntity(word = "solitude", rating = "Hard", timestamp = tenDaysAgoMilli + 1000))
    wordDao.insertReviewLog(ReviewLogEntity(word = "serendipity", rating = "Good", timestamp = tenDaysAgoMilli + 2000))

    // 2 logs 40 days ago (outside 30 days): 1 Again, 1 Good
    val fortyDaysAgoMilli = today.minusDays(40).atStartOfDay(zoneId).toInstant().toEpochMilli()
    wordDao.insertReviewLog(ReviewLogEntity(word = "lucid", rating = "Again", timestamp = fortyDaysAgoMilli + 1000))
    wordDao.insertReviewLog(ReviewLogEntity(word = "eloquent", rating = "Good", timestamp = fortyDaysAgoMilli + 2000))

    // Insert sample words with varying easeFactors and dueDates
    wordDao.insertWord(WordEntity(word = "word1", definition = "def1", easeFactor = 1.5f, dueDate = todayEpochDay, repetitions = 2)) // bucket < 1.8, due today
    wordDao.insertWord(WordEntity(word = "word2", definition = "def2", easeFactor = 2.0f, dueDate = todayEpochDay + 1, repetitions = 1)) // bucket 1.8-2.2, due +1d
    wordDao.insertWord(WordEntity(word = "word3", definition = "def3", easeFactor = 2.4f, dueDate = todayEpochDay + 3, repetitions = 3)) // bucket 2.2-2.6, due +3d
    wordDao.insertWord(WordEntity(word = "word4", definition = "def4", easeFactor = 2.8f, dueDate = todayEpochDay + 7, repetitions = 4)) // bucket 2.6-3.0, due +7d
    wordDao.insertWord(WordEntity(word = "word5", definition = "def5", easeFactor = 3.5f, dueDate = todayEpochDay + 10, repetitions = 5)) // bucket > 3.0, due +10d

    viewModel.refreshStats()

    // Wait for coroutine inside refreshStats to complete across Dispatchers.IO and Main
    repeat(10) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      delay(50)
    }

    val stats = viewModel.detailedSrsStats.value

    // Total reviews all-time = 7. Non-Again = 5 (Good: 4, Easy: 1, Hard: 1 => 5 non-Again: Good, Easy, Hard, Good, Good = 5/7 = 71.4%)
    assertEquals(7, stats.totalReviewsAllTime)
    assertEquals(5, stats.totalReviewsLast30Days) // 3 today + 2 from 10 days ago
    
    // Retention rate: Last 30 days = 4 non-again out of 5 = 80%
    assertEquals(80f, stats.last30DaysRetentionRate, 0.5f)
    // Overall retention rate = 5 non-again out of 7 = 71.42%
    assertEquals(71.4f, stats.overallRetentionRate, 0.5f)

    // Rating distribution
    val ratingDist = stats.ratingDistribution
    assertEquals(2, ratingDist.againCount)
    assertEquals(1, ratingDist.hardCount)
    assertEquals(3, ratingDist.goodCount)
    assertEquals(1, ratingDist.easyCount)
    assertEquals(7, ratingDist.totalCount)

    // Ease factor distribution
    val easeDist = stats.easeFactorDistribution
    assertEquals(1, easeDist.bucketUnder1_8)
    assertEquals(1, easeDist.bucket1_8to2_2)
    assertEquals(1, easeDist.bucket2_2to2_6)
    assertEquals(1, easeDist.bucket2_6to3_0)
    assertEquals(1, easeDist.bucketOver3_0)
    assertEquals(5, easeDist.totalWords)

    // Forecast (14 days)
    assertEquals(14, stats.forecastNext14Days.size)
    assertEquals(1, stats.forecastNext14Days[0].count) // today
    assertEquals(1, stats.forecastNext14Days[1].count) // +1d
    assertEquals(0, stats.forecastNext14Days[2].count) // +2d
    assertEquals(1, stats.forecastNext14Days[3].count) // +3d
    assertEquals(1, stats.forecastNext14Days[7].count) // +7d
    assertEquals(1, stats.forecastNext14Days[10].count) // +10d

    // Reviews per day (30 days)
    assertEquals(30, stats.reviewsPerDayLast30Days.size)
    assertEquals(3, stats.reviewsPerDayLast30Days.last().count) // today

    // Streak heatmap (30 days)
    assertEquals(30, stats.streakHistoryLast30Days.size)
    assertTrue(stats.streakHistoryLast30Days.last().isToday)
    assertTrue(stats.streakHistoryLast30Days.last().hasReview)
  }

  @Test
  fun testLeechDetectionAndLapseCounting() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(application)
    withContext(Dispatchers.IO) {
      db.clearAllTables()
    }
    val wordDao = db.wordDao()
    val repository = WordRepository(wordDao = wordDao, apiService = GeminiApiService.create(), context = application)
    val viewModel = DictionaryViewModel(application, repository)

    // Test 1: Brand new card (repetitions = 0) rated "Again" -> lapses should NOT increment
    val newCard = WordEntity(word = "ephemeral", definition = "lasting a short time", repetitions = 0, lapses = 0, isLeech = false)
    wordDao.insertWord(newCard)
    viewModel.rateCard(newCard, "Again")
    delay(100)

    var updated = wordDao.getWord("ephemeral")!!
    assertEquals(0, updated.lapses)
    assertFalse(updated.isLeech)

    // Test 2: Card with repetitions > 0 rated "Again" -> lapses increments
    val learnedCard = updated.copy(repetitions = 2)
    wordDao.insertWord(learnedCard)
    viewModel.rateCard(learnedCard, "Again")
    delay(100)

    updated = wordDao.getWord("ephemeral")!!
    assertEquals(1, updated.lapses)
    assertFalse(updated.isLeech)

    // Test 3: Set leech threshold to 3 and trigger leech
    viewModel.setLeechThreshold(3)
    assertEquals(3, viewModel.leechThreshold.value)

    val cardLapse1 = updated.copy(repetitions = 1)
    viewModel.rateCard(cardLapse1, "Again") // lapse 2
    delay(100)
    updated = wordDao.getWord("ephemeral")!!
    assertEquals(2, updated.lapses)
    assertFalse(updated.isLeech)

    val cardLapse2 = updated.copy(repetitions = 1)
    viewModel.rateCard(cardLapse2, "Again") // lapse 3 -> triggers leech!
    delay(100)
    updated = wordDao.getWord("ephemeral")!!
    assertEquals(3, updated.lapses)
    assertTrue(updated.isLeech)

    // Test 4: Clear leech flag
    viewModel.clearLeechFlag("ephemeral")
    delay(100)
    updated = wordDao.getWord("ephemeral")!!
    assertFalse(updated.isLeech)
    assertEquals(3, updated.lapses) // lapses counter preserved
  }

  @Test
  fun testFsrsSchedulingAlgorithmAndSettings() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(application)
    withContext(Dispatchers.IO) {
      db.clearAllTables()
    }
    val wordDao = db.wordDao()
    val repository = WordRepository(wordDao = wordDao, apiService = GeminiApiService.create(), context = application)
    val viewModel = DictionaryViewModel(application, repository)

    // Verify defaults
    assertEquals("sm2", viewModel.srsAlgorithm.value)
    assertEquals(90, viewModel.targetRetention.value)

    // Test settings toggle
    viewModel.setSrsAlgorithm("fsrs")
    assertEquals("fsrs", viewModel.srsAlgorithm.value)

    viewModel.setTargetRetention(95)
    assertEquals(95, viewModel.targetRetention.value)

    // Clamping checks
    viewModel.setTargetRetention(70) // Below 80
    assertEquals(80, viewModel.targetRetention.value)
    viewModel.setTargetRetention(99) // Above 97
    assertEquals(97, viewModel.targetRetention.value)
    viewModel.setTargetRetention(90) // Standard

    // Insert a new word with no FSRS memory state
    val newWord = WordEntity(
      word = "quintessential",
      definition = "representing the most perfect or typical example",
      fsrsStability = null,
      fsrsDifficulty = null,
      fsrsDue = null
    )
    wordDao.insertWord(newWord)

    // Review with "Good" under FSRS
    viewModel.rateCard(newWord, "Good")
    delay(150)

    var updated = wordDao.getWord("quintessential")!!
    assertNotNull(updated.fsrsStability)
    assertNotNull(updated.fsrsDifficulty)
    assertNotNull(updated.fsrsDue)
    assertTrue(updated.fsrsStability!! > 0.0)
    assertTrue(updated.fsrsDifficulty!! > 0.0)
    assertTrue(updated.dueDate >= java.time.LocalDate.now().toEpochDay())

    val initialStability = updated.fsrsStability!!

    // Second review with "Good" under FSRS
    viewModel.rateCard(updated, "Good")
    delay(150)

    updated = wordDao.getWord("quintessential")!!
    assertTrue(updated.fsrsStability!! > initialStability) // Stability should increase on subsequent Good
    assertTrue(updated.repetitions >= 2)

    // Test rating "Again" under FSRS
    viewModel.rateCard(updated, "Again")
    delay(150)

    updated = wordDao.getWord("quintessential")!!
    assertEquals(1, updated.lapses)
    assertEquals(0, updated.repetitions)
    assertEquals(java.time.LocalDate.now().toEpochDay(), updated.dueDate)

    // Switch back to SM-2 and verify backward compatibility
    viewModel.setSrsAlgorithm("sm2")
    assertEquals("sm2", viewModel.srsAlgorithm.value)
    val sm2Word = WordEntity(word = "serendipity", definition = "a fortunate stroke of luck", repetitions = 0)
    wordDao.insertWord(sm2Word)
    viewModel.rateCard(sm2Word, "Good")
    delay(150)

    val sm2Updated = wordDao.getWord("serendipity")!!
    assertEquals(1, sm2Updated.repetitions)
    assertNull(sm2Updated.fsrsStability) // SM-2 card has no FSRS state
  }

  @Test
  fun testMasteryRequiresDistinctReviewDaysAndReviewLogsQuery() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(application)
    withContext(Dispatchers.IO) {
      db.clearAllTables()
    }
    val wordDao = db.wordDao()
    val repository = WordRepository(wordDao = wordDao, apiService = GeminiApiService.create(), context = application)
    val viewModel = DictionaryViewModel(application, repository)

    val today = java.time.LocalDate.now().toEpochDay()

    // 1. Initial state: brand new word has 0 distinctReviewDays
    val word = WordEntity(
      word = "perseverance",
      definition = "persistence in doing something despite difficulty",
      repetitions = 0,
      interval = 0,
      distinctReviewDays = 0,
      lastReviewed = null
    )
    wordDao.insertWord(word)

    // First successful review today: distinctReviewDays becomes 1
    viewModel.rateCard(word, "Good")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    delay(100)

    var updated = wordDao.getWord("perseverance")!!
    assertEquals(1, updated.distinctReviewDays)
    assertEquals(today, updated.lastReviewed)

    // Repeated successful review on the SAME calendar day: distinctReviewDays stays 1
    viewModel.rateCard(updated, "Good")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    delay(100)

    updated = wordDao.getWord("perseverance")!!
    assertEquals(1, updated.distinctReviewDays) // Still 1! Not incremented twice on same day

    // Repeated "Again" review on the SAME calendar day: distinctReviewDays stays 1
    viewModel.rateCard(updated, "Again")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    delay(100)

    updated = wordDao.getWord("perseverance")!!
    assertEquals(1, updated.distinctReviewDays)

    // Verify bug fix: Early "Again" on a new day updates lastReviewed, but later same-day "Good" still increments distinctReviewDays
    val nextDayWord = updated.copy(lastReviewed = today - 1, lastSuccessfulReviewDay = today - 1)
    viewModel.rateCard(nextDayWord, "Again")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    delay(100)

    var nextDayUpdated = wordDao.getWord("perseverance")!!
    assertEquals(1, nextDayUpdated.distinctReviewDays)
    assertEquals(today, nextDayUpdated.lastReviewed)
    assertEquals(today - 1, nextDayUpdated.lastSuccessfulReviewDay)

    // Later same-day success ("Good") now properly increments distinctReviewDays
    viewModel.rateCard(nextDayUpdated, "Good")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    delay(100)

    updated = wordDao.getWord("perseverance")!!
    assertEquals(2, updated.distinctReviewDays)
    assertEquals(today, updated.lastReviewed)
    assertEquals(today, updated.lastSuccessfulReviewDay)

    // Simulate 3rd distinct review day with interval >= 6
    val twoDaysAgoWord = updated.copy(lastReviewed = today - 2, lastSuccessfulReviewDay = today - 2, interval = 6)
    viewModel.rateCard(twoDaysAgoWord, "Good")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    delay(100)

    updated = wordDao.getWord("perseverance")!!
    assertEquals(3, updated.distinctReviewDays)
    assertTrue(updated.interval >= 6)

    // Verify Review Logs for this word are recorded and queryable
    val logs = repository.getReviewLogsForWordList("perseverance")
    assertTrue(logs.size >= 4)
    assertEquals("Good", logs.first().rating) // Most recent is Good
  }

  @Test
  fun testFormatPartOfSpeechAbbreviation() {
    assertEquals("n.", com.example.ui.formatPartOfSpeechAbbreviation("noun"))
    assertEquals("n.", com.example.ui.formatPartOfSpeechAbbreviation("NOUN"))
    assertEquals("n.", com.example.ui.formatPartOfSpeechAbbreviation("n"))
    assertEquals("v.", com.example.ui.formatPartOfSpeechAbbreviation("verb"))
    assertEquals("v.", com.example.ui.formatPartOfSpeechAbbreviation("Verb"))
    assertEquals("v.", com.example.ui.formatPartOfSpeechAbbreviation("vb."))
    assertEquals("adj.", com.example.ui.formatPartOfSpeechAbbreviation("adjective"))
    assertEquals("adj.", com.example.ui.formatPartOfSpeechAbbreviation("Adj"))
    assertEquals("adv.", com.example.ui.formatPartOfSpeechAbbreviation("adverb"))
    assertEquals("prep.", com.example.ui.formatPartOfSpeechAbbreviation("preposition"))
    assertEquals("pron.", com.example.ui.formatPartOfSpeechAbbreviation("pronoun"))
    assertEquals("conj.", com.example.ui.formatPartOfSpeechAbbreviation("conjunction"))
    assertEquals("interj.", com.example.ui.formatPartOfSpeechAbbreviation("interjection"))
    assertEquals("phr.", com.example.ui.formatPartOfSpeechAbbreviation("phrase"))
    assertEquals("abbr.", com.example.ui.formatPartOfSpeechAbbreviation("abbreviation"))
    assertEquals("idm.", com.example.ui.formatPartOfSpeechAbbreviation("idiom"))
    assertEquals("art.", com.example.ui.formatPartOfSpeechAbbreviation("article"))
    assertEquals("", com.example.ui.formatPartOfSpeechAbbreviation(null))
    assertEquals("", com.example.ui.formatPartOfSpeechAbbreviation(""))
    assertEquals("", com.example.ui.formatPartOfSpeechAbbreviation("   "))
    assertEquals("pref.", com.example.ui.formatPartOfSpeechAbbreviation("prefix"))
    assertEquals("slan.", com.example.ui.formatPartOfSpeechAbbreviation("slang"))
  }

  @Test
  fun testCustomTagsQuizAndSpellingAndQueueSync() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val db = AppDatabase.getDatabase(application)
    withContext(Dispatchers.IO) {
      db.clearAllTables()
    }
    val wordDao = db.wordDao()
    val repository = WordRepository(wordDao = wordDao, apiService = GeminiApiService.create(), context = application)
    val viewModel = DictionaryViewModel(application, repository)
    val today = java.time.LocalDate.now().toEpochDay()

    // 1. Insert bookmarked word & unbookmarked transient word
    val savedWord = WordEntity(
      word = "Ephemeral",
      phonetic = "/ɪˈfɛm.ər.əl/",
      partOfSpeech = "adjective",
      definition = "Lasting for a very short time.",
      example = "Fashions are ephemeral.",
      synonyms = "transient",
      imagePrompt = "sunset",
      isBookmarked = true,
      dueDate = today,
      easeFactor = 2.5f
    )
    val transientWord = WordEntity(
      word = "randomlookup",
      phonetic = "/r/",
      partOfSpeech = "noun",
      definition = "Just a transient lookup.",
      example = "Test",
      synonyms = "",
      imagePrompt = "",
      isBookmarked = false,
      repetitions = 0,
      dueDate = today + 2,
      easeFactor = 1.4f
    )
    wordDao.insertWord(savedWord)
    wordDao.insertWord(transientWord)

    // Verify case-insensitive DAO lookup
    assertNotNull(wordDao.getWord("ephemeral"))
    assertNotNull(wordDao.getWord("EPHEMERAL"))

    // 2. Test tags addition & deck-filtered review session
    viewModel.updateWordTags("ephemeral", listOf("IELTS", "Academic"))
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    delay(100)

    val tagged = wordDao.getWord("EPHEMERAL")!!
    assertTrue(tagged.tags.contains("IELTS"))
    assertTrue(tagged.tags.contains("Academic"))

    // 3. Test Spelling & Quiz Modes
    viewModel.generateNextSpellingQuestion()
    viewModel.updateSpellingInput("ephemeral")
    viewModel.revealSpellingHint()
    assertEquals(1, viewModel.spellingState.value.hintLevel)

    // 4. Verify queue sync on resetWordProgress & deleteFromHistory
    viewModel.rateCard(tagged, "Good")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    delay(100)
    assertEquals(1, wordDao.getWord("ephemeral")!!.repetitions)

    viewModel.resetWordProgress("EPHEMERAL")
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    delay(100)
    assertEquals(0, wordDao.getWord("ephemeral")!!.repetitions)
  }
}
