package com.example

import android.app.Application
import android.os.Looper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.api.GenerateContentRequest
import com.example.api.GenerateContentResponse
import com.example.api.GeminiApiService
import com.example.data.AppDatabase
import com.example.data.WordEntity
import com.example.data.WordRepository
import com.example.ui.DictionaryViewModel
import com.example.ui.screens.study.StudySessionModal
import com.example.ui.screens.today.TodayScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class StudyFlowUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val offlineOnlyGemini = object : GeminiApiService {
        override suspend fun generateContent(
            apiKey: String,
            request: GenerateContentRequest
        ): GenerateContentResponse {
            throw IllegalStateException("Offline UI test must not hit network")
        }

        override suspend fun generateContentWithModel(
            model: String,
            apiKey: String,
            request: GenerateContentRequest
        ): GenerateContentResponse {
            throw IllegalStateException("Offline UI test must not hit network")
        }
    }

    @Test
    fun todayScreen_showsColdStartWhenDeckEmpty_andHeroCardWhenDueWordsExist() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = WordRepository(db.wordDao(), offlineOnlyGemini, context = app)
            val vm = DictionaryViewModel(app, repo)
            val today = LocalDate.now().toEpochDay()
            var displayedWords by mutableStateOf<List<WordEntity>>(emptyList())

            composeTestRule.setContent {
                MyApplicationTheme {
                    TodayScreen(
                        viewModel = vm,
                        bookmarkedWords = displayedWords,
                        innerPadding = PaddingValues(0.dp),
                        onOpenCoursesBook = {},
                        onOpenCoursesRoot = {},
                        onOpenQuickSearch = {},
                        onOpenQuickQuiz = {},
                        onOpenProfile = {}
                    )
                }
            }

            // State 1: Empty deck -> Honest Cold-Start Setup Card (zero false-positive checkmarks)
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("today_cold_start_card").assertIsDisplayed()
            composeTestRule.onNodeWithTag("today_start_day_one_button").performScrollTo().assertIsDisplayed()
            composeTestRule.onNodeWithTag("today_all_caught_up_card").assertDoesNotExist()

            // State 2: Deck has due words -> Active Study Session Hero Card + 56dp Start Studying CTA
            composeTestRule.runOnIdle {
                displayedWords = listOf(
                    WordEntity(word = "agree", definition = "To agree", arabicMeaning = "يوافق", isBookmarked = true, dueDate = today),
                    WordEntity(word = "arrive", definition = "To arrive", arabicMeaning = "يصل", isBookmarked = true, dueDate = today)
                )
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("today_hero_session_card").assertIsDisplayed()
            composeTestRule.onNodeWithTag("today_start_study_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("today_cold_start_card").assertDoesNotExist()
        } finally {
            db.close()
        }
    }

    @Test
    fun studySessionModal_flips_rates_advancesWithoutSpoilerFlash_andUndoes() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val today = LocalDate.now().toEpochDay()
            runBlocking {
                db.wordDao().insertWord(
                    WordEntity(
                        word = "agree",
                        phonetic = "/əˈɡriː/",
                        partOfSpeech = "verb",
                        definition = "To have the same opinion as someone else.",
                        arabicMeaning = "يوافق",
                        example = "We all agree that the plan is solid.",
                        isBookmarked = true,
                        dueDate = today
                    )
                )
                db.wordDao().insertWord(
                    WordEntity(
                        word = "arrive",
                        phonetic = "/əˈraɪv/",
                        partOfSpeech = "verb",
                        definition = "To reach a place at the end of a journey.",
                        arabicMeaning = "يصل",
                        example = "The train will arrive at nine.",
                        isBookmarked = true,
                        dueDate = today
                    )
                )
            }

            val repo = WordRepository(db.wordDao(), offlineOnlyGemini, context = app)
            val vm = DictionaryViewModel(app, repo)
            vm.setAutoPronounceEnabled(false)
            vm.startUnifiedStudySession()

            var tries = 0
            while (vm.activeStudySession.value == null && tries < 60) {
                shadowOf(Looper.getMainLooper()).idle()
                Thread.sleep(15)
                tries++
            }

            composeTestRule.setContent {
                MyApplicationTheme {
                    val session by vm.activeStudySession.collectAsStateWithLifecycle()
                    val summary by vm.lastSessionSummary.collectAsStateWithLifecycle()
                    session?.let { active ->
                        StudySessionModal(
                            session = active,
                            summary = summary,
                            viewModel = vm,
                            onClose = { vm.closeStudySession() },
                            onStartQuickQuiz = {}
                        )
                    }
                }
            }

            // Card 1 starts front-only: Show Answer visible, rating buttons hidden
            composeTestRule.onNodeWithTag("study_session_modal").assertIsDisplayed()
            composeTestRule.onNodeWithTag("flip_card_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("rate_good_button").assertDoesNotExist()

            // Flip Card 1 -> Back face & 4 FSRS rating buttons appear
            composeTestRule.onNodeWithTag("flip_card_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("rate_good_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("card_open_ai_helper_button").performScrollTo().assertIsDisplayed()

            // Rate Good -> Advances to Card 2 cleanly with ZERO answer spoiler (Show Answer visible, rating buttons hidden)
            composeTestRule.onNodeWithTag("rate_good_button").performClick()
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                shadowOf(Looper.getMainLooper()).idle()
                (vm.activeStudySession.value?.completedCount ?: 0) == 1
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("flip_card_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("rate_good_button").assertDoesNotExist()

            // Undo -> Restores Card 1 cleanly
            composeTestRule.onNodeWithTag("study_session_undo_button").performClick()
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                shadowOf(Looper.getMainLooper()).idle()
                (vm.activeStudySession.value?.completedCount ?: 1) == 0
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("flip_card_button").assertIsDisplayed()
        } finally {
            db.close()
        }
    }
}
