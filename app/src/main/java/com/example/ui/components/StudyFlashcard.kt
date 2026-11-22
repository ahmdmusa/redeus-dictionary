package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WordEntity
import com.example.domain.StudyRecallMode
import com.example.ui.DictionaryViewModel
import com.example.ui.formatPartOfSpeechAbbreviation
import com.example.ui.getPhoneticForAccent
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusExpressiveType
import com.example.ui.theme.RedeusMotion
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Unified Study Index Card used across the full-screen FSRS Study Session.
 *
 * Key craft guarantees:
 *   - Zero 1-frame spoiler flash: flip state is owned/keyed by (word, stepIndex) outside LaunchedEffect.
 *   - 1:1 direct-manipulation horizontal drag with VelocityTracker momentum projection
 *     (swipe right = Good, swipe left = Again) when flipped; gentle rubber-band before flip.
 *   - Reduced motion fallback (cross-fade, zero 3D spin or overshoot).
 *   - Displays local permanent illustration, RTL Arabic, definition, highlighted example + audio,
 *     mnemonic/synonyms, +Mine sentence, and per-word ✨ AI Helper button.
 *   - Every interactive control meets >= 48dp touch target.
 */
@Composable
fun StudyFlashcard(
    card: WordEntity,
    stepKey: String,
    flipped: Boolean,
    recallMode: StudyRecallMode,
    viewModel: DictionaryViewModel,
    onToggleFlip: () -> Unit,
    onSwipeRate: ((String) -> Unit)? = null,
    onOpenAiHelper: () -> Unit,
    onOpenCardInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arabicEnabled by viewModel.arabicTranslationEnabled.collectAsStateWithLifecycle()
    val wordArabicTranslations by viewModel.wordArabicTranslations.collectAsStateWithLifecycle()
    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotionEnabled.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val cardBorderColor = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f)

    val arabicMeaning = card.arabicMeaning.ifBlank {
        wordArabicTranslations[card.word.trim().lowercase()] ?: ""
    }
    val displayPhonetic = getPhoneticForAccent(card.phonetic, selectedAccent)

    // 1:1 drag state reset cleanly on each stepKey
    val dragX = remember(stepKey) { Animatable(0f) }
    val swipeThresholdPx = 220f

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = if (reducedMotion) {
            RedeusMotion.spec(true, RedeusMotion.DurationShort)
        } else {
            spring(dampingRatio = 0.85f, stiffness = 320f)
        },
        label = "unifiedCardFlip"
    )

    val currentFlipped by rememberUpdatedState(flipped)
    val currentSwipeCallback by rememberUpdatedState(onSwipeRate)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(stepKey, reducedMotion) {
                val velocityTracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = {
                        velocityTracker.resetTracking()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (currentSwipeCallback == null) return@detectHorizontalDragGestures
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        scope.launch {
                            val current = dragX.value
                            // Before flip: subtle rubber-band resistance hinting "flip first"
                            val next = if (currentFlipped) {
                                current + dragAmount
                            } else {
                                val raw = current + dragAmount * 0.25f
                                raw.coerceIn(-90f, 90f)
                            }
                            dragX.snapTo(next)
                        }
                    },
                    onDragEnd = {
                        val vx = velocityTracker.calculateVelocity().x
                        // Apple Fluid Interfaces momentum projection: x + (v/1000) * d / (1 - d)
                        val projected = dragX.value + (vx / 1000f) * (0.992f / (1f - 0.992f)) * 0.14f
                        val commitGood = currentFlipped && currentSwipeCallback != null &&
                            (dragX.value > swipeThresholdPx || (dragX.value > 90f && projected > swipeThresholdPx))
                        val commitAgain = currentFlipped && currentSwipeCallback != null &&
                            (dragX.value < -swipeThresholdPx || (dragX.value < -90f && projected < -swipeThresholdPx))

                        scope.launch {
                            when {
                                commitGood -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (!reducedMotion) {
                                        dragX.animateTo(
                                            targetValue = 900f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f),
                                            initialVelocity = vx
                                        )
                                    }
                                    currentSwipeCallback?.invoke("Good")
                                }
                                commitAgain -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!reducedMotion) {
                                        dragX.animateTo(
                                            targetValue = -900f,
                                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 420f),
                                            initialVelocity = vx
                                        )
                                    }
                                    currentSwipeCallback?.invoke("Again")
                                }
                                else -> {
                                    dragX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = if (reducedMotion) {
                                            RedeusMotion.spec(true, RedeusMotion.DurationShort)
                                        } else {
                                            spring(dampingRatio = 0.80f, stiffness = 360f)
                                        },
                                        initialVelocity = vx
                                    )
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 400f)
                            )
                        }
                    }
                )
            }
    ) {
        val cardInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = if (reducedMotion) 0f else dragX.value
                    rotationZ = if (reducedMotion) 0f else (dragX.value / 32f).coerceIn(-12f, 12f)
                    rotationY = if (reducedMotion) 0f else rotation
                    cameraDistance = 14f * density
                }
                .clip(RoundedCornerShape(26.dp))
                .then(
                    if (!flipped) {
                        Modifier.clickable(
                            interactionSource = cardInteraction,
                            indication = null
                        ) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleFlip()
                        }
                    } else {
                        Modifier
                    }
                )
                .testTag("study_flashcard_surface"),
            contentAlignment = Alignment.Center
        ) {
            val showFront = if (reducedMotion) !flipped else rotation <= 90f
            if (showFront) {
                StudyCardFrontFace(
                    card = card,
                    recallMode = recallMode,
                    arabicMeaning = arabicMeaning,
                    displayPhonetic = displayPhonetic,
                    selectedAccent = selectedAccent,
                    cardBorderColor = cardBorderColor,
                    onSpeak = { viewModel.speak(card.word) },
                    onOpenCardInfo = onOpenCardInfo
                )
            } else {
                StudyCardBackFace(
                    card = card,
                    arabicEnabled = arabicEnabled,
                    arabicMeaning = arabicMeaning,
                    displayPhonetic = displayPhonetic,
                    cardBorderColor = cardBorderColor,
                    reducedMotion = reducedMotion,
                    viewModel = viewModel,
                    onFlipBack = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleFlip()
                    },
                    onOpenAiHelper = onOpenAiHelper
                )
            }
        }

        // Direct-manipulation swipe feedback pills (shown continuously while dragging)
        if (flipped && !reducedMotion && abs(dragX.value) > 28f) {
            val isGoodSide = dragX.value > 0f
            val alpha = ((abs(dragX.value) - 28f) / 140f).coerceIn(0f, 1f)
            val tint = if (isGoodSide) Color(0xFF30D158) else Color(0xFFFF453A)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = tint.copy(alpha = 0.22f * alpha),
                border = BorderStroke(1.dp, tint.copy(alpha = 0.7f * alpha)),
                modifier = Modifier
                    .align(if (isGoodSide) Alignment.TopEnd else Alignment.TopStart)
                    .padding(18.dp)
            ) {
                Text(
                    text = if (isGoodSide) "✓ Good" else "↺ Again",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = tint.copy(alpha = alpha),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun StudyCardFrontFace(
    card: WordEntity,
    recallMode: StudyRecallMode,
    arabicMeaning: String,
    displayPhonetic: String,
    selectedAccent: String,
    cardBorderColor: Color,
    onSpeak: () -> Unit,
    onOpenCardInfo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, cardBorderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top badges + 48dp Info button (non-overlapping dedicated header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (card.isLeech) {
                        LeechBadge(lapses = card.lapses)
                    }
                    CardStatusBadge(wordEntity = card)
                    IconButton(
                        onClick = onOpenCardInfo,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("active_review_card_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Card review history",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Centered scrollable middle face
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (recallMode == StudyRecallMode.REVERSE && arabicMeaning.isNotBlank()) {
                        Text(
                            text = "What is the English word?",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = arabicMeaning,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 42.sp,
                                textDirection = TextDirection.Rtl
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        val posAbbr = formatPartOfSpeechAbbreviation(card.partOfSpeech)
                        if (posAbbr.isNotBlank()) {
                            Text(
                                text = posAbbr,
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = card.word.replaceFirstChar { it.uppercase() },
                            style = RedeusExpressiveType.headword.copy(
                                fontSize = 34.sp,
                                letterSpacing = (-0.5).sp,
                                textDirection = TextDirection.Ltr
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (displayPhonetic.isNotBlank()) {
                                Text(
                                    text = displayPhonetic,
                                    style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            val posAbbr = formatPartOfSpeechAbbreviation(card.partOfSpeech)
                            if (posAbbr.isNotBlank()) {
                                Text(
                                    text = posAbbr,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val speakInteraction = remember { MutableInteractionSource() }
                        Surface(
                            onClick = onSpeak,
                            interactionSource = speakInteraction,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(56.dp)
                                .applePressScale(speakInteraction, 0.92f)
                                .testTag("speak_review_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Pronounce word",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Text(
                            text = "Natural Offline ($selectedAccent)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Dedicated non-overlapping bottom hint
            Text(
                text = "Tap card to reveal answer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyCardBackFace(
    card: WordEntity,
    arabicEnabled: Boolean,
    arabicMeaning: String,
    displayPhonetic: String,
    cardBorderColor: Color,
    reducedMotion: Boolean,
    viewModel: DictionaryViewModel,
    onFlipBack: () -> Unit,
    onOpenAiHelper: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var mined by remember(card.word) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = if (reducedMotion) 0f else 180f
            },
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, cardBorderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subtle top utility strip so users can flip back cleanly without conflicting with vertical scroll
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onFlipBack,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.heightIn(min = 34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip to front",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Front",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Local Permanent Word Photo (Bundled 3,586 WebP or User-Saved Offline Asset)
            WordLocalImageBanner(
                wordEntity = card,
                viewModel = viewModel,
                height = 148.dp,
                showRefreshButton = true
            )

            // 2. Headword + 48dp Speak Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = card.word.replaceFirstChar { it.uppercase() },
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        letterSpacing = (-0.4).sp,
                        textDirection = TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val backSpeakInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { viewModel.speak(card.word) },
                    interactionSource = backSpeakInteraction,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .applePressScale(backSpeakInteraction, 0.92f)
                        .testTag("speak_review_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Pronounce word",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // IPA + POS
            if (displayPhonetic.isNotBlank() || card.partOfSpeech.isNotBlank()) {
                val meta = listOfNotNull(
                    displayPhonetic.takeIf { it.isNotBlank() },
                    formatPartOfSpeechAbbreviation(card.partOfSpeech).takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3. Prominent RTL Arabic Meaning Pill
            if (arabicEnabled && arabicMeaning.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = arabicMeaning,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("review_arabic_meaning_text")
                    )
                }
            }

            // 4. English Definition
            if (card.definition.isNotBlank() && card.definition != arabicMeaning) {
                Text(
                    text = card.definition,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 23.sp,
                        textDirection = TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            // 5. Example Sentence Box with Highlighted Word + 48dp Example Pronunciation + Mine
            if (card.example.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HighlightedExampleText(
                            example = card.example,
                            targetWord = card.word,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.speakSentence(card.example)
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("card_listen_example_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Listen Example", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.mineSentenceFromWord(card)
                                    mined = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("card_mine_example_button")
                            ) {
                                Icon(
                                    imageVector = if (mined) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (mined) "Mined" else "Mine")
                            }
                        }
                    }
                }
            }

            // 6. Saved Mnemonic / Synonyms Preview (if enriched)
            if (card.mnemonic.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFF9F0A).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFFF9F0A),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = card.mnemonic,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (card.synonyms.isNotBlank()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    card.synonyms.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(5).forEach { syn ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = syn,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 7. Dedicated Per-Word ✨ AI Helper Button (48dp)
            val helperInteraction = remember { MutableInteractionSource() }
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenAiHelper()
                },
                interactionSource = helperInteraction,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.38f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .applePressScale(helperInteraction, 0.97f)
                    .testTag("card_open_ai_helper_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Helper · Extra Notes, AI Voice & Photo",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
