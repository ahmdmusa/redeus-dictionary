package com.example.ui.screens.study

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.ActiveStudySession
import com.example.domain.CardStudyBucket
import com.example.domain.SessionSummaryData
import com.example.domain.StudyQueuePlanner
import com.example.domain.StudyRecallMode
import com.example.ui.DictionaryViewModel
import com.example.ui.components.*
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusMotion

/**
 * Full-Screen Focus Study Modal (hides TopBar & TabBar per Apple HIG tab-bars.md modal guidance).
 * Hosts the active FSRS session and transitions smoothly to [SessionSummaryScreen] on completion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionModal(
    session: ActiveStudySession,
    summary: SessionSummaryData?,
    viewModel: DictionaryViewModel,
    onClose: () -> Unit,
    onStartQuickQuiz: () -> Unit
) {
    BackHandler { onClose() }

    if (summary != null || session.isFinished) {
        val safeSummary = summary ?: session.toSummary(tomorrowReviews = 0, tomorrowNew = 0)
        SessionSummaryScreen(
            summary = safeSummary,
            viewModel = viewModel,
            onDone = onClose,
            onStudyExtraNew = {
                viewModel.startUnifiedStudySession(extraNewBonus = 5)
            },
            onQuickQuiz = {
                onClose()
                onStartQuickQuiz()
            }
        )
        return
    }

    val currentCard = session.currentCard ?: return
    val stepKey = "${session.sessionId}_${session.stepIndex}_${currentCard.word}"
    // Keying flipped on stepKey eliminates the 1-frame answer spoiler flash!
    var flipped by remember(stepKey) { mutableStateOf(false) }
    var showAiHelper by remember { mutableStateOf(false) }
    var showCardInfo by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    val autoPronounce by viewModel.autoPronounceEnabled.collectAsStateWithLifecycle()
    val arabicEnabled by viewModel.arabicTranslationEnabled.collectAsStateWithLifecycle()
    val isRatingInProgress by viewModel.isRatingInProgress.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotionEnabled.current
    val haptics = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f

    val intervalPreviews = remember(currentCard.word, currentCard.repetitions, currentCard.interval, currentCard.fsrsStability) {
        viewModel.previewReviewIntervals(currentCard)
    }

    LaunchedEffect(stepKey) {
        if (arabicEnabled) {
            viewModel.ensureOfflineArabicLoaded(currentCard)
        }
        if (autoPronounce && session.recallMode == StudyRecallMode.STANDARD) {
            viewModel.speak(currentCard.word)
        }
    }

    if (showAiHelper) {
        WordAiHelperSheet(
            wordEntity = currentCard,
            viewModel = viewModel,
            onDismiss = { showAiHelper = false }
        )
    }

    if (showCardInfo) {
        WordCardInfoDialog(
            word = currentCard,
            viewModel = viewModel,
            onDismiss = { showCardInfo = false }
        )
    }

    if (showOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Session Options",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "RECALL DIRECTION",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppleSegmentedControl(
                    items = listOf(
                        "English → Arabic" to "recall_mode_standard",
                        "Arabic → English" to "recall_mode_reverse"
                    ),
                    selectedIndex = if (session.recallMode == StudyRecallMode.REVERSE) 1 else 0,
                    onSelect = { idx ->
                        viewModel.setSessionRecallMode(
                            if (idx == 1) StudyRecallMode.REVERSE else StudyRecallMode.STANDARD
                        )
                        flipped = false
                    }
                )

                AppleInsetGroupedCard(cornerRadius = 16.dp) {
                    Surface(
                        onClick = {
                            showOptionsSheet = false
                            showAiHelper = true
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFBF5AF2))
                            Text("Open Word AI Assistant", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    AppleInsetDivider(startIndent = 52.dp)
                    Surface(
                        onClick = {
                            showOptionsSheet = false
                            showCardInfo = true
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Card FSRS Info & History", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    AppleInsetDivider(startIndent = 52.dp)
                    Surface(
                        onClick = {
                            showOptionsSheet = false
                            viewModel.burySessionCard()
                        },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF9F0A))
                            Text("Bury Card Until Tomorrow", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = session.progressFraction,
        animationSpec = if (reducedMotion) {
            RedeusMotion.spec(true, RedeusMotion.DurationShort)
        } else {
            spring(dampingRatio = 1.0f, stiffness = 400f)
        },
        label = "sessionProgress"
    )

    val bucket = StudyQueuePlanner.classifyCard(currentCard)
    val (bucketLabel, bucketColor) = when (bucket) {
        CardStudyBucket.NEW -> "New" to Color(0xFF0A84FF)
        CardStudyBucket.LEARNING -> "Learning" to Color(0xFFFF9F0A)
        CardStudyBucket.REVIEW -> "Review" to Color(0xFF30D158)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("study_session_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 580.dp)
                .align(Alignment.Center)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Focus Bar: Close (48dp) | Determinate Progress + Counter | Undo (48dp) | More (48dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("study_session_close_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit session",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${session.completedCount} / ${session.effectiveTotal}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "● $bucketLabel",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = bucketColor
                        )
                    }
                }

                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.undoLastSessionRating()
                    },
                    enabled = session.canUndo && !isRatingInProgress,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("study_session_undo_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo last rating",
                            tint = if (session.canUndo) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
                            },
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                Surface(
                    onClick = { showOptionsSheet = true },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("study_session_more_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "Session options",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            // Unified 3D Spring & 1:1 Swipe Study Card (keyed on stepKey for 0-frame spoiler guarantee)
            key(stepKey) {
                StudyFlashcard(
                    card = currentCard,
                    stepKey = stepKey,
                    flipped = flipped,
                    recallMode = session.recallMode,
                    viewModel = viewModel,
                    onToggleFlip = { flipped = !flipped },
                    onSwipeRate = { rating -> viewModel.rateSessionCard(rating) },
                    onOpenAiHelper = { showAiHelper = true },
                    onOpenCardInfo = { showCardInfo = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Fixed Bottom Action Bar (Show Answer -> 4 FSRS Rating Buttons)
            AnimatedContent(
                targetState = flipped,
                transitionSpec = {
                    fadeIn(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationShort)) togetherWith
                        fadeOut(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationShort))
                },
                label = "sessionRatingBar"
            ) { isFlipped ->
                if (!isFlipped) {
                    val revealInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            flipped = true
                        },
                        interactionSource = revealInteraction,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .applePressScale(revealInteraction, 0.97f)
                            .testTag("flip_card_button")
                    ) {
                        Text(
                            text = "Show Answer",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Swipe ← Again   ·   Good →",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val againAccent = if (isDark) Color(0xFFFF5A5F) else Color(0xFFD70015)
                        val hardAccent = if (isDark) Color(0xFFFF9F0A) else Color(0xFFC95100)
                        val goodAccent = if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
                        val easyAccent = MaterialTheme.colorScheme.primary

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SessionRatingButton(
                                text = "Again",
                                intervalHint = intervalPreviews["Again"] ?: "1m",
                                accentColor = againAccent,
                                enabled = !isRatingInProgress,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.rateSessionCard("Again")
                                },
                                modifier = Modifier.weight(1f).testTag("rate_again_button")
                            )
                            SessionRatingButton(
                                text = "Hard",
                                intervalHint = intervalPreviews["Hard"] ?: "6m",
                                accentColor = hardAccent,
                                enabled = !isRatingInProgress,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.rateSessionCard("Hard")
                                },
                                modifier = Modifier.weight(1f).testTag("rate_hard_button")
                            )
                            SessionRatingButton(
                                text = "Good",
                                intervalHint = intervalPreviews["Good"] ?: "1d",
                                accentColor = goodAccent,
                                enabled = !isRatingInProgress,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.rateSessionCard("Good")
                                },
                                modifier = Modifier.weight(1f).testTag("rate_good_button")
                            )
                            SessionRatingButton(
                                text = "Easy",
                                intervalHint = intervalPreviews["Easy"] ?: "4d",
                                accentColor = easyAccent,
                                enabled = !isRatingInProgress,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.rateSessionCard("Easy")
                                },
                                modifier = Modifier.weight(1f).testTag("rate_easy_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRatingButton(
    text: String,
    intervalHint: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(15.dp),
        color = accentColor.copy(alpha = 0.14f),
        contentColor = accentColor,
        border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.34f)),
        modifier = modifier
            .height(58.dp)
            .applePressScale(interactionSource, 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
            )
            Text(
                text = intervalHint,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontFeatureSettings = "tnum"
                ),
                color = accentColor.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun SessionSummaryScreen(
    summary: SessionSummaryData,
    viewModel: DictionaryViewModel,
    onDone: () -> Unit,
    onStudyExtraNew: () -> Unit,
    onQuickQuiz: () -> Unit
) {
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val dailyGoal by viewModel.dailyGoalTarget.collectAsStateWithLifecycle()
    val detailedStats by viewModel.detailedSrsStats.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotionEnabled.current

    val todayDone = detailedStats.streakHistoryLast30Days.lastOrNull { it.isToday }?.count ?: summary.cardsStudied
    val safeGoal = dailyGoal.coerceAtLeast(1)
    val targetProgress = (todayDone.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
    val animatedRing by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (reducedMotion) RedeusMotion.spec(true) else spring(dampingRatio = 0.85f, stiffness = 200f),
        label = "summaryGoalRing"
    )
    val minutes = summary.durationSeconds / 60
    val seconds = summary.durationSeconds % 60
    val formattedTime = String.format("%d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("session_summary_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Animated Daily Goal Ring
            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFF30D158),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedRing,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$todayDone",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ $safeGoal today",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Session Complete",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "🔥 $currentStreak-day streak",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFFF9F0A)
                )
            }

            // 3 Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatTile(value = "${summary.cardsStudied}", label = "Cards", modifier = Modifier.weight(1f))
                SummaryStatTile(value = "${summary.accuracyPercent}%", label = "Accuracy", modifier = Modifier.weight(1f))
                SummaryStatTile(value = formattedTime, label = "Time", modifier = Modifier.weight(1f))
            }

            // Hardest Words Recap (any card rated Again during this session)
            if (summary.hardestWords.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppleSectionHeader(title = "Hardest Words Recap")
                    AppleInsetGroupedCard(cornerRadius = 18.dp) {
                        summary.hardestWords.forEachIndexed { idx, hardWord ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hardWord.word.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            textDirection = TextDirection.Ltr
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (hardWord.arabicMeaning.isNotBlank()) {
                                        Text(
                                            text = hardWord.arabicMeaning,
                                            style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Rtl),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.speak(hardWord.word) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Pronounce ${hardWord.word}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (idx < summary.hardestWords.lastIndex) {
                                AppleInsetDivider(startIndent = 16.dp)
                            }
                        }
                    }
                }
            }

            Text(
                text = "Tomorrow: ${summary.tomorrowReviews} reviews · ${summary.tomorrowNew} new",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Primary Done CTA
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("session_summary_done_button")
            ) {
                Text(
                    text = "Back to Today",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onStudyExtraNew,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("+5 Extra New")
                }
                OutlinedButton(
                    onClick = onQuickQuiz,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Quick Quiz")
                }
            }
        }
    }
}

@Composable
private fun SummaryStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
