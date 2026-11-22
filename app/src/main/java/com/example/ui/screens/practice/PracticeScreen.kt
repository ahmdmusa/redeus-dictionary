package com.example.ui.screens.practice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.WordEntity
import com.example.ui.*
import com.example.ui.components.CardStatusBadge
import com.example.ui.components.LeechBadge
import com.example.ui.components.WordCardInfoDialog
import com.example.ui.screens.search.TryAnotherPhotoButton
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusExpressiveType
import com.example.ui.theme.RedeusMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsTabContent(
    viewModel: DictionaryViewModel,
    bookmarkedWords: List<WordEntity>,
    innerPadding: PaddingValues
) {
    val studyMode by viewModel.studyMode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // M3 Segmented Button Row for peer study modes
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val modes = listOf(
                Triple(FlashcardStudyMode.CARDS, "Cards", "study_mode_cards"),
                Triple(FlashcardStudyMode.QUIZ, "Quiz", "study_mode_quiz"),
                Triple(FlashcardStudyMode.SPELLING, "Spelling", "study_mode_spelling")
            )
            modes.forEachIndexed { index, (mode, label, tag) ->
                SegmentedButton(
                    selected = studyMode == mode,
                    onClick = { viewModel.setStudyMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    modifier = Modifier.testTag(tag)
                ) {
                    Text(label)
                }
            }
        }

        when (studyMode) {
            FlashcardStudyMode.QUIZ -> {
                QuizPracticeView(viewModel = viewModel, bookmarkedWords = bookmarkedWords)
            }
            FlashcardStudyMode.SPELLING -> {
                SpellingPracticeView(viewModel = viewModel, bookmarkedWords = bookmarkedWords)
            }
            FlashcardStudyMode.CARDS -> {
                FlashcardsDeckView(viewModel = viewModel, bookmarkedWords = bookmarkedWords)
            }
        }
    }
}

@Composable
private fun FlashcardsDeckView(
    viewModel: DictionaryViewModel,
    bookmarkedWords: List<WordEntity>
) {
    if (bookmarkedWords.isEmpty()) {
        PracticeEmptyState(
            title = "No saved words yet",
            subtitle = "Search for vocabulary words and bookmark them to build your flashcards deck.",
            onLoadSample = { viewModel.loadSampleData() }
        )
        return
    }

    var currentIndex by rememberSaveable { mutableStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, bookmarkedWords.lastIndex)
    if (safeIndex != currentIndex) currentIndex = safeIndex

    val currentWord = bookmarkedWords[safeIndex]
    val autoPronounce by viewModel.autoPronounceEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(currentWord.word) {
        if (autoPronounce) {
            viewModel.speak(currentWord.word)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Flashcards",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = "Card ${safeIndex + 1} of ${bookmarkedWords.size}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { (safeIndex + 1).toFloat() / bookmarkedWords.size.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
        )

        // Flashcard Body
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FlashcardItem(word = currentWord, viewModel = viewModel)
        }

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { if (currentIndex > 0) currentIndex-- },
                enabled = currentIndex > 0,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("flashcards_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous card"
                )
            }

            FilledTonalButton(
                onClick = { viewModel.speak(currentWord.word) },
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .testTag("flashcards_speak_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pronounce")
            }

            FilledTonalIconButton(
                onClick = { if (currentIndex < bookmarkedWords.lastIndex) currentIndex++ },
                enabled = currentIndex < bookmarkedWords.lastIndex,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("flashcards_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next card"
                )
            }
        }
    }
}

