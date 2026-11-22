package com.example.ui.screens.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.screens.search.TryAnotherPhotoButton
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusExpressiveType
import com.example.ui.theme.RedeusMotion

@Composable
fun ReviewTabContent(
    viewModel: DictionaryViewModel,
    innerPadding: PaddingValues,
    onNavigateToCourses: () -> Unit = {}
) {
    val reviewQueue by viewModel.reviewQueue.collectAsStateWithLifecycle()
    val bookmarkedWords by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val cardsDueTodayCount by viewModel.cardsDueTodayCount.collectAsStateWithLifecycle()
    val selectedReviewDeck by viewModel.selectedReviewDeck.collectAsStateWithLifecycle()
    val availableTags by viewModel.allAvailableTags.collectAsStateWithLifecycle()
    val arabicEnabled by viewModel.arabicTranslationEnabled.collectAsStateWithLifecycle()
    val wordArabicTranslations by viewModel.wordArabicTranslations.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotionEnabled.current
    val haptics = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f)

    LaunchedEffect(cardsDueTodayCount) {
        if (reviewQueue.isEmpty() && cardsDueTodayCount > 0) {
            viewModel.startReviewSession()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (availableTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(bottom = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (listOf("ALL") + availableTags).forEach { deck ->
                    val isSelected = selectedReviewDeck.equals(deck, ignoreCase = true)
                    val chipInteraction = remember { MutableInteractionSource() }
                    Surface(
                        modifier = Modifier
                            .applePressScale(chipInteraction, 0.96f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = chipInteraction,
                                indication = null,
                                onClick = { viewModel.setSelectedReviewDeck(deck) }
                            )
                            .testTag("review_deck_chip_$deck"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = if (isSelected) null else BorderStroke(0.5.dp, cardBorderColor)
                    ) {
                        Text(
                            text = if (deck == "ALL") "All Decks" else deck,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        if (reviewQueue.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DailyStreakAndHeatmapBanner(
                    viewModel = viewModel,
                    initiallyExpanded = false
                )

                AppleInsetGroupedCard(cornerRadius = 22.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AppleIconBadge(
                            icon = if (bookmarkedWords.isEmpty()) Icons.Default.School else Icons.Default.CheckCircle,
                            tintColor = if (bookmarkedWords.isEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF30D158),
                            size = 56.dp,
                            iconSize = 30.dp
                        )
                        Text(
                            text = if (bookmarkedWords.isEmpty()) "Build Your Study Deck" else "All Caught Up",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (bookmarkedWords.isEmpty()) {
                                "Pick a unit from the 6 Essential Books or 3 IELTS Books and add 10–20 cards to start spaced repetition."
                            } else {
                                "You finished every card due today. Come back tomorrow or add new words from Courses."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (bookmarkedWords.isEmpty()) {
                            Button(
                                onClick = onNavigateToCourses,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("browse_courses_from_review_button")
                            ) {
                                Text("Browse 6 Books & IELTS", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startReviewSession() },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_review_button")
                            ) {
                                Text("Review All (${bookmarkedWords.size} Cards)", style = MaterialTheme.typography.labelLarge)
                            }
                            TextButton(onClick = onNavigateToCourses) {
                                Text("Add more words from Books & IELTS")
                            }
                        }
                    }
                }
            }
        } else {
            val currentCard = reviewQueue[0]
            var flipped by remember { mutableStateOf(false) }
            var showReviewCardInfoDialog by remember { mutableStateOf(false) }
            val autoPronounce by viewModel.autoPronounceEnabled.collectAsStateWithLifecycle()
            val isRatingInProgress by viewModel.isRatingInProgress.collectAsStateWithLifecycle()
            val intervalPreviews = remember(currentCard.word, currentCard.repetitions, currentCard.interval, currentCard.fsrsStability) {
                viewModel.previewReviewIntervals(currentCard)
            }

            if (showReviewCardInfoDialog) {
                WordCardInfoDialog(
                    word = currentCard,
                    viewModel = viewModel,
                    onDismiss = { showReviewCardInfoDialog = false }
                )
            }

            LaunchedEffect(currentCard.word) {
                flipped = false
                if (arabicEnabled) {
                    viewModel.ensureOfflineArabicLoaded(currentCard)
                }
                if (autoPronounce) {
                    viewModel.speak(currentCard.word)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Minimal Apple-style progress bar + calm counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedReviewDeck.equals("ALL", ignoreCase = true)) {
                            "Spaced Repetition"
                        } else {
                            selectedReviewDeck
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${reviewQueue.size} remaining",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Interruptible Spring 3D Study Index Card (apple-fluid-design §3 & §4)
                val rotation by animateFloatAsState(
                    targetValue = if (flipped) 180f else 0f,
                    animationSpec = if (reducedMotion) {
                        RedeusMotion.spec(true, RedeusMotion.DurationShort)
                    } else {
                        spring(dampingRatio = 0.85f, stiffness = 320f)
                    },
                    label = "reviewCardFlip"
                )

                val cardInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationY = if (reducedMotion) 0f else rotation
                            cameraDistance = 14f * density
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            flipped = !flipped
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val showFront = if (reducedMotion) !flipped else rotation <= 90f
                    if (showFront) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.5.dp, cardBorderColor),
                            tonalElevation = 0.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (currentCard.isLeech) {
                                        LeechBadge(lapses = currentCard.lapses)
                                    }
                                    CardStatusBadge(wordEntity = currentCard)
                                    IconButton(
                                        onClick = { showReviewCardInfoDialog = true },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("active_review_card_info_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Card review history",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 28.dp, vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = currentCard.word.replaceFirstChar { it.uppercase() },
                                        style = RedeusExpressiveType.headword.copy(fontSize = 36.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    val posAbbr = formatPartOfSpeechAbbreviation(currentCard.partOfSpeech)
                                    if (posAbbr.isNotBlank()) {
                                        Text(
                                            text = posAbbr,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val speakInteraction = remember { MutableInteractionSource() }
                                    Surface(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .applePressScale(speakInteraction, 0.92f)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = speakInteraction,
                                                indication = null,
                                                onClick = { viewModel.speak(currentCard.word) }
                                            )
                                            .testTag("speak_review_button"),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Pronounce word",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Tap card to reveal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 18.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationY = if (reducedMotion) 0f else 180f
                                },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.5.dp, cardBorderColor),
                            tonalElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
                            ) {
                                if (currentCard.imagePrompt.isNotEmpty()) {
                                    val context = LocalContext.current
                                    val imageLoader = rememberAppImageLoader(context)
                                    var photoFailed by remember(currentCard.word, currentCard.imagePrompt) {
                                        mutableStateOf(false)
                                    }
                                    if (!photoFailed) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(152.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(getAIImageUrl(currentCard.word, currentCard.imagePrompt))
                                                    .crossfade(true)
                                                    .build(),
                                                imageLoader = imageLoader,
                                                contentDescription = "Illustration for ${currentCard.word}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                onError = { photoFailed = true },
                                                loading = {
                                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
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
                                            TryAnotherPhotoButton(
                                                word = currentCard,
                                                viewModel = viewModel,
                                                compact = true,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = currentCard.word.replaceFirstChar { it.uppercase() },
                                        style = TextStyleEditorialWord,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val backSpeakInteraction = remember { MutableInteractionSource() }
                                    Surface(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .applePressScale(backSpeakInteraction, 0.92f)
                                            .clip(CircleShape)
                                            .clickable(
                                                interactionSource = backSpeakInteraction,
                                                indication = null,
                                                onClick = { viewModel.speak(currentCard.word) }
                                            )
                                            .testTag("speak_review_back_button"),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Pronounce word",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
                                val displayPhonetic = getPhoneticForAccent(currentCard.phonetic, selectedAccent)
                                if (displayPhonetic.isNotBlank()) {
                                    Text(
                                        text = displayPhonetic,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                val arabicMeaning = currentCard.arabicMeaning.ifBlank {
                                    wordArabicTranslations[currentCard.word.trim().lowercase()] ?: ""
                                }
                                if (arabicEnabled && arabicMeaning.isNotBlank()) {
                                    Text(
                                        text = arabicMeaning,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 21.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .padding(vertical = 2.dp)
                                            .testTag("review_arabic_meaning_text")
                                    )
                                }

                                if (currentCard.definition.isNotBlank() && currentCard.definition != arabicMeaning) {
                                    Text(
                                        text = currentCard.definition,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                if (currentCard.example.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "\u201c${currentCard.example}\u201d",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontStyle = FontStyle.Italic,
                                                lineHeight = 21.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Apple HIG + Fluid Tactile FSRS Rating Bar
                AnimatedContent(
                    targetState = flipped,
                    transitionSpec = {
                        fadeIn(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationShort)) togetherWith
                            fadeOut(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationShort))
                    },
                    label = "reviewInteractionArea"
                ) { isFlipped ->
                    if (!isFlipped) {
                        val revealInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                flipped = true
                            },
                            interactionSource = revealInteraction,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .applePressScale(revealInteraction, 0.97f)
                                .testTag("flip_card_button")
                        ) {
                            Text(
                                text = "Show Answer",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        val againAccent = if (isDark) Color(0xFFFF453A) else Color(0xFFD70015)
                        val hardAccent = if (isDark) Color(0xFFFF9F0A) else Color(0xFFC95100)
                        val goodAccent = if (isDark) Color(0xFF30D158) else Color(0xFF1B8A3A)
                        val easyAccent = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReviewRatingButton(
                                text = "Again",
                                intervalHint = intervalPreviews["Again"] ?: "1m",
                                accentColor = againAccent,
                                enabled = !isRatingInProgress,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.rateCard(currentCard, "Again")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_again_button")
                            )
                            ReviewRatingButton(
                                text = "Hard",
                                intervalHint = intervalPreviews["Hard"] ?: "6m",
                                accentColor = hardAccent,
                                enabled = !isRatingInProgress,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.rateCard(currentCard, "Hard")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_hard_button")
                            )
                            ReviewRatingButton(
                                text = "Good",
                                intervalHint = intervalPreviews["Good"] ?: "1d",
                                accentColor = goodAccent,
                                enabled = !isRatingInProgress,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.rateCard(currentCard, "Good")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_good_button")
                            )
                            ReviewRatingButton(
                                text = "Easy",
                                intervalHint = intervalPreviews["Easy"] ?: "4d",
                                accentColor = easyAccent,
                                enabled = !isRatingInProgress,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.rateCard(currentCard, "Easy")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_easy_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

private val TextStyleEditorialWord = androidx.compose.ui.text.TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.4).sp
)

@Composable
private fun ReviewRatingButton(
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
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.13f),
        contentColor = accentColor,
        border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.30f)),
        modifier = modifier
            .height(58.dp)
            .applePressScale(interactionSource, 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = intervalHint,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = accentColor.copy(alpha = 0.80f),
                maxLines = 1
            )
        }
    }
}
