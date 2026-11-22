package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.api.GeminiApiService
import com.example.data.AppDatabase
import com.example.data.WordRepository
import com.example.reminder.ReminderHelper
import com.example.ui.*
import com.example.ui.navigation.LocalWindowWidthSizeClass
import com.example.ui.navigation.RedeusAppShell
import com.example.ui.screens.library.LibraryTabContent
import com.example.ui.screens.practice.FlashcardsTabContent
import com.example.ui.screens.reading.DailyReadingTabContent
import com.example.ui.screens.review.ReviewTabContent
import com.example.ui.screens.search.SearchTabContent
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingLookupWord = mutableStateOf<String?>(null)

    private fun extractLookupText(intent: Intent?): String? {
        if (intent == null) return null
        val raw = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            Intent.ACTION_SEND -> if (intent.type == "text/plain") intent.getStringExtra(Intent.EXTRA_TEXT) else null
            else -> null
        }
        return raw?.trim()?.takeIf { it.isNotBlank() }?.take(120)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractLookupText(intent)?.let { pendingLookupWord.value = it }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        extractLookupText(intent)?.let { pendingLookupWord.value = it }

        // Initialize Local Repositories and Services
        val database = AppDatabase.getDatabase(applicationContext)
        val apiService = GeminiApiService.create()
        val pexelsApiService = com.example.api.PexelsApiService.create()
        val openRouterApiService = com.example.api.OpenRouterApiService.create()
        val repository = WordRepository(
            wordDao = database.wordDao(),
            apiService = apiService,
            pexelsApiService = pexelsApiService,
            openRouterApiService = openRouterApiService,
            context = applicationContext,
            readingSessionDao = database.readingSessionDao(),
            minedSentenceDao = database.minedSentenceDao()
        )

        // Instantiate ViewModel
        val factory = DictionaryViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[DictionaryViewModel::class.java]

        // Initialize Study Reminders Notifications Channel & schedule
        ReminderHelper.createNotificationChannel(applicationContext)
        if (ReminderHelper.areRemindersEnabled(applicationContext)) {
            ReminderHelper.scheduleDailyReminder(applicationContext)
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeColor by viewModel.selectedThemeColor.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColorEnabled by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(
                LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass
            ) {
                MyApplicationTheme(
                    themeColor = themeColor,
                    darkTheme = darkTheme,
                    dynamicColor = dynamicColorEnabled
                ) {
                    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0: Today, 1: Study, 2: Courses, 3: Dictionary, 4: Profile
                    var showSettings by rememberSaveable { mutableStateOf(false) }
                    var settingsInitialTab by rememberSaveable { mutableStateOf(0) }
                    var studyInitialSubMode by rememberSaveable { mutableStateOf(com.example.ui.screens.study.StudySubMode.HUB) }

                    val incomingLookup by pendingLookupWord
                    LaunchedEffect(incomingLookup) {
                        val wordToSearch = incomingLookup
                        if (!wordToSearch.isNullOrBlank()) {
                            showSettings = false
                            selectedTab = 3
                            viewModel.openWordFromSearch(wordToSearch, pushToBackStack = false)
                            pendingLookupWord.value = null
                        }
                    }

                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
                    val bookmarkedWords by viewModel.bookmarksState.collectAsStateWithLifecycle()
                    val dailyReadingState by viewModel.dailyReadingUiState.collectAsStateWithLifecycle()
                    val todayQueueSummary by viewModel.todayQueueSummary.collectAsStateWithLifecycle()
                    val activeStudySession by viewModel.activeStudySession.collectAsStateWithLifecycle()
                    val lastSessionSummary by viewModel.lastSessionSummary.collectAsStateWithLifecycle()

                    val snackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(Unit) {
                        viewModel.snackbarMessage.collect { message ->
                            snackbarHostState.showSnackbar(message)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showSettings) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { showSettings = false },
                                initialTab = settingsInitialTab
                            )
                        } else if (activeStudySession != null) {
                            com.example.ui.screens.study.StudySessionModal(
                                session = activeStudySession!!,
                                summary = lastSessionSummary,
                                viewModel = viewModel,
                                onClose = { viewModel.closeStudySession() },
                                onStartQuickQuiz = {
                                    studyInitialSubMode = com.example.ui.screens.study.StudySubMode.QUIZ
                                    selectedTab = 1
                                }
                            )
                        } else {
                            RedeusAppShell(
                                selectedTab = selectedTab,
                                onSelectTab = { newTab ->
                                    if (newTab != 1) {
                                        studyInitialSubMode = com.example.ui.screens.study.StudySubMode.HUB
                                    }
                                    selectedTab = newTab
                                },
                                cardsDueTodayCount = todayQueueSummary.totalToday,
                                snackbarHostState = snackbarHostState,
                                onOpenSettings = {
                                    settingsInitialTab = 1
                                    showSettings = true
                                }
                            ) { innerPadding ->
                                when (selectedTab) {
                                    0 -> com.example.ui.screens.today.TodayScreen(
                                        viewModel = viewModel,
                                        bookmarkedWords = bookmarkedWords,
                                        innerPadding = innerPadding,
                                        onOpenCoursesBook = { book ->
                                            viewModel.selectCourseBook(book)
                                            selectedTab = 2
                                        },
                                        onOpenCoursesRoot = {
                                            viewModel.selectCourseBook(null)
                                            selectedTab = 2
                                        },
                                        onOpenQuickSearch = { selectedTab = 3 },
                                        onOpenProfile = { selectedTab = 4 },
                                        onOpenQuickQuiz = {
                                            studyInitialSubMode = com.example.ui.screens.study.StudySubMode.QUIZ
                                            selectedTab = 1
                                        }
                                    )
                                    1 -> com.example.ui.screens.study.StudyHubScreen(
                                        viewModel = viewModel,
                                        bookmarkedWords = bookmarkedWords,
                                        innerPadding = innerPadding,
                                        initialSubMode = studyInitialSubMode,
                                        onNavigateToCourses = { selectedTab = 2 }
                                    )
                                    2 -> Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding)
                                    ) {
                                        com.example.ui.screens.library.CoursesExplorerContent(
                                            viewModel = viewModel,
                                            bookmarkedWords = bookmarkedWords,
                                            onNavigateToSearch = { selectedTab = 3 },
                                            onNavigateToReview = {
                                                viewModel.startUnifiedStudySession()
                                            }
                                        )
                                    }
                                    3 -> SearchTabContent(
                                        viewModel = viewModel,
                                        searchQuery = searchQuery,
                                        uiState = searchUiState,
                                        innerPadding = innerPadding,
                                        onOpenApiSettings = {
                                            settingsInitialTab = 2
                                            showSettings = true
                                        },
                                        onOpenCourses = { selectedTab = 2 },
                                        onOpenReview = { viewModel.startUnifiedStudySession() }
                                    )
                                    4 -> com.example.ui.screens.profile.ProfileScreen(
                                        viewModel = viewModel,
                                        bookmarkedWords = bookmarkedWords,
                                        dailyReadingState = dailyReadingState,
                                        innerPadding = innerPadding,
                                        onOpenSettingsTab = { tabIdx ->
                                            settingsInitialTab = tabIdx
                                            showSettings = true
                                        }
                                    )
                                }
                            }
                        }

                        if (activeStudySession != null) {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .statusBarsPadding()
                                    .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check for study reminders on app open/foreground
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val bookmarked = db.wordDao().getBookmarkedWordsList()
                val today = java.time.LocalDate.now().toEpochDay()
                val dueCount = bookmarked.count { it.dueDate <= today }
                ReminderHelper.checkAndShowAppOpenReminder(applicationContext, dueCount)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to check app-open reminders", e)
            }
        }
    }
}
