package com.example.ui

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Base64
import com.example.BuildConfig
import com.example.data.WordEntity
import com.example.data.WordRepository
import com.example.data.ReviewLogEntity
import com.example.data.ReadingSessionEntity
import com.example.data.MinedSentenceEntity
import com.example.api.DictionaryAudioApiService
import com.example.api.selectAudioUrl
import com.example.data.LocalWordImageStore
import com.example.data.OfflineBook
import com.example.data.OfflineCurriculumWord
import com.example.data.OfflineDictionaryDatabase
import com.example.data.OfflineSearchSuggestion
import com.example.data.OfflineTopic
import com.example.data.OfflineUnit
import com.example.domain.ActiveStudySession
import com.example.domain.CardStudyBucket
import com.example.domain.CourseProgressCalculator
import com.example.domain.LastReviewSnapshot
import com.example.domain.SessionSummaryData
import com.example.domain.StudyQueuePlanner
import com.example.domain.StudyRecallMode
import com.example.domain.TodayQueueSummary
import io.github.openspacedrepetition.Card
import io.github.openspacedrepetition.Rating
import io.github.openspacedrepetition.Scheduler
import io.github.openspacedrepetition.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DetailedSrsStats(
    val totalReviewsAllTime: Int = 0,
    val totalReviewsLast30Days: Int = 0,
    val overallRetentionRate: Float = 0f,
    val last30DaysRetentionRate: Float = 0f,
    val reviewsPerDayLast30Days: List<DailyReviewStat> = emptyList(),
    val ratingDistribution: SrsRatingDistribution = SrsRatingDistribution(),
    val easeFactorDistribution: EaseFactorDistribution = EaseFactorDistribution(),
    val forecastNext14Days: List<ForecastDayStat> = emptyList(),
    val streakHistoryLast30Days: List<DayStreakStatus> = emptyList(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val leechWordsCount: Int = 0
)

data class DailyReviewStat(
    val epochDay: Long,
    val dateLabel: String,
    val count: Int
)

data class SrsRatingDistribution(
    val againCount: Int = 0,
    val hardCount: Int = 0,
    val goodCount: Int = 0,
    val easyCount: Int = 0,
    val totalCount: Int = 0
) {
    val againPct: Float get() = if (totalCount > 0) (againCount.toFloat() / totalCount) * 100f else 0f
    val hardPct: Float get() = if (totalCount > 0) (hardCount.toFloat() / totalCount) * 100f else 0f
    val goodPct: Float get() = if (totalCount > 0) (goodCount.toFloat() / totalCount) * 100f else 0f
    val easyPct: Float get() = if (totalCount > 0) (easyCount.toFloat() / totalCount) * 100f else 0f
}

data class EaseFactorDistribution(
    val bucketUnder1_8: Int = 0,
    val bucket1_8to2_2: Int = 0,
    val bucket2_2to2_6: Int = 0,
    val bucket2_6to3_0: Int = 0,
    val bucketOver3_0: Int = 0,
    val totalWords: Int = 0
)

data class ForecastDayStat(
    val dayOffset: Int,
    val dateLabel: String,
    val dayOfWeek: String,
    val count: Int
)

data class DayStreakStatus(
    val epochDay: Long,
    val dateLabel: String,
    val hasReview: Boolean,
    val count: Int,
    val isToday: Boolean
)

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val word: WordEntity) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

enum class ReadingPhase {
    READING,
    GRAMMAR_INTRO,
    GRAMMAR
}

sealed interface DailyReadingUiState {
    object Idle : DailyReadingUiState
    object Loading : DailyReadingUiState
    data class Active(
        val passage: String,
        val questions: List<com.example.api.ReadingQuestion>,
        val currentQuestionIndex: Int,
        val selectedOption: String? = null,
        val isCorrect: Boolean? = null,
        val score: Int = 0,
        val readingLevel: Int = 1,
        val cefrLevel: String = "A2",
        val grammarQuestions: List<com.example.api.ReadingQuestion> = emptyList(),
        val currentPhase: ReadingPhase = ReadingPhase.READING,
        val currentGrammarIndex: Int = 0,
        val selectedGrammarOption: String? = null,
        val isGrammarCorrect: Boolean? = null,
        val grammarScore: Int = 0,
        val oldReadingLevel: Int = 1,
        val levelChange: LevelChange = LevelChange.UNCHANGED
    ) : DailyReadingUiState
    data class Finished(
        val passage: String,
        val questions: List<com.example.api.ReadingQuestion>,
        val score: Int,
        val total: Int,
        val grammarQuestions: List<com.example.api.ReadingQuestion> = emptyList(),
        val grammarScore: Int = 0,
        val grammarTotal: Int = 0,
        val oldReadingLevel: Int,
        val newReadingLevel: Int,
        val levelChange: LevelChange,
        val cefrLevel: String
    ) : DailyReadingUiState
    data class Error(val message: String) : DailyReadingUiState
}

enum class LevelChange {
    INCREASED,
    DECREASED,
    UNCHANGED
}

data class AiUsageStats(
    val geminiCallsToday: Int = 0,
    val openRouterCallsToday: Int = 0,
    val lastProviderUsed: String? = null
)

enum class WordStatusFilter { ALL, NEW, LEARNING, MASTERED, LEECH }

enum class FlashcardStudyMode { CARDS, QUIZ, SPELLING }

data class QuizQuestion(
    val targetWord: WordEntity,
    val options: List<String>,
    val selectedAnswer: String? = null,
    val isCorrect: Boolean? = null
)

data class QuizState(
    val currentQuestion: QuizQuestion? = null,
    val score: Int = 0,
    val totalAnswered: Int = 0,
    val currentStreak: Int = 0
)

data class SpellingTestState(
    val targetWord: WordEntity? = null,
    val userInput: String = "",
    val isChecked: Boolean = false,
    val isCorrect: Boolean? = null,
    val hintLevel: Int = 0, // 0 = none, 1 = first letter & length, 2 = first & last letter
    val score: Int = 0,
    val totalAnswered: Int = 0,
    val currentStreak: Int = 0
)

