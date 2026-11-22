package com.example.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.R
import com.example.data.OfflineSearchSuggestion
import com.example.data.WordEntity
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.navigation.LocalWindowWidthSizeClass
import com.example.ui.screens.mining.ExampleSentenceMiningActionsRow
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusExpressiveType
import com.example.ui.theme.RedeusMotion

@Composable
fun SearchTabContent(
    viewModel: DictionaryViewModel,
    searchQuery: String,
    uiState: SearchUiState,
    innerPadding: PaddingValues,
    onOpenApiSettings: () -> Unit = {},
    onOpenCourses: () -> Unit = {},
    onOpenReview: () -> Unit = {}
) {
    val widthSizeClass = LocalWindowWidthSizeClass.current
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded

    if (isExpanded) {
        ExpandedSearchLayout(
            viewModel = viewModel,
            searchQuery = searchQuery,
            uiState = uiState,
            innerPadding = innerPadding,
            onOpenApiSettings = onOpenApiSettings,
            onOpenCourses = onOpenCourses,
            onOpenReview = onOpenReview
        )
    } else {
        CompactMediumSearchLayout(
            viewModel = viewModel,
            searchQuery = searchQuery,
            uiState = uiState,
            innerPadding = innerPadding,
            onOpenApiSettings = onOpenApiSettings,
            onOpenCourses = onOpenCourses,
            onOpenReview = onOpenReview
        )
    }
}

@Composable
private fun CompactMediumSearchLayout(
    viewModel: DictionaryViewModel,
    searchQuery: String,
    uiState: SearchUiState,
    innerPadding: PaddingValues,
    onOpenApiSettings: () -> Unit = {},
    onOpenCourses: () -> Unit = {},
    onOpenReview: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val suggestions by viewModel.searchSuggestions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppleSpotlightSearchSection(
                searchQuery = searchQuery,
                suggestions = suggestions,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClear = { viewModel.clearSearchState() },
                onSubmit = { viewModel.performSearch(searchQuery) },
                onSelectSuggestion = { viewModel.performSearch(it.word) }
            )
            SearchResultSection(
                viewModel = viewModel,
                searchQuery = searchQuery,
                uiState = uiState,
                onOpenApiSettings = onOpenApiSettings,
                onOpenCourses = onOpenCourses,
                onOpenReview = onOpenReview
            )
        }
    }
}

@Composable
private fun ExpandedSearchLayout(
    viewModel: DictionaryViewModel,
    searchQuery: String,
    uiState: SearchUiState,
    innerPadding: PaddingValues,
    onOpenApiSettings: () -> Unit = {},
    onOpenCourses: () -> Unit = {},
    onOpenReview: () -> Unit = {}
) {
    val suggestions by viewModel.searchSuggestions.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppleSpotlightSearchSection(
                searchQuery = searchQuery,
                suggestions = suggestions,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onClear = { viewModel.clearSearchState() },
                onSubmit = { viewModel.performSearch(searchQuery) },
                onSelectSuggestion = { viewModel.performSearch(it.word) }
            )
            StudyHomeDashboard(
                viewModel = viewModel,
                onOpenCourses = onOpenCourses,
                onOpenReview = onOpenReview,
                onSelectWord = { viewModel.performSearch(it) }
            )
        }

        Column(
            modifier = Modifier
                .weight(0.58f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState is SearchUiState.Idle) {
                AppleInsetGroupedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppleIconBadge(
                            icon = Icons.Default.Search,
                            tintColor = MaterialTheme.colorScheme.primary,
                            size = 48.dp,
                            iconSize = 24.dp
                        )
                        Text(
                            text = "Offline Bilingual Dictionary",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Search in English or Arabic across 6 Essential Books, 3 IELTS Books (9,591 words), and 87K Arabic meanings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                SearchResultSection(
                    viewModel = viewModel,
                    searchQuery = searchQuery,
                    uiState = uiState,
                    onOpenApiSettings = onOpenApiSettings,
                    onOpenCourses = onOpenCourses,
                    onOpenReview = onOpenReview
                )
            }
        }
    }
}