@Composable
fun FlashcardItem(
    word: WordEntity,
    viewModel: DictionaryViewModel
) {
    var flipped by remember { mutableStateOf(false) }
    var showCardInfoDialog by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotionEnabled.current

    if (showCardInfoDialog) {
        WordCardInfoDialog(
            word = word,
            viewModel = viewModel,
            onDismiss = { showCardInfoDialog = false }
        )
    }

    LaunchedEffect(word.word) {
        flipped = false
    }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = RedeusMotion.spec(reducedMotion, RedeusMotion.DurationLong),
        label = "flashcardFlip"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                        if (word.isLeech) {
                            LeechBadge(lapses = word.lapses)
                        }
                        CardStatusBadge(wordEntity = word)
                        IconButton(
                            onClick = { showCardInfoDialog = true },
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("flashcard_card_info_button")
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
                            text = word.word.replaceFirstChar { it.uppercase() },
                            style = RedeusExpressiveType.headword,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        val posAbbr = formatPartOfSpeechAbbreviation(word.partOfSpeech)
                        if (posAbbr.isNotBlank()) {
                            Text(
                                text = "($posAbbr)",
                                style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.speak(word.word) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("flashcard_speak_front_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Pronounce word"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "Tap card to reveal definition",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        } else {
            val arabicEnabled by viewModel.arabicTranslationEnabled.collectAsStateWithLifecycle()
            val wordArabicTranslations by viewModel.wordArabicTranslations.collectAsStateWithLifecycle()
            LaunchedEffect(word.word, arabicEnabled) {
                if (arabicEnabled) {
                    viewModel.ensureOfflineArabicLoaded(word)
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = if (reducedMotion) 0f else 180f
                    },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                ),
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
                    if (word.imagePrompt.isNotEmpty()) {
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
                                    .data(getAIImageUrl(word.word, word.imagePrompt))
                                    .crossfade(true)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = "Illustration for ${word.word}",
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
                                word = word,
                                viewModel = viewModel,
                                compact = true,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            )
                        }
                    }

                    if (word.isLeech) {
                        LeechBadge(lapses = word.lapses)
                    }

                    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
                    val displayPhonetic = getPhoneticForAccent(word.phonetic, selectedAccent)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (displayPhonetic.isNotBlank()) {
                            Text(
                                text = displayPhonetic,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { viewModel.speak(word.word) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("flashcard_speak_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Pronounce word",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val arabicMeaning = word.arabicMeaning.ifBlank {
                        wordArabicTranslations[word.word.trim().lowercase()] ?: ""
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
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (word.definition.isNotBlank() && word.definition != arabicMeaning) {
                        Text(
                            text = word.definition,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (word.example.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "\u201c${word.example}\u201d",
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
}

@Composable
fun QuizPracticeView(
    viewModel: DictionaryViewModel,
    bookmarkedWords: List<WordEntity>,
    modifier: Modifier = Modifier
) {
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()

    LaunchedEffect(bookmarkedWords) {
        if (quizState.currentQuestion == null && bookmarkedWords.isNotEmpty()) {
            viewModel.generateNextQuizQuestion()
        }
    }

    if (bookmarkedWords.isEmpty()) {
        PracticeEmptyState(
            title = "No words to quiz",
            subtitle = "Bookmark words from search to test your recall with multiple-choice questions.",
            onLoadSample = { viewModel.loadSampleData() }
        )
        return
    }

    val question = quizState.currentQuestion
    if (question == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = { viewModel.generateNextQuizQuestion() },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Start quiz")
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Score & Streak Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = "Score: ${quizState.score}/${quizState.totalAnswered}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                if (quizState.currentStreak > 1) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text(
                            text = "Streak: ${quizState.currentStreak}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            TextButton(
                onClick = { viewModel.restartQuiz() },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("quiz_restart_button")
            ) {
                Text("Restart")
            }
        }

        // Question Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = question.targetWord.partOfSpeech,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { viewModel.speak(question.targetWord.word) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Pronounce word"
                        )
                    }
                }

                Text(
                    text = "Which word matches this definition?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = question.targetWord.definition,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (question.targetWord.example.isNotBlank()) {
                    val maskedExample = remember(question.targetWord) {
                        val w = Regex.escape(question.targetWord.word)
                        question.targetWord.example.replace(Regex("(?i)\\b$w\\b"), "_____")
                    }
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\u201c$maskedExample\u201d",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                question.options.forEach { option ->
                    val isSelected = question.selectedAnswer == option
                    val isTarget = option.equals(question.targetWord.word, ignoreCase = true)
                    val answered = question.selectedAnswer != null

                    val (container, content) = when {
                        !answered -> MaterialTheme.colorScheme.surfaceContainerLowest to MaterialTheme.colorScheme.onSurface
                        isTarget -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                        isSelected -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        onClick = { if (!answered) viewModel.answerQuiz(option) },
                        enabled = !answered,
                        shape = MaterialTheme.shapes.medium,
                        color = container,
                        contentColor = content,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("quiz_option_$option")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = option.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (answered && (isTarget || isSelected)) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isTarget) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = if (isTarget) "Correct" else "Incorrect",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (question.selectedAnswer != null) {
            Button(
                onClick = { viewModel.generateNextQuizQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("quiz_next_button")
            ) {
                Text("Next question")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SpellingPracticeView(
    viewModel: DictionaryViewModel,
    bookmarkedWords: List<WordEntity>,
    modifier: Modifier = Modifier
) {
    val spellingState by viewModel.spellingState.collectAsStateWithLifecycle()

    LaunchedEffect(bookmarkedWords) {
        if (spellingState.targetWord == null && bookmarkedWords.isNotEmpty()) {
            viewModel.generateNextSpellingQuestion()
        }
    }

    if (bookmarkedWords.isEmpty()) {
        PracticeEmptyState(
            title = "No words for spelling",
            subtitle = "Bookmark words from search to practice listening and spelling.",
            onLoadSample = { viewModel.loadSampleData() }
        )
        return
    }

    val targetWord = spellingState.targetWord
    if (targetWord == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = { viewModel.generateNextSpellingQuestion() },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Start spelling practice")
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 640.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = "Score: ${spellingState.score}/${spellingState.totalAnswered}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                if (spellingState.currentStreak > 1) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text(
                            text = "Streak: ${spellingState.currentStreak}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            TextButton(
                onClick = { viewModel.restartSpelling() },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("spelling_restart_button")
            ) {
                Text("Restart")
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Dictation & spelling",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                FilledTonalButton(
                    onClick = { viewModel.speak(targetWord.word) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("spelling_pronounce_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Tap to listen", style = MaterialTheme.typography.titleSmall)
                }

                Text(
                    text = targetWord.definition,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                val wordText = targetWord.word.trim()
                val hintDisplay = when (spellingState.hintLevel) {
                    1 -> "${wordText.firstOrNull()?.uppercase() ?: ""} " + " _ ".repeat((wordText.length - 1).coerceAtLeast(0)) + " (${wordText.length} letters)"
                    2 -> "${wordText.firstOrNull()?.uppercase() ?: ""} " + " _ ".repeat((wordText.length - 2).coerceAtLeast(0)) + " ${wordText.lastOrNull()?.uppercase() ?: ""}"
                    else -> null
                }

                if (hintDisplay != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = hintDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                if (!spellingState.isChecked && spellingState.hintLevel < 2) {
                    OutlinedButton(
                        onClick = { viewModel.revealSpellingHint() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("spelling_hint_button")
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (spellingState.hintLevel == 0) "Show hint" else "Show last letter")
                    }
                }

                OutlinedTextField(
                    value = spellingState.userInput,
                    onValueChange = { viewModel.updateSpellingInput(it) },
                    enabled = !spellingState.isChecked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spelling_input_field"),
                    label = { Text("Your spelling") },
                    placeholder = { Text("Type the word you heard...") },
                    singleLine = true
                )

                if (spellingState.isChecked) {
                    val isCorrect = spellingState.isCorrect == true
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        contentColor = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isCorrect) "Correct spelling" else "Not quite",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (!isCorrect) {
                                Text(
                                    text = "Correct word: ${targetWord.word}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!spellingState.isChecked) {
            Button(
                onClick = { viewModel.checkSpelling() },
                enabled = spellingState.userInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("spelling_submit_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check spelling")
            }
        } else {
            Button(
                onClick = { viewModel.generateNextSpellingQuestion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("spelling_next_button")
            ) {
                Text("Next word")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PracticeEmptyState(
    title: String,
    subtitle: String,
    onLoadSample: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
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
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onLoadSample,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Load sample words")
                }
            }
        }
    }
}