class DictionaryViewModel(
    application: Application,
    private val repository: WordRepository,
    private val ttsHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _aiUsageStats = MutableStateFlow(
        run {
            val sharedPrefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val today = java.time.LocalDate.now().toEpochDay()
            val savedDate = sharedPrefs.getLong("usage_stats_date", -1L)
            val lastProvider = sharedPrefs.getString("last_provider_used", null)?.takeIf { it.isNotBlank() }
            if (savedDate != today) {
                AiUsageStats(0, 0, lastProvider)
            } else {
                AiUsageStats(
                    geminiCallsToday = sharedPrefs.getInt("gemini_calls_today", 0),
                    openRouterCallsToday = sharedPrefs.getInt("openrouter_calls_today", 0),
                    lastProviderUsed = lastProvider
                )
            }
        }
    )
    val aiUsageStats: StateFlow<AiUsageStats> = _aiUsageStats.asStateFlow()

    fun getUsageStats(): AiUsageStats {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toEpochDay()
        val savedDate = sharedPrefs.getLong("usage_stats_date", -1L)
        val lastProvider = sharedPrefs.getString("last_provider_used", null)?.takeIf { it.isNotBlank() }

        if (savedDate != today) {
            return AiUsageStats(
                geminiCallsToday = 0,
                openRouterCallsToday = 0,
                lastProviderUsed = lastProvider
            )
        }

        return AiUsageStats(
            geminiCallsToday = sharedPrefs.getInt("gemini_calls_today", 0),
            openRouterCallsToday = sharedPrefs.getInt("openrouter_calls_today", 0),
            lastProviderUsed = lastProvider
        )
    }

    fun refreshUsageStats() {
        _aiUsageStats.value = getUsageStats()
        _geminiLocked.value = getGeminiLocked()
    }

    private val _geminiLocked = MutableStateFlow(
        run {
            val sharedPrefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val today = java.time.LocalDate.now().toEpochDay()
            val lockedDate = sharedPrefs.getLong("gemini_locked_date", -1L)
            if (lockedDate != today) {
                sharedPrefs.edit()
                    .putLong("gemini_locked_date", today)
                    .putBoolean("gemini_manually_locked", false)
                    .apply()
                false
            } else {
                sharedPrefs.getBoolean("gemini_manually_locked", false)
            }
        }
    )
    val geminiLocked: StateFlow<Boolean> = _geminiLocked.asStateFlow()

    fun getGeminiLocked(): Boolean {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toEpochDay()
        val lockedDate = sharedPrefs.getLong("gemini_locked_date", -1L)
        if (lockedDate != today) {
            sharedPrefs.edit()
                .putLong("gemini_locked_date", today)
                .putBoolean("gemini_manually_locked", false)
                .apply()
            _geminiLocked.value = false
            return false
        }
        val locked = sharedPrefs.getBoolean("gemini_manually_locked", false)
        _geminiLocked.value = locked
        return locked
    }

    fun setGeminiLocked(value: Boolean) {
        _geminiLocked.value = value
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toEpochDay()
        sharedPrefs.edit()
            .putLong("gemini_locked_date", today)
            .putBoolean("gemini_manually_locked", value)
            .apply()
    }

    private val _autoPronounceEnabled = MutableStateFlow(true)
    val autoPronounceEnabled: StateFlow<Boolean> = _autoPronounceEnabled.asStateFlow()

    private val _pronunciationSource = MutableStateFlow("auto")
    val pronunciationSource: StateFlow<String> = _pronunciationSource.asStateFlow()

    private val _selectedAccent = MutableStateFlow("UK")
    val selectedAccent: StateFlow<String> = _selectedAccent.asStateFlow()

    private val _selectedTtsVoiceName = MutableStateFlow("") // empty = Auto (Best Offline Neural)
    val selectedTtsVoiceName: StateFlow<String> = _selectedTtsVoiceName.asStateFlow()

    private val _availableOfflineVoices = MutableStateFlow<List<OfflineTtsVoiceOption>>(emptyList())
    val availableOfflineVoices: StateFlow<List<OfflineTtsVoiceOption>> = _availableOfflineVoices.asStateFlow()

    private val _activeOfflineVoiceLabel = MutableStateFlow("Auto (Best Offline UK Neural)")
    val activeOfflineVoiceLabel: StateFlow<String> = _activeOfflineVoiceLabel.asStateFlow()

    private val _speechSpeed = MutableStateFlow("Normal")
    val speechSpeed: StateFlow<String> = _speechSpeed.asStateFlow()

    private val _aiVisualizationsEnabled = MutableStateFlow(true)
    val aiVisualizationsEnabled: StateFlow<Boolean> = _aiVisualizationsEnabled.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(true)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _alwaysNotifyEnabled = MutableStateFlow(false)
    val alwaysNotifyEnabled: StateFlow<Boolean> = _alwaysNotifyEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(12)
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(0)
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    private val _selectedThemeColor = MutableStateFlow("purple")
    val selectedThemeColor: StateFlow<String> = _selectedThemeColor.asStateFlow()

    private val _themeMode = MutableStateFlow("system") // "system", "light", "dark"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(true)
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _useSeparateTtsKeys = MutableStateFlow(false)
    val useSeparateTtsKeys: StateFlow<Boolean> = _useSeparateTtsKeys.asStateFlow()

    private val _isTestingGeminiKey = MutableStateFlow(false)
    val isTestingGeminiKey: StateFlow<Boolean> = _isTestingGeminiKey.asStateFlow()

    private val _geminiKeyTestStatus = MutableStateFlow<Pair<Boolean, String>?>(null) // Pair(isSuccess, message)
    val geminiKeyTestStatus: StateFlow<Pair<Boolean, String>?> = _geminiKeyTestStatus.asStateFlow()

    private val _arabicTranslationEnabled = MutableStateFlow(true)
    val arabicTranslationEnabled: StateFlow<Boolean> = _arabicTranslationEnabled.asStateFlow()

    private val _wordArabicTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    val wordArabicTranslations: StateFlow<Map<String, String>> = _wordArabicTranslations.asStateFlow()

    private val _translatingArabicWord = MutableStateFlow<String?>(null)
    val translatingArabicWord: StateFlow<String?> = _translatingArabicWord.asStateFlow()

    private val _enrichingWord = MutableStateFlow<String?>(null)
    val enrichingWord: StateFlow<String?> = _enrichingWord.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<OfflineSearchSuggestion>>(emptyList())
    val searchSuggestions: StateFlow<List<OfflineSearchSuggestion>> = _searchSuggestions.asStateFlow()
    private var suggestionsJob: Job? = null

    // Offline 9-book (6 Essential + 3 IELTS) & 26 Life Topics Anki Courses state
    private val _offlineBooks = MutableStateFlow<List<OfflineBook>>(emptyList())
    val offlineBooks: StateFlow<List<OfflineBook>> = _offlineBooks.asStateFlow()

    private val _offlineTopics = MutableStateFlow<List<OfflineTopic>>(emptyList())
    val offlineTopics: StateFlow<List<OfflineTopic>> = _offlineTopics.asStateFlow()

    private val _bookWordLowersMap = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val bookWordLowersMap: StateFlow<Map<Int, Set<String>>> = _bookWordLowersMap.asStateFlow()

    private val _topicWordLowersMap = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val topicWordLowersMap: StateFlow<Map<String, Set<String>>> = _topicWordLowersMap.asStateFlow()

    private val _selectedCourseBook = MutableStateFlow<OfflineBook?>(null)
    val selectedCourseBook: StateFlow<OfflineBook?> = _selectedCourseBook.asStateFlow()

    private val _courseUnits = MutableStateFlow<List<OfflineUnit>>(emptyList())
    val courseUnits: StateFlow<List<OfflineUnit>> = _courseUnits.asStateFlow()

    private val _unitWordLowersMap = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val unitWordLowersMap: StateFlow<Map<Int, Set<String>>> = _unitWordLowersMap.asStateFlow()

    private val _selectedCourseUnit = MutableStateFlow<OfflineUnit?>(null)
    val selectedCourseUnit: StateFlow<OfflineUnit?> = _selectedCourseUnit.asStateFlow()

    private val _selectedCourseTopic = MutableStateFlow<OfflineTopic?>(null)
    val selectedCourseTopic: StateFlow<OfflineTopic?> = _selectedCourseTopic.asStateFlow()

    private val _courseWords = MutableStateFlow<List<OfflineCurriculumWord>>(emptyList())
    val courseWords: StateFlow<List<OfflineCurriculumWord>> = _courseWords.asStateFlow()

    private val _courseSearchQuery = MutableStateFlow("")
    val courseSearchQuery: StateFlow<String> = _courseSearchQuery.asStateFlow()

    private val _courseSearchResults = MutableStateFlow<List<OfflineCurriculumWord>>(emptyList())
    val courseSearchResults: StateFlow<List<OfflineCurriculumWord>> = _courseSearchResults.asStateFlow()
    private var courseSearchJob: Job? = null

    private val _isAddingCourseWords = MutableStateFlow(false)
    val isAddingCourseWords: StateFlow<Boolean> = _isAddingCourseWords.asStateFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    // Dynamic Gemini TTS API Key list & Active selection (0-based)
    private val _geminiApiKeys = MutableStateFlow<List<String>>(emptyList())
    val geminiApiKeys: StateFlow<List<String>> = _geminiApiKeys.asStateFlow()

    // Backward-compatible flows for existing callers
    val geminiApiKey1: StateFlow<String> = _geminiApiKeys
        .map { it.getOrElse(0) { "" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val geminiApiKey2: StateFlow<String> = _geminiApiKeys
        .map { it.getOrElse(1) { "" } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _activeKeyIndex = MutableStateFlow(0) // 0-based index
    val activeKeyIndex: StateFlow<Int> = _activeKeyIndex.asStateFlow()

    // Global TTS network throttle (configurable in Settings, default 30s)
    private var lastTtsNetworkCallTimestamp: Long = 0L
    var ttsThrottleCooldownMs: Long = 30_000L

    private val _ttsCooldownSeconds = MutableStateFlow(30)
    val ttsCooldownSeconds: StateFlow<Int> = _ttsCooldownSeconds.asStateFlow()

    // Word / AI API network throttle (configurable in Settings, default 5s)
    private var lastWordApiNetworkCallTimestamp: Long = 0L
    private val _wordApiCooldownSeconds = MutableStateFlow(5)
    val wordApiCooldownSeconds: StateFlow<Int> = _wordApiCooldownSeconds.asStateFlow()

    // Last pronunciation source tracker ("Gemini Key 1", "Gemini Key 2", "Device TTS")
    private val _lastPronunciationSource = MutableStateFlow(
        run {
            val sharedPrefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.getString("last_pronunciation_source", "") ?: ""
        }
    )
    val lastPronunciationSource: StateFlow<String> = _lastPronunciationSource.asStateFlow()

    val imageSourcePreference: StateFlow<String> = MutableStateFlow("web").asStateFlow()

    private val _pexelsQuerySource = MutableStateFlow("word")
    val pexelsQuerySource: StateFlow<String> = _pexelsQuerySource.asStateFlow()

    // Dynamic Gemini Meaning & Reading API Key list, Active index, Model selection, OpenRouter Key, Pexels Key
    private val _geminiMeaningApiKeys = MutableStateFlow<List<String>>(emptyList())
    val geminiMeaningApiKeys: StateFlow<List<String>> = _geminiMeaningApiKeys.asStateFlow()

    private val _geminiMeaningActiveKeyIndex = MutableStateFlow(0)
    val geminiMeaningActiveKeyIndex: StateFlow<Int> = _geminiMeaningActiveKeyIndex.asStateFlow()

    private val _geminiMeaningModelChoice = MutableStateFlow("gemini-3.5-flash-lite")
    val geminiMeaningModelChoice: StateFlow<String> = _geminiMeaningModelChoice.asStateFlow()

    private val _geminiMeaningCustomModel = MutableStateFlow("gemini-3.5-flash-lite")
    val geminiMeaningCustomModel: StateFlow<String> = _geminiMeaningCustomModel.asStateFlow()

    private val _openRouterApiKey = MutableStateFlow("")
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _pexelsApiKey = MutableStateFlow("")
    val pexelsApiKey: StateFlow<String> = _pexelsApiKey.asStateFlow()

    private val _userEnglishLevel = MutableStateFlow("A2")
    val userEnglishLevel: StateFlow<String> = _userEnglishLevel.asStateFlow()

    private val _readingLevel = MutableStateFlow(1)
    val readingLevel: StateFlow<Int> = _readingLevel.asStateFlow()

    fun getUserEnglishLevel(): String {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPrefs.getString("user_english_level", "A2") ?: "A2"
    }

    fun setUserEnglishLevel(value: String) {
        val normalized = value.uppercase()
        _userEnglishLevel.value = normalized
        val baseNumericLevel = when (normalized) {
            "A1" -> 1
            "A2" -> 2
            "B1" -> 3
            "B2" -> 4
            "C1" -> 5
            else -> 2
        }
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("user_english_level", normalized)
            .putInt("reading_level", baseNumericLevel)
            .remove("daily_reading_content")
            .remove("daily_reading_date")
            .remove("prev_reading_outcome")
            .apply()
        _readingLevel.value = baseNumericLevel
        _dailyReadingUiState.value = DailyReadingUiState.Idle
    }

    fun getImageSourcePreference(): String = "web"

    fun getPexelsQuerySource(): String {
        return _pexelsQuerySource.value
    }

    fun setPexelsQuerySource(value: String) {
        _pexelsQuerySource.value = value
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("pexels_query_source", value).apply()
    }

    // Transient in-memory pagination map for Pexels photo cycling (word -> currentPage)
    private val wordPhotoPageMap = mutableMapOf<String, Int>()

    fun getWordPhotoPage(word: String): Int = wordPhotoPageMap[word.trim().lowercase()] ?: 1

    fun getDerivedQuery(word: String): String? = repository.getDerivedQuery(word)

    private val _cyclingPhotoWord = MutableStateFlow<String?>(null)
    val cyclingPhotoWord: StateFlow<String?> = _cyclingPhotoWord.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun showSnackbar(message: String) {
        _snackbarMessage.tryEmit(message)
    }

    fun cycleNextPhoto(wordEntity: WordEntity): kotlinx.coroutines.Job? {
        val cleanWord = wordEntity.word.trim().lowercase()
        // Prevent concurrent requests for the same word
        if (_cyclingPhotoWord.value?.equals(cleanWord, ignoreCase = true) == true) {
            return null
        }

        return viewModelScope.launch {
            _cyclingPhotoWord.value = cleanWord
            val currentPage = wordPhotoPageMap[cleanWord] ?: 1
            val nextPage = currentPage + 1

            val result = repository.fetchPexelsPhotoForWord(
                wordEntity = wordEntity,
                page = nextPage,
                querySourceOverride = _pexelsQuerySource.value,
                pexelsApiKey = _pexelsApiKey.value.trim()
            )
            when (result) {
                is WordRepository.CyclePhotoResult.Success -> {
                    wordPhotoPageMap[cleanWord] = result.page
                    repository.updateWordImagePrompt(wordEntity.word, result.photoUrl)

                    // Update searchUiState if the currently displayed word matches
                    val currentSearchState = _searchUiState.value
                    if (currentSearchState is SearchUiState.Success &&
                        currentSearchState.word.word.equals(wordEntity.word, ignoreCase = true)
                    ) {
                        _searchUiState.value = SearchUiState.Success(
                            currentSearchState.word.copy(imagePrompt = result.photoUrl)
                        )
                    }

                    // Update reviewQueue and activeStudySession in-place
                    _reviewQueue.value = _reviewQueue.value.map {
                        if (it.word.equals(wordEntity.word, ignoreCase = true)) {
                            it.copy(imagePrompt = result.photoUrl)
                        } else it
                    }
                    _activeStudySession.value = _activeStudySession.value?.let { s ->
                        s.copy(queue = s.queue.map {
                            if (it.word.equals(wordEntity.word, ignoreCase = true)) it.copy(imagePrompt = result.photoUrl) else it
                        })
                    }
                    _localImageRefreshVersion.value = _localImageRefreshVersion.value + 1
                    refreshLocalImageStats()
                    _snackbarMessage.tryEmit("Saved offline photo for '${wordEntity.word}'")
                }
                is WordRepository.CyclePhotoResult.WrappedToBeginning -> {
                    wordPhotoPageMap[cleanWord] = 1
                    repository.updateWordImagePrompt(wordEntity.word, result.photoUrl)

                    val currentSearchState = _searchUiState.value
                    if (currentSearchState is SearchUiState.Success &&
                        currentSearchState.word.word.equals(wordEntity.word, ignoreCase = true)
                    ) {
                        _searchUiState.value = SearchUiState.Success(
                            currentSearchState.word.copy(imagePrompt = result.photoUrl)
                        )
                    }

                    _reviewQueue.value = _reviewQueue.value.map {
                        if (it.word.equals(wordEntity.word, ignoreCase = true)) {
                            it.copy(imagePrompt = result.photoUrl)
                        } else it
                    }
                    _activeStudySession.value = _activeStudySession.value?.let { s ->
                        s.copy(queue = s.queue.map {
                            if (it.word.equals(wordEntity.word, ignoreCase = true)) it.copy(imagePrompt = result.photoUrl) else it
                        })
                    }
                    _localImageRefreshVersion.value = _localImageRefreshVersion.value + 1
                    refreshLocalImageStats()

                    _snackbarMessage.tryEmit("Back to the first result for '${wordEntity.word}'")
                }
                is WordRepository.CyclePhotoResult.NoMoreResults -> {
                    wordPhotoPageMap[cleanWord] = 1
                    _snackbarMessage.tryEmit("No more photo results for '${wordEntity.word}'")
                }
                is WordRepository.CyclePhotoResult.Error -> {
                    // Leave current image unchanged
                    _snackbarMessage.tryEmit("Failed to load new photo. Please check your connection.")
                }
            }
            _cyclingPhotoWord.value = null
        }
    }

    fun setAutoPronounceEnabled(enabled: Boolean) {
        _autoPronounceEnabled.value = enabled
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("auto_pronounce", enabled).apply()
    }

    fun setPronunciationSource(source: String) {
        _pronunciationSource.value = source
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("pronunciation_source", source).apply()
    }

    fun setSelectedAccent(accent: String) {
        val normalized = if (accent.equals("US", ignoreCase = true)) "US" else "UK"
        _selectedAccent.value = normalized
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("selected_accent", normalized).apply()
        updateTtsSettings()
    }

    fun setSelectedTtsVoiceName(voiceName: String) {
        val clean = voiceName.trim()
        _selectedTtsVoiceName.value = clean
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("selected_tts_voice_name", clean).apply()
        updateTtsSettings()
    }

    fun refreshAvailableTtsVoices() {
        updateTtsSettings()
    }

    fun previewTtsVoice(sampleText: String = "Welcome to Redeus Dictionary. Daily spaced repetition builds lasting fluency.") {
        speakWithTts(sampleText)
    }

    fun setSpeechSpeed(speed: String) {
        _speechSpeed.value = speed
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("speech_speed", speed).apply()
        updateTtsSettings()
    }

    fun setAiVisualizationsEnabled(enabled: Boolean) {
        _aiVisualizationsEnabled.value = enabled
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("ai_visualizations_enabled", enabled).apply()
    }

    fun setRemindersEnabled(enabled: Boolean) {
        _remindersEnabled.value = enabled
        com.example.reminder.ReminderHelper.setRemindersEnabled(getApplication(), enabled)
    }

    fun setAlwaysNotifyEnabled(enabled: Boolean) {
        _alwaysNotifyEnabled.value = enabled
        com.example.reminder.ReminderHelper.setAlwaysNotifyEnabled(getApplication(), enabled)
    }

    fun setSelectedThemeColor(colorName: String) {
        _selectedThemeColor.value = colorName
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("selected_theme_color", colorName).apply()
    }

    private fun saveGeminiApiKeysToPrefs(keys: List<String>) {
        val jsonArray = JSONArray()
        for (k in keys) {
            jsonArray.put(k)
        }
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("gemini_api_keys_list", jsonArray.toString()).apply()
    }

    fun setGeminiApiKeys(keys: List<String>) {
        _geminiApiKeys.value = keys
        saveGeminiApiKeysToPrefs(keys)
        if (keys.isEmpty()) {
            _activeKeyIndex.value = 0
            val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putInt("gemini_active_key_index", 0).apply()
        } else if (_activeKeyIndex.value !in keys.indices) {
            setActiveKeyIndex(0)
        }
    }

    fun addGeminiApiKey(key: String = "") {
        val current = _geminiApiKeys.value.toMutableList()
        current.add(key)
        _geminiApiKeys.value = current
        saveGeminiApiKeysToPrefs(current)
        if (_activeKeyIndex.value !in current.indices) {
            setActiveKeyIndex(0)
        }
    }

    fun updateGeminiApiKey(index: Int, key: String) {
        val current = _geminiApiKeys.value.toMutableList()
        if (index in current.indices) {
            current[index] = key
            _geminiApiKeys.value = current
            saveGeminiApiKeysToPrefs(current)
        }
    }

    fun removeGeminiApiKey(index: Int) {
        val current = _geminiApiKeys.value.toMutableList()
        if (index in current.indices) {
            val wasActive = _activeKeyIndex.value == index
            val oldActive = _activeKeyIndex.value
            current.removeAt(index)
            _geminiApiKeys.value = current
            saveGeminiApiKeysToPrefs(current)

            val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            if (current.isEmpty()) {
                _activeKeyIndex.value = 0
                sharedPrefs.edit().putInt("gemini_active_key_index", 0).apply()
            } else if (wasActive || oldActive !in current.indices) {
                // If the active key was deleted, or active index is now out of range, fall back to index 0
                setActiveKeyIndex(0)
            } else if (oldActive > index) {
                setActiveKeyIndex(oldActive - 1)
            }
        }
    }

    fun setGeminiApiKey1(key: String) {
        val current = _geminiApiKeys.value.toMutableList()
        if (current.isEmpty()) {
            current.add(key)
        } else {
            current[0] = key
        }
        _geminiApiKeys.value = current
        saveGeminiApiKeysToPrefs(current)
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("gemini_api_key_1", key).apply()
    }

    fun setGeminiApiKey2(key: String) {
        val current = _geminiApiKeys.value.toMutableList()
        while (current.size < 2) {
            current.add("")
        }
        current[1] = key
        _geminiApiKeys.value = current
        saveGeminiApiKeysToPrefs(current)
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("gemini_api_key_2", key).apply()
    }

    fun setActiveKeyIndex(index: Int) {
        val keys = _geminiApiKeys.value
        val validIndex = if (keys.isNotEmpty()) {
            if (index in keys.indices) index else 0
        } else {
            0
        }
        _activeKeyIndex.value = validIndex
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("gemini_active_key_index", validIndex).apply()
    }

    fun setLastPronunciationSource(source: String) {
        _lastPronunciationSource.value = source
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("last_pronunciation_source", source).apply()
    }

    fun isTtsNetworkThrottled(): Boolean {
        if (ttsThrottleCooldownMs <= 0L) return false
        val elapsed = System.currentTimeMillis() - lastTtsNetworkCallTimestamp
        return elapsed < ttsThrottleCooldownMs
    }

    fun resetTtsThrottle() {
        lastTtsNetworkCallTimestamp = 0L
    }

    fun setTtsCooldownSeconds(seconds: Int) {
        val safe = seconds.coerceIn(0, 120)
        _ttsCooldownSeconds.value = safe
        ttsThrottleCooldownMs = safe * 1000L
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("tts_cooldown_seconds", safe).apply()
    }

    fun setWordApiCooldownSeconds(seconds: Int) {
        val safe = seconds.coerceIn(0, 120)
        _wordApiCooldownSeconds.value = safe
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("word_api_cooldown_seconds", safe).apply()
    }

    fun isWordApiThrottled(): Boolean {
        val cooldownMs = _wordApiCooldownSeconds.value * 1000L
        if (cooldownMs <= 0L || lastWordApiNetworkCallTimestamp == 0L) return false
        return (System.currentTimeMillis() - lastWordApiNetworkCallTimestamp) < cooldownMs
    }

    private fun getWordApiCooldownRemainingSec(): Long {
        val cooldownMs = _wordApiCooldownSeconds.value * 1000L
        val elapsed = System.currentTimeMillis() - lastWordApiNetworkCallTimestamp
        return ((cooldownMs - elapsed + 999L) / 1000L).coerceAtLeast(1L)
    }

    private fun saveGeminiMeaningApiKeysToPrefs(keys: List<String>) {
        val jsonArray = JSONArray()
        for (k in keys) {
            jsonArray.put(k)
        }
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("gemini_meaning_api_keys_list", jsonArray.toString()).apply()
    }

    fun setGeminiMeaningApiKeys(keys: List<String>) {
        _geminiMeaningApiKeys.value = keys
        saveGeminiMeaningApiKeysToPrefs(keys)
        if (keys.isEmpty()) {
            _geminiMeaningActiveKeyIndex.value = 0
            val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putInt("gemini_meaning_active_key_index", 0).apply()
        } else if (_geminiMeaningActiveKeyIndex.value !in keys.indices) {
            setGeminiMeaningActiveKeyIndex(0)
        }
    }

    fun addGeminiMeaningApiKey(key: String = "") {
        val current = _geminiMeaningApiKeys.value.toMutableList()
        current.add(key)
        _geminiMeaningApiKeys.value = current
        saveGeminiMeaningApiKeysToPrefs(current)
        if (_geminiMeaningActiveKeyIndex.value !in current.indices) {
            setGeminiMeaningActiveKeyIndex(0)
        }
    }

    fun updateGeminiMeaningApiKey(index: Int, key: String) {
        val current = _geminiMeaningApiKeys.value.toMutableList()
        if (index in current.indices) {
            current[index] = key
            _geminiMeaningApiKeys.value = current
            saveGeminiMeaningApiKeysToPrefs(current)
        }
    }

    fun removeGeminiMeaningApiKey(index: Int) {
        val current = _geminiMeaningApiKeys.value.toMutableList()
        if (index in current.indices) {
            val wasActive = _geminiMeaningActiveKeyIndex.value == index
            val oldActive = _geminiMeaningActiveKeyIndex.value
            current.removeAt(index)
            _geminiMeaningApiKeys.value = current
            saveGeminiMeaningApiKeysToPrefs(current)

            val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            if (current.isEmpty()) {
                _geminiMeaningActiveKeyIndex.value = 0
                sharedPrefs.edit().putInt("gemini_meaning_active_key_index", 0).apply()
            } else if (wasActive || oldActive !in current.indices) {
                setGeminiMeaningActiveKeyIndex(0)
            } else if (oldActive > index) {
                setGeminiMeaningActiveKeyIndex(oldActive - 1)
            }
        }
    }

    fun setGeminiMeaningActiveKeyIndex(index: Int) {
        val keys = _geminiMeaningApiKeys.value
        val validIndex = if (keys.isNotEmpty()) {
            if (index in keys.indices) index else 0
        } else {
            0
        }
        _geminiMeaningActiveKeyIndex.value = validIndex
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("gemini_meaning_active_key_index", validIndex).apply()
    }

    fun setGeminiMeaningModelChoice(choice: String) {
        _geminiMeaningModelChoice.value = choice
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("gemini_meaning_model_choice", choice).apply()
    }

    fun setGeminiMeaningCustomModel(model: String) {
        _geminiMeaningCustomModel.value = model
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("gemini_meaning_custom_model", model).apply()
    }

    fun setOpenRouterApiKey(key: String) {
        _openRouterApiKey.value = key
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("openrouter_api_key", key).apply()
    }

    fun setPexelsApiKey(key: String) {
        _pexelsApiKey.value = key
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("pexels_api_key", key).apply()
    }

    fun setThemeMode(mode: String) {
        val normalized = when (mode.lowercase()) {
            "light" -> "light"
            "dark" -> "dark"
            else -> "system"
        }
        _themeMode.value = normalized
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("theme_mode", normalized).apply()
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        _dynamicColorEnabled.value = enabled
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("dynamic_color_enabled", enabled).apply()
    }

    fun setUseSeparateTtsKeys(enabled: Boolean) {
        _useSeparateTtsKeys.value = enabled
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("use_separate_tts_keys", enabled).apply()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        _reminderHour.value = safeHour
        _reminderMinute.value = safeMinute
        com.example.reminder.ReminderHelper.setReminderTime(getApplication(), safeHour, safeMinute)
    }

    fun setArabicTranslationEnabled(enabled: Boolean) {
        _arabicTranslationEnabled.value = enabled
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("arabic_translation_enabled", enabled).apply()
    }

    fun testActiveGeminiKey() {
        val keys = getOrderedGeminiMeaningKeys()
        if (keys.isEmpty()) {
            _geminiKeyTestStatus.value = false to "Add a Gemini API key first."
            return
        }
        val keyToTest = keys.first()
        val model = getEffectiveGeminiMeaningModel()
        _isTestingGeminiKey.value = true
        _geminiKeyTestStatus.value = null
        viewModelScope.launch {
            val result = repository.testGeminiApiKey(keyToTest, model)
            _isTestingGeminiKey.value = false
            result.onSuccess { msg ->
                _geminiKeyTestStatus.value = true to msg
            }.onFailure { err ->
                _geminiKeyTestStatus.value = false to (err.message ?: "Connection failed")
            }
        }
    }

    fun ensureOfflineArabicLoaded(wordEntity: WordEntity) {
        val key = wordEntity.word.trim().lowercase()
        if (key.isBlank()) return
        if (wordEntity.arabicMeaning.isNotBlank()) {
            if (_wordArabicTranslations.value[key] != wordEntity.arabicMeaning) {
                _wordArabicTranslations.value = _wordArabicTranslations.value + (key to wordEntity.arabicMeaning)
            }
            return
        }
        if (_wordArabicTranslations.value.containsKey(key)) return

        val prefs = getApplication<Application>().getSharedPreferences("arabic_translations", android.content.Context.MODE_PRIVATE)
        val cached = prefs.getString("ar_$key", null)
        if (!cached.isNullOrBlank()) {
            _wordArabicTranslations.value = _wordArabicTranslations.value + (key to cached)
            return
        }

        viewModelScope.launch {
            val offlineAr = repository.lookupOfflineArabicTranslation(key)
            if (!offlineAr.isNullOrBlank()) {
                prefs.edit().putString("ar_$key", offlineAr).apply()
                _wordArabicTranslations.value = _wordArabicTranslations.value + (key to offlineAr)
                repository.updateArabicMeaning(key, offlineAr)
            }
        }
    }

    fun translateWordToArabic(wordEntity: WordEntity) {
        val key = wordEntity.word.trim().lowercase()
        if (key.isBlank()) return
        if (wordEntity.arabicMeaning.isNotBlank()) {
            _wordArabicTranslations.value = _wordArabicTranslations.value + (key to wordEntity.arabicMeaning)
            return
        }
        if (_wordArabicTranslations.value.containsKey(key)) return

        val prefs = getApplication<Application>().getSharedPreferences("arabic_translations", android.content.Context.MODE_PRIVATE)
        val cached = prefs.getString("ar_$key", null)
        if (!cached.isNullOrBlank()) {
            _wordArabicTranslations.value = _wordArabicTranslations.value + (key to cached)
            return
        }

        _translatingArabicWord.value = key
        viewModelScope.launch {
            val result = repository.translateWordToArabic(
                word = wordEntity.word,
                definition = wordEntity.definition,
                geminiApiKeys = getOrderedGeminiMeaningKeys(),
                geminiModel = getEffectiveGeminiMeaningModel(),
                openRouterApiKey = _openRouterApiKey.value.trim()
            )
            _translatingArabicWord.value = null
            result.onSuccess { arabicText ->
                prefs.edit().putString("ar_$key", arabicText).apply()
                _wordArabicTranslations.value = _wordArabicTranslations.value + (key to arabicText)
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.message ?: "Failed to translate to Arabic")
            }
        }
    }

    fun enrichWordWithAi(wordEntity: WordEntity) {
        val cleanWord = wordEntity.word.trim().lowercase()
        if (cleanWord.isBlank() || _enrichingWord.value != null) return

        val geminiKeys = getOrderedGeminiMeaningKeys()
        val openRouterKey = _openRouterApiKey.value.trim()
        if (geminiKeys.isEmpty() && openRouterKey.isBlank()) {
            _snackbarMessage.tryEmit("Add a Gemini or OpenRouter API key in Settings to enrich words with AI.")
            return
        }

        _enrichingWord.value = cleanWord
        viewModelScope.launch {
            val result = repository.searchAndFetchWord(
                word = cleanWord,
                aiVisualizationsEnabled = _aiVisualizationsEnabled.value,
                imageSource = "web",
                openRouterApiKey = openRouterKey,
                pexelsQuerySource = _pexelsQuerySource.value,
                geminiApiKeys = geminiKeys,
                geminiModel = getEffectiveGeminiMeaningModel(),
                pexelsApiKey = _pexelsApiKey.value.trim(),
                forceAiEnrich = true
            )
            _enrichingWord.value = null
            refreshUsageStats()
            result.onSuccess { enriched ->
                val current = _searchUiState.value
                if (current is SearchUiState.Success && current.word.word.equals(enriched.word, ignoreCase = true)) {
                    _searchUiState.value = SearchUiState.Success(enriched)
                }
                _reviewQueue.value = _reviewQueue.value.map {
                    if (it.word.equals(enriched.word, ignoreCase = true)) enriched else it
                }
                _activeStudySession.value = _activeStudySession.value?.let { s ->
                    s.copy(queue = s.queue.map {
                        if (it.word.equals(enriched.word, ignoreCase = true)) enriched else it
                    })
                }
                if (enriched.imagePrompt.startsWith("http")) {
                    viewModelScope.launch {
                        LocalWordImageStore.getInstance(getApplication())
                            .downloadAndSavePermanently(enriched.word, enriched.imagePrompt)
                        _localImageRefreshVersion.value = _localImageRefreshVersion.value + 1
                        refreshLocalImageStats()
                    }
                }
                _snackbarMessage.tryEmit("Saved offline AI notes for \"${enriched.word}\"")
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.message ?: "AI enrichment failed")
            }
        }
    }

    // --- Offline 9-Book (6 Essential + 3 IELTS) & 26 Life Topics Anki Courses ---

    fun loadOfflineCatalog() {
        viewModelScope.launch {
            _offlineBooks.value = repository.getOfflineBooks()
            _offlineTopics.value = repository.getOfflineTopics()
            _bookWordLowersMap.value = repository.getOfflineBookWordLowersMap()
            _topicWordLowersMap.value = repository.getOfflineTopicWordLowersMap()
        }
    }

    fun selectCourseBook(book: OfflineBook?) {
        _selectedCourseBook.value = book
        _selectedCourseUnit.value = null
        _selectedCourseTopic.value = null
        _courseWords.value = emptyList()
        if (book == null) {
            _courseUnits.value = emptyList()
            _unitWordLowersMap.value = emptyMap()
            return
        }
        viewModelScope.launch {
            _courseUnits.value = repository.getOfflineUnitsForBook(book.bookNum)
            _unitWordLowersMap.value = repository.getOfflineUnitWordLowersMap(book.bookNum)
        }
    }

    fun selectCourseUnit(unit: OfflineUnit?) {
        _selectedCourseUnit.value = unit
        if (unit == null) {
            _courseWords.value = emptyList()
            return
        }
        viewModelScope.launch {
            _courseWords.value = repository.getOfflineWordsForUnit(unit.bookNum, unit.unitNum)
        }
    }

    fun selectCourseTopic(topic: OfflineTopic?) {
        _selectedCourseTopic.value = topic
        _selectedCourseBook.value = null
        _selectedCourseUnit.value = null
        if (topic == null) {
            _courseWords.value = emptyList()
            return
        }
        viewModelScope.launch {
            _courseWords.value = repository.getOfflineWordsForTopic(topic.id)
        }
    }

    fun setCourseSearchQuery(query: String, category: String? = null) {
        _courseSearchQuery.value = query
        courseSearchJob?.cancel()
        val clean = query.trim()
        if (clean.length < 2) {
            _courseSearchResults.value = emptyList()
            return
        }
        courseSearchJob = viewModelScope.launch {
            delay(150)
            _courseSearchResults.value = repository.searchOfflineCurriculumWords(clean, category, limit = 60)
        }
    }

    fun openCourseWord(
        item: OfflineCurriculumWord,
        extraTags: List<String> = emptyList(),
        onOpened: (WordEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val entity = repository.openCurriculumWord(item, extraTags)
            _searchQuery.value = entity.word
            _searchSuggestions.value = emptyList()
            _searchUiState.value = SearchUiState.Success(entity)
            if (entity.arabicMeaning.isNotBlank()) {
                _wordArabicTranslations.value = _wordArabicTranslations.value + (entity.word.lowercase() to entity.arabicMeaning)
            }
            onOpened(entity)
        }
    }

    fun toggleCourseWordInDeck(
        item: OfflineCurriculumWord,
        isCurrentlyInDeck: Boolean,
        extraTags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            if (isCurrentlyInDeck) {
                repository.toggleBookmark(item.word.trim().lowercase(), false)
                _reviewQueue.value = _reviewQueue.value.filter { !it.word.equals(item.word.trim(), ignoreCase = true) }
                refreshStats()
                _snackbarMessage.tryEmit("Removed \"${item.word}\" from Deck")
            } else {
                repository.addCurriculumWordsToDeck(listOf(item), extraTags)
                refreshStats()
                _snackbarMessage.tryEmit("Added \"${item.word}\" to Anki FSRS Deck")
            }
        }
    }

    /**
     * Anki-style daily batch import: adds the next [batchSize] unbookmarked words from [words]
     * into the user's Personal Deck with dueDate = today so FSRS schedules them right away.
     */
    fun addNextBatchToDeck(
        words: List<OfflineCurriculumWord>,
        batchSize: Int,
        extraTags: List<String> = emptyList(),
        label: String
    ) {
        if (_isAddingCourseWords.value) return
        val existingDeckWords = bookmarksState.value.map { it.word.lowercase() }.toSet()
        val unadded = words.filter { it.word.trim().lowercase() !in existingDeckWords }
        if (unadded.isEmpty()) {
            _snackbarMessage.tryEmit("All words in $label are already in your Deck")
            return
        }
        val batch = unadded.take(batchSize)
        _isAddingCourseWords.value = true
        viewModelScope.launch {
            val added = repository.addCurriculumWordsToDeck(batch, extraTags)
            _isAddingCourseWords.value = false
            refreshStats()
            _snackbarMessage.tryEmit("Added $added new cards from $label to your FSRS Deck")
        }
    }

    fun addAllWordsToDeck(
        words: List<OfflineCurriculumWord>,
        extraTags: List<String> = emptyList(),
        label: String
    ) {
        if (_isAddingCourseWords.value) return
        if (words.isEmpty()) return
        _isAddingCourseWords.value = true
        viewModelScope.launch {
            val added = repository.addCurriculumWordsToDeck(words, extraTags)
            _isAddingCourseWords.value = false
            refreshStats()
            if (added > 0) {
                _snackbarMessage.tryEmit("Added $added new cards from $label to your FSRS Deck")
            } else {
                _snackbarMessage.tryEmit("All words in $label are already in your Deck")
            }
        }
    }

    fun studyDeckTagInReview(deckTag: String) {
        setSelectedReviewDeck(deckTag)
        startReviewSession(deckTag)
    }

    fun exportBackupJson(uri: android.net.Uri) {
        if (_isBackupBusy.value) return
        _isBackupBusy.value = true
        viewModelScope.launch {
            val result = com.example.data.BackupManager.exportBackupJson(getApplication(), uri)
            _isBackupBusy.value = false
            result.onSuccess { summary ->
                _snackbarMessage.tryEmit(
                    "Backup exported (${summary.wordsCount} words, ${summary.sentencesCount} sentences)"
                )
            }.onFailure { err ->
                _snackbarMessage.tryEmit("Export failed: ${err.message ?: "Unknown error"}")
            }
        }
    }

    fun importBackupJson(uri: android.net.Uri) {
        if (_isBackupBusy.value) return
        _isBackupBusy.value = true
        viewModelScope.launch {
            val result = com.example.data.BackupManager.importBackupJson(getApplication(), uri)
            _isBackupBusy.value = false
            result.onSuccess { summary ->
                refreshStats()
                _snackbarMessage.tryEmit(
                    "Restored ${summary.wordsCount} words, ${summary.sentencesCount} sentences"
                )
            }.onFailure { err ->
                _snackbarMessage.tryEmit("Restore failed: ${err.message ?: "Invalid backup file"}")
            }
        }
    }

    fun exportAnkiCsv(uri: android.net.Uri) {
        if (_isBackupBusy.value) return
        _isBackupBusy.value = true
        viewModelScope.launch {
            val result = com.example.data.BackupManager.exportAnkiCsv(getApplication(), uri)
            _isBackupBusy.value = false
            result.onSuccess { count ->
                _snackbarMessage.tryEmit("Exported $count words to Anki CSV")
            }.onFailure { err ->
                _snackbarMessage.tryEmit("CSV export failed: ${err.message ?: "Unknown error"}")
            }
        }
    }

    fun getEffectiveGeminiMeaningModel(): String {
        val choice = _geminiMeaningModelChoice.value.trim()
        val raw = if (choice.equals("custom", ignoreCase = true) || choice.equals("custom...", ignoreCase = true)) {
            val custom = _geminiMeaningCustomModel.value.trim()
            if (custom.isNotBlank()) custom else "gemini-3.5-flash-lite"
        } else if (choice.isNotBlank()) {
            choice
        } else {
            "gemini-3.5-flash-lite"
        }
        return normalizeGeminiModel(raw)
    }

    private fun normalizeGeminiModel(model: String): String {
        val trimmed = model.trim().lowercase()
        return when {
            trimmed.contains("hydrogen") || trimmed.contains("هيدروجين") -> "gemini-4-hydrogen"
            trimmed.contains("flash lite 3.5") || trimmed.contains("flash lite 3,5") || trimmed.contains("falsh lite 3,5") ||
                trimmed.contains("falsh lite 3.5") || trimmed.contains("3.5 flash lite") || trimmed.contains("3,5 flash lite") ||
                trimmed == "gemini-3.5-flash-lite" -> "gemini-3.5-flash-lite"
            trimmed.contains("flash 3.5") || trimmed.contains("3.5 flash") || trimmed == "gemini-3.5-flash" -> "gemini-3.5-flash"
            trimmed.contains("pro 3.5") || trimmed.contains("3.5 pro") || trimmed == "gemini-3.5-pro" -> "gemini-3.5-pro"
            trimmed.contains("flash 2.5") || trimmed.contains("2.5 flash") || trimmed == "gemini-2.5-flash" -> "gemini-2.5-flash"
            trimmed.contains("pro 2.5") || trimmed.contains("2.5 pro") || trimmed == "gemini-2.5-pro" -> "gemini-2.5-pro"
            trimmed.contains("flash 2.0") || trimmed.contains("2.0 flash") || trimmed == "gemini-2.0-flash" -> "gemini-2.0-flash"
            trimmed.contains("flash lite 2.0") || trimmed.contains("2.0 flash lite") || trimmed == "gemini-2.0-flash-lite" -> "gemini-2.0-flash-lite"
            trimmed.contains("flash 1.5") || trimmed.contains("1.5 flash") || trimmed == "gemini-1.5-flash" -> "gemini-1.5-flash"
            trimmed.contains("pro 1.5") || trimmed.contains("1.5 pro") || trimmed == "gemini-1.5-pro" -> "gemini-1.5-pro"
            else -> model.trim()
        }
    }

    fun getOrderedGeminiMeaningKeys(): List<String> {
        val rawKeys = _geminiMeaningApiKeys.value
        val activeIdx = _geminiMeaningActiveKeyIndex.value
        val ordered = mutableListOf<String>()

        if (activeIdx in rawKeys.indices) {
            val activeKey = rawKeys[activeIdx].trim()
            if (activeKey.isNotBlank() && !isInvalidApiKey(activeKey)) {
                ordered.add(activeKey)
            }
        }
        for (i in rawKeys.indices) {
            if (i == activeIdx) continue
            val k = rawKeys[i].trim()
            if (k.isNotBlank() && !isInvalidApiKey(k) && !ordered.contains(k)) {
                ordered.add(k)
            }
        }
        return ordered
    }

    // History & Bookmarks flows
    val historyState: StateFlow<List<WordEntity>> = repository.allWords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val bookmarksState: StateFlow<List<WordEntity>> = repository.bookmarkedWords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val readingSessionsState: StateFlow<List<ReadingSessionEntity>> = repository.allReadingSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val minedSentencesState: StateFlow<List<MinedSentenceEntity>> = repository.allMinedSentences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedSentenceCategory = MutableStateFlow("ALL")
    val selectedSentenceCategory: StateFlow<String> = _selectedSentenceCategory.asStateFlow()

    private val _sentenceSearchQuery = MutableStateFlow("")
    val sentenceSearchQuery: StateFlow<String> = _sentenceSearchQuery.asStateFlow()

    private val _activityHeatmap60Days = MutableStateFlow<List<DayStreakStatus>>(emptyList())
    val activityHeatmap60Days: StateFlow<List<DayStreakStatus>> = _activityHeatmap60Days.asStateFlow()

    private val _todayHabitActionsCount = MutableStateFlow(0)
    val todayHabitActionsCount: StateFlow<Int> = _todayHabitActionsCount.asStateFlow()

    private val _dailyGoalTarget = MutableStateFlow(10)
    val dailyGoalTarget: StateFlow<Int> = _dailyGoalTarget.asStateFlow()

    fun setSelectedSentenceCategory(category: String) {
        _selectedSentenceCategory.value = category
    }

    fun setSentenceSearchQuery(query: String) {
        _sentenceSearchQuery.value = query
    }

    fun setDailyGoalTarget(target: Int) {
        val clamped = target.coerceIn(3, 50)
        _dailyGoalTarget.value = clamped
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("daily_goal_target", clamped).apply()
    }

    fun speakSentence(sentence: String) {
        val clean = sentence.trim()
        if (clean.isEmpty()) return
        if (isTtsReady) {
            setLastPronunciationSource("Device TTS")
            updateTtsSettings()
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "sentence_${System.currentTimeMillis()}")
        } else {
            speak(clean)
        }
    }

    private fun recordDailyHabitAction(actionLabel: String, actionType: String = "Sentence") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.logReview(actionLabel, actionType)
                val today = java.time.LocalDate.now().toEpochDay()
                val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val lastStudy = sharedPrefs.getLong("last_study_date", -1L)
                if (lastStudy != today) {
                    val curStreak = if (lastStudy == today - 1) {
                        sharedPrefs.getInt("current_streak", 0) + 1
                    } else {
                        1
                    }
                    val longStreak = maxOf(sharedPrefs.getInt("longest_streak", 0), curStreak)
                    sharedPrefs.edit()
                        .putLong("last_study_date", today)
                        .putInt("current_streak", curStreak)
                        .putInt("longest_streak", longStreak)
                        .apply()
                    _currentStreak.value = curStreak
                    _longestStreak.value = longStreak
                }
                refreshStats()
            } catch (e: Exception) {
                Log.e("DictionaryViewModel", "Error recording habit action", e)
            }
        }
    }

    fun mineSentenceFromWord(wordEntity: WordEntity, category: String = "Casual") {
        val sentenceText = wordEntity.example.trim()
        if (sentenceText.isEmpty()) {
            showSnackbar("No example sentence available to mine for '${wordEntity.word}'.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.findMinedSentence(sentenceText, wordEntity.word)
            if (existing != null) {
                showSnackbar("Sentence for '${wordEntity.word}' is already in your Sentence Miner!")
                return@launch
            }
            val inferredCategory = when {
                wordEntity.tags.contains("work", ignoreCase = true) || wordEntity.tags.contains("business", ignoreCase = true) -> "Workplace"
                wordEntity.tags.contains("food", ignoreCase = true) || wordEntity.tags.contains("dining", ignoreCase = true) -> "Dining"
                wordEntity.tags.contains("travel", ignoreCase = true) -> "Travel"
                wordEntity.tags.contains("idiom", ignoreCase = true) -> "Idioms"
                else -> category
            }
            val entity = MinedSentenceEntity(
                sentence = sentenceText,
                targetWord = wordEntity.word,
                meaning = wordEntity.definition,
                category = inferredCategory,
                notes = wordEntity.mnemonic.takeIf { it.isNotBlank() } ?: "",
                isFavorite = wordEntity.isBookmarked,
                practiceCount = 0,
                lastPracticed = null,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMinedSentence(entity)
            recordDailyHabitAction(wordEntity.word, "Mined")
            showSnackbar("⛏️ Mined sentence for '${wordEntity.word}' to your Sentence Hub!")
        }
    }

    fun addCustomMinedSentence(
        sentence: String,
        targetWord: String,
        meaning: String,
        category: String,
        notes: String = ""
    ) {
        val cleanSentence = sentence.trim()
        val cleanTarget = targetWord.trim()
        val cleanMeaning = meaning.trim()
        if (cleanSentence.isEmpty() || cleanTarget.isEmpty() || cleanMeaning.isEmpty()) {
            showSnackbar("Please fill in the sentence, target phrase/word, and meaning.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val entity = MinedSentenceEntity(
                sentence = cleanSentence,
                targetWord = cleanTarget,
                meaning = cleanMeaning,
                category = category.ifBlank { "Casual" },
                notes = notes.trim(),
                isFavorite = true,
                practiceCount = 0,
                lastPracticed = null,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMinedSentence(entity)
            recordDailyHabitAction(cleanTarget, "Mined")
            showSnackbar("⛏️ Added '$cleanTarget' to your Sentence Mining Hub!")
        }
    }

    fun toggleSentenceFavorite(sentence: MinedSentenceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleMinedSentenceFavorite(sentence.id, !sentence.isFavorite)
        }
    }

    fun deleteMinedSentence(sentence: MinedSentenceEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMinedSentence(sentence)
            showSnackbar("Removed sentence for '${sentence.targetWord}'")
        }
    }

    fun practiceMinedSentence(sentence: MinedSentenceEntity, gotIt: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val newCount = if (gotIt) sentence.practiceCount + 1 else sentence.practiceCount
            repository.updateMinedSentencePractice(sentence.id, newCount, now)
            recordDailyHabitAction(sentence.targetWord, if (gotIt) "Good" else "Hard")
        }
    }

    val cardsDueTodayCount: StateFlow<Int> = repository.bookmarkedWords
        .map { list ->
            val today = java.time.LocalDate.now().toEpochDay()
            list.count { it.dueDate <= today }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _reviewQueue = MutableStateFlow<List<WordEntity>>(emptyList())
    val reviewQueue: StateFlow<List<WordEntity>> = _reviewQueue.asStateFlow()

    private val _isRatingInProgress = MutableStateFlow(false)
    val isRatingInProgress: StateFlow<Boolean> = _isRatingInProgress.asStateFlow()

    // =========================================================================
    // v1.3.0 Unified Study Session, Active Course Auto-Feed, Smart Search & Local Assets
    // =========================================================================

    private val dictionaryAudioApiService = DictionaryAudioApiService.create()

    private val _newCardsPerDay = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getInt("new_cards_per_day", 10).coerceIn(5, 50)
    )
    val newCardsPerDay: StateFlow<Int> = _newCardsPerDay.asStateFlow()

    private val _activeCourseBookNum = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getInt("active_course_book_num", 0) // 0 = none chosen yet
    )
    val activeCourseBookNum: StateFlow<Int> = _activeCourseBookNum.asStateFlow()

    private val _quizCountsInFsrs = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("quiz_counts_in_fsrs", false)
    )
    val quizCountsInFsrs: StateFlow<Boolean> = _quizCountsInFsrs.asStateFlow()

    private val _uiLanguage = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getString("ui_language", "en") ?: "en"
    )
    val uiLanguage: StateFlow<String> = _uiLanguage.asStateFlow()

    private val _newCardsIntroducedToday = MutableStateFlow(loadNewCardsIntroducedToday())
    val newCardsIntroducedToday: StateFlow<Int> = _newCardsIntroducedToday.asStateFlow()

    private fun loadNewCardsIntroducedToday(): Int {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong("new_cards_introduced_day", -1L)
        return if (storedDay == today) prefs.getInt("new_cards_introduced_count", 0).coerceAtLeast(0) else 0
    }

    private fun saveNewCardsIntroducedToday(count: Int) {
        val safe = count.coerceAtLeast(0)
        val today = java.time.LocalDate.now().toEpochDay()
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putLong("new_cards_introduced_day", today)
            .putInt("new_cards_introduced_count", safe)
            .apply()
        _newCardsIntroducedToday.value = safe
    }

    fun setNewCardsPerDay(limit: Int) {
        val clamped = limit.coerceIn(5, 50)
        _newCardsPerDay.value = clamped
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putInt("new_cards_per_day", clamped).apply()
    }

    fun setActiveCourseBookNum(bookNum: Int) {
        val safe = bookNum.coerceIn(0, 9)
        _activeCourseBookNum.value = safe
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putInt("active_course_book_num", safe).apply()
        if (safe > 0) {
            loadActiveCourseUnitMap(safe)
        }
    }

    fun setQuizCountsInFsrs(enabled: Boolean) {
        _quizCountsInFsrs.value = enabled
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("quiz_counts_in_fsrs", enabled).apply()
    }

    fun setUiLanguage(lang: String) {
        val normalized = if (lang.equals("ar", ignoreCase = true)) "ar" else "en"
        _uiLanguage.value = normalized
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString("ui_language", normalized).apply()
    }

    private val _activeCourseUnitWordMap = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val activeCourseUnitWordMap: StateFlow<Map<Int, Set<String>>> = _activeCourseUnitWordMap.asStateFlow()

    fun loadActiveCourseUnitMap(bookNum: Int = _activeCourseBookNum.value) {
        if (bookNum <= 0) return
        viewModelScope.launch {
            _activeCourseUnitWordMap.value = repository.getOfflineUnitWordLowersMap(bookNum)
        }
    }

    /** Achievable daily queue summary respecting newCardsPerDay (prevents 99+ badge overload). */
    val todayQueueSummary: StateFlow<TodayQueueSummary> = combine(
        repository.bookmarkedWords,
        _newCardsPerDay,
        _newCardsIntroducedToday
    ) { bookmarked, limit, introduced ->
        StudyQueuePlanner.summarizeToday(
            bookmarkedWords = bookmarked,
            todayEpochDay = java.time.LocalDate.now().toEpochDay(),
            newCardsDailyLimit = limit,
            newCardsIntroducedToday = introduced
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayQueueSummary(0, 0, 0, 0, 0, 10)
    )

    private val _activeStudySession = MutableStateFlow<ActiveStudySession?>(null)
    val activeStudySession: StateFlow<ActiveStudySession?> = _activeStudySession.asStateFlow()

    private val _lastSessionSummary = MutableStateFlow<SessionSummaryData?>(null)
    val lastSessionSummary: StateFlow<SessionSummaryData?> = _lastSessionSummary.asStateFlow()

    private val _localImageRefreshVersion = MutableStateFlow(0)
    val localImageRefreshVersion: StateFlow<Int> = _localImageRefreshVersion.asStateFlow()

    private val _bundledLocalImagesCount = MutableStateFlow(0)
    val bundledLocalImagesCount: StateFlow<Int> = _bundledLocalImagesCount.asStateFlow()

    private val _savedLocalImagesCount = MutableStateFlow(0)
    val savedLocalImagesCount: StateFlow<Int> = _savedLocalImagesCount.asStateFlow()

    fun refreshLocalImageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val store = LocalWordImageStore.getInstance(getApplication())
            _bundledLocalImagesCount.value = store.getBundledImageCount()
            _savedLocalImagesCount.value = store.getSavedPermanentImageCount()
        }
    }

    fun getLocalImageOrigin(word: String): com.example.data.LocalImageOrigin {
        return LocalWordImageStore.getInstance(getApplication()).getLocalImageOrigin(word)
    }

    fun startUnifiedStudySession(
        deckFilter: String = _selectedReviewDeck.value,
        deckTitle: String = if (deckFilter.equals("ALL", ignoreCase = true)) "Today's Session" else deckFilter,
        extraNewBonus: Int = 0
    ) {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toEpochDay()
            val introduced = loadNewCardsIntroducedToday()
            _newCardsIntroducedToday.value = introduced
            val freshBookmarked = repository.getBookmarkedWordsList()
            val planned = StudyQueuePlanner.buildDailyQueue(
                bookmarkedWords = freshBookmarked,
                todayEpochDay = today,
                newCardsDailyLimit = _newCardsPerDay.value,
                newCardsIntroducedToday = introduced,
                deckFilter = deckFilter,
                extraNewBonus = extraNewBonus
            )
            if (planned.isEmpty()) {
                _snackbarMessage.tryEmit("All caught up! Add new words from Courses or study ahead.")
                return@launch
            }
            _lastSessionSummary.value = null
            _activeStudySession.value = ActiveStudySession(
                sessionId = System.currentTimeMillis(),
                deckTag = deckFilter,
                deckTitle = deckTitle,
                queue = planned,
                completedCount = 0,
                initialTotal = planned.size,
                againCount = 0,
                againWords = emptyList(),
                startedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun startLeechesDrillSession() {
        viewModelScope.launch {
            val freshBookmarked = repository.getBookmarkedWordsList()
            val leeches = StudyQueuePlanner.buildLeechesDrillQueue(freshBookmarked, limit = 20)
            if (leeches.isEmpty()) {
                _snackbarMessage.tryEmit("No weak or leech words right now — great job!")
                return@launch
            }
            _lastSessionSummary.value = null
            _activeStudySession.value = ActiveStudySession(
                sessionId = System.currentTimeMillis(),
                deckTag = "LEECHES",
                deckTitle = "Weak Words Drill",
                queue = leeches,
                completedCount = 0,
                initialTotal = leeches.size,
                againCount = 0,
                againWords = emptyList(),
                startedAtMs = System.currentTimeMillis()
            )
        }
    }

    fun setSessionRecallMode(mode: StudyRecallMode) {
        val current = _activeStudySession.value ?: return
        _activeStudySession.value = current.copy(recallMode = mode)
    }

    fun closeStudySession() {
        _activeStudySession.value = null
        _lastSessionSummary.value = null
        refreshStats()
    }

    fun dismissSessionSummary() {
        _lastSessionSummary.value = null
        _activeStudySession.value = null
        refreshStats()
    }

    fun burySessionCard() {
        val session = _activeStudySession.value ?: return
        val card = session.currentCard ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val tomorrow = java.time.LocalDate.now().toEpochDay() + 1L
            repository.updateWordReview(card.copy(dueDate = tomorrow))
            val remaining = session.queue.drop(1)
            val updated = session.copy(
                queue = remaining,
                stepIndex = session.stepIndex + 1
            )
            if (updated.isFinished) {
                finishSessionToSummary(updated)
            } else {
                _activeStudySession.value = updated
            }
            _snackbarMessage.tryEmit("Buried '${card.word}' until tomorrow")
        }
    }

    /**
     * Rates the current card inside [activeStudySession], persists FSRS + ReviewLog + Streak,
     * saves an exact [LastReviewSnapshot] for instant Undo, and transitions cleanly to Summary when done.
     */
    fun rateSessionCard(rating: String) {
        val session = _activeStudySession.value ?: return
        val card = session.currentCard ?: return
        if (_isRatingInProgress.value) return
        _isRatingInProgress.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = java.time.LocalDate.now().toEpochDay()
                val wasNew = StudyQueuePlanner.classifyCard(card) == CardStudyBucket.NEW
                val isAgain = rating.equals("Again", ignoreCase = true)

                val updatedCard = computeUpdatedFsrsOrSm2Card(card, rating, today)
                repository.updateWordReview(updatedCard)
                val insertedLogId = repository.logReviewReturningId(card.word, rating)

                // Update streak
                val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val lastStudy = sharedPrefs.getLong("last_study_date", -1L)
                if (lastStudy != today) {
                    val curStreak = if (lastStudy == today - 1) sharedPrefs.getInt("current_streak", 0) + 1 else 1
                    val longStreak = maxOf(sharedPrefs.getInt("longest_streak", 0), curStreak)
                    sharedPrefs.edit()
                        .putLong("last_study_date", today)
                        .putInt("current_streak", curStreak)
                        .putInt("longest_streak", longStreak)
                        .apply()
                    _currentStreak.value = curStreak
                    _longestStreak.value = longStreak
                }

                if (wasNew) {
                    saveNewCardsIntroducedToday(loadNewCardsIntroducedToday() + 1)
                }

                val snapshot = LastReviewSnapshot(
                    previousWord = card,
                    insertedReviewLogId = insertedLogId,
                    previousQueue = session.queue,
                    previousCompletedCount = session.completedCount,
                    previousAgainCount = session.againCount,
                    previousAgainWords = session.againWords,
                    wasNewIntroduction = wasNew
                )

                val tail = session.queue.drop(1)
                val nextQueue = if (isAgain) {
                    StudyQueuePlanner.requeueAgainCard(tail, updatedCard, spacing = 3)
                } else {
                    tail
                }

                val nextSession = session.copy(
                    queue = nextQueue,
                    completedCount = session.completedCount + 1,
                    againCount = if (isAgain) session.againCount + 1 else session.againCount,
                    againWords = if (isAgain) session.againWords + card else session.againWords,
                    stepIndex = session.stepIndex + 1,
                    undoSnapshot = snapshot
                )

                refreshStats()

                if (nextSession.isFinished) {
                    finishSessionToSummary(nextSession)
                } else {
                    _activeStudySession.value = nextSession
                }
            } finally {
                _isRatingInProgress.value = false
            }
        }
    }

    private suspend fun finishSessionToSummary(finishedSession: ActiveStudySession) {
        val today = java.time.LocalDate.now().toEpochDay()
        val allBookmarked = repository.getBookmarkedWordsList()
        val tomorrowRev = StudyQueuePlanner.countTomorrowDueReviews(allBookmarked, today)
        val remainingNew = allBookmarked.count { StudyQueuePlanner.classifyCard(it) == CardStudyBucket.NEW }
        val tomorrowNew = minOf(remainingNew, _newCardsPerDay.value)
        _lastSessionSummary.value = finishedSession.toSummary(
            tomorrowReviews = tomorrowRev,
            tomorrowNew = tomorrowNew
        )
        _activeStudySession.value = finishedSession
    }

    /**
     * Exact single-step Undo: restores previous WordEntity in Room, deletes the inserted ReviewLog,
     * and restores the previous study session state cleanly.
     */
    fun undoLastSessionRating() {
        val session = _activeStudySession.value ?: return
        val snap = session.undoSnapshot ?: return
        if (_isRatingInProgress.value) return
        _isRatingInProgress.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateWordReview(snap.previousWord)
                snap.insertedReviewLogId?.let { id ->
                    if (id > 0L) repository.deleteReviewLogById(id)
                }
                if (snap.wasNewIntroduction) {
                    saveNewCardsIntroducedToday((loadNewCardsIntroducedToday() - 1).coerceAtLeast(0))
                }
                _lastSessionSummary.value = null
                _activeStudySession.value = session.copy(
                    queue = snap.previousQueue,
                    completedCount = snap.previousCompletedCount,
                    againCount = snap.previousAgainCount,
                    againWords = snap.previousAgainWords,
                    stepIndex = session.stepIndex + 1,
                    undoSnapshot = null
                )
                refreshStats()
                _snackbarMessage.tryEmit("Undid rating for '${snap.previousWord.word}'")
            } finally {
                _isRatingInProgress.value = false
            }
        }
    }

    /**
     * Cold-start 1-tap onboarding: sets Active Course + New Cards/Day, imports the first batch
     * from Unit 1, and immediately launches the learner's first study session.
     */
    fun startDayOnePlan(bookNum: Int, dailyNewLimit: Int) {
        setActiveCourseBookNum(bookNum)
        setNewCardsPerDay(dailyNewLimit)
        viewModelScope.launch {
            _isAddingCourseWords.value = true
            try {
                if (_offlineBooks.value.isEmpty()) {
                    _offlineBooks.value = repository.getOfflineBooks()
                    _offlineTopics.value = repository.getOfflineTopics()
                    _bookWordLowersMap.value = repository.getOfflineBookWordLowersMap()
                    _topicWordLowersMap.value = repository.getOfflineTopicWordLowersMap()
                }
                val units = repository.getOfflineUnitsForBook(bookNum)
                val firstUnit = units.firstOrNull()
                if (firstUnit != null) {
                    val words = repository.getOfflineWordsForUnit(bookNum, firstUnit.unitNum)
                    val batch = words.take(dailyNewLimit.coerceAtLeast(5))
                    repository.addCurriculumWordsToDeck(batch)
                    loadActiveCourseUnitMap(bookNum)
                    val bookTitle = _offlineBooks.value.firstOrNull { it.bookNum == bookNum }?.shortTag ?: "Book $bookNum"
                    startUnifiedStudySession(deckFilter = "ALL", deckTitle = "$bookTitle · Day 1")
                }
            } finally {
                _isAddingCourseWords.value = false
            }
        }
    }

    /**
     * Auto-feeds the next [count] unseen words from the learner's Active Course into the deck
     * and optionally starts studying them immediately.
     */
    fun autoFeedActiveCourseWords(count: Int = _newCardsPerDay.value, launchSession: Boolean = true) {
        val bookNum = _activeCourseBookNum.value.takeIf { it > 0 } ?: 1
        viewModelScope.launch {
            _isAddingCourseWords.value = true
            try {
                val units = repository.getOfflineUnitsForBook(bookNum)
                val existingDeck = repository.getBookmarkedWordsList().map { it.word.trim().lowercase() }.toSet()
                val gathered = mutableListOf<OfflineCurriculumWord>()
                for (u in units) {
                    if (gathered.size >= count) break
                    val uWords = repository.getOfflineWordsForUnit(bookNum, u.unitNum)
                    val picked = CourseProgressCalculator.selectNextAutoFeedWords(
                        orderedUnitWords = uWords,
                        existingDeckWordsLower = existingDeck + gathered.map { it.word.trim().lowercase() },
                        batchSize = count - gathered.size
                    )
                    gathered.addAll(picked)
                }
                if (gathered.isEmpty()) {
                    _snackbarMessage.tryEmit("You already added all words from this course!")
                } else {
                    val added = repository.addCurriculumWordsToDeck(gathered)
                    loadActiveCourseUnitMap(bookNum)
                    _snackbarMessage.tryEmit("Added +$added new words from your active course")
                    if (launchSession) {
                        startUnifiedStudySession(extraNewBonus = added)
                    }
                }
            } finally {
                _isAddingCourseWords.value = false
            }
        }
    }

    /**
     * 1-tap "Study Unit Now": ensures the next batch of words from the unit are in the deck,
     * then launches a focused full-screen FSRS session for that unit immediately.
     */
    fun studyUnitNow(
        book: OfflineBook,
        unit: OfflineUnit,
        unitWords: List<OfflineCurriculumWord>,
        batchSize: Int = _newCardsPerDay.value
    ) {
        viewModelScope.launch {
            _isAddingCourseWords.value = true
            try {
                val existingDeck = repository.getBookmarkedWordsList().map { it.word.trim().lowercase() }.toSet()
                val toAdd = CourseProgressCalculator.selectNextAutoFeedWords(unitWords, existingDeck, batchSize)
                val added = if (toAdd.isNotEmpty()) repository.addCurriculumWordsToDeck(toAdd) else 0
                val unitTag = OfflineDictionaryDatabase.buildDeckTagForUnit(book.shortTag, unit.unitNum)
                startUnifiedStudySession(
                    deckFilter = unitTag,
                    deckTitle = "${book.shortTag} · ${unit.title}",
                    extraNewBonus = maxOf(added, batchSize)
                )
            } finally {
                _isAddingCourseWords.value = false
            }
        }
    }

    private fun computeUpdatedFsrsOrSm2Card(word: WordEntity, rating: String, today: Long): WordEntity {
        val eBonus = _easyBonus.value
        val sEase = _startingEase.value
        val iModifier = _intervalModifier.value
        val hMultiplier = _hardIntervalMultiplier.value
        val currentEase = if (word.repetitions == 0) sEase else word.easeFactor
        val hadRepetitions = word.repetitions > 0
        val isAgain = rating.equals("Again", ignoreCase = true)
        val newLapses = if (isAgain && hadRepetitions) word.lapses + 1 else word.lapses
        val becameLeech = (newLapses >= _leechThreshold.value) && !word.isLeech
        val newIsLeech = word.isLeech || becameLeech
        val hadNewSuccessDay = !isAgain && (word.lastSuccessfulReviewDay == null || word.lastSuccessfulReviewDay != today)
        val newDistinctDays = if (hadNewSuccessDay) word.distinctReviewDays + 1 else word.distinctReviewDays
        val newLastSuccessfulReviewDay = if (!isAgain) today else word.lastSuccessfulReviewDay

        if (becameLeech) {
            showSnackbar("'${word.word}' is now marked as a Leech — try the AI Helper for a memory aid")
        }

        return if (_srsAlgorithm.value.equals("fsrs", ignoreCase = true)) {
            val fsrsRating = when (rating) {
                "Again" -> Rating.AGAIN
                "Hard" -> Rating.HARD
                "Good" -> Rating.GOOD
                "Easy" -> Rating.EASY
                else -> Rating.GOOD
            }
            val retentionFraction = (_targetRetention.value.toDouble() / 100.0).coerceIn(0.80, 0.97)
            val scheduler = Scheduler.builder().desiredRetention(retentionFraction).build()
            val currentFsrsCard = if (word.fsrsStability != null && word.fsrsDifficulty != null) {
                val builder = Card.builder().stability(word.fsrsStability).difficulty(word.fsrsDifficulty)
                if (word.fsrsDue != null) builder.due(java.time.Instant.ofEpochMilli(word.fsrsDue))
                if (word.fsrsLastReview != null) builder.lastReview(java.time.Instant.ofEpochMilli(word.fsrsLastReview))
                if (word.fsrsStep != null) builder.step(word.fsrsStep)
                val parsedState = try {
                    if (word.fsrsState != null) State.valueOf(word.fsrsState) else State.REVIEW
                } catch (_: Exception) { State.REVIEW }
                builder.state(parsedState).build()
            } else {
                Card.builder().build()
            }
            val nowInstant = java.time.Instant.now()
            val result = scheduler.reviewCard(currentFsrsCard, fsrsRating, nowInstant)
            val nextCard = result.card()
            val dueInstant = nextCard.due ?: nowInstant
            val calculatedDueDay = dueInstant.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
            val rawDaysDiff = maxOf(0, (calculatedDueDay - today).toInt())
            val modifiedDays = if (rawDaysDiff > 0) (rawDaysDiff * iModifier).toInt().coerceAtLeast(1) else 0
            val nextDueEpochDay = if (fsrsRating == Rating.AGAIN) today else today + modifiedDays
            word.copy(
                repetitions = if (fsrsRating == Rating.AGAIN) 0 else word.repetitions + 1,
                interval = modifiedDays,
                lapses = newLapses,
                isLeech = newIsLeech,
                distinctReviewDays = newDistinctDays,
                lastSuccessfulReviewDay = newLastSuccessfulReviewDay,
                dueDate = nextDueEpochDay,
                lastReviewed = today,
                fsrsStability = nextCard.stability,
                fsrsDifficulty = nextCard.difficulty,
                fsrsDue = dueInstant.toEpochMilli(),
                fsrsState = nextCard.state?.name,
                fsrsStep = nextCard.step,
                fsrsLastReview = nextCard.lastReview?.toEpochMilli() ?: nowInstant.toEpochMilli()
            )
        } else {
            val baseInterval = when (rating) {
                "Again" -> 0
                "Hard" -> if (word.repetitions == 0) 1 else (word.interval * hMultiplier).toInt().coerceAtLeast(1)
                "Good" -> when (word.repetitions) {
                    0 -> 1
                    1 -> 6
                    else -> (word.interval * currentEase).toInt().coerceAtLeast(1)
                }
                "Easy" -> when (word.repetitions) {
                    0 -> 4
                    1 -> (6 * eBonus).toInt().coerceAtLeast(7)
                    else -> (word.interval * currentEase * eBonus).toInt().coerceAtLeast(1)
                }
                else -> 1
            }
            val finalInterval = if (baseInterval > 0) (baseInterval * iModifier).toInt().coerceAtLeast(1) else 0
            val nextEase = when (rating) {
                "Again" -> (currentEase - 0.2f).coerceAtLeast(1.3f)
                "Hard" -> (currentEase - 0.15f).coerceAtLeast(1.3f)
                "Easy" -> currentEase + 0.15f
                else -> currentEase
            }
            word.copy(
                repetitions = if (isAgain) 0 else word.repetitions + 1,
                easeFactor = nextEase,
                interval = finalInterval,
                lapses = newLapses,
                isLeech = newIsLeech,
                distinctReviewDays = newDistinctDays,
                lastSuccessfulReviewDay = newLastSuccessfulReviewDay,
                dueDate = today + finalInterval,
                lastReviewed = today
            )
        }
    }

    // --- Smart Search StateFlows (Scope, Fuzzy, Word of the Day, CEFR, Back Stack, Occurrences) ---
    private val _fuzzySuggestions = MutableStateFlow<List<OfflineSearchSuggestion>>(emptyList())
    val fuzzySuggestions: StateFlow<List<OfflineSearchSuggestion>> = _fuzzySuggestions.asStateFlow()

    private val _wordOfTheDay = MutableStateFlow<OfflineCurriculumWord?>(null)
    val wordOfTheDay: StateFlow<OfflineCurriculumWord?> = _wordOfTheDay.asStateFlow()

    private val _selectedCefrBrowse = MutableStateFlow<String?>(null)
    val selectedCefrBrowse: StateFlow<String?> = _selectedCefrBrowse.asStateFlow()

    private val _cefrBrowseWords = MutableStateFlow<List<OfflineCurriculumWord>>(emptyList())
    val cefrBrowseWords: StateFlow<List<OfflineCurriculumWord>> = _cefrBrowseWords.asStateFlow()

    private val _currentWordOccurrences = MutableStateFlow<List<OfflineCurriculumWord>>(emptyList())
    val currentWordOccurrences: StateFlow<List<OfflineCurriculumWord>> = _currentWordOccurrences.asStateFlow()

    private val _searchWordBackStack = MutableStateFlow<List<String>>(emptyList())
    val searchWordBackStack: StateFlow<List<String>> = _searchWordBackStack.asStateFlow()

    fun loadWordOfTheDay() {
        viewModelScope.launch {
            if (_wordOfTheDay.value == null) {
                _wordOfTheDay.value = repository.getWordOfTheDay()
            }
        }
    }

    fun selectCefrBrowse(cefr: String?) {
        _selectedCefrBrowse.value = cefr
        if (cefr.isNullOrBlank()) {
            _cefrBrowseWords.value = emptyList()
            return
        }
        viewModelScope.launch {
            _cefrBrowseWords.value = repository.getWordsByCefr(cefr, limit = 40)
        }
    }

    fun searchSuggestionsWithScope(query: String, booksOnly: Boolean = false) {
        _searchQuery.value = query
        suggestionsJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _searchSuggestions.value = emptyList()
            _fuzzySuggestions.value = emptyList()
            return
        }
        suggestionsJob = viewModelScope.launch {
            delay(90)
            val hits = repository.searchOfflineSuggestions(trimmed, limit = 12, booksOnly = booksOnly)
            _searchSuggestions.value = hits
            _fuzzySuggestions.value = if (hits.isEmpty()) {
                repository.findFuzzyOfflineSuggestions(trimmed, limit = 5)
            } else {
                emptyList()
            }
        }
    }

    fun openWordFromSearch(word: String, pushToBackStack: Boolean = true) {
        val currentState = _searchUiState.value
        if (pushToBackStack && currentState is SearchUiState.Success &&
            !currentState.word.word.equals(word, ignoreCase = true)
        ) {
            _searchWordBackStack.value = (_searchWordBackStack.value + currentState.word.word).takeLast(12)
        }
        performSearch(word)
        viewModelScope.launch {
            _currentWordOccurrences.value = repository.findCurriculumOccurrences(word)
        }
    }

    fun navigateBackInDictionary(): Boolean {
        val stack = _searchWordBackStack.value
        if (stack.isNotEmpty()) {
            val prev = stack.last()
            _searchWordBackStack.value = stack.dropLast(1)
            performSearch(prev)
            viewModelScope.launch {
                _currentWordOccurrences.value = repository.findCurriculumOccurrences(prev)
            }
            return true
        }
        if (_searchUiState.value !is SearchUiState.Idle) {
            _searchUiState.value = SearchUiState.Idle
            _currentWordOccurrences.value = emptyList()
            return true
        }
        return false
    }

    fun quickToggleStudyWord(word: String, translationAr: String = "") {
        val clean = word.trim().lowercase()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getLocalWord(clean)
            if (existing != null) {
                toggleBookmark(existing)
                val msg = if (!existing.isBookmarked) "Added '${existing.word}' to Study Deck" else "Removed '${existing.word}' from Study Deck"
                _snackbarMessage.tryEmit(msg)
            } else {
                val res = repository.searchAndFetchWord(clean)
                res.onSuccess { entity ->
                    repository.toggleBookmark(entity.word, true)
                    _snackbarMessage.tryEmit("Added '${entity.word}' to Study Deck")
                }.onFailure {
                    if (translationAr.isNotBlank()) {
                        val fallback = WordEntity(
                            word = clean,
                            definition = translationAr,
                            arabicMeaning = translationAr,
                            isBookmarked = true,
                            tags = "Offline Dictionary"
                        )
                        repository.insertWordIfNotExist(fallback)
                        repository.toggleBookmark(clean, true)
                        _snackbarMessage.tryEmit("Added '$clean' to Study Deck")
                    }
                }
            }
        }
    }

    /**
     * Explicit on-demand AI Voice (Gemini TTS) for the per-word AI Helper button.
     * Caches the audio permanently to filesDir/audio/ when generated so future replays are 100% offline.
     */
    fun speakWithAiVoice(text: String, cacheWordKey: String = text) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val geminiKeys = _geminiApiKeys.value.map { it.trim() }.filter { !isInvalidApiKey(it) }
            if (geminiKeys.isEmpty()) {
                _snackbarMessage.tryEmit("Add a Gemini API key in Settings for AI Voice — playing Natural Offline voice instead.")
                setLastPronunciationSource("Device TTS")
                speakWithTts(clean)
                return@launch
            }
            if (isTtsNetworkThrottled()) {
                _snackbarMessage.tryEmit("AI Voice cooldown active — playing Natural Offline voice instead.")
                setLastPronunciationSource("Device TTS")
                speakWithTts(clean)
                return@launch
            }
            val audioBytes = fetchGeminiTtsAudio(clean)
            if (audioBytes != null && audioBytes.isNotEmpty()) {
                if (cacheWordKey.trim().lowercase() == clean.lowercase()) {
                    checkAndSaveIfBookmarked(cacheWordKey, audioBytes)
                }
                playAudioBytes(audioBytes) {
                    setLastPronunciationSource("Device TTS")
                    speakWithTts(clean)
                }
            } else {
                _snackbarMessage.tryEmit("AI Voice unavailable right now — using Natural Offline voice.")
                setLastPronunciationSource("Device TTS")
                speakWithTts(clean)
            }
        }
    }

    private suspend fun fetchOnlineDictionaryAudioBytes(word: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val entries = dictionaryAudioApiService.getWordEntries(word)
            val urls = entries.flatMap { it.phonetics.orEmpty() }.mapNotNull { it.audio }
            val preferUk = _selectedAccent.value.equals("UK", ignoreCase = true)
            val chosenUrl = selectAudioUrl(urls, preferUk) ?: return@withContext null
            val req = Request.Builder()
                .url(chosenUrl)
                .header("User-Agent", "RedeusDictionary/1.3")
                .get()
                .build()
            ttsHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bytes = resp.body?.bytes()
                if (bytes != null && bytes.size > 128) bytes else null
            }
        } catch (e: Exception) {
            Log.w(TAG_TTS, "Online dictionary audio fetch failed for '$word': ${e.message}")
            null
        }
    }

    private suspend fun saveAudioPermanentlyForWord(word: String, audioBytes: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val cleanKey = word.trim().lowercase(Locale.ROOT)
            val audioDir = File(getApplication<Application>().filesDir, "audio").apply { mkdirs() }
            val safeName = cleanKey.replace(Regex("[^a-z0-9_-]"), "_").ifBlank { "word" }
            val ext = if (audioBytes.size >= 4 && audioBytes[0] == 'R'.code.toByte() && audioBytes[1] == 'I'.code.toByte()) "wav" else "mp3"
            val outFile = File(audioDir, "${safeName}_${cleanKey.hashCode().toUInt().toString(16)}.$ext")
            outFile.writeBytes(audioBytes)
            if (repository.getLocalWord(cleanKey) != null) {
                repository.updateAudioPath(cleanKey, outFile.absolutePath)
            }
        } catch (e: Exception) {
            Log.w(TAG_TTS, "Failed to persist audio for '$word'", e)
        }
    }

    // --- Advanced Search & Filters State ---
    private val _filterSearchQuery = MutableStateFlow("")
    val filterSearchQuery: StateFlow<String> = _filterSearchQuery.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow(WordStatusFilter.ALL)
    val selectedStatusFilter: StateFlow<WordStatusFilter> = _selectedStatusFilter.asStateFlow()

    private val _selectedPosFilter = MutableStateFlow("ALL")
    val selectedPosFilter: StateFlow<String> = _selectedPosFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow("")
    val selectedTagFilter: StateFlow<String> = _selectedTagFilter.asStateFlow()

    private val _selectedReviewDeck = MutableStateFlow("ALL")
    val selectedReviewDeck: StateFlow<String> = _selectedReviewDeck.asStateFlow()

    fun setFilterSearchQuery(query: String) { _filterSearchQuery.value = query }
    fun setSelectedStatusFilter(filter: WordStatusFilter) { _selectedStatusFilter.value = filter }
    fun setSelectedPosFilter(pos: String) { _selectedPosFilter.value = pos }
    fun setSelectedTagFilter(tag: String) { _selectedTagFilter.value = tag }
    fun setSelectedReviewDeck(deck: String) {
        _selectedReviewDeck.value = deck
        startReviewSession(deck)
    }

    // Dynamic list of all available tags across all words
    val allAvailableTags: StateFlow<List<String>> = historyState.map { words ->
        words.flatMap { word ->
            word.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tag management methods
    fun updateWordTags(word: String, tags: List<String>) {
        val cleanTags = tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(", ")
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWordTags(word, cleanTags)
            val currentState = _searchUiState.value
            if (currentState is SearchUiState.Success && currentState.word.word.equals(word, ignoreCase = true)) {
                _searchUiState.value = SearchUiState.Success(currentState.word.copy(tags = cleanTags))
            }
        }
    }

    fun addTagToWord(word: String, newTag: String) {
        val tagClean = newTag.trim()
        if (tagClean.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getLocalWord(word)
            if (existing != null) {
                val currentTags = existing.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                if (!currentTags.any { it.equals(tagClean, ignoreCase = true) }) {
                    currentTags.add(tagClean)
                    updateWordTags(word, currentTags)
                }
            }
        }
    }

    fun removeTagFromWord(word: String, tagToRemove: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getLocalWord(word)
            if (existing != null) {
                val currentTags = existing.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val updated = currentTags.filterNot { it.equals(tagToRemove.trim(), ignoreCase = true) }
                updateWordTags(word, updated)
            }
        }
    }

    // --- Study Modes in Cards tab ---
    private val _studyMode = MutableStateFlow(FlashcardStudyMode.CARDS)
    val studyMode: StateFlow<FlashcardStudyMode> = _studyMode.asStateFlow()

    fun setStudyMode(mode: FlashcardStudyMode) {
        _studyMode.value = mode
        if (mode == FlashcardStudyMode.QUIZ && _quizState.value.currentQuestion == null) {
            generateNextQuizQuestion()
        } else if (mode == FlashcardStudyMode.SPELLING && _spellingState.value.targetWord == null) {
            generateNextSpellingQuestion()
        }
    }

    // --- Interactive Quiz Logic ---
    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    fun generateNextQuizQuestion() {
        val candidates = bookmarksState.value
        if (candidates.isEmpty()) {
            _quizState.value = _quizState.value.copy(currentQuestion = null)
            return
        }
        val target = candidates.random()
        val otherWords = candidates.filter { it.word.lowercase() != target.word.lowercase() }.map { it.word }.shuffled()
        val distractors = if (otherWords.size >= 3) {
            otherWords.take(3)
        } else {
            val fallbackPool = listOf("ubiquitous", "ephemeral", "resilience", "eloquent", "benevolent", "serendipity", "lucid", "perseverance", "meticulous", "tenacious")
                .filter { it.lowercase() != target.word.lowercase() && !otherWords.contains(it) }
                .shuffled()
            (otherWords + fallbackPool).take(3)
        }
        val options = (distractors + target.word).shuffled()
        _quizState.value = _quizState.value.copy(
            currentQuestion = QuizQuestion(targetWord = target, options = options)
        )
    }

    fun answerQuiz(choice: String) {
        val currentQ = _quizState.value.currentQuestion ?: return
        if (currentQ.selectedAnswer != null) return // already answered
        val isCorrect = choice.equals(currentQ.targetWord.word, ignoreCase = true)
        val newScore = if (isCorrect) _quizState.value.score + 1 else _quizState.value.score
        val newStreak = if (isCorrect) _quizState.value.currentStreak + 1 else 0
        _quizState.value = _quizState.value.copy(
            currentQuestion = currentQ.copy(selectedAnswer = choice, isCorrect = isCorrect),
            score = newScore,
            totalAnswered = _quizState.value.totalAnswered + 1,
            currentStreak = newStreak
        )
        if (isCorrect) {
            speak(currentQ.targetWord.word)
        }
        if (_quizCountsInFsrs.value) {
            rateCard(currentQ.targetWord, if (isCorrect) "Good" else "Again")
        }
    }

    fun restartQuiz() {
        _quizState.value = QuizState()
        generateNextQuizQuestion()
    }

    // --- Spelling Test Logic ---
    private val _spellingState = MutableStateFlow(SpellingTestState())
    val spellingState: StateFlow<SpellingTestState> = _spellingState.asStateFlow()

    fun generateNextSpellingQuestion() {
        val candidates = bookmarksState.value
        if (candidates.isEmpty()) {
            _spellingState.value = _spellingState.value.copy(targetWord = null)
            return
        }
        val target = candidates.random()
        _spellingState.value = _spellingState.value.copy(
            targetWord = target,
            userInput = "",
            isChecked = false,
            isCorrect = null,
            hintLevel = 0
        )
        speak(target.word)
    }

    fun updateSpellingInput(input: String) {
        if (!_spellingState.value.isChecked) {
            _spellingState.value = _spellingState.value.copy(userInput = input)
        }
    }

    fun revealSpellingHint() {
        val current = _spellingState.value
        if (current.hintLevel < 2) {
            _spellingState.value = current.copy(hintLevel = current.hintLevel + 1)
        }
    }

    fun checkSpelling() {
        val current = _spellingState.value
        val target = current.targetWord ?: return
        if (current.isChecked) return
        val isCorrect = current.userInput.trim().equals(target.word.trim(), ignoreCase = true)
        val newScore = if (isCorrect) current.score + 1 else current.score
        val newStreak = if (isCorrect) current.currentStreak + 1 else 0
        _spellingState.value = current.copy(
            isChecked = true,
            isCorrect = isCorrect,
            score = newScore,
            totalAnswered = current.totalAnswered + 1,
            currentStreak = newStreak
        )
        speak(target.word)
        if (_quizCountsInFsrs.value) {
            rateCard(target, if (isCorrect) "Good" else "Again")
        }
    }

    fun restartSpelling() {
        _spellingState.value = SpellingTestState()
        generateNextSpellingQuestion()
    }

    fun startReviewSession(deckFilter: String? = null) {
        val today = java.time.LocalDate.now().toEpochDay()
        val targetDeck = deckFilter ?: _selectedReviewDeck.value
        val due = bookmarksState.value.filter { card ->
            val isDue = card.dueDate <= today
            val matchesDeck = targetDeck.isBlank() || targetDeck.equals("ALL", ignoreCase = true) ||
                card.tags.split(",").map { it.trim().lowercase() }.contains(targetDeck.trim().lowercase())
            isDue && matchesDeck
        }
        _reviewQueue.value = due
    }

    // Spaced Repetition Algorithm Settings StateFlows
    private val _srsAlgorithm = MutableStateFlow("fsrs")
    val srsAlgorithm: StateFlow<String> = _srsAlgorithm.asStateFlow()

    private val _targetRetention = MutableStateFlow(90)
    val targetRetention: StateFlow<Int> = _targetRetention.asStateFlow()

    // Advanced SRS Settings StateFlows
    private val _easyBonus = MutableStateFlow(1.3f)
    val easyBonus: StateFlow<Float> = _easyBonus.asStateFlow()

    private val _startingEase = MutableStateFlow(2.5f)
    val startingEase: StateFlow<Float> = _startingEase.asStateFlow()

    private val _intervalModifier = MutableStateFlow(1.0f)
    val intervalModifier: StateFlow<Float> = _intervalModifier.asStateFlow()

    private val _hardIntervalMultiplier = MutableStateFlow(1.2f)
    val hardIntervalMultiplier: StateFlow<Float> = _hardIntervalMultiplier.asStateFlow()

    private val _leechThreshold = MutableStateFlow(8)
    val leechThreshold: StateFlow<Int> = _leechThreshold.asStateFlow()

    // Streak StateFlows
    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()

    // Statistics StateFlows
    private val _successRate = MutableStateFlow(0f)
    val successRate: StateFlow<Float> = _successRate.asStateFlow()

    private val _reviewsToday = MutableStateFlow(0)
    val reviewsToday: StateFlow<Int> = _reviewsToday.asStateFlow()

    private val _reviewsThisWeek = MutableStateFlow(0)
    val reviewsThisWeek: StateFlow<Int> = _reviewsThisWeek.asStateFlow()

    private val _detailedSrsStats = MutableStateFlow(DetailedSrsStats())
    val detailedSrsStats: StateFlow<DetailedSrsStats> = _detailedSrsStats.asStateFlow()

    // Reactively computed statistics from historyState
    val totalWordsCount: StateFlow<Int> = historyState.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val wordsMasteredCount: StateFlow<Int> = historyState.map { list ->
        list.count { it.distinctReviewDays >= 3 && it.interval >= 6 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val wordsLearningCount: StateFlow<Int> = historyState.map { list ->
        list.count { (it.repetitions > 0 || it.distinctReviewDays > 0) && !(it.distinctReviewDays >= 3 && it.interval >= 6) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val wordsNewCount: StateFlow<Int> = historyState.map { list ->
        list.count { it.repetitions == 0 && it.distinctReviewDays == 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val leechWordsList: StateFlow<List<WordEntity>> = historyState.map { list ->
        list.filter { it.isLeech }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leechWordsCount: StateFlow<Int> = historyState.map { list ->
        list.count { it.isLeech }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSrsAlgorithm(algorithm: String) {
        val valid = if (algorithm.equals("fsrs", ignoreCase = true)) "fsrs" else "sm2"
        _srsAlgorithm.value = valid
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("srs_algorithm", valid).apply()
    }

    fun setTargetRetention(retention: Int) {
        val clamped = retention.coerceIn(80, 97)
        _targetRetention.value = clamped
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("srs_target_retention", clamped).apply()
    }

    // Advanced SRS Settings Setters
    fun setEasyBonus(value: Float) {
        _easyBonus.value = value
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putFloat("srs_easy_bonus", value).apply()
    }

    fun setStartingEase(value: Float) {
        _startingEase.value = value
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putFloat("srs_starting_ease", value).apply()
    }

    fun setIntervalModifier(value: Float) {
        _intervalModifier.value = value
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putFloat("srs_interval_modifier", value).apply()
    }

    fun setHardIntervalMultiplier(value: Float) {
        _hardIntervalMultiplier.value = value
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putFloat("srs_hard_interval_multiplier", value).apply()
    }

    fun setLeechThreshold(value: Int) {
        val clamped = value.coerceIn(1, 99)
        _leechThreshold.value = clamped
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("srs_leech_threshold", clamped).apply()
    }

    fun clearLeechFlag(word: String) {
        viewModelScope.launch {
            repository.clearLeechFlag(word)
            refreshStats()
        }
    }

    fun resetSrsSettingsToDefault() {
        setSrsAlgorithm("sm2")
        setTargetRetention(90)
        setEasyBonus(1.3f)
        setStartingEase(2.5f)
        setIntervalModifier(1.0f)
        setHardIntervalMultiplier(1.2f)
        setLeechThreshold(8)
    }

    // Refresh review statistics from history logs
    fun refreshStats() {
        refreshUsageStats()
        viewModelScope.launch {
            try {
                val logs = repository.getAllReviewLogs()
                val total = logs.size
                if (total > 0) {
                    val goodOrEasy = logs.count { it.rating == "Good" || it.rating == "Easy" }
                    _successRate.value = (goodOrEasy.toFloat() / total) * 100f
                } else {
                    _successRate.value = 0f
                }

                val today = java.time.LocalDate.now()
                val zoneId = java.time.ZoneId.systemDefault()
                val todayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val oneWeekAgo = todayStart - (7 * 24 * 60 * 60 * 1000L)
                val thirtyDaysAgoStart = today.minusDays(29).atStartOfDay(zoneId).toInstant().toEpochMilli()

                _reviewsToday.value = logs.count { it.timestamp >= todayStart }
                _reviewsThisWeek.value = logs.count { it.timestamp >= oneWeekAgo }

                // 1. RETENTION RATE (Overall vs Last 30 Days)
                val totalReviewsAllTime = total
                val nonAgainAllTime = logs.count { !it.rating.equals("Again", ignoreCase = true) }
                val overallRetentionRate = if (totalReviewsAllTime > 0) {
                    (nonAgainAllTime.toFloat() / totalReviewsAllTime) * 100f
                } else 0f

                val logsLast30Days = logs.filter { it.timestamp >= thirtyDaysAgoStart }
                val totalReviewsLast30Days = logsLast30Days.size
                val nonAgainLast30Days = logsLast30Days.count { !it.rating.equals("Again", ignoreCase = true) }
                val last30DaysRetentionRate = if (totalReviewsLast30Days > 0) {
                    (nonAgainLast30Days.toFloat() / totalReviewsLast30Days) * 100f
                } else 0f

                // 2. REVIEWS PER DAY (Last 30 Days) & STREAK HEATMAP
                val logsByDay = mutableMapOf<Long, Int>()
                for (log in logs) {
                    try {
                        val logDate = java.time.Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
                        val epochDay = logDate.toEpochDay()
                        logsByDay[epochDay] = (logsByDay[epochDay] ?: 0) + 1
                    } catch (_: Exception) {}
                }

                val reviewsPerDayLast30Days = (29 downTo 0).map { offset ->
                    val date = today.minusDays(offset.toLong())
                    val epochDay = date.toEpochDay()
                    val count = logsByDay[epochDay] ?: 0
                    val label = "${date.monthValue}/${date.dayOfMonth}"
                    DailyReviewStat(epochDay = epochDay, dateLabel = label, count = count)
                }

                val streakHistoryLast30Days = (29 downTo 0).map { offset ->
                    val date = today.minusDays(offset.toLong())
                    val epochDay = date.toEpochDay()
                    val count = logsByDay[epochDay] ?: 0
                    val label = "${date.monthValue}/${date.dayOfMonth}"
                    DayStreakStatus(
                        epochDay = epochDay,
                        dateLabel = label,
                        hasReview = count > 0,
                        count = count,
                        isToday = (offset == 0)
                    )
                }

                val streakHistoryLast60Days = (59 downTo 0).map { offset ->
                    val date = today.minusDays(offset.toLong())
                    val epochDay = date.toEpochDay()
                    val count = logsByDay[epochDay] ?: 0
                    val label = "${date.monthValue}/${date.dayOfMonth}"
                    DayStreakStatus(
                        epochDay = epochDay,
                        dateLabel = label,
                        hasReview = count > 0,
                        count = count,
                        isToday = (offset == 0)
                    )
                }
                _activityHeatmap60Days.value = streakHistoryLast60Days
                _todayHabitActionsCount.value = logsByDay[today.toEpochDay()] ?: 0

                // 3. RATING DISTRIBUTION (All-Time)
                val againCount = logs.count { it.rating.equals("Again", ignoreCase = true) }
                val hardCount = logs.count { it.rating.equals("Hard", ignoreCase = true) }
                val goodCount = logs.count { it.rating.equals("Good", ignoreCase = true) }
                val easyCount = logs.count { it.rating.equals("Easy", ignoreCase = true) }
                val ratingDistribution = SrsRatingDistribution(
                    againCount = againCount,
                    hardCount = hardCount,
                    goodCount = goodCount,
                    easyCount = easyCount,
                    totalCount = totalReviewsAllTime
                )

                // 4. EASE FACTOR DISTRIBUTION & 5. FORECAST (Next 14 Days)
                // Restrict calculation to active learning cards (bookmarked or reviewed) so unbookmarked search history does not distort forecasts
                val allWords = repository.getAllWordsList()
                val words = allWords.filter { it.isBookmarked || it.repetitions > 0 }
                var bucketUnder1_8 = 0
                var bucket1_8to2_2 = 0
                var bucket2_2to2_6 = 0
                var bucket2_6to3_0 = 0
                var bucketOver3_0 = 0

                for (w in words) {
                    when {
                        w.easeFactor < 1.8f -> bucketUnder1_8++
                        w.easeFactor < 2.2f -> bucket1_8to2_2++
                        w.easeFactor < 2.6f -> bucket2_2to2_6++
                        w.easeFactor <= 3.0f -> bucket2_6to3_0++
                        else -> bucketOver3_0++
                    }
                }
                val easeFactorDistribution = EaseFactorDistribution(
                    bucketUnder1_8 = bucketUnder1_8,
                    bucket1_8to2_2 = bucket1_8to2_2,
                    bucket2_2to2_6 = bucket2_2to2_6,
                    bucket2_6to3_0 = bucket2_6to3_0,
                    bucketOver3_0 = bucketOver3_0,
                    totalWords = words.size
                )

                val todayEpochDay = today.toEpochDay()
                val forecastNext14Days = (0..13).map { offset ->
                    val targetEpochDay = todayEpochDay + offset
                    val count = if (offset == 0) {
                        words.count { it.dueDate <= targetEpochDay }
                    } else {
                        words.count { it.dueDate == targetEpochDay }
                    }
                    val targetDate = java.time.LocalDate.ofEpochDay(targetEpochDay)
                    val dayOfWeek = targetDate.dayOfWeek.name.take(3)
                    val label = if (offset == 0) "Today" else "${targetDate.monthValue}/${targetDate.dayOfMonth}"
                    ForecastDayStat(
                        dayOffset = offset,
                        dateLabel = label,
                        dayOfWeek = dayOfWeek,
                        count = count
                    )
                }

                val currentLeechCount = words.count { it.isLeech }

                _detailedSrsStats.value = DetailedSrsStats(
                    totalReviewsAllTime = totalReviewsAllTime,
                    totalReviewsLast30Days = totalReviewsLast30Days,
                    overallRetentionRate = overallRetentionRate,
                    last30DaysRetentionRate = last30DaysRetentionRate,
                    reviewsPerDayLast30Days = reviewsPerDayLast30Days,
                    ratingDistribution = ratingDistribution,
                    easeFactorDistribution = easeFactorDistribution,
                    forecastNext14Days = forecastNext14Days,
                    streakHistoryLast30Days = streakHistoryLast30Days,
                    currentStreak = _currentStreak.value,
                    longestStreak = _longestStreak.value,
                    leechWordsCount = currentLeechCount
                )
            } catch (e: Exception) {
                android.util.Log.e("DictionaryViewModel", "Error refreshing stats", e)
            }
        }
    }

    // Progress reset methods
    fun resetWordProgress(word: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetWordProgress(word)
            val today = java.time.LocalDate.now().toEpochDay()
            // Immediately fetch fresh bookmarked words from database to avoid stale state in active session
            val freshBookmarked = repository.getBookmarkedWordsList()
            _reviewQueue.value = freshBookmarked.filter { it.dueDate <= today }
            refreshStats()
        }
    }

    fun resetAllWordsProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetAllWordsProgress()
            repository.clearReviewLogs()
            // Reset streak to 0 as all review history is being cleared
            val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putInt("current_streak", 0)
                .putLong("last_study_date", -1L)
                .apply()
            _currentStreak.value = 0
            _reviewQueue.value = emptyList()
            refreshStats()
        }
    }

    /**
     * Computes Anki-style next-interval preview labels (e.g. "1m", "10m", "1d", "4d")
     * for Again / Hard / Good / Easy using the currently active FSRS or SM-2 scheduler.
     */
    fun previewReviewIntervals(word: WordEntity): Map<String, String> {
        val ratings = listOf("Again", "Hard", "Good", "Easy")
        if (_srsAlgorithm.value.equals("fsrs", ignoreCase = true)) {
            return try {
                val retentionFraction = (_targetRetention.value.toDouble() / 100.0).coerceIn(0.80, 0.97)
                val scheduler = Scheduler.builder()
                    .desiredRetention(retentionFraction)
                    .build()
                val baseCard = if (word.fsrsStability != null && word.fsrsDifficulty != null) {
                    val builder = Card.builder()
                        .stability(word.fsrsStability)
                        .difficulty(word.fsrsDifficulty)
                    word.fsrsDue?.let { builder.due(java.time.Instant.ofEpochMilli(it)) }
                    word.fsrsState?.let { st -> runCatching { State.valueOf(st) }.getOrNull()?.let { builder.state(it) } }
                    word.fsrsStep?.let { builder.step(it) }
                    word.fsrsLastReview?.let { builder.lastReview(java.time.Instant.ofEpochMilli(it)) }
                    builder.build()
                } else {
                    Card.builder().build()
                }
                val now = java.time.Instant.now()
                ratings.associateWith { label ->
                    val fsrsRating = when (label) {
                        "Again" -> Rating.AGAIN
                        "Hard" -> Rating.HARD
                        "Good" -> Rating.GOOD
                        else -> Rating.EASY
                    }
                    val due = scheduler.reviewCard(baseCard, fsrsRating, now).card().due ?: now
                    val minutes = maxOf(1L, java.time.Duration.between(now, due).toMinutes())
                    formatIntervalMinutes(minutes)
                }
            } catch (_: Exception) {
                mapOf("Again" to "1m", "Hard" to "6m", "Good" to "10m", "Easy" to "4d")
            }
        } else {
            val sEase = _startingEase.value
            val eBonus = _easyBonus.value
            val iMod = _intervalModifier.value
            val hMult = _hardIntervalMultiplier.value
            val ease = if (word.repetitions == 0) sEase else word.easeFactor
            val hardDays = if (word.repetitions == 0) 1 else maxOf(1, kotlin.math.round(word.interval * hMult * iMod).toInt())
            val goodDays = when (word.repetitions) {
                0 -> 1
                1 -> maxOf(1, kotlin.math.round(6 * iMod).toInt())
                else -> maxOf(1, kotlin.math.round(word.interval * ease * iMod).toInt())
            }
            val easyDays = maxOf(goodDays + 1, kotlin.math.round(goodDays * eBonus).toInt())
            return mapOf(
                "Again" to "1m",
                "Hard" to formatIntervalMinutes(hardDays * 1440L),
                "Good" to formatIntervalMinutes(goodDays * 1440L),
                "Easy" to formatIntervalMinutes(easyDays * 1440L)
            )
        }
    }

    private fun formatIntervalMinutes(minutes: Long): String {
        return when {
            minutes < 60L -> "${minutes}m"
            minutes < 1440L -> "${minutes / 60L}h"
            minutes < 43200L -> "${(minutes + 720L) / 1440L}d"
            minutes < 525600L -> "${String.format(Locale.US, "%.1f", minutes / 43200.0).removeSuffix(".0")}mo"
            else -> "${String.format(Locale.US, "%.1f", minutes / 525600.0).removeSuffix(".0")}y"
        }
    }

    fun rateCard(word: WordEntity, rating: String) {
        if (_isRatingInProgress.value) return
        _isRatingInProgress.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = java.time.LocalDate.now().toEpochDay()
                
                // Advanced SRS Settings multipliers
                val eBonus = _easyBonus.value
                val sEase = _startingEase.value
                val iModifier = _intervalModifier.value
                val hMultiplier = _hardIntervalMultiplier.value
                
                val currentEase = if (word.repetitions == 0) sEase else word.easeFactor

                // Lapse counting: Increment lapse count when rated "Again" after leaving "new" state (repetitions > 0)
                val hadRepetitions = word.repetitions > 0
                val isAgain = rating.equals("Again", ignoreCase = true)
                val newLapses = if (isAgain && hadRepetitions) {
                    word.lapses + 1
                } else {
                    word.lapses
                }
                val threshold = _leechThreshold.value
                val becameLeech = (newLapses >= threshold) && !word.isLeech
                val newIsLeech = word.isLeech || becameLeech

                val hadNewSuccessDay = !isAgain && (word.lastSuccessfulReviewDay == null || word.lastSuccessfulReviewDay != today)
                val newDistinctDays = if (hadNewSuccessDay) word.distinctReviewDays + 1 else word.distinctReviewDays
                val newLastSuccessfulReviewDay = if (!isAgain) today else word.lastSuccessfulReviewDay

                if (becameLeech) {
                    showSnackbar("'${word.word}' is now marked as a Leech — consider a new mnemonic or example for it")
                }

                val updatedCard = if (_srsAlgorithm.value.equals("fsrs", ignoreCase = true)) {
                    // Official FSRS Library Scheduling
                    val fsrsRating = when (rating) {
                        "Again" -> Rating.AGAIN
                        "Hard" -> Rating.HARD
                        "Good" -> Rating.GOOD
                        "Easy" -> Rating.EASY
                        else -> Rating.GOOD
                    }

                    val retentionFraction = (_targetRetention.value.toDouble() / 100.0).coerceIn(0.80, 0.97)
                    val scheduler = Scheduler.builder()
                        .desiredRetention(retentionFraction)
                        .build()

                    // If word has no FSRS state yet, initialize it as a new card using Card.builder().build()
                    val currentFsrsCard = if (word.fsrsStability != null && word.fsrsDifficulty != null) {
                        val builder = Card.builder()
                            .stability(word.fsrsStability)
                            .difficulty(word.fsrsDifficulty)
                        if (word.fsrsDue != null) {
                            builder.due(java.time.Instant.ofEpochMilli(word.fsrsDue))
                        }
                        if (word.fsrsState != null) {
                            val parsedState = runCatching { State.valueOf(word.fsrsState) }.getOrNull()
                            if (parsedState != null) {
                                builder.state(parsedState)
                            }
                        }
                        if (word.fsrsStep != null) {
                            builder.step(word.fsrsStep)
                        }
                        if (word.fsrsLastReview != null) {
                            builder.lastReview(java.time.Instant.ofEpochMilli(word.fsrsLastReview))
                        }
                        builder.build()
                    } else {
                        Card.builder().build()
                    }

                    val nowInstant = java.time.Instant.now()
                    val cardAndReviewLog = scheduler.reviewCard(currentFsrsCard, fsrsRating, nowInstant)
                    val updatedFsrs = cardAndReviewLog.card()

                    val dueInstant = updatedFsrs.due ?: nowInstant
                    val calculatedDueDay = dueInstant.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
                    val finalDueDay = maxOf(today, calculatedDueDay)
                    val finalInterval = maxOf(0, (finalDueDay - today).toInt())

                    word.copy(
                        repetitions = if (isAgain) 0 else word.repetitions + 1,
                        interval = finalInterval,
                        lapses = newLapses,
                        isLeech = newIsLeech,
                        distinctReviewDays = newDistinctDays,
                        lastSuccessfulReviewDay = newLastSuccessfulReviewDay,
                        dueDate = finalDueDay,
                        lastReviewed = today,
                        fsrsStability = updatedFsrs.stability,
                        fsrsDifficulty = updatedFsrs.difficulty,
                        fsrsDue = dueInstant.toEpochMilli(),
                        fsrsState = updatedFsrs.state?.name,
                        fsrsStep = updatedFsrs.step,
                        fsrsLastReview = updatedFsrs.lastReview?.toEpochMilli() ?: nowInstant.toEpochMilli()
                    )
                } else {
                    // SM-2 (Classic): existing logic, completely unchanged
                    val intervalMultiplier = when (rating) {
                        "Hard" -> hMultiplier
                        "Easy" -> currentEase * eBonus
                        else -> currentEase
                    }
                    when (rating) {
                        "Again" -> {
                            word.copy(
                                repetitions = 0,
                                interval = 0,
                                lapses = newLapses,
                                isLeech = newIsLeech,
                                distinctReviewDays = newDistinctDays,
                                lastSuccessfulReviewDay = newLastSuccessfulReviewDay,
                                dueDate = today,
                                lastReviewed = today
                            )
                        }
                        else -> { // Hard, Good, Easy
                            val calculatedInterval = if (word.repetitions < 2) {
                                val base = if (word.repetitions == 0) 1f else 6f
                                val mult = when (rating) {
                                    "Hard" -> hMultiplier
                                    "Easy" -> eBonus
                                    else -> 1.0f
                                }
                                Math.round(base * mult).toInt()
                            } else {
                                Math.round(word.interval * intervalMultiplier).toInt()
                            }
                            
                            val finalInterval = maxOf(1, Math.round(calculatedInterval * iModifier).toInt())

                            val newEaseFactor = when (rating) {
                                "Hard" -> maxOf(1.3f, currentEase - 0.15f)
                                "Good" -> currentEase
                                "Easy" -> currentEase + 0.15f
                                else -> currentEase
                            }
                            word.copy(
                                repetitions = word.repetitions + 1,
                                interval = finalInterval,
                                easeFactor = newEaseFactor,
                                lapses = newLapses,
                                isLeech = newIsLeech,
                                distinctReviewDays = newDistinctDays,
                                lastSuccessfulReviewDay = newLastSuccessfulReviewDay,
                                dueDate = today + finalInterval,
                                lastReviewed = today
                            )
                        }
                    }
                }
                
                // Persist to database
                repository.updateWordReview(updatedCard)
                
                // Log review to history
                repository.logReview(word.word, rating)
                
                // Update Streak
                val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val lastStudy = sharedPrefs.getLong("last_study_date", -1L)
                if (lastStudy != today) {
                    val curStreak = if (lastStudy == today - 1) {
                        sharedPrefs.getInt("current_streak", 0) + 1
                    } else {
                        1
                    }
                    val longStreak = maxOf(sharedPrefs.getInt("longest_streak", 0), curStreak)
                    
                    sharedPrefs.edit()
                        .putLong("last_study_date", today)
                        .putInt("current_streak", curStreak)
                        .putInt("longest_streak", longStreak)
                        .apply()
                        
                    _currentStreak.value = curStreak
                    _longestStreak.value = longStreak
                }
                
                // Refresh stats
                refreshStats()

                // Update review queue
                val currentQueue = _reviewQueue.value.toMutableList()
                if (currentQueue.isNotEmpty() && currentQueue[0].word == word.word) {
                    currentQueue.removeAt(0)
                    if (rating == "Again") {
                        // Re-appears at the end of the session
                        currentQueue.add(updatedCard)
                    }
                    _reviewQueue.value = currentQueue
                }
            } finally {
                _isRatingInProgress.value = false
            }
        }
    }

    fun getReviewLogsForWord(word: String): Flow<List<ReviewLogEntity>> =
        repository.getReviewLogsForWord(word)

    // TTS Setup
    private var tts: TextToSpeech? = TextToSpeech(application, this)
    private var isTtsReady = false

    // Media player, AudioTrack & Gemini TTS audio cache
    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var currentTempAudioFile: File? = null
    private val ttsAudioCache = Collections.synchronizedMap(mutableMapOf<String, ByteArray?>())

    private val _lastPronunciationLog = MutableStateFlow<String>("No pronunciation request logged yet.")
    val lastPronunciationLog: StateFlow<String> = _lastPronunciationLog.asStateFlow()

    // Daily Reading States
    private val _dailyReadingUiState = MutableStateFlow<DailyReadingUiState>(DailyReadingUiState.Idle)
    val dailyReadingUiState: StateFlow<DailyReadingUiState> = _dailyReadingUiState.asStateFlow()

    // Preloaded vocabulary words to seed quizzes if history is empty
    private val preloadedVocabulary = listOf(
        WordEntity(
            word = "aesthetic",
            phonetic = "/esˈθet.ɪk/",
            partOfSpeech = "adjective",
            definition = "Concerned with beauty or the appreciation of beauty",
            synonyms = "artistic, beautiful, gorgeous, stylish",
            antonyms = "ugly, unappealing",
            example = "The pictures give great aesthetic pleasure.",
            mnemonic = "Aesthetic starts with 'AES', think of Artistic Eye Styling.",
            imagePrompt = "A minimalist art gallery with soft lighting showcasing a single elegant marble sculpture"
        ),
        WordEntity(
            word = "benevolent",
            phonetic = "/bəˈnev.əl.ənt/",
            partOfSpeech = "adjective",
            definition = "Well-meaning and kindly; serving a charitable purpose",
            synonyms = "kind, generous, charitable, compassionate",
            antonyms = "malevolent, cruel",
            example = "A benevolent old gentleman donated money to the school.",
            mnemonic = "Benevolent: 'Bene' means good, 'Vol' means wish, literally wishing good to others.",
            imagePrompt = "Warm hands cradling a glowing sprout, scattering golden seeds under gentle rain"
        ),
        WordEntity(
            word = "diligent",
            phonetic = "/ˈdɪl.ɪ.dʒənt/",
            partOfSpeech = "adjective",
            definition = "Having or showing care and conscientiousness in one's work or duties",
            synonyms = "industrious, hard-working, assiduous, careful",
            antonyms = "lazy, idle, negligent",
            example = "Many hours of diligent research went into writing this report.",
            mnemonic = "Diligent: Think of 'Dilli' agent, who works day and night with full attention.",
            imagePrompt = "A scholar in a cozy wooden library late at night meticulously studying ancient scrolls"
        ),
        WordEntity(
            word = "ephemeral",
            phonetic = "/ɪˈfem.ər.əl/",
            partOfSpeech = "adjective",
            definition = "Lasting for a very short time",
            synonyms = "transitory, brief, transient, fleeting, short-lived",
            antonyms = "permanent, eternal, lasting",
            example = "Dandelions have ephemeral bright yellow blossoms.",
            mnemonic = "Ephemeral: sounds like 'e-femoral', like a temporary lapse in time.",
            imagePrompt = "A pristine dewdrop on a cherry blossom petal about to evaporate in the golden morning sun"
        ),
        WordEntity(
            word = "fortuitous",
            phonetic = "/fɔːˈtʃuː.ɪ.təs/",
            partOfSpeech = "adjective",
            definition = "Happening by accident or chance rather than design; lucky",
            synonyms = "accidental, coincidental, unexpected, lucky, fortunate",
            antonyms = "planned, intentional, deliberate",
            example = "Their meeting was purely fortuitous.",
            mnemonic = "Fortuitous: sounds like 'fortune', happening lucky or fortuitously by chance.",
            imagePrompt = "An unexpected four-leaf clover growing vibrant green between cold concrete cracks"
        ),
        WordEntity(
            word = "resilient",
            phonetic = "/rɪˈzɪl.jənt/",
            partOfSpeech = "adjective",
            definition = "Able to withstand or recover quickly from difficult conditions",
            synonyms = "strong, tough, flexible, hardy, adaptable",
            antonyms = "fragile, sensitive, weak",
            example = "Baby plants are remarkably resilient to changes in light.",
            mnemonic = "Resilient: 'Re-silent', even after noise or trauma, they quietly bounce back.",
            imagePrompt = "A single green sprout pushing victoriously through heavy gravel under a stormy sky"
        ),
        WordEntity(
            word = "pragmatic",
            phonetic = "/præɡˈmæt.ɪk/",
            partOfSpeech = "adjective",
            definition = "Dealing with things sensibly and realistically based on practical conditions",
            synonyms = "practical, realistic, sensible, logical",
            antonyms = "idealistic, impractical",
            example = "We need a pragmatic approach to solve this equation.",
            mnemonic = "Pragmatic: think of 'practical automatic' decision making.",
            imagePrompt = "A precise draft planner laying out sturdy stone foundations instead of castles in the air"
        ),
        WordEntity(
            word = "serendipity",
            phonetic = "/ˌser.ənˈdɪp.ə.ti/",
            partOfSpeech = "noun",
            definition = "The occurrence of events by chance in a happy or beneficial way",
            synonyms = "luck, coincidence, fluke, providence",
            antonyms = "misfortune, bad luck",
            example = "Finding her old wedding ring in the garden was pure serendipity.",
            mnemonic = "Serendipity: think of 'serene dip', dipping into a stream and finding a precious pearl by chance.",
            imagePrompt = "A hand catching a falling shooting star on a dark starry night with a bright crescent moon"
        )
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        suggestionsJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _searchSuggestions.value = emptyList()
            return
        }
        suggestionsJob = viewModelScope.launch {
            delay(120)
            _searchSuggestions.value = repository.searchOfflineSuggestions(trimmed, limit = 8)
        }
    }

    fun updateTtsSettings() {
        val engine = tts ?: return
        if (!isTtsReady) return

        val accent = _selectedAccent.value
        val targetLocale = if (accent.equals("UK", ignoreCase = true)) Locale.UK else Locale.US
        try {
            val langResult = engine.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.US)
            }
        } catch (_: Exception) {
            // Safe fallback in test / restricted environments
        }

        try {
            val allVoices = engine.voices
            val rankedOptions = TtsVoiceSelector.rankOfflineEnglishVoices(allVoices, accent)
            _availableOfflineVoices.value = rankedOptions

            val preferredName = _selectedTtsVoiceName.value
            val matchedVoice = if (preferredName.isNotBlank()) {
                allVoices?.firstOrNull { it.name == preferredName }
            } else {
                val topOption = rankedOptions.firstOrNull()
                if (topOption != null) allVoices?.firstOrNull { it.name == topOption.name } else null
            }

            if (matchedVoice != null) {
                engine.voice = matchedVoice
                val matchedOption = rankedOptions.firstOrNull { it.name == matchedVoice.name }
                _activeOfflineVoiceLabel.value = matchedOption?.displayName
                    ?: TtsVoiceSelector.formatFriendlyVoiceName(
                        matchedVoice.name,
                        matchedVoice.locale?.country ?: accent,
                        !matchedVoice.isNetworkConnectionRequired
                    )
            } else {
                _activeOfflineVoiceLabel.value = if (accent.equals("UK", ignoreCase = true)) {
                    "System UK English (Install Google UK Offline Pack)"
                } else {
                    "System US English (Install Google US Offline Pack)"
                }
            }
        } catch (_: Exception) {
            _activeOfflineVoiceLabel.value = "System $accent English"
        }

        val rate = if (_speechSpeed.value == "Slow") 0.72f else 0.96f
        engine.setSpeechRate(rate)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            updateTtsSettings()
        }
    }

    private fun speakWithTts(word: String) {
        setLastPronunciationSource("Device TTS")
        if (isTtsReady) {
            updateTtsSettings()
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun showPronunciationUnavailableToast(word: String, failureReason: String? = null) {
        val rawReason = failureReason ?: _lastPronunciationLog.value
        val message = if (!rawReason.isNullOrBlank() && rawReason.startsWith("Voice failed:")) {
            rawReason
        } else if (!rawReason.isNullOrBlank() && rawReason.startsWith("Voice failed")) {
            rawReason
        } else if (!rawReason.isNullOrBlank() && !rawReason.startsWith("No pronunciation") && !rawReason.startsWith("Playback:")) {
            "Voice failed: $rawReason"
        } else {
            "Voice failed for \"$word\""
        }
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(
                getApplication(),
                message,
                Toast.LENGTH_LONG
            ).show()
        }
        showSnackbar(message)
    }

    companion object {
        val GEMINI_TTS_MODELS = listOf(
            "gemini-3.1-flash-tts-preview",
            "gemini-2.5-flash-preview-tts",
            "gemini-2.5-pro-preview-tts"
        )
        private const val TAG_TTS = "GeminiTTS"
    }

    var apiKeyOverride: String? = null

    fun isInvalidApiKey(key: String?): Boolean {
        if (key.isNullOrBlank()) return true
        val trimmed = key.trim()
        return trimmed.startsWith("MY_", ignoreCase = true) ||
                trimmed.equals("MY_GEMINI_TTS_API_KEY", ignoreCase = true) ||
                trimmed.equals("MY_GEMINI_API_KEY", ignoreCase = true)
    }

    private fun effectiveTtsKeysAndActiveIndex(): Pair<List<String>, Int> {
        if (_useSeparateTtsKeys.value) {
            return _geminiApiKeys.value to _activeKeyIndex.value
        }
        val meaningKeys = _geminiMeaningApiKeys.value
        if (meaningKeys.any { !isInvalidApiKey(it) }) {
            return meaningKeys to _geminiMeaningActiveKeyIndex.value
        }
        return _geminiApiKeys.value to _activeKeyIndex.value
    }

    fun getGeminiTtsApiKeyForIndex(index: Int): String {
        val override = apiKeyOverride
        if (!override.isNullOrBlank()) {
            return override.trim()
        }
        val (keys, _) = effectiveTtsKeysAndActiveIndex()
        if (index in keys.indices) {
            val candidate = keys[index]
            if (!isInvalidApiKey(candidate)) {
                return candidate.trim()
            }
        }
        // If not customized by user in preferences (e.g. index 0), check BuildConfig as default
        if (index == 0) {
            val ttsKey = try {
                BuildConfig.GEMINI_TTS_API_KEY
            } catch (e: Throwable) {
                ""
            }
            if (!isInvalidApiKey(ttsKey)) {
                return ttsKey.trim()
            }
            val mainKey = try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                ""
            }
            if (!isInvalidApiKey(mainKey)) {
                return mainKey.trim()
            }
        }
        return ""
    }

    fun getGeminiTtsApiKey(): String {
        val override = apiKeyOverride
        if (!override.isNullOrBlank()) {
            return override.trim()
        }
        val activeIdx = _activeKeyIndex.value
        return getGeminiTtsApiKeyForIndex(activeIdx)
    }

    fun clearTtsAudioCache() {
        ttsAudioCache.clear()
    }

    /**
     * Executes the ordered list of Gemini TTS models (GEMINI_TTS_MODELS) for a specific API key.
     * Returns Pair(audioBytes, encountered429).
     */
    private suspend fun executeModelFallbackLoop(
        cleanWord: String,
        apiKey: String,
        requestJsonString: String,
        mediaType: okhttp3.MediaType,
        cacheKey: String
    ): Pair<ByteArray?, Boolean> {
        var encountered429 = false

        for (modelName in GEMINI_TTS_MODELS) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"
            Log.d(TAG_TTS, "Attempting Gemini TTS with model '$modelName' for word '$cleanWord' (endpoint: $url)...")

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .post(requestJsonString.toRequestBody(mediaType))
                    .build()

                ttsHttpClient.newCall(request).execute().use { resp ->
                    val statusCode = resp.code
                    val responseBody = resp.body?.string() ?: ""
                    val preview = responseBody.take(200).replace("\n", " ").trim()

                    val cleanErrorMessage = try {
                        val json = JSONObject(responseBody)
                        val errorObj = json.optJSONObject("error")
                        val rawMsg = errorObj?.optString("message")?.takeIf { it.isNotBlank() }
                        if (rawMsg != null) "HTTP $statusCode $rawMsg" else "HTTP $statusCode $preview"
                    } catch (ignored: Exception) {
                        "HTTP $statusCode $preview"
                    }

                    if (statusCode == 429) {
                        encountered429 = true
                        Log.w(TAG_TTS, "Gemini TTS model '$modelName' failed: HTTP 429 - $preview")
                        _lastPronunciationLog.value = "Voice failed: $cleanErrorMessage"
                        return@use
                    }

                    if (statusCode == 503) {
                        Log.w(TAG_TTS, "Gemini TTS model '$modelName' failed: HTTP $statusCode - $preview")
                        _lastPronunciationLog.value = "Voice failed: $cleanErrorMessage"
                        return@use
                    }

                    if (!resp.isSuccessful) {
                        Log.w(TAG_TTS, "Gemini TTS model '$modelName' failed: HTTP $statusCode - $preview")
                        _lastPronunciationLog.value = "Voice failed: $cleanErrorMessage"
                        return@use
                    }

                    try {
                        val json = JSONObject(responseBody)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null) {
                                for (i in 0 until parts.length()) {
                                    val part = parts.getJSONObject(i)
                                    val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                                    val base64Data = inlineData?.optString("data")
                                    if (!base64Data.isNullOrBlank()) {
                                        val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                        if (audioBytes != null && audioBytes.isNotEmpty()) {
                                            Log.d(TAG_TTS, "Successfully fetched ${audioBytes.size} audio bytes from model '$modelName' for '$cleanWord'")
                                            _lastPronunciationLog.value = "Success: Model '$modelName' returned ${audioBytes.size} bytes PCM audio"
                                            ttsAudioCache[cacheKey] = audioBytes
                                            return Pair(audioBytes, false)
                                        }
                                    }
                                }
                            }
                        }
                        Log.w(TAG_TTS, "Gemini TTS model '$modelName' failed: HTTP 200 - no audio data in response")
                        _lastPronunciationLog.value = "Voice failed: HTTP 200 model returned no audio"
                    } catch (parseEx: Exception) {
                        Log.w(TAG_TTS, "Gemini TTS model '$modelName' failed: JSON parse error - ${parseEx.message}")
                        _lastPronunciationLog.value = "Voice failed: JSON parse error"
                    }
                }
            } catch (networkEx: Exception) {
                val errorMsg = networkEx.message ?: "network error"
                val preview = errorMsg.take(200)
                Log.w(TAG_TTS, "Gemini TTS model '$modelName' failed: $preview", networkEx)
                _lastPronunciationLog.value = "Voice failed: $preview"
            }
        }

        return Pair(null, encountered429)
    }

    /**
     * Tries a list of Gemini TTS models in order, using the active Gemini API key.
     * Stops at the first one that succeeds, returning audio bytes.
     * If a model returns 429 or 503, immediately moves to the next model without delay.
     * If the active key fails with HTTP 429 across models, automatically retries once using
     * the other configured Gemini API key before giving up.
     * Respects the global 30-second throttle on real network requests.
     * Returns null if all attempts fail.
     */
    suspend fun fetchGeminiTtsAudio(word: String): ByteArray? = withContext(Dispatchers.IO) {
        val cleanWord = word.trim()
        if (cleanWord.isEmpty()) return@withContext null

        val cacheKey = cleanWord.lowercase(Locale.ROOT)
        synchronized(ttsAudioCache) {
            if (ttsAudioCache.containsKey(cacheKey)) {
                Log.d(TAG_TTS, "TTS audio cache hit for '$cacheKey'")
                return@withContext ttsAudioCache[cacheKey]
            }
        }

        // Global 30-second throttle on new network calls
        if (isTtsNetworkThrottled()) {
            val elapsed = System.currentTimeMillis() - lastTtsNetworkCallTimestamp
            val remainingSec = ((ttsThrottleCooldownMs - elapsed) / 1000L).coerceAtLeast(1)
            Log.w(TAG_TTS, "Global TTS 30-second throttle active (${remainingSec}s remaining). Skipping Gemini network call for '$cleanWord'.")
            _lastPronunciationLog.value = "Voice throttled: 30s cooldown active (${remainingSec}s left)"
            return@withContext null
        }

        val (keys, activeIdx) = effectiveTtsKeysAndActiveIndex()
        val candidateIndices: List<Int> = if (keys.isEmpty()) {
            listOf(0)
        } else {
            val startIdx = if (activeIdx in keys.indices) activeIdx else 0
            (0 until keys.size).map { (startIdx + it) % keys.size }
        }

        // Check if there are any valid keys configured across candidate indices
        val hasAnyValidKey = candidateIndices.any { idx ->
            val key = getGeminiTtsApiKeyForIndex(idx)
            !isInvalidApiKey(key)
        }
        if (!hasAnyValidKey) {
            val errorMsg = "Voice failed: No valid Gemini API key configured"
            Log.w(TAG_TTS, errorMsg)
            _lastPronunciationLog.value = errorMsg
            return@withContext null
        }

        val requestJsonString = try {
            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", "Say clearly: $cleanWord")
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            val generationConfig = JSONObject()
            val responseModalities = JSONArray().apply { put("AUDIO") }
            generationConfig.put("responseModalities", responseModalities)

            val speechConfig = JSONObject()
            val voiceConfig = JSONObject()
            val prebuiltVoiceConfig = JSONObject().apply {
                put("voiceName", "Kore")
            }
            voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
            speechConfig.put("voiceConfig", voiceConfig)
            generationConfig.put("speechConfig", speechConfig)

            root.put("generationConfig", generationConfig)
            root.toString()
        } catch (e: Exception) {
            Log.e(TAG_TTS, "Failed to build Gemini TTS request JSON for '$cleanWord'", e)
            _lastPronunciationLog.value = "Voice failed: JSON build error"
            return@withContext null
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()

        // Track timestamp of actual network call
        lastTtsNetworkCallTimestamp = System.currentTimeMillis()

        // Failover loop: try every key in _geminiApiKeys in list order starting from the active key
        // and wrapping around, stopping at first success or once all keys tried.
        for (idx in candidateIndices) {
            val candidateKey = getGeminiTtsApiKeyForIndex(idx)
            if (isInvalidApiKey(candidateKey)) {
                continue
            }
            val displayKeyNumber = idx + 1

            Log.d(TAG_TTS, "Attempting TTS with Gemini Key $displayKeyNumber for word '$cleanWord'...")

            val (audioBytes, encountered429) = executeModelFallbackLoop(
                cleanWord = cleanWord,
                apiKey = candidateKey,
                requestJsonString = requestJsonString,
                mediaType = mediaType,
                cacheKey = cacheKey
            )

            if (audioBytes != null && audioBytes.isNotEmpty()) {
                Log.d(TAG_TTS, "Gemini TTS succeeded using Key $displayKeyNumber for '$cleanWord'")
                setLastPronunciationSource("Gemini Key $displayKeyNumber")
                return@withContext audioBytes
            }

            if (encountered429) {
                Log.w(TAG_TTS, "Gemini Key $displayKeyNumber hit HTTP 429. Retrying with next available key for '$cleanWord'...")
                _lastPronunciationLog.value = "Key $displayKeyNumber rate limited (429), retrying..."
            } else {
                // If it failed due to other error (non-429), stop attempting further keys
                break
            }
        }

        Log.w(TAG_TTS, "All Gemini TTS models/keys failed for word '$cleanWord'. Reason: ${_lastPronunciationLog.value}")
        null
    }

    fun pcmToWav(pcmData: ByteArray, sampleRate: Int = 24000, channels: Short = 1, bitsPerSample: Short = 16): ByteArray {
        if (pcmData.size >= 4) {
            val isRiff = pcmData[0] == 'R'.code.toByte() && pcmData[1] == 'I'.code.toByte() &&
                    pcmData[2] == 'F'.code.toByte() && pcmData[3] == 'F'.code.toByte()
            val isId3 = pcmData[0] == 'I'.code.toByte() && pcmData[1] == 'D'.code.toByte() &&
                    pcmData[2] == '3'.code.toByte()
            val isMp3Sync = (pcmData[0].toInt() and 0xFF) == 0xFF && (pcmData[1].toInt() and 0xE0) == 0xE0
            if (isRiff || isId3 || isMp3Sync) {
                return pcmData
            }
        }

        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val wavData = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavData, 0, 44)
        System.arraycopy(pcmData, 0, wavData, 44, pcmData.size)
        return wavData
    }

    private fun playViaAudioTrack(pcmBytes: ByteArray, onPlaybackFailed: () -> Unit) {
        try {
            stopAndReleasePlayer()
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(24000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmBytes.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack = track
            track.write(pcmBytes, 0, pcmBytes.size)
            track.play()
            Log.d(TAG_TTS, "AudioTrack playing ${pcmBytes.size} PCM bytes successfully.")
            _lastPronunciationLog.value = "Playback: Playing via AudioTrack (24kHz 16-bit PCM, ${pcmBytes.size} bytes)"
        } catch (e: Exception) {
            Log.e(TAG_TTS, "AudioTrack playback failed", e)
            _lastPronunciationLog.value = "Playback error: AudioTrack failed: ${e.message}"
            onPlaybackFailed()
        }
    }

    private fun playAudioBytes(audioBytes: ByteArray, onPlaybackFailed: () -> Unit) {
        if (audioBytes.isEmpty()) {
            Log.e(TAG_TTS, "Cannot play empty audio byte array")
            _lastPronunciationLog.value = "Playback error: audioBytes is empty"
            onPlaybackFailed()
            return
        }

        stopAndReleasePlayer()

        try {
            if (isTtsReady) {
                tts?.stop()
            }
        } catch (ignored: Exception) {}

        try {
            // Convert raw PCM to a valid WAV container if needed so MediaPlayer Stagefright can parse it
            val wavAudio = pcmToWav(audioBytes)
            val tempFile = File.createTempFile("gemini_tts_", ".wav", getApplication<Application>().cacheDir)
            tempFile.deleteOnExit()
            tempFile.writeBytes(wavAudio)
            currentTempAudioFile = tempFile

            val player = MediaPlayer()
            mediaPlayer = player

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            Log.d(TAG_TTS, "Setting DataSource to temp audio file: ${tempFile.absolutePath} (${wavAudio.size} bytes)")
            player.setDataSource(tempFile.absolutePath)

            player.setOnPreparedListener { mp ->
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        val speed = if (_speechSpeed.value == "Slow") 0.8f else 1.0f
                        mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    }
                    Log.d(TAG_TTS, "MediaPlayer prepared successfully. Starting Gemini TTS playback.")
                    _lastPronunciationLog.value = "Playback: Playing via MediaPlayer (${wavAudio.size} bytes WAV)"
                    mp.start()
                } catch (e: Exception) {
                    Log.e(TAG_TTS, "Error starting MediaPlayer playback for Gemini TTS", e)
                    releasePlayer(mp)
                    // Fall back to AudioTrack
                    playViaAudioTrack(audioBytes, onPlaybackFailed)
                }
            }
            player.setOnCompletionListener { mp ->
                Log.d(TAG_TTS, "MediaPlayer playback completed for Gemini TTS")
                releasePlayer(mp)
            }
            player.setOnErrorListener { mp, what, extra ->
                Log.e(TAG_TTS, "MediaPlayer playback error: what=$what, extra=$extra for Gemini TTS. Trying AudioTrack fallback.")
                releasePlayer(mp)
                // Fall back to AudioTrack directly with raw PCM
                playViaAudioTrack(audioBytes, onPlaybackFailed)
                true
            }
            Log.d(TAG_TTS, "Calling prepareAsync() on MediaPlayer for Gemini TTS audio")
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG_TTS, "Failed to initialize or prepare MediaPlayer for Gemini TTS audio. Falling back to AudioTrack.", e)
            stopAndReleasePlayer()
            playViaAudioTrack(audioBytes, onPlaybackFailed)
        }
    }

    private fun releasePlayer(player: MediaPlayer) {
        try {
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (ignored: Exception) {}
            try {
                player.reset()
            } catch (ignored: Exception) {}
            try {
                player.release()
            } catch (ignored: Exception) {}
        } catch (ignored: Exception) {}

        if (mediaPlayer === player) {
            mediaPlayer = null
        }
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
        try {
            currentTempAudioFile?.delete()
        } catch (ignored: Exception) {}
        currentTempAudioFile = null
    }

    private fun stopAndReleasePlayer() {
        mediaPlayer?.let { releasePlayer(it) }
        mediaPlayer = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
        try {
            currentTempAudioFile?.delete()
        } catch (ignored: Exception) {}
        currentTempAudioFile = null
    }

    fun getPronunciationAudioFile(word: String): File {
        val cleanWord = word.trim().lowercase(Locale.ROOT)
        val sanitized = cleanWord.replace(Regex("[^a-z0-9_-]"), "_")
        val dir = File(getApplication<Application>().filesDir, "pronunciations")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "$sanitized.wav")
    }

    suspend fun saveAudioFileForWord(word: String, audioBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            if (audioBytes.isEmpty()) return@withContext null
            val wavBytes = pcmToWav(audioBytes)
            val file = getPronunciationAudioFile(word)
            file.parentFile?.mkdirs()
            file.writeBytes(wavBytes)
            val path = file.absolutePath
            repository.updateAudioPath(word, path)
            Log.d(TAG_TTS, "Saved permanent pronunciation audio for '$word' at $path (${wavBytes.size} bytes)")
            path
        } catch (e: Exception) {
            Log.e(TAG_TTS, "Failed to save audio file for '$word'", e)
            null
        }
    }

    private fun deleteAudioFileForWord(wordEntity: WordEntity) {
        try {
            val path = wordEntity.audioPath
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
            val defaultFile = getPronunciationAudioFile(wordEntity.word)
            if (defaultFile.exists()) {
                defaultFile.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG_TTS, "Error deleting audio file for word '${wordEntity.word}'", e)
        }
    }

    private fun saveAudioForBookmarkedWordInBackground(wordEntity: WordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentWord = repository.getLocalWord(wordEntity.word) ?: wordEntity
                val existingPath = currentWord.audioPath
                if (!existingPath.isNullOrBlank()) {
                    val file = File(existingPath)
                    if (file.exists() && file.isFile && file.length() > 44) {
                        return@launch
                    }
                }

                val cleanWord = wordEntity.word.trim().lowercase(Locale.ROOT)
                val cachedBytes = synchronized(ttsAudioCache) { ttsAudioCache[cleanWord] }
                val audioBytes = if (cachedBytes != null && cachedBytes.isNotEmpty()) {
                    cachedBytes
                } else {
                    fetchGeminiTtsAudio(wordEntity.word)
                }

                if (audioBytes != null && audioBytes.isNotEmpty()) {
                    val savedPath = saveAudioFileForWord(wordEntity.word, audioBytes)
                    if (savedPath != null) {
                        val curState = _searchUiState.value
                        if (curState is SearchUiState.Success && curState.word.word.equals(wordEntity.word, ignoreCase = true)) {
                            _searchUiState.value = SearchUiState.Success(curState.word.copy(audioPath = savedPath))
                        }
                    }
                }
            } catch (e: Exception) {
                // Background generation failure must not affect the user bookmark
                Log.w(TAG_TTS, "Background audio saving for bookmark failed for '${wordEntity.word}'", e)
            }
        }
    }

    private suspend fun checkAndSaveIfBookmarked(word: String, audioBytes: ByteArray) {
        try {
            val localWord = repository.getLocalWord(word)
            if (localWord != null && localWord.isBookmarked) {
                val currentPath = localWord.audioPath
                if (currentPath.isNullOrBlank() || !File(currentPath).exists() || File(currentPath).length() <= 44) {
                    saveAudioFileForWord(word, audioBytes)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG_TTS, "Failed to check or save audio for bookmarked word '$word'", e)
        }
    }

    private fun playSavedAudioFile(file: File, onPlaybackFailed: () -> Unit): Boolean {
        stopAndReleasePlayer()

        try {
            if (isTtsReady) {
                tts?.stop()
            }
        } catch (ignored: Exception) {}

        return try {
            val player = MediaPlayer()
            mediaPlayer = player

            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setDataSource(file.absolutePath)

            player.setOnPreparedListener { mp ->
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        val speed = if (_speechSpeed.value == "Slow") 0.8f else 1.0f
                        mp.playbackParams = mp.playbackParams.setSpeed(speed)
                    }
                    Log.d(TAG_TTS, "MediaPlayer prepared successfully for saved audio file: ${file.name}")
                    _lastPronunciationLog.value = "Playback: Playing saved file (${file.length()} bytes)"
                    mp.start()
                } catch (e: Exception) {
                    Log.e(TAG_TTS, "Error starting MediaPlayer playback for saved audio file", e)
                    releasePlayer(mp)
                    onPlaybackFailed()
                }
            }
            player.setOnCompletionListener { mp ->
                Log.d(TAG_TTS, "MediaPlayer playback completed for saved audio file")
                releasePlayer(mp)
            }
            player.setOnErrorListener { mp, what, extra ->
                Log.e(TAG_TTS, "MediaPlayer playback error: what=$what, extra=$extra for saved audio file")
                releasePlayer(mp)
                onPlaybackFailed()
                true
            }
            player.prepareAsync()
            true
        } catch (e: Exception) {
            Log.e(TAG_TTS, "Failed to setDataSource or prepare for saved audio file: ${file.absolutePath}", e)
            stopAndReleasePlayer()
            onPlaybackFailed()
            false
        }
    }

    private fun proceedWithStandardSpeak(cleanWord: String) {
        val cacheKey = cleanWord.lowercase(Locale.ROOT)
        val isCached = synchronized(ttsAudioCache) { ttsAudioCache.containsKey(cacheKey) }

        when (_pronunciationSource.value.lowercase(Locale.ROOT)) {
            "device" -> {
                setLastPronunciationSource("Device TTS")
                speakWithTts(cleanWord)
            }
            "online" -> {
                // If not in cache and throttled, do NOT call device TTS.
                // Do not play any audio. Show cooldown message to user.
                if (!isCached && isTtsNetworkThrottled()) {
                    val elapsed = System.currentTimeMillis() - lastTtsNetworkCallTimestamp
                    val remainingSec = ((ttsThrottleCooldownMs - elapsed + 999L) / 1000L).coerceAtLeast(1)
                    val message = if (remainingSec > 1) {
                        "Please wait $remainingSec seconds before pronouncing a new word"
                    } else {
                        "Please wait 1 second before pronouncing a new word"
                    }
                    Log.w(TAG_TTS, "Global TTS 30-second cooldown active (${remainingSec}s remaining). Skipping pronunciation for '$cleanWord' in online mode.")
                    _lastPronunciationLog.value = "TTS cooldown active (${remainingSec}s left) - skipped in online mode"
                    showSnackbar(message)
                    viewModelScope.launch(Dispatchers.Main) {
                        try {
                            Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
                        } catch (ignored: Exception) {}
                    }
                    return
                }

                viewModelScope.launch {
                    val audioBytes = fetchGeminiTtsAudio(cleanWord)
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        checkAndSaveIfBookmarked(cleanWord, audioBytes)
                        playAudioBytes(audioBytes) {
                            showPronunciationUnavailableToast(cleanWord)
                        }
                    } else {
                        showPronunciationUnavailableToast(cleanWord)
                    }
                }
            }
            else -> { // "auto" — Strict Priority: 1) Natural Offline Neural -> 2) Online Dictionary Audio -> 3) AI TTS
                // Priority 1: Natural Offline Neural TTS (instant, 100% offline, UK/US ranked via TtsVoiceSelector)
                if (isTtsReady) {
                    setLastPronunciationSource("Device TTS")
                    _lastPronunciationLog.value = "Playback: Natural Offline Neural TTS (${_activeOfflineVoiceLabel.value})"
                    speakWithTts(cleanWord)
                    return
                }

                // If throttled and TTS isn't initialized yet, fail gracefully to Device TTS status
                if (!isCached && isTtsNetworkThrottled()) {
                    val elapsed = System.currentTimeMillis() - lastTtsNetworkCallTimestamp
                    val remainingSec = ((ttsThrottleCooldownMs - elapsed + 999L) / 1000L).coerceAtLeast(1)
                    Log.w(TAG_TTS, "Global TTS 30-second cooldown active (${remainingSec}s remaining). Falling back to Device TTS for '$cleanWord'.")
                    _lastPronunciationLog.value = "TTS cooldown active (${remainingSec}s left) - fell back to device TTS"
                    setLastPronunciationSource("Device TTS")
                    speakWithTts(cleanWord)
                    return
                }

                viewModelScope.launch {
                    // Priority 2: Online Dictionary Audio (UK/US MP3, cached permanently to filesDir/audio/)
                    val onlineDictBytes = fetchOnlineDictionaryAudioBytes(cleanWord)
                    if (onlineDictBytes != null && onlineDictBytes.isNotEmpty()) {
                        saveAudioPermanentlyForWord(cleanWord, onlineDictBytes)
                        setLastPronunciationSource("Online Dictionary")
                        _lastPronunciationLog.value = "Playback: Online Dictionary Audio (${_selectedAccent.value}) — saved locally"
                        playAudioBytes(onlineDictBytes) {
                            setLastPronunciationSource("Device TTS")
                            speakWithTts(cleanWord)
                        }
                        return@launch
                    }

                    // Priority 3: AI TTS (Gemini, cached permanently)
                    val audioBytes = fetchGeminiTtsAudio(cleanWord)
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        checkAndSaveIfBookmarked(cleanWord, audioBytes)
                        playAudioBytes(audioBytes) {
                            Log.w(TAG_TTS, "Gemini audio playback failed for '$cleanWord', falling back to device TTS.")
                            setLastPronunciationSource("Device TTS")
                            speakWithTts(cleanWord)
                        }
                    } else {
                        Log.w(TAG_TTS, "Online & AI TTS unavailable for '$cleanWord', falling back to device TTS.")
                        setLastPronunciationSource("Device TTS")
                        speakWithTts(cleanWord)
                    }
                }
            }
        }
    }

    fun speak(word: String) {
        val cleanWord = word.trim()
        if (cleanWord.isEmpty()) return

        viewModelScope.launch {
            // First look up the word in the database
            val localWord = repository.getLocalWord(cleanWord)
            val savedPath = localWord?.audioPath

            if (!savedPath.isNullOrBlank()) {
                val file = File(savedPath)
                if (file.exists() && file.isFile && file.length() > 44) {
                    val played = playSavedAudioFile(file) {
                        // File missing or corrupted during playback: clear audioPath and fall back to current logic
                        viewModelScope.launch {
                            repository.updateAudioPath(cleanWord, null)
                            proceedWithStandardSpeak(cleanWord)
                        }
                    }
                    if (played) {
                        return@launch
                    }
                } else {
                    // Saved file is missing or corrupted
                    Log.w(TAG_TTS, "Saved audio file missing or corrupted for '$cleanWord', clearing audioPath.")
                    repository.updateAudioPath(cleanWord, null)
                }
            }

            // Only if there is no saved file (or missing/corrupted), continue with current logic
            proceedWithStandardSpeak(cleanWord)
        }
    }

    fun performSearch(word: String) {
        val cleanWord = word.trim()
        if (cleanWord.isEmpty()) return

        suggestionsJob?.cancel()
        _searchSuggestions.value = emptyList()
        _searchQuery.value = cleanWord
        _searchUiState.value = SearchUiState.Loading

        viewModelScope.launch {
            val localCached = repository.getLocalWord(cleanWord)
            val isOfflineAvailable = localCached != null || repository.hasOfflineCurriculumOrDictWord(cleanWord)
            if (!isOfflineAvailable && isWordApiThrottled()) {
                val remainingSec = getWordApiCooldownRemainingSec()
                _searchUiState.value = SearchUiState.Error(
                    "Cooldown active — please wait ${remainingSec}s before the next AI word request."
                )
                return@launch
            }
            if (!isOfflineAvailable) {
                lastWordApiNetworkCallTimestamp = System.currentTimeMillis()
            }

            val geminiKeys = getOrderedGeminiMeaningKeys()
            val geminiModel = getEffectiveGeminiMeaningModel()
            val openRouterKey = _openRouterApiKey.value.trim()
            val pexelsKey = _pexelsApiKey.value.trim()

            val result = repository.searchAndFetchWord(
                word = cleanWord,
                aiVisualizationsEnabled = _aiVisualizationsEnabled.value,
                imageSource = "web",
                openRouterApiKey = openRouterKey,
                pexelsQuerySource = _pexelsQuerySource.value,
                geminiApiKeys = geminiKeys,
                geminiModel = geminiModel,
                pexelsApiKey = pexelsKey
            )
            refreshUsageStats()
            result.onSuccess { wordEntity ->
                if (wordEntity.arabicMeaning.isNotBlank()) {
                    val key = wordEntity.word.trim().lowercase()
                    _wordArabicTranslations.value = _wordArabicTranslations.value + (key to wordEntity.arabicMeaning)
                }
                _searchUiState.value = SearchUiState.Success(wordEntity)
            }.onFailure { exception ->
                _searchUiState.value = SearchUiState.Error(exception.message ?: "Failed to find word details.")
            }
        }
    }

    fun selectHistoryWord(wordEntity: WordEntity) {
        _searchQuery.value = wordEntity.word
        _searchUiState.value = SearchUiState.Success(wordEntity)
        viewModelScope.launch {
            // Update timestamp
            val updated = wordEntity.copy(timestamp = System.currentTimeMillis())
            val geminiKeys = getOrderedGeminiMeaningKeys()
            val geminiModel = getEffectiveGeminiMeaningModel()
            val openRouterKey = _openRouterApiKey.value.trim()
            val pexelsKey = _pexelsApiKey.value.trim()

            repository.searchAndFetchWord(
                word = updated.word,
                aiVisualizationsEnabled = _aiVisualizationsEnabled.value,
                imageSource = "web",
                openRouterApiKey = openRouterKey,
                pexelsQuerySource = _pexelsQuerySource.value,
                geminiApiKeys = geminiKeys,
                geminiModel = geminiModel,
                pexelsApiKey = pexelsKey
            )
            refreshUsageStats()
        }
    }

    fun toggleBookmark(wordEntity: WordEntity) {
        viewModelScope.launch {
            val newBookmarkStatus = !wordEntity.isBookmarked
            repository.toggleBookmark(wordEntity.word, newBookmarkStatus)
            
            // If the currently viewed success word is this word, update it
            val currentState = _searchUiState.value
            if (currentState is SearchUiState.Success && currentState.word.word == wordEntity.word) {
                _searchUiState.value = SearchUiState.Success(wordEntity.copy(isBookmarked = newBookmarkStatus))
            }

            if (!newBookmarkStatus) {
                _reviewQueue.value = _reviewQueue.value.filter { it.word.lowercase() != wordEntity.word.lowercase() }
            }

            if (newBookmarkStatus) {
                saveAudioForBookmarkedWordInBackground(wordEntity)
            }
            refreshStats()
        }
    }

    fun deleteFromHistory(wordEntity: WordEntity) {
        viewModelScope.launch {
            deleteAudioFileForWord(wordEntity)
            repository.deleteWord(wordEntity)
            // If the deleted word is current success state, return to idle
            val currentState = _searchUiState.value
            if (currentState is SearchUiState.Success && currentState.word.word == wordEntity.word) {
                _searchUiState.value = SearchUiState.Idle
            }
            _reviewQueue.value = _reviewQueue.value.filter { it.word.lowercase() != wordEntity.word.lowercase() }
            refreshStats()
        }
    }

    fun clearSearchState() {
        suggestionsJob?.cancel()
        _searchSuggestions.value = emptyList()
        _searchUiState.value = SearchUiState.Idle
        _searchQuery.value = ""
    }

    fun clearBookmarks() {
        viewModelScope.launch {
            repository.clearBookmarks()
            _reviewQueue.value = emptyList()
            refreshStats()
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toEpochDay()
            val samples = listOf(
                WordEntity(
                    word = "benevolent",
                    phonetic = "/bəˈnev.əl.ənt/",
                    partOfSpeech = "adjective",
                    definition = "Well-meaning and kindly; serving a charitable purpose",
                    synonyms = "kind, generous, charitable, compassionate",
                    antonyms = "malevolent, cruel",
                    example = "A benevolent old gentleman donated money to the school.",
                    mnemonic = "Benevolent: 'Bene' means good, 'Vol' means wish, wishing good to others.",
                    imagePrompt = "Warm hands cradling a glowing sprout, scattering golden seeds under gentle rain",
                    isBookmarked = true,
                    timestamp = System.currentTimeMillis(),
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    dueDate = today,
                    lastReviewed = null
                ),
                WordEntity(
                    word = "ephemeral",
                    phonetic = "/ɪˈfem.ər.əl/",
                    partOfSpeech = "adjective",
                    definition = "Lasting for a very short time",
                    synonyms = "transitory, brief, transient, fleeting, short-lived",
                    antonyms = "permanent, eternal, lasting",
                    example = "Dandelions have ephemeral bright yellow blossoms.",
                    mnemonic = "Ephemeral: sounds like 'e-femoral', like a temporary lapse in time.",
                    imagePrompt = "A pristine dewdrop on a cherry blossom petal about to evaporate in the golden morning sun",
                    isBookmarked = true,
                    timestamp = System.currentTimeMillis(),
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    dueDate = today,
                    lastReviewed = null
                ),
                WordEntity(
                    word = "resilient",
                    phonetic = "/rɪˈzɪl.jənt/",
                    partOfSpeech = "adjective",
                    definition = "Able to withstand or recover quickly from difficult conditions",
                    synonyms = "strong, tough, flexible, hardy, adaptable",
                    antonyms = "fragile, sensitive, weak",
                    example = "Baby plants are remarkably resilient to changes in light.",
                    mnemonic = "Resilient: 'Re-silent', even after noise or trauma, they quietly bounce back.",
                    imagePrompt = "A single green sprout pushing victoriously through heavy gravel under a stormy sky",
                    isBookmarked = true,
                    timestamp = System.currentTimeMillis(),
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    dueDate = today,
                    lastReviewed = null
                ),
                WordEntity(
                    word = "ambiguous",
                    phonetic = "/æmˈbɪɡ.ju.əs/",
                    partOfSpeech = "adjective",
                    definition = "Open to more than one interpretation; not having one obvious meaning",
                    synonyms = "equivocal, obscure, vague, unclear, puzzling",
                    antonyms = "clear, obvious, unambiguous",
                    example = "The election result was ambiguous, leading to weeks of debate.",
                    mnemonic = "Ambiguous: 'Ambi' means both, so it can go both ways and be unclear.",
                    imagePrompt = "A misty crossroad under a twilight sky with a signpost pointing in two foggy directions",
                    isBookmarked = true,
                    timestamp = System.currentTimeMillis(),
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    dueDate = today,
                    lastReviewed = null
                ),
                WordEntity(
                    word = "meticulous",
                    phonetic = "/məˈtɪk.jə.ləs/",
                    partOfSpeech = "adjective",
                    definition = "Showing great attention to detail; very careful and precise",
                    synonyms = "careful, conscientious, diligent, scrupulous, detailed",
                    antonyms = "careless, sloppy, slapdash",
                    example = "The researcher kept meticulous records of the temperature changes.",
                    mnemonic = "Meticulous: 'Metic-u-less', think of making things perfect with 'met-it-clues' down to the last clue.",
                    imagePrompt = "A watchmaker with magnifying glasses working on intricate golden gears in a vintage workshop",
                    isBookmarked = true,
                    timestamp = System.currentTimeMillis(),
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    dueDate = today,
                    lastReviewed = null
                ),
                WordEntity(
                    word = "candid",
                    phonetic = "/ˈkæn.dɪd/",
                    partOfSpeech = "adjective",
                    definition = "Truthful and straightforward; frank",
                    synonyms = "frank, honest, sincere, straightforward, open",
                    antonyms = "insincere, guarded, secretive",
                    example = "His candid response surprised the committee but was deeply appreciated.",
                    mnemonic = "Candid: think of a camera 'candid' shot, showing people exactly as they really are.",
                    imagePrompt = "A laughing girl captured in an unposed photo with soft natural sunlight filtered through green leaves",
                    isBookmarked = true,
                    timestamp = System.currentTimeMillis(),
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    dueDate = today,
                    lastReviewed = null
                ),
                WordEntity(
                    word = "tenacious",
                    phonetic = "/təˈneɪ.ʃəs/",
                    partOfSpeech = "adjective",
                    definition = "Tending to keep a firm hold of something; clinging or adhering closely",
                    synonyms = "persistent, determined, resolute, stubborn, strong-willed",
                    antonyms = "irresolute, weak, yielding",
                    example = "She is a tenacious advocate for human rights, never giving up on her clients.",
                    mnemonic = "Tenacious: 'Ten-asia-us', think of ten people holding onto a rope with absolute determination.",
                    imagePrompt = "Vines growing tenaciously and climbing up a steep, weathered stone wall into bright sunlight",
                    isBookmarked = true,
                    timestamp = System.currentTimeMillis(),
                    easeFactor = 2.5f,
                    interval = 0,
                    repetitions = 0,
                    dueDate = today,
                    lastReviewed = null
                )
            )
            for (word in samples) {
                repository.insertWordIfNotExist(word)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                val dir = File(getApplication<Application>().filesDir, "pronunciations")
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        try { file.delete() } catch (ignored: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG_TTS, "Error cleaning up pronunciation audio files during clearHistory", e)
            }
            repository.clearHistory()
            _reviewQueue.value = emptyList()
            refreshStats()
        }
    }

    // --- Daily Reading Logic ---
    fun getReadingLevel(): Int {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPrefs.getInt("reading_level", 1)
    }

    fun setReadingLevel(level: Int) {
        val clamped = level.coerceIn(1, 5)
        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("reading_level", clamped).apply()
        _readingLevel.value = clamped
    }

    fun getCefrLabel(level: Int): String {
        return when (level) {
            1, 2 -> "A2"
            3 -> "A2-B1 transitional"
            4, 5 -> "B1"
            else -> "A2"
        }
    }

    fun loadDailyReadingIfNeeded(forceRefresh: Boolean = false) {
        if (_dailyReadingUiState.value is DailyReadingUiState.Loading) return

        val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toEpochDay()
        val savedDate = sharedPrefs.getLong("daily_reading_date", -1L)
        val cachedContent = sharedPrefs.getString("daily_reading_content", null)

        if (!forceRefresh && savedDate == today && !cachedContent.isNullOrBlank()) {
            val currentState = _dailyReadingUiState.value
            if (currentState is DailyReadingUiState.Active || currentState is DailyReadingUiState.Finished) {
                return
            }
            try {
                val response = repository.getReadingJsonAdapter().fromJson(cachedContent)
                if (response != null && response.passage.isNotBlank() && response.questions.isNotEmpty()) {
                    val level = getReadingLevel()
                    _dailyReadingUiState.value = DailyReadingUiState.Active(
                        passage = response.passage,
                        questions = response.questions,
                        currentQuestionIndex = 0,
                        readingLevel = level,
                        cefrLevel = getCefrLabel(level),
                        grammarQuestions = response.grammarQuestions
                    )
                    return
                }
            } catch (e: Exception) {
                // Parse failed, will regenerate below
            }
        }

        generateDailyReading()
    }

    fun generateDailyReading() {
        _dailyReadingUiState.value = DailyReadingUiState.Loading
        viewModelScope.launch {
            val level = getReadingLevel()
            val targetWords = repository.getSavedWordsForReading()
            val geminiKeys = getOrderedGeminiMeaningKeys()
            val geminiModel = getEffectiveGeminiMeaningModel()
            val openRouterKey = _openRouterApiKey.value.trim()

            val result = repository.generateDailyReading(
                readingLevel = level,
                targetWords = targetWords,
                openRouterApiKey = openRouterKey,
                geminiApiKeys = geminiKeys,
                geminiModel = geminiModel
            )
            refreshUsageStats()
            result.onSuccess { response ->
                val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val today = java.time.LocalDate.now().toEpochDay()
                val jsonString = repository.getReadingJsonAdapter().toJson(response)
                sharedPrefs.edit()
                    .putString("daily_reading_content", jsonString)
                    .putLong("daily_reading_date", today)
                    .apply()

                _dailyReadingUiState.value = DailyReadingUiState.Active(
                    passage = response.passage,
                    questions = response.questions,
                    currentQuestionIndex = 0,
                    readingLevel = level,
                    cefrLevel = getCefrLabel(level),
                    grammarQuestions = response.grammarQuestions
                )
            }.onFailure { ex ->
                _dailyReadingUiState.value = DailyReadingUiState.Error(
                    ex.message ?: "Failed to generate daily reading passage. Please check your connection and try again."
                )
            }
        }
    }

    fun selectReadingOption(option: String) {
        val currentState = _dailyReadingUiState.value
        if (currentState is DailyReadingUiState.Active && currentState.currentPhase == ReadingPhase.READING && currentState.selectedOption == null) {
            val currentQuestion = currentState.questions.getOrNull(currentState.currentQuestionIndex) ?: return
            val isCorrect = option.trim().equals(currentQuestion.correctAnswer.trim(), ignoreCase = true)
            val newScore = if (isCorrect) currentState.score + 1 else currentState.score

            _dailyReadingUiState.value = currentState.copy(
                selectedOption = option,
                isCorrect = isCorrect,
                score = newScore
            )
        }
    }

    fun nextReadingQuestion() {
        val currentState = _dailyReadingUiState.value
        if (currentState is DailyReadingUiState.Active && currentState.currentPhase == ReadingPhase.READING) {
            val nextIndex = currentState.currentQuestionIndex + 1
            if (nextIndex < currentState.questions.size) {
                _dailyReadingUiState.value = currentState.copy(
                    currentQuestionIndex = nextIndex,
                    selectedOption = null,
                    isCorrect = null
                )
            } else {
                val score = currentState.score
                val total = currentState.questions.size
                val oldLevel = getReadingLevel()
                val ratio = if (total > 0) score.toFloat() / total.toFloat() else 0f
                val currentOutcome = when {
                    ratio >= 0.8f -> "HIGH"
                    ratio <= 0.4f -> "LOW"
                    else -> "NEUTRAL"
                }

                viewModelScope.launch {
                    val sharedPrefs = getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    val storedPrevOutcome = sharedPrefs.getString("prev_reading_outcome", null)
                    val prevOutcome = if (storedPrevOutcome != null) {
                        storedPrevOutcome
                    } else {
                        val lastSession = repository.getLastReadingSession()
                        if (lastSession != null && lastSession.total > 0) {
                            val lastRatio = lastSession.score.toFloat() / lastSession.total.toFloat()
                            when {
                                lastRatio >= 0.8f -> "HIGH"
                                lastRatio <= 0.4f -> "LOW"
                                else -> "NEUTRAL"
                            }
                        } else null
                    }

                    val newLevel = when {
                        prevOutcome == "HIGH" && currentOutcome == "HIGH" -> (oldLevel + 1).coerceAtMost(5)
                        prevOutcome == "LOW" && currentOutcome == "LOW" -> (oldLevel - 1).coerceAtLeast(1)
                        else -> oldLevel
                    }

                    if (newLevel != oldLevel) {
                        setReadingLevel(newLevel)
                        sharedPrefs.edit().remove("prev_reading_outcome").apply()
                    } else {
                        sharedPrefs.edit().putString("prev_reading_outcome", currentOutcome).apply()
                    }

                    val levelChange = when {
                        newLevel > oldLevel -> LevelChange.INCREASED
                        newLevel < oldLevel -> LevelChange.DECREASED
                        else -> LevelChange.UNCHANGED
                    }

                    repository.insertReadingSession(
                        ReadingSessionEntity(
                            date = java.time.LocalDate.now().toEpochDay(),
                            score = score,
                            total = total,
                            levelBefore = oldLevel,
                            levelAfter = newLevel
                        )
                    )

                    if (currentState.grammarQuestions.isNotEmpty()) {
                        _dailyReadingUiState.value = currentState.copy(
                            currentPhase = ReadingPhase.GRAMMAR_INTRO,
                            readingLevel = newLevel,
                            cefrLevel = getCefrLabel(newLevel),
                            oldReadingLevel = oldLevel,
                            levelChange = levelChange
                        )
                    } else {
                        _dailyReadingUiState.value = DailyReadingUiState.Finished(
                            passage = currentState.passage,
                            questions = currentState.questions,
                            score = score,
                            total = total,
                            grammarQuestions = emptyList(),
                            grammarScore = 0,
                            grammarTotal = 0,
                            oldReadingLevel = oldLevel,
                            newReadingLevel = newLevel,
                            levelChange = levelChange,
                            cefrLevel = getCefrLabel(newLevel)
                        )
                    }
                }
            }
        }
    }

    fun startGrammarCheck() {
        val currentState = _dailyReadingUiState.value
        if (currentState is DailyReadingUiState.Active && currentState.currentPhase == ReadingPhase.GRAMMAR_INTRO) {
            _dailyReadingUiState.value = currentState.copy(
                currentPhase = ReadingPhase.GRAMMAR,
                currentGrammarIndex = 0,
                selectedGrammarOption = null,
                isGrammarCorrect = null,
                grammarScore = 0
            )
        }
    }

    fun selectGrammarOption(option: String) {
        val currentState = _dailyReadingUiState.value
        if (currentState is DailyReadingUiState.Active && currentState.currentPhase == ReadingPhase.GRAMMAR && currentState.selectedGrammarOption == null) {
            val currentQuestion = currentState.grammarQuestions.getOrNull(currentState.currentGrammarIndex) ?: return
            val isCorrect = option.trim().equals(currentQuestion.correctAnswer.trim(), ignoreCase = true)
            val newGrammarScore = if (isCorrect) currentState.grammarScore + 1 else currentState.grammarScore

            _dailyReadingUiState.value = currentState.copy(
                selectedGrammarOption = option,
                isGrammarCorrect = isCorrect,
                grammarScore = newGrammarScore
            )
        }
    }

    fun nextGrammarQuestion() {
        val currentState = _dailyReadingUiState.value
        if (currentState is DailyReadingUiState.Active && currentState.currentPhase == ReadingPhase.GRAMMAR) {
            val nextIndex = currentState.currentGrammarIndex + 1
            if (nextIndex < currentState.grammarQuestions.size) {
                _dailyReadingUiState.value = currentState.copy(
                    currentGrammarIndex = nextIndex,
                    selectedGrammarOption = null,
                    isGrammarCorrect = null
                )
            } else {
                _dailyReadingUiState.value = DailyReadingUiState.Finished(
                    passage = currentState.passage,
                    questions = currentState.questions,
                    score = currentState.score,
                    total = currentState.questions.size,
                    grammarQuestions = currentState.grammarQuestions,
                    grammarScore = currentState.grammarScore,
                    grammarTotal = currentState.grammarQuestions.size,
                    oldReadingLevel = currentState.oldReadingLevel,
                    newReadingLevel = currentState.readingLevel,
                    levelChange = currentState.levelChange,
                    cefrLevel = currentState.cefrLevel
                )
            }
        }
    }

    fun restartCurrentPassageQuestions() {
        val currentState = _dailyReadingUiState.value
        if (currentState is DailyReadingUiState.Finished) {
            val level = getReadingLevel()
            _dailyReadingUiState.value = DailyReadingUiState.Active(
                passage = currentState.passage,
                questions = currentState.questions,
                currentQuestionIndex = 0,
                selectedOption = null,
                isCorrect = null,
                score = 0,
                readingLevel = level,
                cefrLevel = getCefrLabel(level),
                grammarQuestions = currentState.grammarQuestions,
                currentPhase = ReadingPhase.READING,
                currentGrammarIndex = 0,
                selectedGrammarOption = null,
                isGrammarCorrect = null,
                grammarScore = 0,
                oldReadingLevel = level,
                levelChange = LevelChange.UNCHANGED
            )
        }
    }

    init {
        val sharedPrefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        _autoPronounceEnabled.value = sharedPrefs.getBoolean("auto_pronounce", true)
        _pronunciationSource.value = sharedPrefs.getString("pronunciation_source", "auto") ?: "auto"
        _speechSpeed.value = sharedPrefs.getString("speech_speed", "Normal") ?: "Normal"
        _aiVisualizationsEnabled.value = sharedPrefs.getBoolean("ai_visualizations_enabled", true)

        // Load and migrate Gemini TTS API Keys list
        val keysListJson = sharedPrefs.getString("gemini_api_keys_list", null)
        val loadedKeys = mutableListOf<String>()
        if (keysListJson != null) {
            try {
                val jsonArray = JSONArray(keysListJson)
                for (i in 0 until jsonArray.length()) {
                    loadedKeys.add(jsonArray.optString(i, ""))
                }
            } catch (e: Exception) {
                Log.e(TAG_TTS, "Failed to parse gemini_api_keys_list JSON", e)
            }
        } else {
            // MIGRATION: on first load, if "gemini_api_keys_list" does not exist yet but old
            // "gemini_api_key_1" and/or "gemini_api_key_2" preferences are non-blank, migrate them into the new list
            // (in order: key_1 first, then key_2, skipping blanks), save the new list, and leave old entries in place.
            val oldKey1 = sharedPrefs.getString("gemini_api_key_1", "")?.trim() ?: ""
            val oldKey2 = sharedPrefs.getString("gemini_api_key_2", "")?.trim() ?: ""
            if (oldKey1.isNotBlank()) {
                loadedKeys.add(oldKey1)
            }
            if (oldKey2.isNotBlank()) {
                loadedKeys.add(oldKey2)
            }
            if (loadedKeys.isNotEmpty()) {
                val jsonArray = JSONArray()
                for (k in loadedKeys) {
                    jsonArray.put(k)
                }
                sharedPrefs.edit().putString("gemini_api_keys_list", jsonArray.toString()).apply()
            }
        }
        _geminiApiKeys.value = loadedKeys

        // Active key index (0-based)
        var savedActiveIndex = sharedPrefs.getInt("gemini_active_key_index", 0)
        // If migrating from 1-based old preference:
        if (keysListJson == null && sharedPrefs.contains("gemini_active_key_index")) {
            val oldActive = sharedPrefs.getInt("gemini_active_key_index", 1)
            savedActiveIndex = if (oldActive == 2 && loadedKeys.size > 1) 1 else 0
            sharedPrefs.edit().putInt("gemini_active_key_index", savedActiveIndex).apply()
        }
        if (loadedKeys.isNotEmpty()) {
            if (savedActiveIndex !in loadedKeys.indices) {
                savedActiveIndex = 0
                sharedPrefs.edit().putInt("gemini_active_key_index", 0).apply()
            }
        } else {
            savedActiveIndex = 0
        }
        _activeKeyIndex.value = savedActiveIndex

        // Load Gemini Meaning & Reading API Keys
        val meaningKeysListJson = sharedPrefs.getString("gemini_meaning_api_keys_list", null)
        val loadedMeaningKeys = mutableListOf<String>()
        if (meaningKeysListJson != null) {
            try {
                val jsonArray = JSONArray(meaningKeysListJson)
                for (i in 0 until jsonArray.length()) {
                    loadedMeaningKeys.add(jsonArray.optString(i, ""))
                }
            } catch (e: Exception) {
                Log.e("DictionaryViewModel", "Failed to parse gemini_meaning_api_keys_list JSON", e)
            }
        }
        _geminiMeaningApiKeys.value = loadedMeaningKeys

        var savedMeaningActiveIndex = sharedPrefs.getInt("gemini_meaning_active_key_index", 0)
        if (loadedMeaningKeys.isNotEmpty()) {
            if (savedMeaningActiveIndex !in loadedMeaningKeys.indices) {
                savedMeaningActiveIndex = 0
                sharedPrefs.edit().putInt("gemini_meaning_active_key_index", 0).apply()
            }
        } else {
            savedMeaningActiveIndex = 0
        }
        _geminiMeaningActiveKeyIndex.value = savedMeaningActiveIndex

        _geminiMeaningModelChoice.value = sharedPrefs.getString("gemini_meaning_model_choice", "gemini-3.5-flash-lite") ?: "gemini-3.5-flash-lite"
        _geminiMeaningCustomModel.value = sharedPrefs.getString("gemini_meaning_custom_model", "gemini-3.5-flash-lite") ?: "gemini-3.5-flash-lite"
        _openRouterApiKey.value = sharedPrefs.getString("openrouter_api_key", "") ?: ""
        _pexelsApiKey.value = sharedPrefs.getString("pexels_api_key", "") ?: ""
        val savedTtsCooldown = sharedPrefs.getInt("tts_cooldown_seconds", 30).coerceIn(0, 120)
        _ttsCooldownSeconds.value = savedTtsCooldown
        ttsThrottleCooldownMs = savedTtsCooldown * 1000L
        _wordApiCooldownSeconds.value = sharedPrefs.getInt("word_api_cooldown_seconds", 5).coerceIn(0, 120)
        
        _remindersEnabled.value = com.example.reminder.ReminderHelper.areRemindersEnabled(application)
        _alwaysNotifyEnabled.value = com.example.reminder.ReminderHelper.isAlwaysNotifyEnabled(application)
        _reminderHour.value = com.example.reminder.ReminderHelper.getReminderHour(application)
        _reminderMinute.value = com.example.reminder.ReminderHelper.getReminderMinute(application)
        
        _selectedThemeColor.value = sharedPrefs.getString("selected_theme_color", "indigo") ?: "indigo"
        _themeMode.value = sharedPrefs.getString("theme_mode", "system") ?: "system"
        _dynamicColorEnabled.value = sharedPrefs.getBoolean("dynamic_color_enabled", false)
        _arabicTranslationEnabled.value = sharedPrefs.getBoolean("arabic_translation_enabled", true)
        _selectedAccent.value = sharedPrefs.getString("selected_accent", "UK") ?: "UK"
        _selectedTtsVoiceName.value = sharedPrefs.getString("selected_tts_voice_name", "") ?: ""
        _useSeparateTtsKeys.value = if (sharedPrefs.contains("use_separate_tts_keys")) {
            sharedPrefs.getBoolean("use_separate_tts_keys", false)
        } else {
            loadedKeys.any { it.isNotBlank() } && loadedKeys != loadedMeaningKeys
        }
        if (sharedPrefs.getString("image_source", null) != "web") {
            sharedPrefs.edit().putString("image_source", "web").apply()
        }
        _pexelsQuerySource.value = sharedPrefs.getString("pexels_query_source", "word") ?: "word"
        _userEnglishLevel.value = sharedPrefs.getString("user_english_level", "A2") ?: "A2"
        _readingLevel.value = sharedPrefs.getInt("reading_level", 1)

        _easyBonus.value = sharedPrefs.getFloat("srs_easy_bonus", 1.3f)
        _startingEase.value = sharedPrefs.getFloat("srs_starting_ease", 2.5f)
        _intervalModifier.value = sharedPrefs.getFloat("srs_interval_modifier", 1.0f)
        _hardIntervalMultiplier.value = sharedPrefs.getFloat("srs_hard_interval_multiplier", 1.2f)
        _leechThreshold.value = sharedPrefs.getInt("srs_leech_threshold", sharedPrefs.getInt("leech_threshold", 8))
        _srsAlgorithm.value = sharedPrefs.getString("srs_algorithm", "fsrs") ?: "fsrs"
        _targetRetention.value = sharedPrefs.getInt("srs_target_retention", 90).coerceIn(80, 97)

        _currentStreak.value = sharedPrefs.getInt("current_streak", 0)
        _longestStreak.value = sharedPrefs.getInt("longest_streak", 0)
        _dailyGoalTarget.value = sharedPrefs.getInt("daily_goal_target", 10)

        val today = java.time.LocalDate.now().toEpochDay()
        val lastStudy = sharedPrefs.getLong("last_study_date", -1)
        if (lastStudy != -1L && today > lastStudy + 1) {
            _currentStreak.value = 0
            sharedPrefs.edit().putInt("current_streak", 0).apply()
        }

        refreshStats()
        loadOfflineCatalog()

        viewModelScope.launch {
            val isSentencesSeeded = sharedPrefs.getBoolean("is_sentences_seeded_v1", false)
            if (!isSentencesSeeded) {
                val now = System.currentTimeMillis()
                val defaultSentences = listOf(
                    MinedSentenceEntity(
                        sentence = "Let's play it by ear and decide where to eat once we get downtown.",
                        targetWord = "play it by ear",
                        meaning = "To decide how to deal with a situation as it develops, rather than planning ahead.",
                        category = "Idioms",
                        notes = "Super common in everyday plans with friends or coworkers.",
                        isFavorite = true,
                        timestamp = now + 1
                    ),
                    MinedSentenceEntity(
                        sentence = "I completely lost track of time while catching up with my old friend.",
                        targetWord = "catching up",
                        meaning = "Talking to someone you haven't seen in a while to find out what they have been doing.",
                        category = "Casual",
                        notes = "Great for coffee chats and casual greetings.",
                        isFavorite = true,
                        timestamp = now + 2
                    ),
                    MinedSentenceEntity(
                        sentence = "Could you keep me in the loop if the client changes the deadline?",
                        targetWord = "keep me in the loop",
                        meaning = "To keep someone informed and updated about developments in a project or situation.",
                        category = "Workplace",
                        notes = "Essential phrase for daily emails, Slack messages, and meetings.",
                        isFavorite = true,
                        timestamp = now + 3
                    ),
                    MinedSentenceEntity(
                        sentence = "I'm a bit swamped right now, can we touch base after lunch?",
                        targetWord = "swamped",
                        meaning = "Extremely busy with too much work to do.",
                        category = "Workplace",
                        notes = "Natural alternative to saying 'I am very busy'.",
                        isFavorite = false,
                        timestamp = now + 4
                    ),
                    MinedSentenceEntity(
                        sentence = "Could I get the iced latte to go, and let's split the check please?",
                        targetWord = "split the check",
                        meaning = "To divide the restaurant bill equally or separately among the people eating.",
                        category = "Dining",
                        notes = "Used daily at cafes and restaurants in the US/UK.",
                        isFavorite = true,
                        timestamp = now + 5
                    ),
                    MinedSentenceEntity(
                        sentence = "Honestly, that new thriller movie totally lived up to the hype.",
                        targetWord = "lived up to the hype",
                        meaning = "Was as good and exciting as everyone said it would be.",
                        category = "Casual",
                        notes = "Used when reviewing movies, restaurants, or tech products.",
                        isFavorite = false,
                        timestamp = now + 6
                    ),
                    MinedSentenceEntity(
                        sentence = "We should probably hit the road before rush hour traffic gets crazy.",
                        targetWord = "hit the road",
                        meaning = "To leave a place and begin a journey or trip.",
                        category = "Travel",
                        notes = "Casual everyday expression when leaving home or a party.",
                        isFavorite = false,
                        timestamp = now + 7
                    ),
                    MinedSentenceEntity(
                        sentence = "Let's wrap things up for today and pick this up again tomorrow morning.",
                        targetWord = "wrap things up",
                        meaning = "To finish a meeting, task, or workday.",
                        category = "Workplace",
                        notes = "Polite way to end a call or meeting.",
                        isFavorite = true,
                        timestamp = now + 8
                    ),
                    MinedSentenceEntity(
                        sentence = "It's on the house today since you waited so long for your table.",
                        targetWord = "on the house",
                        meaning = "Provided free of charge by the restaurant or cafe.",
                        category = "Dining",
                        notes = "Common hospitality phrase.",
                        isFavorite = false,
                        timestamp = now + 9
                    ),
                    MinedSentenceEntity(
                        sentence = "Don't sweat it, everyone makes mistakes on their first week.",
                        targetWord = "Don't sweat it",
                        meaning = "Don't worry about it; it's not a big problem.",
                        category = "Casual",
                        notes = "Friendly, reassuring response when someone apologizes.",
                        isFavorite = true,
                        timestamp = now + 10
                    ),
                    MinedSentenceEntity(
                        sentence = "Do you mind keeping an eye on my luggage while I grab a coffee?",
                        targetWord = "keeping an eye on",
                        meaning = "Watching or taking care of something temporarily so it stays safe.",
                        category = "Travel",
                        notes = "Very practical at airports, train stations, and cafes.",
                        isFavorite = false,
                        timestamp = now + 11
                    ),
                    MinedSentenceEntity(
                        sentence = "That makes total sense—we're finally on the same page.",
                        targetWord = "on the same page",
                        meaning = "In agreement and understanding a situation in the same way.",
                        category = "Idioms",
                        notes = "Used in both casual conversations and workplace discussions.",
                        isFavorite = true,
                        timestamp = now + 12
                    )
                )
                val existingList = repository.getAllMinedSentencesList()
                if (existingList.isEmpty()) {
                    repository.insertMinedSentences(defaultSentences)
                }
                sharedPrefs.edit().putBoolean("is_sentences_seeded_v1", true).apply()
            }

            val isSeeded = sharedPrefs.getBoolean("is_seeded_v2", false)
            if (!isSeeded) {
                val today = java.time.LocalDate.now().toEpochDay()
                val defaults = listOf(
                    WordEntity(
                        word = "home",
                        phonetic = "/hoʊm/ (US)",
                        partOfSpeech = "noun",
                        definition = "The place where one lives permanently, especially as a member of a family or household.",
                        synonyms = "residence, house, dwelling, abode, domicile",
                        antonyms = "office, wilderness, outdoors",
                        example = "After a long trip, they were glad to be back home.",
                        mnemonic = "Home: Think of 'homely', a comfortable place where you always roam back to.",
                        imagePrompt = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600&auto=format&fit=crop",
                        isBookmarked = true,
                        timestamp = System.currentTimeMillis(),
                        easeFactor = 2.5f,
                        interval = 0,
                        repetitions = 0,
                        dueDate = today,
                        lastReviewed = null
                    ),
                    WordEntity(
                        word = "man",
                        phonetic = "/mæn/ (US)",
                        partOfSpeech = "noun",
                        definition = "An adult male human being.",
                        synonyms = "gentleman, male, fellow, guy, adult",
                        antonyms = "woman, boy, child",
                        example = "The man was walking his dog in the park.",
                        mnemonic = "Man: Think of 'mankind', representing strength and responsibility.",
                        imagePrompt = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&auto=format&fit=crop",
                        isBookmarked = true,
                        timestamp = System.currentTimeMillis() + 10,
                        easeFactor = 2.5f,
                        interval = 0,
                        repetitions = 0,
                        dueDate = today,
                        lastReviewed = null
                    ),
                    WordEntity(
                        word = "woman",
                        phonetic = "/ˈwʊm.ən/ (US)",
                        partOfSpeech = "noun",
                        definition = "An adult female human being.",
                        synonyms = "lady, female, adult, dame, lass",
                        antonyms = "man, girl, child",
                        example = "She is a successful woman who runs her own business.",
                        mnemonic = "Woman: Think of 'womb-man', the giver of life and nurturer of humanity.",
                        imagePrompt = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=600&auto=format&fit=crop",
                        isBookmarked = true,
                        timestamp = System.currentTimeMillis() + 20,
                        easeFactor = 2.5f,
                        interval = 0,
                        repetitions = 0,
                        dueDate = today,
                        lastReviewed = null
                    ),
                    WordEntity(
                        word = "dog",
                        phonetic = "/dɔːɡ/ (US)",
                        partOfSpeech = "noun",
                        definition = "A domesticated carnivorous mammal that typically has a long snout, an acute sense of smell, and a barking voice.",
                        synonyms = "canine, hound, pup, pooch, man's best friend",
                        antonyms = "cat, feline",
                        example = "The friendly dog wagged its tail when it saw its owner.",
                        mnemonic = "Dog: Spell it backwards and you get 'God', reflecting their unconditional love and loyalty.",
                        imagePrompt = "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=600&auto=format&fit=crop",
                        isBookmarked = true,
                        timestamp = System.currentTimeMillis() + 30,
                        easeFactor = 2.5f,
                        interval = 0,
                        repetitions = 0,
                        dueDate = today,
                        lastReviewed = null
                    ),
                    WordEntity(
                        word = "door",
                        phonetic = "/dɔːr/ (US)",
                        partOfSpeech = "noun",
                        definition = "A hinged, sliding, or revolving barrier that opens and closes the entrance to a building, room, or vehicle.",
                        synonyms = "portal, gateway, entrance, entry, barrier",
                        antonyms = "wall, window, blockage",
                        example = "Please close the door behind you to keep the cold air out.",
                        mnemonic = "Door: Think of 'do or die', you must go through the door of opportunity to succeed.",
                        imagePrompt = "https://images.unsplash.com/photo-1517644493876-786e69312b6b?w=600&auto=format&fit=crop",
                        isBookmarked = true,
                        timestamp = System.currentTimeMillis() + 40,
                        easeFactor = 2.5f,
                        interval = 0,
                        repetitions = 0,
                        dueDate = today,
                        lastReviewed = null
                    )
                )
                for (word in defaults) {
                    repository.insertWordIfNotExist(word)
                }
                sharedPrefs.edit().putBoolean("is_seeded_v2", true).apply()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAndReleasePlayer()
        tts?.shutdown()
        tts = null
    }
}

class DictionaryViewModelFactory(
    private val application: Application,
    private val repository: WordRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DictionaryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DictionaryViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
