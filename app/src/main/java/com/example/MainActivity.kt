package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

            CompositionLocalProvider(
                LocalWindowWidthSizeClass provides windowSizeClass.widthSizeClass
            ) {
                MyApplicationTheme(
                    themeColor = themeColor
                ) {
                    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0: Search, 1: Library, 2: Reading, 3: Cards, 4: Review
                    var showSettings by rememberSaveable { mutableStateOf(false) }

                    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
                    val historyWords by viewModel.historyState.collectAsStateWithLifecycle()
                    val bookmarkedWords by viewModel.bookmarksState.collectAsStateWithLifecycle()
                    val dailyReadingState by viewModel.dailyReadingUiState.collectAsStateWithLifecycle()
                    val cardsDueTodayCount by viewModel.cardsDueTodayCount.collectAsStateWithLifecycle()

                    val snackbarHostState = remember { SnackbarHostState() }

                    LaunchedEffect(Unit) {
                        viewModel.snackbarMessage.collect { message ->
                            snackbarHostState.showSnackbar(message)
                        }
                    }

                    if (showSettings) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { showSettings = false }
                        )
                    } else {
                        RedeusAppShell(
                            selectedTab = selectedTab,
                            onSelectTab = { selectedTab = it },
                            cardsDueTodayCount = cardsDueTodayCount,
                            snackbarHostState = snackbarHostState,
                            onOpenSettings = { showSettings = true }
                        ) { innerPadding ->
                            when (selectedTab) {
                                0 -> SearchTabContent(
                                    viewModel = viewModel,
                                    searchQuery = searchQuery,
                                    uiState = searchUiState,
                                    innerPadding = innerPadding
                                )
                                1 -> LibraryTabContent(
                                    viewModel = viewModel,
                                    historyWords = historyWords,
                                    bookmarkedWords = bookmarkedWords,
                                    innerPadding = innerPadding,
                                    onNavigateToSearch = { selectedTab = 0 }
                                )
                                2 -> DailyReadingTabContent(
                                    viewModel = viewModel,
                                    readingState = dailyReadingState,
                                    innerPadding = innerPadding
                                )
                                3 -> FlashcardsTabContent(
                                    viewModel = viewModel,
                                    bookmarkedWords = bookmarkedWords,
                                    innerPadding = innerPadding
                                )
                                4 -> ReviewTabContent(
                                    viewModel = viewModel,
                                    innerPadding = innerPadding
                                )
                            }
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
