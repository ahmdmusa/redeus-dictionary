package com.example.ui.screens.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OfflineBook
import com.example.data.WordEntity
import com.example.domain.CourseProgressCalculator
import com.example.ui.DayStreakStatus
import com.example.ui.DictionaryViewModel
import com.example.ui.components.*
import com.example.ui.navigation.LocalWindowWidthSizeClass
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusMotion
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tab 0: Study-First Daily Habit Command Center ("Today").
 * Handles 3 honest states:
 *   1) Cold-Start (Empty Deck): 1-tap course + daily pace setup (ZERO false-positive checkmarks!)
 *   2) Active Daily Session: Anki 3-bucket Hero Card (New · Learning · Review) + 56dp Start CTA
 *   3) All Caught Up: Calm completion card + Tomorrow forecast + "+5 Extra New" / "Quick Quiz"
 */
@Composable
fun TodayScreen(
    viewModel: DictionaryViewModel,
    bookmarkedWords: List<WordEntity>,
    innerPadding: PaddingValues,
    onOpenCoursesBook: (OfflineBook) -> Unit,
    onOpenCoursesRoot: () -> Unit,
    onOpenQuickSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenQuickQuiz: () -> Unit,
    onOpenIeltsReading: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val todaySummary by viewModel.todayQueueSummary.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val dailyGoalTarget by viewModel.dailyGoalTarget.collectAsStateWithLifecycle()
    val weeklyTarget by viewModel.weeklyNewWordsTarget.collectAsStateWithLifecycle()
    val weeklyStudied by viewModel.weeklyWordsStudiedCount.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val todayWords by viewModel.todayStudiedWords.collectAsStateWithLifecycle()
    val weekWords by viewModel.weekStudiedWords.collectAsStateWithLifecycle()
    val detailedStats by viewModel.detailedSrsStats.collectAsStateWithLifecycle()
    val activeCourseBookNum by viewModel.activeCourseBookNum.collectAsStateWithLifecycle()
    val offlineBooks by viewModel.offlineBooks.collectAsStateWithLifecycle()
    val bookWordLowersMap by viewModel.bookWordLowersMap.collectAsStateWithLifecycle()
    val activeCourseUnitMap by viewModel.activeCourseUnitWordMap.collectAsStateWithLifecycle()
    val isAddingWords by viewModel.isAddingCourseWords.collectAsStateWithLifecycle()
    val newCardsPerDay by viewModel.newCardsPerDay.collectAsStateWithLifecycle()
    val widthSizeClass = LocalWindowWidthSizeClass.current
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
        if (offlineBooks.isEmpty()) {
            viewModel.loadOfflineCatalog()
        }
        if (activeCourseBookNum > 0) {
            viewModel.loadActiveCourseUnitMap(activeCourseBookNum)
        }
    }

    val baseGreeting = remember {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val greeting = remember(baseGreeting, userName) {
        val cleanName = userName.trim()
        if (cleanName.isNotBlank()) "$baseGreeting, $cleanName" else baseGreeting
    }
    val dateHeader = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH))
    }

    val deckMap = remember(bookmarkedWords) {
        bookmarkedWords.associateBy { it.word.trim().lowercase() }
    }
    val activeBook = remember(offlineBooks, activeCourseBookNum) {
        offlineBooks.firstOrNull { it.bookNum == activeCourseBookNum }
            ?: offlineBooks.firstOrNull { it.bookNum == 1 }
    }
    val activeBookProgress = remember(activeBook, bookWordLowersMap, deckMap, activeCourseUnitMap) {
        activeBook?.let { book ->
            CourseProgressCalculator.calculateBookProgress(
                book = book,
                bookWordLowers = bookWordLowersMap[book.bookNum].orEmpty(),
                deckWordMap = deckMap,
                unitWordLowersMap = activeCourseUnitMap
            )
        }
    }

    val todayReviews = remember(detailedStats) {
        detailedStats.streakHistoryLast30Days.lastOrNull { it.isToday }?.count ?: 0
    }
    val last7Days = remember(detailedStats.streakHistoryLast30Days) {
        val raw = detailedStats.streakHistoryLast30Days.takeLast(7)
        if (raw.size == 7) raw else {
            val today = LocalDate.now()
            (6L downTo 0L).map { offset ->
                val date = today.minusDays(offset)
                val epoch = date.toEpochDay()
                raw.firstOrNull { it.epochDay == epoch } ?: DayStreakStatus(
                    epochDay = epoch,
                    dateLabel = date.dayOfWeek.name.take(3),
                    hasReview = false,
                    count = 0,
                    isToday = offset == 0L
                )
            }
        }
    }

    var showGoalSheet by rememberSaveable { mutableStateOf(false) }

    if (showGoalSheet) {
        DailyTargetsBottomSheet(
            dailyGoal = dailyGoalTarget,
            newCardsPerDay = newCardsPerDay,
            weeklyTarget = weeklyTarget,
            onSelectDailyGoal = { viewModel.setDailyGoalTarget(it) },
            onSelectNewPerDay = { viewModel.setNewCardsPerDay(it) },
            onSelectWeeklyTarget = { viewModel.setWeeklyNewWordsTarget(it) },
            onDismiss = { showGoalSheet = false }
        )
    }

    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("today_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isExpanded) 960.dp else 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Personal Editorial Header + Sada Eyebrow + Streak Pill + 48dp Settings Gear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Sada · صَدى",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "· $dateHeader",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val streakInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = onOpenProfile,
                        interactionSource = streakInteraction,
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .applePressScale(streakInteraction, 0.95f)
                            .testTag("today_streak_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = "${currentStreak}d",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("today_settings_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Hero Card (3 honest states: Cold-Start / Active Session / All Caught Up)
            if (bookmarkedWords.isEmpty()) {
                ColdStartPlanCard(
                    isLoading = isAddingWords,
                    onStartDayOne = { bookNum, pace ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.startDayOnePlan(bookNum, pace)
                    }
                )
            } else if (todaySummary.totalToday > 0) {
                ActiveSessionHeroCard(
                    newCount = todaySummary.newCount,
                    learningCount = todaySummary.learningCount,
                    reviewCount = todaySummary.reviewCount,
                    estimatedMinutes = todaySummary.estimatedMinutes,
                    onStartSession = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.startUnifiedStudySession()
                    }
                )
            } else {
                AllCaughtUpHeroCard(
                    todayReviews = todayReviews,
                    totalNewRemainingInDeck = todaySummary.totalNewRemainingInDeck,
                    onStudyExtraNew = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (todaySummary.totalNewRemainingInDeck > 0) {
                            viewModel.startUnifiedStudySession(extraNewBonus = 5)
                        } else {
                            viewModel.autoFeedActiveCourseWords(count = 5, launchSession = true)
                        }
                    },
                    onQuickQuiz = onOpenQuickQuiz
                )
            }

            // 3. Daily Goal Ring + Weekly Target Progress + 7-Day Activity Strip
            DailyGoalAndWeekCard(
                todayReviews = todayReviews,
                dailyGoalTarget = dailyGoalTarget,
                newCardsPerDay = newCardsPerDay,
                weeklyStudied = weeklyStudied,
                weeklyTarget = weeklyTarget,
                currentStreak = currentStreak,
                last7Days = last7Days,
                onEditGoal = { showGoalSheet = true }
            )

            // 3b. IELTS Speaking Agent Prompt + IELTS Reading Corner (Practice real language with today's/week's words!)
            if (bookmarkedWords.isNotEmpty()) {
                val dailyDone = todayReviews >= dailyGoalTarget.coerceAtLeast(1)
                if (dailyDone || todayWords.isNotEmpty()) {
                    IeltsSpeakingPromptCard(
                        viewModel = viewModel,
                        todayWords = todayWords,
                        weekWords = weekWords,
                        defaultScope = com.example.domain.PracticeWordScope.TODAY,
                        headline = if (dailyDone) {
                            "Daily Goal Hit — Speak with AI Agent!"
                        } else {
                            "IELTS Speaking Practice Prompt"
                        },
                        subtitle = if (dailyDone) {
                            "You hit today's target! Copy this tailored prompt to practice today's words in IELTS Speaking with ChatGPT Voice or Gemini."
                        } else {
                            "Practice your daily & weekly words in a 3-part IELTS Speaking conversation with any AI voice agent."
                        }
                    )
                }

                AppleInsetGroupedCard(cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenIeltsReading)
                            .padding(14.dp)
                            .testTag("today_ielts_reading_card"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppleIconBadge(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            tintColor = MaterialTheme.colorScheme.primary,
                            size = 40.dp,
                            iconSize = 20.dp
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "IELTS Reading Corner",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Practice IELTS passages & stories crafted from your daily & weekly words",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open IELTS Reading",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Continue Active Course Card (deep-links to exact Book & Unit!)
            if (activeBook != null && activeBookProgress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "Continue Active Course")
                    AppleInsetGroupedCard(cornerRadius = 20.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenCoursesBook(activeBook) }
                                .padding(16.dp)
                                .testTag("today_continue_course_card"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = bookAccentColor(activeBook.bookNum),
                                modifier = Modifier.size(width = 50.dp, height = 56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = activeBook.cefr.take(5),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = activeBook.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Next: Unit ${activeBookProgress.currentUnitNum} · ${activeBookProgress.inDeckCount}/${activeBookProgress.totalWords} in deck",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LinearProgressIndicator(
                                    progress = { (activeBookProgress.inDeckPercent / 100f).coerceIn(0f, 1f) },
                                    color = bookAccentColor(activeBook.bookNum),
                                    trackColor = bookAccentColor(activeBook.bookNum).copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open active course",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                AppleInsetGroupedCard(cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenCoursesRoot)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppleIconBadge(icon = Icons.Default.School, tintColor = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Explore 9 Offline Books & IELTS", style = MaterialTheme.typography.titleMedium)
                            Text("9,591 words with definitions, examples & local photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 5. Quick Offline Dictionary Lookup Pill (1 tap to Dictionary!)
            val searchInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = onOpenQuickSearch,
                interactionSource = searchInteraction,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .applePressScale(searchInteraction, 0.98f)
                    .testTag("today_quick_search_pill")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Quick offline lookup (97K English ↔ Arabic)...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ColdStartPlanCard(
    isLoading: Boolean,
    onStartDayOne: (bookNum: Int, dailyNewLimit: Int) -> Unit
) {
    var selectedBook by rememberSaveable { mutableStateOf(1) }
    var selectedPace by rememberSaveable { mutableStateOf(10) }
    val choices = listOf(
        Triple(1, "Essential Book 1 (A1)", Color(0xFF30D158)),
        Triple(3, "Essential Book 3 (B1)", Color(0xFF0A84FF)),
        Triple(7, "Barron's IELTS", Color(0xFFFF9F0A)),
        Triple(9, "IELTS Vocab Bank", Color(0xFF5E5CE6))
    )

    AppleInsetGroupedCard(
        cornerRadius = 24.dp,
        modifier = Modifier.testTag("today_cold_start_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Start Your Daily Study Plan",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pick your starting book and daily pace. We'll load Day 1 right into your spaced-repetition deck — 100% offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 2x2 Book Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (bookNum, label, accent) ->
                            val isSelected = selectedBook == bookNum
                            Surface(
                                onClick = { selectedBook = bookNum },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) accent else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily New Words Pace Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "NEW WORDS PER DAY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20).forEach { pace ->
                        val isSelected = selectedPace == pace
                        Surface(
                            onClick = { selectedPace = pace },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$pace",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = if (selectedPace == pace) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onStartDayOne(selectedBook, selectedPace) },
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("today_start_day_one_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Preparing Day 1...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Start Day 1 ($selectedPace Words)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSessionHeroCard(
    newCount: Int,
    learningCount: Int,
    reviewCount: Int,
    estimatedMinutes: Int,
    onStartSession: () -> Unit
) {
    val startInteraction = remember { MutableInteractionSource() }
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val gradStart = if (isDark) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary
    val gradEnd = if (isDark) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.secondary
    val heroTextColor = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary
    val ctaBg = if (isDark) MaterialTheme.colorScheme.primary else Color.White
    val ctaFg = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(26.dp),
        border = if (isDark) BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_hero_session_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(colors = listOf(gradStart, gradEnd))
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Session",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = heroTextColor
                    )
                    Text(
                        text = "~$estimatedMinutes min",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = heroTextColor.copy(alpha = 0.88f)
                    )
                }

                // Anki 3-Bucket Counters (Color + Symbol + Label)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HeroBucketTile(count = newCount, label = "● New", accent = Color(0xFF99F6E4), textTint = heroTextColor, modifier = Modifier.weight(1f))
                    HeroBucketTile(count = learningCount, label = "◐ Learning", accent = Color(0xFFFFD08A), textTint = heroTextColor, modifier = Modifier.weight(1f))
                    HeroBucketTile(count = reviewCount, label = "✓ Review", accent = Color(0xFFA6F5B9), textTint = heroTextColor, modifier = Modifier.weight(1f))
                }

                Surface(
                    onClick = onStartSession,
                    interactionSource = startInteraction,
                    shape = RoundedCornerShape(16.dp),
                    color = ctaBg,
                    contentColor = ctaFg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .applePressScale(startInteraction, 0.97f)
                        .testTag("today_start_study_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start Studying",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBucketTile(
    count: Int,
    label: String,
    accent: Color,
    textTint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.22f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textTint.copy(alpha = 0.92f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun AllCaughtUpHeroCard(
    todayReviews: Int,
    totalNewRemainingInDeck: Int,
    onStudyExtraNew: () -> Unit,
    onQuickQuiz: () -> Unit
) {
    AppleInsetGroupedCard(
        cornerRadius = 24.dp,
        modifier = Modifier.testTag("today_all_caught_up_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppleIconBadge(
                icon = Icons.Default.CheckCircle,
                tintColor = Color(0xFF30D158),
                size = 54.dp,
                iconSize = 30.dp
            )
            Text(
                text = "All Caught Up for Today",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = if (todayReviews > 0) {
                    "You completed $todayReviews cards today. Want to study ahead?"
                } else {
                    "No reviews due right now. Add 5 fresh words from your active course!"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStudyExtraNew,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(
                        text = if (totalNewRemainingInDeck > 0) "+5 Extra New" else "+5 Course",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FilledTonalButton(
                    onClick = onQuickQuiz,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Quick Quiz", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DailyGoalAndWeekCard(
    todayReviews: Int,
    dailyGoalTarget: Int,
    newCardsPerDay: Int,
    weeklyStudied: Int,
    weeklyTarget: Int,
    currentStreak: Int,
    last7Days: List<DayStreakStatus>,
    onEditGoal: () -> Unit
) {
    val safeGoal = dailyGoalTarget.coerceAtLeast(1)
    val safeWeekly = weeklyTarget.coerceAtLeast(1)
    val progress = (todayReviews.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val weeklyProgress = (weeklyStudied.toFloat() / safeWeekly.toFloat()).coerceIn(0f, 1f)
    val reducedMotion = LocalReducedMotionEnabled.current
    val animatedRing by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (reducedMotion) RedeusMotion.spec(true) else spring(dampingRatio = 0.85f, stiffness = 240f),
        label = "todayGoalRing"
    )
    val animatedWeekly by animateFloatAsState(
        targetValue = weeklyProgress,
        animationSpec = if (reducedMotion) RedeusMotion.spec(true) else spring(dampingRatio = 0.88f, stiffness = 220f),
        label = "weeklyGoalBar"
    )
    val milestoneDays = remember(currentStreak) {
        com.example.domain.IeltsPracticePromptBuilder.detectStreakMilestone(currentStreak)
    }
    val appleGreen = Color(0xFF30D158)
    val appleBlue = MaterialTheme.colorScheme.primary
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f

    AppleInsetGroupedCard(cornerRadius = 20.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditGoal)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Goal Ring
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val track = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 7.dp.toPx()
                        drawArc(track, 0f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(
                            if (todayReviews >= safeGoal) appleGreen else appleBlue,
                            -90f,
                            360f * animatedRing,
                            false,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$todayReviews/$safeGoal",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Last 7 Days Bar Strip
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Goal · $newCardsPerDay new/day",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            color = appleBlue
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7Days.forEach { day ->
                            val localDate = LocalDate.ofEpochDay(day.epochDay)
                            val initial = localDate.dayOfWeek.name.take(1)
                            val barRatio = (day.count.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
                            val barHeight = (6f + barRatio * 24f).dp
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                day.count >= safeGoal -> appleGreen
                                                day.count > 0 || day.isToday -> appleBlue
                                                else -> if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                                            }
                                        )
                                )
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (day.isToday) appleBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Weekly Habit Target Bar + Milestone Badge
            AppleInsetDivider(startIndent = 0.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Weekly Words Target",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (milestoneDays != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "★ ${milestoneDays}d Milestone",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "$weeklyStudied / $safeWeekly",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (weeklyStudied >= safeWeekly) appleGreen else appleBlue
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedWeekly },
                    color = if (weeklyStudied >= safeWeekly) appleGreen else appleBlue,
                    trackColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyTargetsBottomSheet(
    dailyGoal: Int,
    newCardsPerDay: Int,
    weeklyTarget: Int,
    onSelectDailyGoal: (Int) -> Unit,
    onSelectNewPerDay: (Int) -> Unit,
    onSelectWeeklyTarget: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Daily & Weekly Habit Targets",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DAILY REVIEW GOAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 20, 30, 50).forEach { target ->
                        val selected = dailyGoal == target
                        Surface(
                            onClick = { onSelectDailyGoal(target) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$target",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NEW WORDS PER DAY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20, 30).forEach { limit ->
                        val selected = newCardsPerDay == limit
                        Surface(
                            onClick = { onSelectNewPerDay(limit) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$limit",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WEEKLY NEW WORDS TARGET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(35, 70, 105, 140, 210).forEach { target ->
                        val selected = weeklyTarget == target
                        Surface(
                            onClick = { onSelectWeeklyTarget(target) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$target",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Done")
            }
        }
    }
}

fun bookAccentColor(bookNum: Int): Color = when (bookNum) {
    1, 2 -> Color(0xFF0F766E)
    3, 4 -> Color(0xFF0369A1)
    5, 6 -> Color(0xFF7E22CE)
    7 -> Color(0xFFC2590A)
    8 -> Color(0xFF0E7490)
    else -> Color(0xFF4338CA)
}
