package com.example.ui.screens.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WordEntity
import com.example.domain.StudyQueuePlanner
import com.example.ui.DictionaryViewModel
import com.example.ui.FlashcardStudyMode
import com.example.ui.components.*
import com.example.ui.screens.practice.FlashcardsTabContent
import com.example.ui.screens.practice.QuizPracticeView
import com.example.ui.screens.practice.SpellingPracticeView
import java.time.LocalDate

enum class StudySubMode { HUB, QUIZ, SPELLING, FREE_BROWSE }

/**
 * Tab 1: Unified Study Hub (merges former "Review" + "Cards" tabs cleanly).
 * - Deck filter chips with per-deck due counts
 * - FSRS Spaced Repetition session launcher
 * - Extra Practice: Leeches Drill, Multiple Choice Quiz (optional FSRS), Spelling, Free Browse
 */
@Composable
fun StudyHubScreen(
    viewModel: DictionaryViewModel,
    bookmarkedWords: List<WordEntity>,
    innerPadding: PaddingValues,
    initialSubMode: StudySubMode = StudySubMode.HUB,
    onNavigateToCourses: () -> Unit
) {
    var subMode by rememberSaveable(initialSubMode) { mutableStateOf(initialSubMode) }
    val selectedDeck by viewModel.selectedReviewDeck.collectAsStateWithLifecycle()
    val availableTags by viewModel.allAvailableTags.collectAsStateWithLifecycle()
    val newCardsPerDay by viewModel.newCardsPerDay.collectAsStateWithLifecycle()
    val introducedToday by viewModel.newCardsIntroducedToday.collectAsStateWithLifecycle()
    val quizCountsInFsrs by viewModel.quizCountsInFsrs.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val borderCol = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f)

    if (subMode != StudySubMode.HUB) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { subMode = StudySubMode.HUB },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Study Hub")
                }
                Text(
                    text = when (subMode) {
                        StudySubMode.QUIZ -> "Multiple Choice Quiz"
                        StudySubMode.SPELLING -> "Spelling Practice"
                        StudySubMode.FREE_BROWSE -> "Free Browse Cards (No FSRS)"
                        StudySubMode.HUB -> "Study"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
            }

            if (subMode == StudySubMode.QUIZ || subMode == StudySubMode.SPELLING) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Count answers in FSRS schedule",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = quizCountsInFsrs,
                        onCheckedChange = { viewModel.setQuizCountsInFsrs(it) }
                    )
                }
            }

            when (subMode) {
                StudySubMode.QUIZ -> QuizPracticeView(viewModel = viewModel, bookmarkedWords = bookmarkedWords)
                StudySubMode.SPELLING -> SpellingPracticeView(viewModel = viewModel, bookmarkedWords = bookmarkedWords)
                StudySubMode.FREE_BROWSE -> {
                    LaunchedEffect(Unit) { viewModel.setStudyMode(FlashcardStudyMode.CARDS) }
                    FlashcardsTabContent(
                        viewModel = viewModel,
                        bookmarkedWords = bookmarkedWords,
                        innerPadding = PaddingValues(0.dp)
                    )
                }
                StudySubMode.HUB -> Unit
            }
        }
        return
    }

    val today = remember { LocalDate.now().toEpochDay() }
    val deckSummary = remember(bookmarkedWords, selectedDeck, newCardsPerDay, introducedToday) {
        StudyQueuePlanner.summarizeToday(
            bookmarkedWords = bookmarkedWords,
            todayEpochDay = today,
            newCardsDailyLimit = newCardsPerDay,
            newCardsIntroducedToday = introducedToday,
            deckFilter = selectedDeck
        )
    }
    val leechCount = remember(bookmarkedWords) {
        StudyQueuePlanner.buildLeechesDrillQueue(bookmarkedWords).size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("study_hub_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Deck Filter Chips with Live Due Counts
            if (availableTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (listOf("ALL") + availableTags).forEach { deck ->
                        val isSelected = selectedDeck.equals(deck, ignoreCase = true)
                        val count = remember(bookmarkedWords, deck, newCardsPerDay, introducedToday) {
                            StudyQueuePlanner.summarizeToday(
                                bookmarkedWords = bookmarkedWords,
                                todayEpochDay = today,
                                newCardsDailyLimit = newCardsPerDay,
                                newCardsIntroducedToday = introducedToday,
                                deckFilter = deck
                            ).totalToday
                        }
                        val label = if (deck == "ALL") "All Decks ($count)" else "$deck ($count)"
                        Surface(
                            onClick = { viewModel.setSelectedReviewDeck(deck) },
                            shape = RoundedCornerShape(22.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                            border = if (isSelected) null else BorderStroke(0.5.dp, borderCol),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("study_deck_chip_$deck")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Main FSRS Spaced Repetition Card
            AppleInsetGroupedCard(cornerRadius = 22.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (bookmarkedWords.isEmpty()) {
                        AppleIconBadge(
                            icon = Icons.Default.School,
                            tintColor = MaterialTheme.colorScheme.primary,
                            size = 54.dp,
                            iconSize = 28.dp
                        )
                        Text(
                            text = "Build Your Study Deck",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Choose any book or unit from Courses to add words into your Anki FSRS study queue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onNavigateToCourses,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) {
                            Text("Browse 9 Books & IELTS")
                        }
                    } else {
                        Text(
                            text = if (selectedDeck.equals("ALL", ignoreCase = true)) {
                                "Spaced Repetition (FSRS)"
                            } else {
                                selectedDeck
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StudyCountColumn(value = deckSummary.newCount, label = "New", color = Color(0xFF0A84FF))
                            StudyCountColumn(value = deckSummary.learningCount, label = "Learning", color = Color(0xFFFF9F0A))
                            StudyCountColumn(value = deckSummary.reviewCount, label = "Review", color = Color(0xFF30D158))
                        }

                        val startInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (deckSummary.totalToday > 0) {
                                    viewModel.startUnifiedStudySession(
                                        deckFilter = selectedDeck,
                                        deckTitle = if (selectedDeck.equals("ALL", true)) "Today's Session" else selectedDeck
                                    )
                                } else {
                                    viewModel.startUnifiedStudySession(
                                        deckFilter = selectedDeck,
                                        deckTitle = "Study Ahead",
                                        extraNewBonus = 5
                                    )
                                }
                            },
                            interactionSource = startInteraction,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .applePressScale(startInteraction, 0.97f)
                                .testTag("study_hub_start_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (deckSummary.totalToday > 0) {
                                    "Start Study Session (${deckSummary.totalToday} Cards)"
                                } else {
                                    "Study Ahead (+5 New Cards)"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Voice Priority: Natural Offline Neural → Online → AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Extra Practice Modes (No Listening Recall — deferred to v1.4+)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppleSectionHeader(title = "Extra Practice")
                AppleInsetGroupedCard(cornerRadius = 18.dp) {
                    if (leechCount > 0) {
                        StudyModeRow(
                            icon = Icons.Default.WarningAmber,
                            tint = Color(0xFFFF453A),
                            title = "Weak & Leeches Drill",
                            subtitle = "Focus on words you missed repeatedly",
                            badgeText = "$leechCount",
                            onClick = { viewModel.startLeechesDrillSession() },
                            testTag = "study_mode_leeches_row"
                        )
                        AppleInsetDivider(startIndent = 60.dp)
                    }
                    StudyModeRow(
                        icon = Icons.Default.Quiz,
                        tint = Color(0xFFFF375F),
                        title = "Multiple Choice Quiz",
                        subtitle = if (quizCountsInFsrs) "Updates FSRS schedule" else "Free recall drill",
                        onClick = { subMode = StudySubMode.QUIZ },
                        testTag = "study_mode_quiz_row"
                    )
                    AppleInsetDivider(startIndent = 60.dp)
                    StudyModeRow(
                        icon = Icons.Default.Spellcheck,
                        tint = Color(0xFF5E5CE6),
                        title = "Spelling Practice",
                        subtitle = "Type words from definition & offline audio",
                        onClick = { subMode = StudySubMode.SPELLING },
                        testTag = "study_mode_spelling_row"
                    )
                    AppleInsetDivider(startIndent = 60.dp)
                    StudyModeRow(
                        icon = Icons.Default.Style,
                        tint = Color(0xFF8E8E93),
                        title = "Free Browse Flashcards",
                        subtitle = "Flip through your deck without changing FSRS",
                        onClick = { subMode = StudySubMode.FREE_BROWSE },
                        testTag = "study_mode_browse_row"
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyCountColumn(value: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StudyModeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppleIconBadge(icon = icon, tintColor = tint, size = 36.dp, iconSize = 19.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (badgeText != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tint.copy(alpha = 0.16f)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = tint,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}
