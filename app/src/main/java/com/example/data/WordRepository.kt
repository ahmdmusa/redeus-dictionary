package com.example.data

import android.content.Context
import android.util.Log
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GeminiApiService
import com.example.api.GenerationConfig
import com.example.api.OpenRouterApiService
import com.example.api.OpenRouterChatRequest
import com.example.api.OpenRouterMessage
import com.example.api.Part
import com.example.api.PexelsApiService
import com.example.api.ReadingPassageResponse
import com.example.api.ReadingQuestion
import com.example.api.WordApiResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WordRepository(
    private val wordDao: WordDao,
    private val apiService: GeminiApiService,
    private val pexelsApiService: PexelsApiService = PexelsApiService.create(),
    private val openRouterApiService: OpenRouterApiService = OpenRouterApiService.create(),
    private val context: Context? = null,
    private val readingSessionDao: ReadingSessionDao? = null,
    private val minedSentenceDao: MinedSentenceDao? = null
) {
    val allWords: Flow<List<WordEntity>> = wordDao.getAllWords()
    val bookmarkedWords: Flow<List<WordEntity>> = wordDao.getBookmarkedWords()
    val allReadingSessions: Flow<List<ReadingSessionEntity>> =
        readingSessionDao?.getAllSessions() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allMinedSentences: Flow<List<MinedSentenceEntity>> =
        minedSentenceDao?.getAllSentences() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertReadingSession(session: ReadingSessionEntity) = withContext(Dispatchers.IO) {
        readingSessionDao?.insertSession(session)
    }

    suspend fun getAllMinedSentencesList(): List<MinedSentenceEntity> = withContext(Dispatchers.IO) {
        minedSentenceDao?.getAllSentencesList() ?: emptyList()
    }

    suspend fun findMinedSentence(sentence: String, targetWord: String): MinedSentenceEntity? = withContext(Dispatchers.IO) {
        minedSentenceDao?.findBySentenceAndWord(sentence.trim(), targetWord.trim())
    }

    suspend fun insertMinedSentence(sentence: MinedSentenceEntity): Long = withContext(Dispatchers.IO) {
        minedSentenceDao?.insertSentence(sentence) ?: 0L
    }

    suspend fun insertMinedSentences(sentences: List<MinedSentenceEntity>) = withContext(Dispatchers.IO) {
        minedSentenceDao?.insertSentences(sentences)
    }

    suspend fun toggleMinedSentenceFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        minedSentenceDao?.updateFavorite(id, isFavorite)
    }

    suspend fun updateMinedSentencePractice(id: Long, practiceCount: Int, lastPracticed: Long) = withContext(Dispatchers.IO) {
        minedSentenceDao?.updatePractice(id, practiceCount, lastPracticed)
    }

    suspend fun deleteMinedSentence(sentence: MinedSentenceEntity) = withContext(Dispatchers.IO) {
        minedSentenceDao?.deleteSentence(sentence)
    }

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val responseAdapter = moshi.adapter(WordApiResponse::class.java).lenient()
    private val readingResponseAdapter = moshi.adapter(ReadingPassageResponse::class.java).lenient()

    fun getReadingJsonAdapter() = readingResponseAdapter

    suspend fun getLocalWord(word: String): WordEntity? = withContext(Dispatchers.IO) {
        wordDao.getWord(word.trim().lowercase())
    }

    suspend fun toggleBookmark(word: String, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        wordDao.updateBookmarkStatus(word, isBookmarked)
    }

    suspend fun updateWordTags(word: String, tags: String) = withContext(Dispatchers.IO) {
        wordDao.updateWordTags(word.trim(), tags)
    }

    suspend fun getBookmarkedWordsList(): List<WordEntity> = withContext(Dispatchers.IO) {
        wordDao.getBookmarkedWordsList()
    }

    suspend fun updateAudioPath(word: String, path: String?) = withContext(Dispatchers.IO) {
        wordDao.updateAudioPath(word.trim(), path)
    }

    suspend fun deleteWord(word: WordEntity) = withContext(Dispatchers.IO) {
        wordDao.deleteWord(word)
    }

    suspend fun updateWordReview(word: WordEntity) = withContext(Dispatchers.IO) {
        wordDao.insertWord(word)
    }

    suspend fun clearBookmarks() = withContext(Dispatchers.IO) {
        wordDao.clearBookmarks()
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        wordDao.clearHistory()
    }

    suspend fun getAllWordsList(): List<WordEntity> = withContext(Dispatchers.IO) {
        wordDao.getAllWordsList()
    }

    suspend fun clearLeechFlag(word: String) = withContext(Dispatchers.IO) {
        wordDao.clearLeechFlag(word.trim().lowercase())
    }

    suspend fun resetWordProgress(word: String) = withContext(Dispatchers.IO) {
        val today = java.time.LocalDate.now().toEpochDay()
        wordDao.resetWordProgress(word, today)
    }

    suspend fun resetAllWordsProgress() = withContext(Dispatchers.IO) {
        val today = java.time.LocalDate.now().toEpochDay()
        wordDao.resetAllWordsProgress(today)
    }

    private val wordDerivedQueries = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val wordAiImagePrompts = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun getDerivedQuery(word: String): String? = wordDerivedQueries[word.trim().lowercase()]

    suspend fun updateWordImagePrompt(word: String, imagePrompt: String) = withContext(Dispatchers.IO) {
        wordDao.updateWordImagePrompt(word, imagePrompt)
    }

    sealed class CyclePhotoResult {
        data class Success(val photoUrl: String, val page: Int) : CyclePhotoResult()
        data class WrappedToBeginning(val photoUrl: String) : CyclePhotoResult()
        object NoMoreResults : CyclePhotoResult()
        data class Error(val throwable: Throwable) : CyclePhotoResult()
    }

    suspend fun fetchPexelsPhotoForWord(
        wordEntity: WordEntity,
        page: Int,
        derivedQueryOverride: String? = null,
        querySourceOverride: String? = null,
        pexelsApiKey: String? = null
    ): CyclePhotoResult = withContext(Dispatchers.IO) {
        val userKey = pexelsApiKey
            ?: context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)?.getString("pexels_api_key", null)
            ?: ""
        val hasValidKey = userKey.isNotBlank() && userKey != "MY_PEXELS_API_KEY"
        if (!hasValidKey && java.lang.reflect.Proxy.isProxyClass(pexelsApiService.javaClass)) {
            return@withContext CyclePhotoResult.Error(IllegalStateException("Pexels API key is not configured"))
        }
        val pexelsKey = if (hasValidKey) userKey else "test_pexels_key"

        val effectiveQuerySource = querySourceOverride
            ?: context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)?.getString("pexels_query_source", "word")
            ?: "word"

        val cleanWord = wordEntity.word.trim().lowercase()
        val cacheKey = "$cleanWord:$effectiveQuerySource"
        val cachedQuery = wordDerivedQueries[cacheKey]
        val prefs = context?.getSharedPreferences("word_image_prompts", Context.MODE_PRIVATE)

        // 1. Get imagePrompt for the word (already generated by Gemini for that word)
        val imagePrompt = when {
            !wordEntity.imagePrompt.startsWith("http") && wordEntity.imagePrompt.isNotBlank() ->
                wordEntity.imagePrompt
            !wordAiImagePrompts[cleanWord].isNullOrBlank() ->
                wordAiImagePrompts[cleanWord]!!
            else ->
                prefs?.getString("ai_prompt_$cleanWord", null) ?: ""
        }
        if (imagePrompt.isNotBlank()) {
            wordAiImagePrompts[cleanWord] = imagePrompt
        }

        // 2. Query building based on pexels_query_source:
        // - "word": use the raw word/phrase directly as the query (no extraction needed)
        // - "definition": extract 2-4 concrete visual keywords from the definition field text
        // - "image_prompt": extract 2-4 concrete visual keywords from imagePrompt
        // (In all three cases, explicitly exclude the example field from ever being used)
        val query = when {
            !derivedQueryOverride.isNullOrBlank() -> derivedQueryOverride.trim()
            !cachedQuery.isNullOrBlank() -> cachedQuery
            effectiveQuerySource == "word" -> cleanWord
            effectiveQuerySource == "definition" -> extractPexelsKeywords(wordEntity.definition)
            else -> { // "image_prompt"
                if (imagePrompt.isNotBlank()) extractPexelsKeywords(imagePrompt)
                else prefs?.getString("pexels_query_image_prompt_$cleanWord", null)
                    ?: prefs?.getString("pexels_query_$cleanWord", null) ?: ""
            }
        }

        if (query.isBlank()) {
            Log.w("WordRepository", "No query could be derived using source '$effectiveQuerySource' for '$cleanWord'")
            return@withContext CyclePhotoResult.NoMoreResults
        }

        wordDerivedQueries[cacheKey] = query
        wordDerivedQueries[cleanWord] = query
        prefs?.edit()?.apply {
            putString("pexels_query_${effectiveQuerySource}_$cleanWord", query)
            putString("pexels_query_$cleanWord", query)
            if (imagePrompt.isNotBlank()) {
                putString("ai_prompt_$cleanWord", imagePrompt)
            }
            apply()
        }

        try {
            // 3. Send those keywords as the Pexels query parameter
            Log.d("WordRepository", "Searching Pexels for '$cleanWord' (query: '$query', page=$page)")
            var searchResponse = pexelsApiService.searchPhotos(
                apiKey = pexelsKey,
                query = query,
                perPage = 1,
                page = page
            )
            var photoUrl = searchResponse.photos?.firstOrNull()?.src?.medium

            if (!photoUrl.isNullOrBlank()) {
                return@withContext CyclePhotoResult.Success(photoUrl, page)
            }

            // If page > 1 and returned no results, wrap around to page 1
            if (page > 1) {
                Log.d("WordRepository", "No results on page $page for query '$query'. Wrapping around to page 1.")
                val wrapResponse = pexelsApiService.searchPhotos(
                    apiKey = pexelsKey,
                    query = query,
                    perPage = 1,
                    page = 1
                )
                val wrapUrl = wrapResponse.photos?.firstOrNull()?.src?.medium
                if (!wrapUrl.isNullOrBlank()) {
                    return@withContext CyclePhotoResult.WrappedToBeginning(wrapUrl)
                }
            }

            // Fallback: 2-word query (still derived strictly from imagePrompt keywords)
            val twoWordQuery = query.split(" ").take(2).joinToString(" ")
            if (twoWordQuery.isNotBlank() && twoWordQuery != query) {
                val fallbackResponse = pexelsApiService.searchPhotos(
                    apiKey = pexelsKey,
                    query = twoWordQuery,
                    perPage = 1,
                    page = page
                )
                val fallbackUrl = fallbackResponse.photos?.firstOrNull()?.src?.medium
                if (!fallbackUrl.isNullOrBlank()) {
                    return@withContext CyclePhotoResult.Success(fallbackUrl, page)
                }
            }

            CyclePhotoResult.NoMoreResults
        } catch (e: Exception) {
            Log.e("WordRepository", "Pexels photo search failed for '$cleanWord' at page $page", e)
            CyclePhotoResult.Error(e)
        }
    }

    suspend fun logReview(word: String, rating: String) = withContext(Dispatchers.IO) {
        wordDao.insertReviewLog(ReviewLogEntity(word = word, rating = rating))
    }

    suspend fun getAllReviewLogs(): List<ReviewLogEntity> = withContext(Dispatchers.IO) {
        wordDao.getAllReviewLogs()
    }

    fun getReviewLogsForWord(word: String): Flow<List<ReviewLogEntity>> =
        wordDao.getReviewLogsForWord(word)

    suspend fun getReviewLogsForWordList(word: String): List<ReviewLogEntity> = withContext(Dispatchers.IO) {
        wordDao.getReviewLogsForWordList(word)
    }

    suspend fun clearReviewLogs() = withContext(Dispatchers.IO) {
        wordDao.clearReviewLogs()
    }

    suspend fun searchAndFetchWord(
        word: String,
        apiKey: String = "",
        aiVisualizationsEnabled: Boolean = true,
        imageSource: String? = null,
        openRouterApiKey: String = "",
        pexelsQuerySource: String? = null,
        geminiApiKeys: List<String> = emptyList(),
        geminiModel: String = "gemini-3.5-flash-lite",
        pexelsApiKey: String = ""
    ): Result<WordEntity> = withContext(Dispatchers.IO) {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Word cannot be empty"))
        }

        // 1. Check local cache
        val localCached = wordDao.getWord(cleanWord)
        if (localCached != null) {
            // Update timestamp to bring it to the top of history
            val updated = localCached.copy(timestamp = System.currentTimeMillis())
            wordDao.insertWord(updated)
            return@withContext Result.success(updated)
        }

        val systemPrompt = """
            You are an expert English learning assistant. Analyze the English term provided, which may be a single word, a Latin-origin word or phrase commonly used in English (e.g. 'de facto', 'et cetera'), a medical or scientific term (which may be one or more words, e.g. 'myocardial infarction'), or a common multi-word expression/idiom/compound phrase (e.g. 'bad person', 'give up', 'on the fence').
            Define the word precisely and prepare learning materials.
            You MUST return a response that conforms strictly to the specified JSON schema.
            If the prompt is not a valid English word (for example, random gibberish like 'asdfasdf'), do NOT invent details. Instead, set 'definition' to 'NOT_A_REAL_WORD'. Only set 'definition' to 'NOT_A_REAL_WORD' for genuine gibberish or typos, never for legitimate multi-word terms or phrases.
            Return ONLY raw, valid JSON. Do NOT wrap your output in markdown code blocks (e.g. do not use ```json or ```).
        """.trimIndent()

        // Real photo ("web") is always used as the image source
        val effectiveImageSource = "web"

        val shouldGenerateImagePrompt = aiVisualizationsEnabled || effectiveImageSource == "web"

        val userPrompt = if (shouldGenerateImagePrompt) {
            """
            Please analyze the following English word, phrase, or term: "$cleanWord".
            Provide details in JSON conforming strictly to the following parameters:
            {
              "word": "The exact word searched",
              "phonetic": "American and/or British pronunciation IPA. If they differ, provide both labeled, e.g. '/ˈkæst.əl/ (US) /ˈkɑːs.əl/ (UK)'. If they are the same or only one is available, provide it as a single IPA enclosed in slashes, e.g. '/ˈæp.əl/'",
              "partOfSpeech": "Part of speech lowercase (noun, verb, adjective, adverb, pronoun, preposition, conjunction, interjection, idiom, phrase, medical term, or compound noun)",
              "definition": "A clear, simple, concise dictionary definition tailored for language learners",
              "synonyms": ["List of 3-5 distinct synonyms"],
              "antonyms": ["List of 1-3 distinct antonyms"],
              "example": "A natural example sentence demonstrating the word in context",
              "mnemonic": "A short, clever memory hook or trick to remember the meaning (e.g. 'Abate: think about bate/bait, when fish bite less, the biting abates')",
              "imagePrompt": "A 15-25 word text-to-image prompt describing a concrete, literal scene that directly illustrates this word's specific meaning (based on its partOfSpeech and example sentence)—not a vague symbolic or emotional mood image. The main subject and action must be immediately recognizable as representing this exact word's meaning to someone who doesn't know the word yet. Avoid generic or interchangeable imagery (like glowing plants, lone figures on hills, or vague nature metaphors). Keep the core subject clear and specific, with light artistic style and lighting. Do NOT include text, letters, or spelling in the prompt."
            }
            """.trimIndent()
        } else {
            """
            Please analyze the following English word, phrase, or term: "$cleanWord".
            Provide details in JSON conforming strictly to the following parameters:
            {
              "word": "The exact word searched",
              "phonetic": "American and/or British pronunciation IPA. If they differ, provide both labeled, e.g. '/ˈkæst.əl/ (US) /ˈkɑːs.əl/ (UK)'. If they are the same or only one is available, provide it as a single IPA enclosed in slashes, e.g. '/ˈæp.əl/'",
              "partOfSpeech": "Part of speech lowercase (noun, verb, adjective, adverb, pronoun, preposition, conjunction, interjection, idiom, phrase, medical term, or compound noun)",
              "definition": "A clear, simple, concise dictionary definition tailored for language learners",
              "synonyms": ["List of 3-5 distinct synonyms"],
              "antonyms": ["List of 1-3 distinct antonyms"],
              "example": "A natural example sentence demonstrating the word in context",
              "mnemonic": "A short, clever memory hook or trick to remember the meaning (e.g. 'Abate: think about bate/bait, when fish bite less, the biting abates')",
              "imagePrompt": ""
            }
            Note: Set "imagePrompt" to an empty string. Do not generate an image prompt.
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = userPrompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val keysToUse = if (geminiApiKeys.isNotEmpty()) geminiApiKeys else if (apiKey.isNotBlank()) listOf(apiKey) else emptyList()
        val effectiveOpenRouterKey = if (openRouterApiKey.isNotBlank()) openRouterApiKey else (context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)?.getString("openrouter_api_key", "") ?: "")
        val effectivePexelsKey = if (pexelsApiKey.isNotBlank()) pexelsApiKey else (context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)?.getString("pexels_api_key", "") ?: "")
        val effectiveModel = geminiModel.removePrefix("models/").trim().ifBlank { "gemini-3.5-flash-lite" }

        val aiResult = fetchAiTextWithFallback(
            tag = "searchWord",
            geminiApiKeys = keysToUse,
            geminiModel = effectiveModel,
            geminiRequest = request,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            openRouterApiKey = effectiveOpenRouterKey
        )

        aiResult.fold(
            onSuccess = { candidateText ->
                try {
                    val sanitizedJson = sanitizeJsonResponse(candidateText)
                    val apiResponse = responseAdapter.fromJson(sanitizedJson)
                        ?: return@withContext Result.failure(Exception("Failed to parse JSON response: $sanitizedJson"))

                    if (apiResponse.definition == "NOT_A_REAL_WORD") {
                        return@withContext Result.failure(IllegalArgumentException("'$cleanWord' was not recognized as a valid English word, phrase, or term."))
                    }

                    // Real photo ("web") is always used as the image source
                    val effectiveImageSource = "web"

                    var finalImagePrompt = apiResponse.imagePrompt
                    val cleanAiPrompt = apiResponse.imagePrompt
                    if (cleanAiPrompt.isNotBlank()) {
                        wordAiImagePrompts[cleanWord] = cleanAiPrompt
                        context?.getSharedPreferences("word_image_prompts", Context.MODE_PRIVATE)?.edit()
                            ?.putString("ai_prompt_$cleanWord", cleanAiPrompt)
                            ?.apply()
                    }

                    if (effectiveImageSource == "web") {
                        val hasValidKey = effectivePexelsKey.isNotBlank() && effectivePexelsKey != "MY_PEXELS_API_KEY"
                        val isProxy = java.lang.reflect.Proxy.isProxyClass(pexelsApiService.javaClass)
                        if (hasValidKey || !isProxy) {
                            val pexelsKey = if (hasValidKey) effectivePexelsKey else "test_pexels_key"
                            try {
                                val effectivePexelsQuerySource = pexelsQuerySource
                                    ?: context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)?.getString("pexels_query_source", "word")
                                    ?: "word"

                                val derivedQuery = when (effectivePexelsQuerySource) {
                                    "word" -> cleanWord
                                    "definition" -> extractPexelsKeywords(apiResponse.definition)
                                    else -> extractPexelsKeywords(cleanAiPrompt) // "image_prompt"
                                }

                                if (derivedQuery.isNotBlank()) {
                                    wordDerivedQueries[cleanWord] = derivedQuery
                                    wordDerivedQueries["$cleanWord:$effectivePexelsQuerySource"] = derivedQuery
                                    context?.getSharedPreferences("word_image_prompts", Context.MODE_PRIVATE)?.edit()
                                        ?.putString("pexels_query_$cleanWord", derivedQuery)
                                        ?.apply()
                                    Log.d("WordRepository", "Pexels search using derived query '$derivedQuery' (source: '$effectivePexelsQuerySource', word: '$cleanWord')")

                                    // 3. Send those keywords as the Pexels query parameter
                                    var searchResponse = pexelsApiService.searchPhotos(
                                        apiKey = pexelsKey,
                                        query = derivedQuery,
                                        perPage = 1,
                                        page = 1
                                    )
                                    var photoUrl = searchResponse.photos?.firstOrNull()?.src?.medium

                                    // Fall back to 2-word query if 3+ keywords returned no results
                                    if (photoUrl.isNullOrBlank()) {
                                        val twoWordQuery = derivedQuery.split(" ").take(2).joinToString(" ")
                                        if (twoWordQuery.isNotBlank() && twoWordQuery != derivedQuery) {
                                            Log.d("WordRepository", "Pexels fallback to 2-word query: '$twoWordQuery'")
                                            searchResponse = pexelsApiService.searchPhotos(
                                                apiKey = pexelsKey,
                                                query = twoWordQuery,
                                                perPage = 1
                                            )
                                            photoUrl = searchResponse.photos?.firstOrNull()?.src?.medium
                                        }
                                    }

                                    // Fall back to the word itself if derived query returned no results
                                    if (photoUrl.isNullOrBlank() && cleanWord != derivedQuery) {
                                        Log.d("WordRepository", "Pexels fallback to word itself: '$cleanWord'")
                                        searchResponse = pexelsApiService.searchPhotos(
                                            apiKey = pexelsKey,
                                            query = cleanWord,
                                            perPage = 1
                                        )
                                        photoUrl = searchResponse.photos?.firstOrNull()?.src?.medium
                                    }

                                    if (!photoUrl.isNullOrBlank()) {
                                        finalImagePrompt = photoUrl
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w("WordRepository", "Pexels photo search failed, falling back to AI prompt", e)
                            }
                        } else {
                            Log.d("WordRepository", "Pexels API key not configured, falling back to AI generated prompt")
                        }
                    }

                    // Map and save to database
                    val wordEntity = WordEntity(
                        word = apiResponse.word.trim().lowercase(),
                        phonetic = apiResponse.phonetic,
                        partOfSpeech = apiResponse.partOfSpeech,
                        definition = apiResponse.definition,
                        synonyms = apiResponse.synonyms.joinToString(", "),
                        antonyms = apiResponse.antonyms.joinToString(", "),
                        example = apiResponse.example,
                        mnemonic = apiResponse.mnemonic,
                        imagePrompt = finalImagePrompt,
                        isBookmarked = false,
                        timestamp = System.currentTimeMillis()
                    )

                    wordDao.insertWord(wordEntity)
                    Result.success(wordEntity)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun insertWordIfNotExist(word: WordEntity) = withContext(Dispatchers.IO) {
        val existing = wordDao.getWord(word.word.lowercase())
        if (existing == null) {
            wordDao.insertWord(word)
        } else if (!existing.isBookmarked) {
            wordDao.insertWord(existing.copy(isBookmarked = true))
        }
    }

    fun getRecentlyUsedReadingWordsMap(): Map<String, Long> {
        val prefs = context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) ?: return emptyMap()
        val jsonStr = prefs.getString("reading_words_last_used", null) ?: return emptyMap()
        val map = mutableMapOf<String, Long>()
        try {
            val json = org.json.JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key.lowercase()] = json.optLong(key, 0L)
            }
        } catch (_: Exception) {}
        return map
    }

    suspend fun recordWordsUsedInReading(words: List<String>) = withContext(Dispatchers.IO) {
        if (words.isEmpty()) return@withContext
        val now = System.currentTimeMillis()
        words.forEach { word ->
            wordDao.updateLastUsedInReadingDate(word, now)
        }
        val prefs = context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (prefs != null) {
            val map = getRecentlyUsedReadingWordsMap().toMutableMap()
            words.forEach { map[it.lowercase()] = now }
            try {
                val json = org.json.JSONObject()
                map.forEach { (k, v) -> json.put(k, v) }
                prefs.edit().putString("reading_words_last_used", json.toString()).apply()
            } catch (_: Exception) {}
        }
    }

    suspend fun getLastReadingSession(): ReadingSessionEntity? = withContext(Dispatchers.IO) {
        readingSessionDao?.getLastSession()
    }

    suspend fun getSavedWordsForReading(): List<String> = withContext(Dispatchers.IO) {
        val allWords = wordDao.getAllWordsList()
        val bookmarked = wordDao.getBookmarkedWordsList()

        val combinedMap = LinkedHashMap<String, WordEntity>()
        bookmarked.forEach { combinedMap[it.word.lowercase()] = it }
        allWords.forEach {
            if (!combinedMap.containsKey(it.word.lowercase())) {
                combinedMap[it.word.lowercase()] = it
            }
        }
        val uniqueWords = combinedMap.values.toList()
        if (uniqueWords.size < 2) {
            return@withContext emptyList()
        }

        // Randomly select only 1-2 words per passage
        val wordsToPick = kotlin.random.Random.nextInt(1, 3).coerceAtMost(uniqueWords.size)

        // Prioritize words that haven't been picked recently using lastUsedInReadingDate and preferences
        val usedMap = getRecentlyUsedReadingWordsMap()
        val sortedByLastUsed = uniqueWords.sortedBy { entity ->
            entity.lastUsedInReadingDate ?: usedMap[entity.word.lowercase()] ?: 0L
        }

        val minTimestamp = sortedByLastUsed.firstOrNull()?.let {
            it.lastUsedInReadingDate ?: usedMap[it.word.lowercase()] ?: 0L
        } ?: 0L
        val leastRecentlyUsed = sortedByLastUsed.filter {
            (it.lastUsedInReadingDate ?: usedMap[it.word.lowercase()] ?: 0L) == minTimestamp
        }
        val candidatePool = if (leastRecentlyUsed.size >= wordsToPick) {
            leastRecentlyUsed.shuffled()
        } else {
            val poolSize = minOf(uniqueWords.size, maxOf(wordsToPick * 2, 4))
            sortedByLastUsed.take(poolSize).shuffled()
        }

        candidatePool.take(wordsToPick).map { it.word }
    }

    suspend fun generateDailyReading(
        readingLevel: Int,
        targetWords: List<String>,
        apiKey: String = "",
        openRouterApiKey: String = "",
        geminiApiKeys: List<String> = emptyList(),
        geminiModel: String = "gemini-3.5-flash-lite"
    ): Result<ReadingPassageResponse> = withContext(Dispatchers.IO) {
        val cefrLevel = when (readingLevel) {
            1, 2 -> "A2"
            3 -> "A2-B1 transitional"
            4, 5 -> "B1"
            else -> "A2"
        }

        val systemPrompt = """
            You are an expert English language learning and reading comprehension assistant.
            Generate an engaging, educational short reading passage, exactly 5 multiple-choice reading comprehension questions, and exactly 2 multiple-choice grammar questions based on structures in the passage.
            You MUST return a response that conforms strictly to the specified JSON schema.
            Return ONLY raw, valid JSON. Do NOT wrap your output in markdown code blocks (e.g. do not use ```json or ```).
        """.trimIndent()

        val userPrompt = if (targetWords.isNotEmpty()) {
            """
            Create a reading comprehension and grammar check exercise for an English learner at CEFR level: $cefrLevel.
            Use ONLY the following 1-2 vocabulary word(s) naturally in the story — do not force in extra vocabulary, prioritize a coherent, natural short story over vocabulary density. If a word doesn't fit naturally, use it just once in a clear, well-formed sentence rather than repeating or over-emphasizing it.

            Vocabulary word(s) to incorporate:
            ${targetWords.joinToString(", ")}

            Requirements:
            1. "passage": An 80-120 word short story or paragraph at $cefrLevel level naturally incorporating the specified vocabulary word(s). Prioritize a coherent, natural story over vocabulary density.
            2. "questions": Exactly 5 multiple-choice questions mixing reading comprehension of the passage's content with meaning-in-context questions for the embedded vocabulary word(s). No grammar questions in this list.
            Each question MUST have:
            - "question": Clear question text
            - "options": An array of exactly 4 distinct options
            - "correctAnswer": Exact string matching one of the 4 options
            - "explanation": Short, clear explanation of why the correct answer is right
            3. "grammarQuestions": Exactly 2 multiple-choice grammar questions, each based on an actual grammatical structure that genuinely appears in the passage (such as a verb tense, a conditional, an article, a preposition, subject-verb agreement, etc.). Pick the structure from the passage itself; NEVER invent one that isn't present in the text.
            Each grammar question MUST have:
            - "question": Clear question text testing the grammatical structure from the passage
            - "options": An array of exactly 4 distinct options
            - "correctAnswer": Exact string matching one of the 4 options
            - "explanation": State the grammar rule being tested in plain, simple terms matching the user's current CEFR level ($cefrLevel). Explain why the correct option is right AND why common mistake options for that rule are wrong (e.g. "We use 'have been' here because the action started in the past and is still continuing (present perfect continuous). 'Was' would only be correct if the action had already finished.").

            JSON schema:
            {
              "passage": "80-120 word short story or paragraph",
              "questions": [
                {
                  "question": "question text",
                  "options": ["option 1", "option 2", "option 3", "option 4"],
                  "correctAnswer": "exact matching option",
                  "explanation": "explanation text"
                }
              ],
              "grammarQuestions": [
                {
                  "question": "grammar question text",
                  "options": ["option 1", "option 2", "option 3", "option 4"],
                  "correctAnswer": "exact matching option",
                  "explanation": "grammar rule explanation why correct and why common mistakes are wrong"
                }
              ]
            }
            """.trimIndent()
        } else {
            """
            Create a reading comprehension and grammar check exercise for an English learner at CEFR level: $cefrLevel on an engaging, relatable everyday topic (such as travel, daily habits, friendship, hobbies, or community).

            Requirements:
            1. "passage": An 80-120 word short story or paragraph at $cefrLevel level with natural, accessible language.
            2. "questions": Exactly 5 multiple-choice questions focusing purely on reading comprehension of the passage's content (e.g. key details, main theme, character motives, cause and effect). Do NOT ask about specific-word meaning-in-context, since no target vocabulary was embedded. No grammar questions in this list.
            Each question MUST have:
            - "question": Clear question text
            - "options": An array of exactly 4 distinct options
            - "correctAnswer": Exact string matching one of the 4 options
            - "explanation": Short, clear explanation of why the correct answer is right
            3. "grammarQuestions": Exactly 2 multiple-choice grammar questions, each based on an actual grammatical structure that genuinely appears in the passage (such as a verb tense, a conditional, an article, a preposition, subject-verb agreement, etc.). Pick the structure from the passage itself; NEVER invent one that isn't present in the text.
            Each grammar question MUST have:
            - "question": Clear question text testing the grammatical structure from the passage
            - "options": An array of exactly 4 distinct options
            - "correctAnswer": Exact string matching one of the 4 options
            - "explanation": State the grammar rule being tested in plain, simple terms matching the user's current CEFR level ($cefrLevel). Explain why the correct option is right AND why common mistake options for that rule are wrong (e.g. "We use 'have been' here because the action started in the past and is still continuing (present perfect continuous). 'Was' would only be correct if the action had already finished.").

            JSON schema:
            {
              "passage": "80-120 word short story or paragraph",
              "questions": [
                {
                  "question": "question text",
                  "options": ["option 1", "option 2", "option 3", "option 4"],
                  "correctAnswer": "exact matching option",
                  "explanation": "explanation text"
                }
              ],
              "grammarQuestions": [
                {
                  "question": "grammar question text",
                  "options": ["option 1", "option 2", "option 3", "option 4"],
                  "correctAnswer": "exact matching option",
                  "explanation": "grammar rule explanation why correct and why common mistakes are wrong"
                }
              ]
            }
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = userPrompt)))
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.5f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val keysToUse = if (geminiApiKeys.isNotEmpty()) geminiApiKeys else if (apiKey.isNotBlank()) listOf(apiKey) else emptyList()
        val effectiveOpenRouterKey = if (openRouterApiKey.isNotBlank()) openRouterApiKey else (context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)?.getString("openrouter_api_key", "") ?: "")
        val effectiveModel = geminiModel.removePrefix("models/").trim().ifBlank { "gemini-3.5-flash-lite" }

        val aiResult = fetchAiTextWithFallback(
            tag = "generateDailyReading",
            geminiApiKeys = keysToUse,
            geminiModel = effectiveModel,
            geminiRequest = request,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            openRouterApiKey = effectiveOpenRouterKey
        )

        aiResult.fold(
            onSuccess = { candidateText ->
                try {
                    val sanitizedJson = sanitizeJsonResponse(candidateText)
                    val passageResponse = readingResponseAdapter.fromJson(sanitizedJson)
                        ?: return@withContext Result.failure(Exception("Failed to parse reading response: $sanitizedJson"))

                    if (passageResponse.passage.isBlank() || passageResponse.questions.isEmpty()) {
                        return@withContext Result.failure(Exception("Incomplete reading passage response generated"))
                    }

                    if (targetWords.isNotEmpty()) {
                        recordWordsUsedInReading(targetWords)
                    }

                    val typedQuestions = passageResponse.questions.map { it.copy(type = "comprehension") }
                    val typedGrammar = passageResponse.grammarQuestions.map { it.copy(type = "grammar") }
                    Result.success(passageResponse.copy(questions = typedQuestions, grammarQuestions = typedGrammar))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    fun isGeminiManuallyLocked(): Boolean {
        val ctx = context ?: return false
        val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toEpochDay()
        val lockedDate = prefs.getLong("gemini_locked_date", -1L)
        if (lockedDate != today) {
            prefs.edit()
                .putLong("gemini_locked_date", today)
                .putBoolean("gemini_manually_locked", false)
                .apply()
            return false
        }
        return prefs.getBoolean("gemini_manually_locked", false)
    }

    private fun isHttp429(e: Throwable): Boolean {
        var cur: Throwable? = e
        while (cur != null) {
            if (cur is retrofit2.HttpException && cur.code() == 429) return true
            val msg = cur.message?.lowercase() ?: ""
            if (msg.contains("429") || msg.contains("resource_exhausted") || msg.contains("quota")) {
                return true
            }
            cur = cur.cause
        }
        return false
    }

    private suspend fun fetchAiTextWithFallback(
        tag: String,
        geminiApiKeys: List<String>,
        geminiModel: String = "gemini-3.5-flash-lite",
        geminiRequest: GenerateContentRequest,
        systemPrompt: String,
        userPrompt: String,
        openRouterApiKey: String
    ): Result<String> {
        val isLocked = isGeminiManuallyLocked()
        val validGeminiKeys = geminiApiKeys.map { it.trim() }.filter { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }

        if (!isLocked && validGeminiKeys.isNotEmpty()) {
            var lastGeminiEx: Exception? = null
            val effectiveModel = geminiModel.removePrefix("models/").trim().ifBlank { "gemini-3.5-flash-lite" }

            for ((idx, key) in validGeminiKeys.withIndex()) {
                try {
                    Log.d("WordRepository", "[$tag] Trying Gemini key ${idx + 1}/${validGeminiKeys.size} with model '$effectiveModel'...")
                    val response = apiService.generateContentWithModel(effectiveModel, key, geminiRequest)
                    val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!candidateText.isNullOrBlank()) {
                        Log.d("WordRepository", "[$tag] Response successfully served by Gemini (key ${idx + 1})")
                        recordAiProviderUsage("gemini")
                        return Result.success(candidateText)
                    }
                    throw Exception("No text content received from Gemini model")
                } catch (geminiEx: Exception) {
                    lastGeminiEx = geminiEx
                    val is429 = isHttp429(geminiEx)
                    Log.w("WordRepository", "[$tag] Gemini key ${idx + 1} failed: ${geminiEx.message} (is429=$is429)")

                    // If HTTP 429 and there are remaining keys, try the next key in order
                    if (is429 && idx < validGeminiKeys.size - 1) {
                        Log.w("WordRepository", "[$tag] HTTP 429 rate limit encountered; trying next Gemini key...")
                        continue
                    } else {
                        // Either not 429, or all keys exhausted; break and fall through to OpenRouter
                        break
                    }
                }
            }
            Log.w("WordRepository", "[$tag] Gemini calls failed/exhausted (${lastGeminiEx?.message}), attempting OpenRouter fallback...", lastGeminiEx)
            return callOpenRouter(tag, systemPrompt, userPrompt, openRouterApiKey, lastGeminiEx)
        } else if (isLocked) {
            Log.d("WordRepository", "[$tag] Gemini manually locked for today, skipping Gemini and calling OpenRouter directly...")
            return callOpenRouter(tag, systemPrompt, userPrompt, openRouterApiKey, null)
        } else {
            // No Gemini keys configured: skip Gemini silently and go straight to OpenRouter
            Log.d("WordRepository", "[$tag] No Gemini keys configured, skipping Gemini and calling OpenRouter directly...")
            return callOpenRouter(tag, systemPrompt, userPrompt, openRouterApiKey, null)
        }
    }

    private suspend fun callOpenRouter(
        tag: String,
        systemPrompt: String,
        userPrompt: String,
        openRouterApiKey: String,
        geminiEx: Exception?
    ): Result<String> {
        val cleanKey = openRouterApiKey.trim()
        if (cleanKey.isBlank() || cleanKey == "MY_OPENROUTER_API_KEY") {
            val ex = if (geminiEx != null) {
                IllegalStateException("OpenRouter fallback unavailable: No OpenRouter API key configured. (Gemini error: ${geminiEx.message})", geminiEx)
            } else {
                IllegalStateException("No API key configured for Gemini or OpenRouter.")
            }
            return Result.failure(ex)
        }

        try {
            val messages = mutableListOf<OpenRouterMessage>()
            if (systemPrompt.isNotBlank()) {
                messages.add(OpenRouterMessage(role = "system", content = systemPrompt))
            }
            messages.add(OpenRouterMessage(role = "user", content = userPrompt))

            val openRouterRequest = OpenRouterChatRequest(
                model = "openrouter/free",
                messages = messages
            )
            val authHeader = if (cleanKey.startsWith("Bearer ")) cleanKey else "Bearer $cleanKey"
            val orResponse = openRouterApiService.createChatCompletion(authHeader, openRouterRequest)
            val choiceText = orResponse.choices?.firstOrNull()?.message?.content
            if (!choiceText.isNullOrBlank()) {
                Log.d("WordRepository", "[$tag] Response successfully served by OpenRouter fallback")
                recordAiProviderUsage("openrouter")
                return Result.success(choiceText)
            }
            throw Exception("No text content returned from OpenRouter choices", geminiEx)
        } catch (openRouterEx: Exception) {
            val geminiMsg = geminiEx?.message ?: "skipped (locked or no key)"
            Log.e("WordRepository", "[$tag] OpenRouter call failed. Gemini: $geminiMsg, OpenRouter: ${openRouterEx.message}", openRouterEx)
            return Result.failure(openRouterEx)
        }
    }

    fun recordAiProviderUsage(provider: String) {
        val ctx = context ?: return
        try {
            val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val today = java.time.LocalDate.now().toEpochDay()
            val savedDate = prefs.getLong("usage_stats_date", -1L)

            val editor = prefs.edit()
            var geminiCalls = prefs.getInt("gemini_calls_today", 0)
            var openRouterCalls = prefs.getInt("openrouter_calls_today", 0)

            if (savedDate != today) {
                geminiCalls = 0
                openRouterCalls = 0
                editor.putLong("usage_stats_date", today)
            }

            if (provider == "gemini") {
                geminiCalls += 1
                editor.putInt("gemini_calls_today", geminiCalls)
                editor.putInt("openrouter_calls_today", openRouterCalls)
                editor.putString("last_provider_used", "gemini")
            } else if (provider == "openrouter") {
                openRouterCalls += 1
                editor.putInt("gemini_calls_today", geminiCalls)
                editor.putInt("openrouter_calls_today", openRouterCalls)
                editor.putString("last_provider_used", "openrouter")
            }
            editor.apply()
        } catch (e: Exception) {
            Log.e("WordRepository", "Failed to update AI usage stats", e)
        }
    }

    private fun sanitizeJsonResponse(rawText: String): String {
        var text = rawText.trim()
        if (text.contains("```")) {
            val afterFirstFence = text.substringAfter("```")
            val codeBlock = afterFirstFence.substringBeforeLast("```")
            text = codeBlock.removePrefix("json").trim()
        }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }
        return text.trim()
    }

    suspend fun testGeminiApiKey(
        apiKey: String,
        geminiModel: String = "gemini-3.5-flash-lite"
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a Gemini API key first."))
        }
        val effectiveModel = geminiModel.removePrefix("models/").trim().ifBlank { "gemini-3.5-flash-lite" }
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = "Reply with OK")))),
            generationConfig = GenerationConfig(temperature = 0.0f)
        )
        runCatching {
            val response = apiService.generateContentWithModel(effectiveModel, cleanKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text.isNullOrBlank()) {
                error("API responded with an empty message.")
            }
            "Connected ($effectiveModel)"
        }
    }

    suspend fun translateWordToArabic(
        word: String,
        definition: String,
        geminiApiKeys: List<String>,
        geminiModel: String = "gemini-3.5-flash-lite",
        openRouterApiKey: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val systemPrompt = "You are a concise English-to-Arabic dictionary translator. Return ONLY the Arabic translation and brief Arabic meaning in 1-2 short lines, with no English intro or markdown."
        val userPrompt = "Translate the English word \"$word\" (definition: \"$definition\") into natural Arabic."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
            generationConfig = GenerationConfig(temperature = 0.2f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )
        val effectiveOpenRouterKey = if (openRouterApiKey.isNotBlank()) openRouterApiKey else (
            context?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)?.getString("openrouter_api_key", "") ?: ""
        )
        val effectiveModel = geminiModel.removePrefix("models/").trim().ifBlank { "gemini-3.5-flash-lite" }
        fetchAiTextWithFallback(
            tag = "arabicTranslation",
            geminiApiKeys = geminiApiKeys,
            geminiModel = effectiveModel,
            geminiRequest = request,
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            openRouterApiKey = effectiveOpenRouterKey
        ).map { it.trim() }
    }

    companion object {
        private val PEXELS_STOP_WORDS = setOf(
            // Articles
            "a", "an", "the",
            // Prepositions
            "about", "above", "across", "after", "against", "along", "among", "around", "at",
            "before", "behind", "below", "beneath", "beside", "between", "beyond", "by",
            "down", "during", "except", "for", "from", "in", "inside", "into", "near",
            "of", "off", "on", "onto", "out", "outside", "over", "past", "through",
            "throughout", "to", "toward", "towards", "under", "underneath", "until", "up",
            "upon", "with", "within", "without",
            // Conjunctions
            "and", "but", "or", "nor", "so", "yet", "both", "either", "neither",
            "as", "because", "although", "though", "even", "while", "if", "unless", "since",
            // Pronouns
            "i", "me", "my", "myself", "we", "us", "our", "ours", "ourselves",
            "you", "your", "yours", "yourself", "yourselves",
            "he", "him", "his", "himself", "she", "her", "hers", "herself",
            "it", "its", "itself", "they", "them", "their", "theirs", "themselves",
            "what", "which", "who", "whom", "this", "that", "these", "those", "whose",
            // Auxiliary & linking verbs
            "is", "am", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "having",
            "do", "does", "did", "doing",
            "can", "could", "shall", "should", "will", "would", "may", "might", "must",
            // Prompt filler, photo meta descriptions & generic artistic words
            "photo", "photograph", "photography", "picture", "image", "shot", "scene", "view",
            "showing", "shows", "depicting", "depicts", "illustrating", "illustrates", "representing", "represents",
            "realistic", "hyperrealistic", "photorealistic", "cinematic", "lighting", "style",
            "background", "foreground", "closeup", "close-up", "detailed", "focus", "sharp", "blur",
            "high-resolution", "render", "illustration", "camera", "lens", "macro", "portrait", "landscape",
            // Filler modifiers
            "very", "extremely", "gently", "slowly", "slightly", "really", "quite",
            "always", "never", "just", "also", "some", "any", "many", "much", "more",
            "most", "several", "few", "all", "each", "every", "other", "another",
            "such", "no", "not", "only", "own", "same", "than", "too"
        )

        /**
         * Extracts 2-4 concrete visual keywords from the Gemini-generated imagePrompt specifically.
         * The query sent to Pexels must be built ONLY from the imagePrompt field (the AI visual-scene
         * description meant for the Pollinations image, e.g. a description that visually represents
         * the word's core meaning) — never from the example field, and never from the raw word alone.
         */
        fun extractPexelsKeywords(imagePrompt: String): String {
            if (imagePrompt.isBlank() || imagePrompt.startsWith("http://") || imagePrompt.startsWith("https://")) {
                return ""
            }

            val cleaned = imagePrompt.lowercase()
                .replace(Regex("[^a-z0-9\\s-]"), " ")

            val words = cleaned.split(Regex("\\s+"))
                .map { it.trim().trim('-') }
                .filter { it.length > 2 && it !in PEXELS_STOP_WORDS }
                .distinct()

            return when {
                words.isEmpty() -> ""
                words.size <= 2 -> words.joinToString(" ")
                else -> words.take(3).joinToString(" ") // Extract 3 concrete visual keywords (within 2-4 range)
            }
        }

        @Deprecated("Query must be built only from imagePrompt, never from raw word or fallback word", ReplaceWith("extractPexelsKeywords(imagePrompt)"))
        fun extractPexelsKeywords(imagePrompt: String, @Suppress("UNUSED_PARAMETER") fallbackWord: String): String {
            return extractPexelsKeywords(imagePrompt)
        }
    }
}
