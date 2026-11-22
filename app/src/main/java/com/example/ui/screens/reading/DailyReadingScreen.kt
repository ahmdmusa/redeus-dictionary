package com.example.ui.screens.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.*
import com.example.ui.navigation.LocalWindowWidthSizeClass
import com.example.ui.screens.search.ErrorStateCard
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusExpressiveType
import com.example.ui.theme.RedeusMotion

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyReadingTabContent(
    viewModel: DictionaryViewModel,
    readingState: DailyReadingUiState,
    innerPadding: PaddingValues
) {
    val readingStyle by viewModel.readingStyle.collectAsStateWithLifecycle()
    val readingWordScope by viewModel.readingWordScope.collectAsStateWithLifecycle()
    val todayWords by viewModel.todayStudiedWords.collectAsStateWithLifecycle()
    val weekWords by viewModel.weekStudiedWords.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDailyReadingIfNeeded()
    }

    val widthSizeClass = LocalWindowWidthSizeClass.current
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // IELTS Reading Mode & Word Scope Selector Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ielts_reading_controls_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.domain.ReadingStyle.entries.forEach { style ->
                        val selected = readingStyle == style
                        Surface(
                            onClick = {
                                if (!selected) viewModel.generateDailyReading(styleOverride = style)
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .testTag("reading_style_${style.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    text = style.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.generateDailyReading() },
                        enabled = readingState !is DailyReadingUiState.Loading,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("regenerate_reading_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Generate new passage",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    com.example.domain.PracticeWordScope.entries.forEach { scope ->
                        val selected = readingWordScope == scope
                        Surface(
                            onClick = {
                                if (!selected) viewModel.generateDailyReading(scopeOverride = scope)
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 34.dp)
                                .testTag("reading_scope_${scope.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                                Text(
                                    text = scope.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (readingState) {
                is DailyReadingUiState.Idle, is DailyReadingUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 500.dp)
                                .testTag("reading_loading_card"),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = if (readingStyle == com.example.domain.ReadingStyle.IELTS_ACADEMIC) {
                                        "Crafting IELTS Academic passage..."
                                    } else {
                                        "Generating short story..."
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Weaving your ${readingWordScope.label.lowercase()} at CEFR ${viewModel.getCefrLabel(viewModel.getReadingLevel())}...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                is DailyReadingUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.widthIn(max = 560.dp)) {
                            ErrorStateCard(
                                message = readingState.message,
                                onRetry = { viewModel.generateDailyReading() }
                            )
                        }
                    }
                }
                is DailyReadingUiState.Active -> {
                    when (readingState.currentPhase) {
                        ReadingPhase.READING -> {
                            ActiveReadingPhaseContent(
                                readingState = readingState,
                                isExpanded = isExpanded,
                                onSpeakWord = { viewModel.speak(it) },
                                onSelectOption = { viewModel.selectReadingOption(it) },
                                onNextQuestion = { viewModel.nextReadingQuestion() }
                            )
                        }
                        ReadingPhase.GRAMMAR_INTRO -> {
                            GrammarIntroContent(
                                readingState = readingState,
                                onStartGrammar = { viewModel.startGrammarCheck() }
                            )
                        }
                        ReadingPhase.GRAMMAR -> {
                            ActiveGrammarPhaseContent(
                                readingState = readingState,
                                isExpanded = isExpanded,
                                onSelectOption = { viewModel.selectGrammarOption(it) },
                                onNextQuestion = { viewModel.nextGrammarQuestion() }
                            )
                        }
                    }
                }
                is DailyReadingUiState.Finished -> {
                    ReadingFinishedContent(
                        readingState = readingState,
                        viewModel = viewModel,
                        todayWords = todayWords,
                        weekWords = weekWords,
                        onRetry = { viewModel.restartCurrentPassageQuestions() },
                        onNewPassage = { viewModel.generateDailyReading() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveReadingPhaseContent(
    readingState: DailyReadingUiState.Active,
    isExpanded: Boolean,
    onSpeakWord: (String) -> Unit = {},
    onSelectOption: (String) -> Unit,
    onNextQuestion: () -> Unit
) {
    val reducedMotion = LocalReducedMotionEnabled.current
    val currentQuestion = readingState.questions.getOrNull(readingState.currentQuestionIndex)
    val progress = if (readingState.questions.isNotEmpty()) {
        (readingState.currentQuestionIndex + 1).toFloat() / readingState.questions.size
    } else 0f

    val accentColor = MaterialTheme.colorScheme.primary
    val highlightedPassage = androidx.compose.runtime.remember(readingState.passage, readingState.targetWords, accentColor) {
        androidx.compose.ui.text.buildAnnotatedString {
            val text = readingState.passage
            val targets = readingState.targetWords.map { it.trim() }.filter { it.length >= 3 }
            if (targets.isEmpty()) {
                append(text)
            } else {
                val pattern = targets.joinToString("|") { w ->
                    val stem = if (w.length >= 5) w.dropLast(1) else w
                    "\\b${Regex.escape(stem)}[a-zA-Z']*\\b"
                }
                val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                var lastIdx = 0
                for (m in regex.findAll(text)) {
                    if (m.range.first > lastIdx) append(text.substring(lastIdx, m.range.first))
                    pushStyle(
                        androidx.compose.ui.text.SpanStyle(
                            color = accentColor,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    )
                    append(m.value)
                    pop()
                    lastIdx = m.range.last + 1
                }
                if (lastIdx < text.length) append(text.substring(lastIdx))
            }
        }
    }

    val passageCard: @Composable () -> Unit = {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_passage_card"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (readingState.readingStyle == com.example.domain.ReadingStyle.IELTS_ACADEMIC) {
                                "IELTS Academic Passage"
                            } else {
                                "Today's Story"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Text(
                            text = "CEFR ${readingState.cefrLevel}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (readingState.targetWords.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        readingState.targetWords.forEach { w ->
                            Surface(
                                onClick = { onSpeakWord(w) },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "🔊 ${w.replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = highlightedPassage,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    val questionPane: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentQuestion != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reading_question_card"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Question ${readingState.currentQuestionIndex + 1} of ${readingState.questions.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = currentQuestion.question,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                currentQuestion.options.forEach { option ->
                    val isSelected = readingState.selectedOption == option
                    val isAnswerCorrect = currentQuestion.correctAnswer.trim().equals(option.trim(), ignoreCase = true)
                    val hasAnswered = readingState.selectedOption != null

                    ReadingOptionRow(
                        option = option,
                        isSelected = isSelected,
                        isAnswerCorrect = isAnswerCorrect,
                        hasAnswered = hasAnswered,
                        onClick = { onSelectOption(option) },
                        modifier = Modifier.testTag("reading_option_${option.take(15)}")
                    )
                }

                AnimatedVisibility(
                    visible = readingState.selectedOption != null,
                    enter = fadeIn(RedeusMotion.spec(reducedMotion)),
                    exit = fadeOut(RedeusMotion.spec(reducedMotion))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Explanation",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = currentQuestion.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isLast = readingState.currentQuestionIndex == readingState.questions.lastIndex
                        val buttonLabel = if (isLast) {
                            if (readingState.grammarQuestions.isNotEmpty()) "Continue to grammar check" else "View results"
                        } else {
                            "Next question"
                        }
                        Button(
                            onClick = onNextQuestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("next_reading_question_button")
                        ) {
                            Text(buttonLabel)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isExpanded) 24.dp else 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!isExpanded) Modifier.widthIn(max = 680.dp) else Modifier),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${readingState.currentQuestionIndex + 1} of ${readingState.questions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "Score ${readingState.score}/${readingState.questions.size}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = "Lvl ${readingState.readingLevel} • ${readingState.cefrLevel}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
            )
        }

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    passageCard()
                }
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    questionPane()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                passageCard()
                questionPane()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GrammarIntroContent(
    readingState: DailyReadingUiState.Active,
    onStartGrammar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .testTag("reading_score_intro_card"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Reading completed",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${readingState.score} / ${readingState.questions.size}",
                    style = RedeusExpressiveType.metricDisplay,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Comprehension questions finished",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .testTag("grammar_check_intro_card"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Grammar check",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Explore ${readingState.grammarQuestions.size} grammar structures used in today's passage. Each question includes a rule explanation.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onStartGrammar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("start_grammar_check_button")
                ) {
                    Text("Start grammar check")
                }
            }
        }
    }
}

@Composable
private fun ActiveGrammarPhaseContent(
    readingState: DailyReadingUiState.Active,
    isExpanded: Boolean,
    onSelectOption: (String) -> Unit,
    onNextQuestion: () -> Unit
) {
    val reducedMotion = LocalReducedMotionEnabled.current
    val currentGrammarQ = readingState.grammarQuestions.getOrNull(readingState.currentGrammarIndex)
    val progress = if (readingState.grammarQuestions.isNotEmpty()) {
        (readingState.currentGrammarIndex + 1).toFloat() / readingState.grammarQuestions.size
    } else 0f

    val passageCard: @Composable () -> Unit = {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("grammar_passage_card"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Reference passage",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = readingState.passage,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    val questionPane: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentGrammarQ != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grammar_question_card"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Grammar ${readingState.currentGrammarIndex + 1} of ${readingState.grammarQuestions.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = currentGrammarQ.question,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                currentGrammarQ.options.forEachIndexed { index, option ->
                    val isSelected = readingState.selectedGrammarOption == option
                    val isAnswerCorrect = currentGrammarQ.correctAnswer.trim().equals(option.trim(), ignoreCase = true)
                    val hasAnswered = readingState.selectedGrammarOption != null

                    ReadingOptionRow(
                        option = option,
                        isSelected = isSelected,
                        isAnswerCorrect = isAnswerCorrect,
                        hasAnswered = hasAnswered,
                        onClick = { onSelectOption(option) },
                        modifier = Modifier.testTag("grammar_option_$index")
                    )
                }

                AnimatedVisibility(
                    visible = readingState.selectedGrammarOption != null,
                    enter = fadeIn(RedeusMotion.spec(reducedMotion)),
                    exit = fadeOut(RedeusMotion.spec(reducedMotion))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grammar_explanation_card"),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Rule & explanation",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = currentGrammarQ.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        val isLastGrammar = readingState.currentGrammarIndex == readingState.grammarQuestions.lastIndex
                        Button(
                            onClick = onNextQuestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("next_grammar_question_button")
                        ) {
                            Text(if (isLastGrammar) "View final results" else "Next grammar question")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isExpanded) 24.dp else 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!isExpanded) Modifier.widthIn(max = 680.dp) else Modifier),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grammar ${readingState.currentGrammarIndex + 1} of ${readingState.grammarQuestions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = "Score ${readingState.grammarScore}/${readingState.grammarQuestions.size}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraSmall)
            )
        }

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    passageCard()
                }
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    questionPane()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                passageCard()
                questionPane()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReadingOptionRow(
    option: String,
    isSelected: Boolean,
    isAnswerCorrect: Boolean,
    hasAnswered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (container, content) = when {
        !hasAnswered -> MaterialTheme.colorScheme.surfaceContainerLow to MaterialTheme.colorScheme.onSurface
        isAnswerCorrect -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        isSelected -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val icon = when {
        !hasAnswered -> Icons.Outlined.Circle
        isAnswerCorrect -> Icons.Default.CheckCircle
        isSelected -> Icons.Default.Cancel
        else -> Icons.Outlined.Cancel
    }

    Surface(
        onClick = { if (!hasAnswered) onClick() },
        enabled = !hasAnswered,
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = option,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ReadingFinishedContent(
    readingState: DailyReadingUiState.Finished,
    viewModel: DictionaryViewModel,
    todayWords: List<com.example.data.WordEntity>,
    weekWords: List<com.example.data.WordEntity>,
    onRetry: () -> Unit,
    onNewPassage: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .testTag("reading_finished_card"),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Text(
                        text = "Session completed",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("final_reading_score_card"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Reading", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "Reading: ${readingState.score}/${readingState.total}",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (readingState.grammarTotal > 0) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("final_grammar_score_card"),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Grammar", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = "Grammar: ${readingState.grammarScore}/${readingState.grammarTotal}",
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    val (levelBg, levelFg, levelTitle, levelDesc) = when (readingState.levelChange) {
                        LevelChange.INCREASED -> ReadingLevelDisplay(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            "Level increased: Level ${readingState.newReadingLevel} (${readingState.cefrLevel})",
                            "Your reading level advanced from Level ${readingState.oldReadingLevel} to Level ${readingState.newReadingLevel}."
                        )
                        LevelChange.DECREASED -> ReadingLevelDisplay(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                            "Level adjusted: Level ${readingState.newReadingLevel} (${readingState.cefrLevel})",
                            "We adjusted your level to Level ${readingState.newReadingLevel} to help build stronger mastery."
                        )
                        LevelChange.UNCHANGED -> ReadingLevelDisplay(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.onSurface,
                            "Level maintained: Level ${readingState.newReadingLevel} (${readingState.cefrLevel})",
                            "You maintained your current reading level at Level ${readingState.newReadingLevel}."
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = levelBg,
                        contentColor = levelFg
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = levelTitle,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = levelDesc,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("review_passage_button")
                        ) {
                            Text("Retry")
                        }
                        Button(
                            onClick = onNewPassage,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("new_passage_button")
                        ) {
                            Text("New passage")
                        }
                    }
                }
            }

            // Follow-up IELTS Speaking Practice Prompt with today's / this week's words
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                com.example.ui.components.IeltsSpeakingPromptCard(
                    viewModel = viewModel,
                    todayWords = todayWords,
                    weekWords = weekWords,
                    headline = "Now Practice Speaking These Words (IELTS)",
                    subtitle = "Paste this tailored 3-part IELTS Speaking prompt into ChatGPT Voice, Gemini Live, or Claude to actively speak the vocabulary you just read."
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        com.example.ui.components.MilestoneConfettiBurst(
            triggerKey = "${readingState.score}_${readingState.newReadingLevel}_${readingState.grammarScore}",
            modifier = Modifier.fillMaxSize()
        )
    }
}

private data class ReadingLevelDisplay(
    val bg: androidx.compose.ui.graphics.Color,
    val fg: androidx.compose.ui.graphics.Color,
    val title: String,
    val desc: String
)
