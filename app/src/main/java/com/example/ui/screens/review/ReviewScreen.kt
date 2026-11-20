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
    innerPadding: PaddingValues
) {
    val reviewQueue by viewModel.reviewQueue.collectAsStateWithLifecycle()
    val bookmarkedWords by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val cardsDueTodayCount by viewModel.cardsDueTodayCount.collectAsStateWithLifecycle()
    val selectedReviewDeck by viewModel.selectedReviewDeck.collectAsStateWithLifecycle()
    val availableTags by viewModel.allAvailableTags.collectAsStateWithLifecycle()
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (availableTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(bottom = 12.dp)
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
                    initiallyExpanded = true
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
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
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = "All caught up",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "You have completed all vocabulary reviews scheduled for today. Return tomorrow for your next spaced-repetition session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.startReviewSession() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("start_review_button")
                        ) {
                            Text("Practice all (${bookmarkedWords.size} cards)")
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

            if (showReviewCardInfoDialog) {
                WordCardInfoDialog(
                    word = currentCard,
                    viewModel = viewModel,
                    onDismiss = { showReviewCardInfoDialog = false }
                )
            }

            LaunchedEffect(currentCard.word) {
                flipped = false
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active review",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "${reviewQueue.size} left",
                            style = MaterialTheme.typography.labelLarge,
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
                            tonalElevation = 2.dp
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
                                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                            .testTag("active_review_card_info_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Card review history"
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .verticalScroll(rememberScrollState())
                                        .padding(24.dp),
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
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = "Tap card to reveal details",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                            ) {
                                if (currentCard.imagePrompt.isNotEmpty()) {
                                    val context = LocalContext.current
                                    val imageLoader = rememberAppImageLoader(context)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
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
                                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    FilledTonalIconButton(
                                        onClick = { viewModel.speak(currentCard.word) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("speak_review_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Pronounce word"
                                        )
                                    }
                                }

                                val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
                                val displayPhonetic = getPhoneticForAccent(currentCard.phonetic, selectedAccent)
                                Text(
                                    text = "$displayPhonetic ($selectedAccent)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = currentCard.definition,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                if (currentCard.example.isNotBlank()) {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "\u201c${currentCard.example}\u201d",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Rating / Reveal Controls
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("flip_card_button")
                        ) {
                            Text("Reveal answer")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ReviewRatingButton(
                                text = "Again",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Again") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_again_button")
                            )
                            ReviewRatingButton(
                                text = "Hard",
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Hard") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_hard_button")
                            )
                            ReviewRatingButton(
                                text = "Good",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Good") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("rate_good_button")
                            )
                            ReviewRatingButton(
                                text = "Easy",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = 52.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}
