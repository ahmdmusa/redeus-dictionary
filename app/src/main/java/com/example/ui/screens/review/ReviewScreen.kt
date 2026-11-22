package com.example.ui.screens.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.*
import com.example.ui.components.CardStatusBadge
import com.example.ui.components.DailyStreakAndHeatmapBanner
import com.example.ui.components.LeechBadge
import com.example.ui.components.WordCardInfoDialog
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedReviewDeck(deck) },
                        label = { Text(if (deck == "ALL") "All decks" else "#$deck") },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("review_deck_chip_$deck")
                    )
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

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (bookmarkedWords.isEmpty()) Icons.Default.School else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Text(
                            text = if (bookmarkedWords.isEmpty()) "Start your Anki FSRS Deck" else "All caught up",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (bookmarkedWords.isEmpty()) {
                                "Add 10–20 words from the 6 Essential Books or 3 IELTS Books to begin smart spaced-repetition study."
                            } else {
                                "You have completed all vocabulary reviews scheduled for today. Return tomorrow for your next spaced-repetition session."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (bookmarkedWords.isEmpty()) {
                            Button(
                                onClick = onNavigateToCourses,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("browse_courses_from_review_button")
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Browse 6 Books & IELTS")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startReviewSession() },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("start_review_button")
                            ) {
                                Text("Practice all (${bookmarkedWords.size} cards)")
                            }
                            OutlinedButton(
                                onClick = onNavigateToCourses,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Refined Apple-style session header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (selectedReviewDeck.equals("ALL", ignoreCase = true)) "Active Review" else "#$selectedReviewDeck",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "${reviewQueue.size} left",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Spaced-Repetition Card
                val rotation by animateFloatAsState(
                    targetValue = if (flipped) 180f else 0f,
                    animationSpec = RedeusMotion.spec(reducedMotion, RedeusMotion.DurationLong),
                    label = "reviewCardFlip"
                )

                val cardBorder = BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            rotationY = if (reducedMotion) 0f else rotation
                            cameraDistance = 12f * density
                        }
                        .clip(MaterialTheme.shapes.large)
                        .clickable { flipped = !flipped },
                    contentAlignment = Alignment.Center
                ) {
                    val showFront = if (reducedMotion) !flipped else rotation <= 90f
                    if (showFront) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = cardBorder,
                            tonalElevation = 0.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
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
                                            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                                            .testTag("active_review_card_info_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Card review history",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 24.dp, vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = currentCard.word.replaceFirstChar { it.uppercase() },
                                        style = RedeusExpressiveType.headword,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    val posAbbr = formatPartOfSpeechAbbreviation(currentCard.partOfSpeech)
                                    if (posAbbr.isNotBlank()) {
                                        Text(
                                            text = "($posAbbr)",
                                            style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalIconButton(
                                        onClick = { viewModel.speak(currentCard.word) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("speak_review_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Pronounce word"
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = "Tap card to reveal answer",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationY = if (reducedMotion) 0f else 180f
                                },
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = cardBorder,
                            tonalElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top)
                            ) {
                                if (currentCard.imagePrompt.isNotEmpty()) {
                                    val context = LocalContext.current
                                    val imageLoader = rememberAppImageLoader(context)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(164.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                                            loading = {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                                }
                                            },
                                            error = {
                                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.ImageNotSupported,
                                                        contentDescription = "Image unavailable",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = currentCard.word.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    FilledTonalIconButton(
                                        onClick = { viewModel.speak(currentCard.word) },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("speak_review_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Pronounce word",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
                                val displayPhonetic = getPhoneticForAccent(currentCard.phonetic, selectedAccent)
                                if (displayPhonetic.isNotBlank()) {
                                    Text(
                                        text = displayPhonetic,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                val arabicMeaning = currentCard.arabicMeaning.ifBlank {
                                    wordArabicTranslations[currentCard.word.trim().lowercase()] ?: ""
                                }
                                if (arabicEnabled && arabicMeaning.isNotBlank()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Text(
                                            text = arabicMeaning,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                                .testTag("review_arabic_meaning_text")
                                        )
                                    }
                                }

                                if (currentCard.definition.isNotBlank() && currentCard.definition != arabicMeaning) {
                                    Text(
                                        text = currentCard.definition,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                if (currentCard.example.isNotBlank()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "\u201c${currentCard.example}\u201d",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
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

                // Apple HIG + Anki FSRS Rating / Reveal Controls
                AnimatedContent(
                    targetState = flipped,
                    transitionSpec = {
                        fadeIn(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationShort)) togetherWith
                            fadeOut(RedeusMotion.spec(reducedMotion, RedeusMotion.DurationShort))
                    },
                    label = "reviewInteractionArea"
                ) { isFlipped ->
                    if (!isFlipped) {
                        Button(
                            onClick = { flipped = true },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("flip_card_button")
                        ) {
                            Text(
                                text = "Reveal answer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
                        val againAccent = if (isDark) Color(0xFFFF6961) else Color(0xFFD70015)
                        val hardAccent = if (isDark) Color(0xFFFFB340) else Color(0xFFC95100)
                        val goodAccent = if (isDark) Color(0xFF32D74B) else Color(0xFF1B8A3A)
                        val easyAccent = if (isDark) Color(0xFF409CFF) else Color(0xFF0066CC)

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
                                onClick = { viewModel.rateCard(currentCard, "Again") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_again_button")
                            )
                            ReviewRatingButton(
                                text = "Hard",
                                intervalHint = intervalPreviews["Hard"] ?: "6m",
                                accentColor = hardAccent,
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Hard") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_hard_button")
                            )
                            ReviewRatingButton(
                                text = "Good",
                                intervalHint = intervalPreviews["Good"] ?: "1d",
                                accentColor = goodAccent,
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Good") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_good_button")
                            )
                            ReviewRatingButton(
                                text = "Easy",
                                intervalHint = intervalPreviews["Easy"] ?: "4d",
                                accentColor = easyAccent,
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Easy") },
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

@Composable
private fun ReviewRatingButton(
    text: String,
    intervalHint: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        color = accentColor.copy(alpha = 0.14f),
        contentColor = accentColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = intervalHint,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}
