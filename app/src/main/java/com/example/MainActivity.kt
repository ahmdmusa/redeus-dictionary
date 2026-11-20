package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.api.GeminiApiService
import com.example.data.AppDatabase
import com.example.data.WordRepository
import com.example.ui.*
import com.example.ui.theme.*
import com.example.reminder.ReminderHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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
            val themeColor by viewModel.selectedThemeColor.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeColor = themeColor
            ) {
                var selectedTab by rememberSaveable { mutableStateOf(0) } // 0: Search, 1: Library, 2: Quiz, 3: Flashcards, 4: Review
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
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .testTag("app_snackbar_host")
                            ) { data ->
                                Box(
                                    modifier = Modifier
                                        .background(NeoBlack, shape = RoundedCornerShape(8.dp))
                                        .border(2.dp, NeoYellow, shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = data.visuals.message,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = NeoWhite
                                    )
                                }
                            }
                        },
                        topBar = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .statusBarsPadding()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(com.example.ui.theme.NeoYellow, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                                .border(2.dp, com.example.ui.theme.NeoBlack, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                                                contentDescription = "Redeus Logo",
                                                tint = com.example.ui.theme.NeoBlack,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "REDEUS DICTIONARY",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "SMART VOCABULARY DECK",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp,
                                                color = com.example.ui.theme.NeoRed
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(com.example.ui.theme.NeoWhite, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                            .border(2.dp, com.example.ui.theme.NeoBlack, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                            .clickable { showSettings = true }
                                            .testTag("settings_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = com.example.ui.theme.NeoBlack,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.5.dp)
                                        .background(com.example.ui.theme.NeoBlack)
                                )
                            }
                        },
                        bottomBar = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .navigationBarsPadding()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.5.dp)
                                        .background(com.example.ui.theme.NeoBlack)
                                )
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.height(72.dp)
                                ) {
                                    // 0: Search, 1: Library, 2: Reading, 3: Flashcards, 4: Review
                                    val navItems = listOf(
                                        Triple("SEARCH", Icons.Filled.Search to Icons.Outlined.Search, "Word Lookup"),
                                        Triple("LIBRARY", Icons.Filled.Bookmark to Icons.Outlined.BookmarkBorder, "Personal Deck"),
                                        Triple("READING", Icons.Filled.MenuBook to Icons.Outlined.MenuBook, "Daily Reading"),
                                        Triple("CARDS", Icons.Filled.Style to Icons.Outlined.Style, "Flashcards"),
                                        Triple("REVIEW", Icons.Filled.RateReview to Icons.Outlined.RateReview, "Smart Review")
                                    )

                                    navItems.forEachIndexed { index, (label, icons, contentDesc) ->
                                        val isSelected = selectedTab == index
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = { selectedTab = index },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = com.example.ui.theme.NeoBlack,
                                                unselectedIconColor = com.example.ui.theme.NeoBlack.copy(alpha = 0.7f),
                                                selectedTextColor = com.example.ui.theme.NeoBlack,
                                                unselectedTextColor = com.example.ui.theme.NeoBlack.copy(alpha = 0.7f),
                                                indicatorColor = com.example.ui.theme.NeoYellow
                                            ),
                                            icon = {
                                                Box {
                                                    Icon(
                                                        imageVector = if (isSelected) icons.first else icons.second,
                                                        contentDescription = contentDesc,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    if (index == 4 && cardsDueTodayCount > 0) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .offset(x = 10.dp, y = (-6).dp)
                                                                .background(com.example.ui.theme.NeoRed, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                                                .border(1.dp, com.example.ui.theme.NeoBlack, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = cardsDueTodayCount.toString(),
                                                                color = com.example.ui.theme.NeoWhite,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp)
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
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