@Composable
private fun AppleSpotlightSearchSection(
    searchQuery: String,
    suggestions: List<OfflineSearchSuggestion>,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    onSelectSuggestion: (OfflineSearchSuggestion) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.5.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search English or Arabic...",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                onSubmit()
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_word_input")
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    val goInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = {
                            onSubmit()
                            focusManager.clearFocus()
                        },
                        interactionSource = goInteraction,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier
                            .size(34.dp)
                            .applePressScale(goInteraction, 0.92f)
                            .testTag("search_word_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Look up word",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            AppleInsetGroupedCard(
                modifier = Modifier.testTag("offline_suggestions_card"),
                cornerRadius = 14.dp
            ) {
                suggestions.forEachIndexed { idx, suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                onSelectSuggestion(suggestion)
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                            .testTag("offline_suggestion_${suggestion.word.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = suggestion.word.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (suggestion.translationAr.isNotBlank()) {
                                Text(
                                    text = suggestion.translationAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = suggestion.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (idx < suggestions.lastIndex) {
                        AppleInsetDivider(startIndent = 14.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyHomeDashboard(
    viewModel: DictionaryViewModel,
    onOpenCourses: () -> Unit,
    onOpenReview: () -> Unit,
    onSelectWord: (String) -> Unit
) {
    val cardsDue by viewModel.cardsDueTodayCount.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val dailyGoalTarget by viewModel.dailyGoalTarget.collectAsStateWithLifecycle()
    val detailedStats by viewModel.detailedSrsStats.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

    val masteredCount = remember(bookmarked) {
        bookmarked.count { it.distinctReviewDays >= 3 && it.interval >= 21 && !it.isLeech }
    }
    val todayReviews = remember(detailedStats) {
        detailedStats.streakHistoryLast30Days.lastOrNull { it.isToday }?.count ?: 0
    }
    val safeGoal = dailyGoalTarget.coerceAtLeast(1)
    val goalProgress = (todayReviews.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val goalReached = todayReviews >= safeGoal
    val goalPercent = (goalProgress * 100f).toInt()

    val last7Days = remember(detailedStats.streakHistoryLast30Days) {
        val raw = detailedStats.streakHistoryLast30Days.takeLast(7)
        if (raw.size == 7) {
            raw
        } else {
            val today = java.time.LocalDate.now()
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

    val goalSteps = listOf(10, 20, 30, 50)
    val nextGoal = goalSteps.firstOrNull { it > dailyGoalTarget } ?: goalSteps.first()
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val appleGreen = Color(0xFF30D158)
    val appleOrange = Color(0xFFFF9F0A)
    val appleBlue = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("study_home_dashboard"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Apple Health / Fitness Style Daily Tracking & 7-Day Activity Card
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AppleSectionHeader(title = "Daily Overview")
            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Row: Title + Streak Pill + Tappable Goal Target Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = appleOrange.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalFireDepartment,
                                        contentDescription = "Study streak",
                                        tint = appleOrange,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "${currentStreak}d Streak",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        color = appleOrange
                                    )
                                }
                            }
                        }

                        val goalInteraction = remember { MutableInteractionSource() }
                        Surface(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setDailyGoalTarget(nextGoal)
                            },
                            interactionSource = goalInteraction,
                            shape = RoundedCornerShape(20.dp),
                            color = appleBlue.copy(alpha = 0.12f),
                            modifier = Modifier
                                .applePressScale(goalInteraction, 0.95f)
                                .testTag("overview_daily_goal_pill")
                        ) {
                            Text(
                                text = "Goal: $dailyGoalTarget/day",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = appleBlue,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Daily Review Progress Metric + Bar
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "$todayReviews",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "/ $dailyGoalTarget reviews today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                text = if (goalReached) "Goal Done" else "$goalPercent%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (goalReached) appleGreen else appleBlue
                            )
                        }

                        LinearProgressIndicator(
                            progress = { goalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (goalReached) appleGreen else appleBlue,
                            trackColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
                        )
                    }

                    // Compact 3-Metric Row (Due / In Deck / Mastered)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OverviewMiniMetric(label = "Due Now", value = "$cardsDue", accentColor = if (cardsDue > 0) appleOrange else appleGreen)
                        OverviewMiniMetric(label = "In Deck", value = "${bookmarked.size}", accentColor = appleBlue)
                        OverviewMiniMetric(label = "Mastered", value = "$masteredCount", accentColor = appleGreen)
                    }
                }

                AppleInsetDivider(startIndent = 16.dp)

                // 7-Day Daily Tracker Strip
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "LAST 7 DAYS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        last7Days.forEach { day ->
                            val localDate = java.time.LocalDate.ofEpochDay(day.epochDay)
                            val dayInitial = localDate.dayOfWeek.name.take(1)
                            val active = day.count > 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = dayInitial,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (day.isToday) appleBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        active -> appleGreen
                                        day.isToday -> appleBlue.copy(alpha = 0.14f)
                                        else -> if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                                    },
                                    border = if (day.isToday && !active) {
                                        BorderStroke(1.5.dp, appleBlue)
                                    } else null,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (active) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "${day.count} reviews",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        } else {
                                            Text(
                                                text = if (day.isToday) "•" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = appleBlue
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (active) "${day.count}" else "—",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Today's Smart Anki + Curriculum Study Plan
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AppleSectionHeader(title = "Today's Study Plan")
            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                // Step 1: FSRS Spaced Repetition Reviews
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = if (cardsDue > 0) onOpenReview else onOpenCourses)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppleIconBadge(
                        icon = if (cardsDue > 0) Icons.Default.Bolt else Icons.Default.CheckCircle,
                        tintColor = if (cardsDue > 0) appleGreen else appleGreen,
                        size = 38.dp,
                        iconSize = 20.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (cardsDue > 0) "1. Review $cardsDue Due Cards" else "1. Daily Reviews Complete",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (cardsDue > 0) "FSRS spaced repetition scheduled today" else "No overdue cards waiting right now",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (cardsDue > 0) {
                        val reviewInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = onOpenReview,
                            interactionSource = reviewInteraction,
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .applePressScale(reviewInteraction, 0.95f)
                        ) {
                            Text(
                                text = "Review",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                AppleInsetDivider(startIndent = 64.dp)

                // Step 2: Add Daily New Cards from 6 Books / IELTS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenCourses)
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppleIconBadge(
                        icon = Icons.Default.School,
                        tintColor = appleBlue,
                        size = 38.dp,
                        iconSize = 20.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "2. Add +10 New Words Today",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pick a unit from 6 Essential Books or 3 IELTS Books",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val coursesInteraction = remember { MutableInteractionSource() }
                    FilledTonalButton(
                        onClick = onOpenCourses,
                        interactionSource = coursesInteraction,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .applePressScale(coursesInteraction, 0.95f)
                    ) {
                        Text(
                            text = "Courses",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // 3. Inset Grouped Offline Curriculum Quick-Access
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AppleSectionHeader(title = "Offline Library")
            AppleInsetGroupedCard {
                StudyHomeQuickRow(
                    icon = Icons.Default.WorkspacePremium,
                    tintColor = appleOrange,
                    title = "IELTS Preparation",
                    subtitle = "3 Books • Barron's, Listening 1200 & Bank (5,991 words)",
                    onClick = onOpenCourses
                )
                AppleInsetDivider(startIndent = 60.dp)
                StudyHomeQuickRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    tintColor = appleBlue,
                    title = "6 Essential English Books",
                    subtitle = "Levels A1 to C1 • 181 Units (3,600 words)",
                    onClick = onOpenCourses
                )
                AppleInsetDivider(startIndent = 60.dp)
                StudyHomeQuickRow(
                    icon = Icons.Default.Category,
                    tintColor = appleGreen,
                    title = "26 Life Topics & 87K Arabic",
                    subtitle = "Offline instant English ↔ Arabic search",
                    onClick = onOpenCourses
                )
            }
        }

        // 4. Minimal Discovery Chips
        DiscoverySuggestions(onSelectWord = onSelectWord)
    }
}

@Composable
private fun OverviewMiniMetric(
    label: String,
    value: String,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            color = accentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StudyHomeQuickRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppleIconBadge(icon = icon, tintColor = tintColor, size = 34.dp, iconSize = 18.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SearchResultSection(
    viewModel: DictionaryViewModel,
    searchQuery: String,
    uiState: SearchUiState,
    onOpenApiSettings: () -> Unit = {},
    onOpenCourses: () -> Unit = {},
    onOpenReview: () -> Unit = {}
) {
    val reducedMotion = LocalReducedMotionEnabled.current

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationMedium)) togetherWith
                fadeOut(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationShort))
        },
        label = "searchState"
    ) { state ->
        when (state) {
            is SearchUiState.Idle -> {
                StudyHomeDashboard(
                    viewModel = viewModel,
                    onOpenCourses = onOpenCourses,
                    onOpenReview = onOpenReview,
                    onSelectWord = { viewModel.performSearch(it) }
                )
            }
            is SearchUiState.Loading -> {
                AppleInsetGroupedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.5.dp)
                        Text(
                            text = "Looking up word...",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            is SearchUiState.Success -> {
                WordDetailsCard(
                    wordEntity = state.word,
                    onSpeak = { viewModel.speak(state.word.word) },
                    onBookmarkToggle = { viewModel.toggleBookmark(state.word) },
                    viewModel = viewModel
                )
            }
            is SearchUiState.Error -> {
                ErrorStateCard(
                    message = state.message,
                    onRetry = { viewModel.performSearch(searchQuery) },
                    onOpenApiSettings = onOpenApiSettings
                )
            }
        }
    }
}

/**
 * Preserved for GreetingScreenshotTest compatibility.
 */
@Composable
fun HeroBannerSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Redeus Dictionary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "6 Essential Books • 3 IELTS Books • 87K Arabic • FSRS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverySuggestions(onSelectWord: (String) -> Unit) {
    val list = listOf("aquatic", "resilient", "serendipity", "ephemeral", "benevolent", "pragmatic")
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppleSectionHeader(title = "Try a Word")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            list.forEach { word ->
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { onSelectWord(word) },
                    interactionSource = interaction,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.5.dp, borderColor),
                    modifier = Modifier
                        .applePressScale(interaction, 0.95f)
                        .testTag("seed_word_$word")
                ) {
                    Text(
                        text = word.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TryAnotherPhotoButton(
    word: WordEntity,
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val imageSource by viewModel.imageSourcePreference.collectAsStateWithLifecycle()
    if (imageSource != "web") return

    val cyclingWord by viewModel.cyclingPhotoWord.collectAsStateWithLifecycle()
    val isCycling = cyclingWord?.equals(word.word, ignoreCase = true) == true

    if (compact) {
        Surface(
            onClick = { viewModel.cycleNextPhoto(word) },
            enabled = !isCycling,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.50f),
            contentColor = Color.White,
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
            modifier = modifier
                .size(32.dp)
                .testTag("try_another_photo_button_${word.word}")
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCycling) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .size(13.dp)
                            .testTag("try_another_photo_spinner"),
                        strokeWidth = 1.8.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Try another photo",
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        return
    }

    Surface(
        onClick = { viewModel.cycleNextPhoto(word) },
        enabled = !isCycling,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.52f),
        contentColor = Color.White,
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.22f)),
        modifier = modifier.testTag("try_another_photo_button_${word.word}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isCycling) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .size(13.dp)
                        .testTag("try_another_photo_spinner"),
                    strokeWidth = 2.dp
                )
                Text(text = "Fetching...", style = MaterialTheme.typography.labelSmall)
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(text = "Change photo", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordDetailsCard(
    wordEntity: WordEntity,
    onSpeak: () -> Unit,
    onBookmarkToggle: () -> Unit,
    viewModel: DictionaryViewModel
) {
    var showCardInfoDialog by remember { mutableStateOf(false) }
    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
    val displayPhonetic = getPhoneticForAccent(wordEntity.phonetic, selectedAccent)
    val haptics = LocalHapticFeedback.current

    if (showCardInfoDialog) {
        WordCardInfoDialog(
            word = wordEntity,
            viewModel = viewModel,
            onDismiss = { showCardInfoDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_details_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Apple Dictionary Editorial Headword Header (Directly on clean canvas — NO heavy colored box!)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = wordEntity.word.replaceFirstChar { it.uppercase() },
                        style = RedeusExpressiveType.headword.copy(fontSize = 36.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (displayPhonetic.isNotBlank()) {
                            Text(
                                text = displayPhonetic,
                                style = RedeusExpressiveType.phonetic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (wordEntity.partOfSpeech.isNotBlank()) {
                            Text(
                                text = "• ${wordEntity.partOfSpeech}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val speakInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = onSpeak,
                        interactionSource = speakInteraction,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(40.dp)
                            .applePressScale(speakInteraction, 0.92f)
                            .testTag("speak_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Pronounce word",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val bookmarkInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBookmarkToggle()
                        },
                        interactionSource = bookmarkInteraction,
                        shape = CircleShape,
                        color = if (wordEntity.isBookmarked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        contentColor = if (wordEntity.isBookmarked) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .applePressScale(bookmarkInteraction, 0.92f)
                            .testTag("bookmark_toggle")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (wordEntity.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (wordEntity.isBookmarked) "Remove bookmark" else "Bookmark word",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showCardInfoDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("word_card_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Card review history",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CardStatusBadge(wordEntity = wordEntity)
                if (wordEntity.isLeech) {
                    LeechBadge(lapses = wordEntity.lapses)
                }
            }

            WordTagsRow(word = wordEntity, viewModel = viewModel)
        }

        // 2. Unified Inset Grouped Meaning Card (Arabic + English Definition + Optional AI Enrich)
        AppleInsetGroupedCard(cornerRadius = 18.dp) {
            val arabicEnabled by viewModel.arabicTranslationEnabled.collectAsStateWithLifecycle()
            val translations by viewModel.wordArabicTranslations.collectAsStateWithLifecycle()
            val translatingWord by viewModel.translatingArabicWord.collectAsStateWithLifecycle()
            val enrichingWord by viewModel.enrichingWord.collectAsStateWithLifecycle()
            val wordKey = wordEntity.word.trim().lowercase()
            val arabicText = wordEntity.arabicMeaning.ifBlank { translations[wordKey] ?: "" }
            val isTranslating = translatingWord == wordKey
            val isEnriching = enrichingWord == wordKey

            LaunchedEffect(wordKey, arabicEnabled) {
                if (arabicEnabled && arabicText.isBlank()) {
                    viewModel.ensureOfflineArabicLoaded(wordEntity)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (arabicEnabled && arabicText.isNotBlank()) {
                    Text(
                        text = arabicText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("arabic_translation_text")
                    )
                    if (wordEntity.definition.isNotBlank() && wordEntity.definition != arabicText) {
                        AppleInsetDivider(startIndent = 0.dp)
                    }
                }

                if (wordEntity.definition.isNotBlank() && wordEntity.definition != arabicText) {
                    Text(
                        text = wordEntity.definition,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (arabicEnabled && arabicText.isBlank()) {
                    TextButton(
                        onClick = { viewModel.translateWordToArabic(wordEntity) },
                        enabled = !isTranslating,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.testTag("show_arabic_meaning_button")
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Translating...")
                        } else {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Translate to Arabic")
                        }
                    }
                }
            }

            val needsEnrichment = wordEntity.synonyms.isBlank() &&
                wordEntity.mnemonic.isBlank() &&
                wordEntity.imagePrompt.isBlank()
            if (needsEnrichment) {
                AppleInsetDivider(startIndent = 18.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isEnriching) { viewModel.enrichWordWithAi(wordEntity) }
                        .padding(horizontal = 18.dp, vertical = 13.dp)
                        .testTag("enrich_with_ai_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isEnriching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Enriching with AI...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Enrich with AI (Synonyms, Mnemonic & Photo)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 3. Editorial Quoted Usage Example Card
        if (wordEntity.example.isNotBlank()) {
            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EXAMPLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .heightIn(min = 24.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "\u201c${wordEntity.example}\u201d",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ExampleSentenceMiningActionsRow(
                        wordEntity = wordEntity,
                        viewModel = viewModel
                    )
                }
            }
        }

        // 4. Visual Illustration / Photo Card (collapses cleanly when offline)
        if (wordEntity.imagePrompt.isNotEmpty()) {
            val context = LocalContext.current
            val imageLoader = rememberAppImageLoader(context)
            var photoFailed by remember(wordEntity.word, wordEntity.imagePrompt) {
                mutableStateOf(false)
            }
            if (!photoFailed) {
                val isWebPhoto = wordEntity.imagePrompt.startsWith("http")
                val captionText = if (isWebPhoto) {
                    val derived = viewModel.getDerivedQuery(wordEntity.word)
                    if (!derived.isNullOrBlank()) "Photo matching \"$derived\"" else "Photo from Pexels"
                } else {
                    "\"${wordEntity.imagePrompt}\""
                }

                AppleInsetGroupedCard(cornerRadius = 18.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(getAIImageUrl(wordEntity.word, wordEntity.imagePrompt))
                                .crossfade(true)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = "Visual illustration of ${wordEntity.word}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = { photoFailed = true },
                            loading = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            },
                            error = {},
                            success = {
                                SubcomposeAsyncImageContent(
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                                        startY = 140f
                                    )
                                )
                        )

                        TryAnotherPhotoButton(
                            word = wordEntity,
                            viewModel = viewModel,
                            compact = true,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        )

                        Text(
                            text = captionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                        )
                    }
                }
            }
        }

        // 5. Synonyms & Antonyms
        if (wordEntity.synonyms.isNotBlank() || wordEntity.antonyms.isNotBlank()) {
            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (wordEntity.synonyms.isNotBlank()) {
                        Text(
                            text = "SYNONYMS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wordEntity.synonyms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { syn ->
                                Surface(
                                    onClick = { viewModel.performSearch(syn) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = syn,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (wordEntity.antonyms.isNotBlank()) {
                        if (wordEntity.synonyms.isNotBlank()) {
                            AppleInsetDivider(startIndent = 0.dp)
                        }
                        Text(
                            text = "ANTONYMS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wordEntity.antonyms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { ant ->
                                Surface(
                                    onClick = { viewModel.performSearch(ant) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = ant,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Restrained Memory Aid Card
        if (wordEntity.mnemonic.isNotBlank()) {
            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AppleIconBadge(
                        icon = Icons.Default.Lightbulb,
                        tintColor = Color(0xFFFF9F0A),
                        size = 32.dp,
                        iconSize = 18.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MEMORY AID",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = Color(0xFFFF9F0A)
                        )
                        Text(
                            text = wordEntity.mnemonic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    onOpenApiSettings: (() -> Unit)? = null
) {
    val isOffline = remember(message) {
        val lower = message.lowercase()
        lower.contains("offline") ||
            lower.contains("network") ||
            lower.contains("internet") ||
            lower.contains("unable to resolve host") ||
            lower.contains("timeout")
    }

    AppleInsetGroupedCard(
        modifier = Modifier.testTag("error_state_card"),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppleIconBadge(
                icon = if (isOffline) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                tintColor = MaterialTheme.colorScheme.error,
                size = 44.dp,
                iconSize = 24.dp
            )
            Text(
                text = if (isOffline) "You Appear to Be Offline" else "Word Not Found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Try Again")
                }
                if (onOpenApiSettings != null && !isOffline) {
                    OutlinedButton(
                        onClick = onOpenApiSettings,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("error_open_api_settings_button")
                    ) {
                        Text("API Settings")
                    }
                }
            }
        }
    }
}
