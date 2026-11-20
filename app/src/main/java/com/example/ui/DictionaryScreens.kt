package com.example.ui

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Build
import android.content.Context
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.data.WordEntity
import com.example.data.ReviewLogEntity
import com.example.ui.theme.*
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

import com.example.ui.screens.search.ErrorStateCard
import com.example.ui.screens.search.TryAnotherPhotoButton
import com.example.ui.components.*


@Composable
fun HeroBannerSection() = com.example.ui.screens.search.HeroBannerSection()


fun filterWordList(
    list: List<WordEntity>,
    searchQuery: String,
    statusFilter: WordStatusFilter,
    posFilter: String,
    tagFilter: String
): List<WordEntity> {
    return list.filter { word ->
        val query = searchQuery.trim().lowercase()
        val matchesQuery = query.isEmpty() ||
            word.word.lowercase().contains(query) ||
            word.definition.lowercase().contains(query) ||
            word.tags.lowercase().contains(query)

        val matchesStatus = when (statusFilter) {
            WordStatusFilter.ALL -> true
            WordStatusFilter.NEW -> word.repetitions == 0 && word.distinctReviewDays == 0
            WordStatusFilter.LEARNING -> (word.repetitions > 0 || word.distinctReviewDays > 0) && !(word.distinctReviewDays >= 3 && word.interval >= 6)
            WordStatusFilter.MASTERED -> word.distinctReviewDays >= 3 && word.interval >= 6
            WordStatusFilter.LEECH -> word.isLeech
        }

        val matchesPos = when (posFilter.uppercase()) {
            "ALL" -> true
            "NOUN" -> word.partOfSpeech.lowercase().startsWith("n")
            "VERB" -> word.partOfSpeech.lowercase().startsWith("v")
            "ADJECTIVE" -> word.partOfSpeech.lowercase().startsWith("adj")
            "ADVERB" -> word.partOfSpeech.lowercase().startsWith("adv")
            "OTHER" -> !word.partOfSpeech.lowercase().let {
                it.startsWith("n") || it.startsWith("v") || it.startsWith("adj") || it.startsWith("adv")
            }
            else -> true
        }

        val matchesTag = tagFilter.isBlank() || tagFilter.equals("ALL", ignoreCase = true) ||
            word.tags.split(",").map { it.trim().lowercase() }.contains(tagFilter.trim().lowercase())

        matchesQuery && matchesStatus && matchesPos && matchesTag
    }
}

@Composable
fun DailyReadingTabContent(
    viewModel: DictionaryViewModel,
    readingState: DailyReadingUiState,
    innerPadding: PaddingValues
) {
    LaunchedEffect(Unit) {
        viewModel.loadDailyReadingIfNeeded()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (readingState) {
            is DailyReadingUiState.Idle, is DailyReadingUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NeoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 500.dp)
                            .testTag("reading_loading_card"),
                        backgroundColor = com.example.ui.theme.NeoWhite
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = com.example.ui.theme.NeoBlack,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "GENERATING DAILY READING",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                color = com.example.ui.theme.NeoBlack,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Preparing your customized passage and comprehension questions at Level ${viewModel.getReadingLevel()} (${viewModel.getCefrLabel(viewModel.getReadingLevel())})...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = com.example.ui.theme.NeoBlack.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            is DailyReadingUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorStateCard(
                        message = readingState.message,
                        onRetry = { viewModel.generateDailyReading() }
                    )
                }
            }
            is DailyReadingUiState.Active -> {
                when (readingState.currentPhase) {
                    ReadingPhase.READING -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                        ) {
                            // Header progress and status badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeoSectionBadge(
                                    title = "QUESTION ${readingState.currentQuestionIndex + 1} OF ${readingState.questions.size}",
                                    containerColor = com.example.ui.theme.NeoCream
                                )
                                NeoSectionBadge(
                                    title = "SCORE: ${readingState.score}/${readingState.questions.size}",
                                    containerColor = com.example.ui.theme.NeoGreen
                                )
                                NeoSectionBadge(
                                    title = "LVL ${readingState.readingLevel} â€¢ ${readingState.cefrLevel}",
                                    containerColor = com.example.ui.theme.NeoYellow
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            NeoLinearProgressBar(
                                progress = if (readingState.questions.isNotEmpty()) {
                                    (readingState.currentQuestionIndex + 1).toFloat() / readingState.questions.size
                                } else 0f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Passage Card
                            NeoCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("daily_passage_card"),
                                backgroundColor = com.example.ui.theme.NeoWhite
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = com.example.ui.theme.NeoBlack,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "TODAY'S READING PASSAGE",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp,
                                                color = com.example.ui.theme.NeoBlack
                                            )
                                        }
                                        NeoSectionBadge(
                                            title = "CEFR ${readingState.cefrLevel}",
                                            containerColor = com.example.ui.theme.NeoYellow
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = readingState.passage,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 26.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = com.example.ui.theme.NeoBlack
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Current Question
                            val currentQuestion = readingState.questions.getOrNull(readingState.currentQuestionIndex)
                            if (currentQuestion != null) {
                                NeoCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reading_question_card"),
                                    backgroundColor = com.example.ui.theme.NeoYellow
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp)
                                    ) {
                                        Text(
                                            text = "QUESTION ${readingState.currentQuestionIndex + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = com.example.ui.theme.NeoBlack.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = currentQuestion.question,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = com.example.ui.theme.NeoBlack,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.3.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Options list
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    currentQuestion.options.forEach { option ->
                                        val isSelected = readingState.selectedOption == option
                                        val isAnswerCorrect = currentQuestion.correctAnswer.trim().equals(option.trim(), ignoreCase = true)
                                        val hasAnswered = readingState.selectedOption != null

                                        val cardBg = when {
                                            !hasAnswered -> com.example.ui.theme.NeoWhite
                                            isSelected && isAnswerCorrect -> com.example.ui.theme.NeoGreen
                                            isSelected && !isAnswerCorrect -> com.example.ui.theme.NeoRed
                                            isAnswerCorrect -> com.example.ui.theme.NeoGreen
                                            else -> com.example.ui.theme.NeoWhite.copy(alpha = 0.6f)
                                        }

                                        val textClr = when {
                                            isSelected && !isAnswerCorrect -> com.example.ui.theme.NeoWhite
                                            else -> com.example.ui.theme.NeoBlack
                                        }

                                        NeoCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("reading_option_${option.take(15)}"),
                                            backgroundColor = cardBg,
                                            onClick = if (!hasAnswered) { { viewModel.selectReadingOption(option) } } else null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        !hasAnswered -> Icons.Outlined.Circle
                                                        isSelected && isAnswerCorrect -> Icons.Default.CheckCircle
                                                        isSelected && !isAnswerCorrect -> Icons.Default.Cancel
                                                        isAnswerCorrect -> Icons.Default.CheckCircle
                                                        else -> Icons.Outlined.Cancel
                                                    },
                                                    contentDescription = null,
                                                    tint = textClr,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = textClr
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Explanation & Next Question button
                                AnimatedVisibility(
                                    visible = readingState.selectedOption != null,
                                    enter = slideInVertically { it } + fadeIn()
                                ) {
                                    Column {
                                        NeoCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            backgroundColor = com.example.ui.theme.NeoCream
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lightbulb,
                                                        contentDescription = null,
                                                        tint = com.example.ui.theme.NeoBlack,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "EXPLANATION",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 0.5.sp,
                                                        color = com.example.ui.theme.NeoBlack
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = currentQuestion.explanation,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = com.example.ui.theme.NeoBlack
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        val isLast = readingState.currentQuestionIndex == readingState.questions.size - 1
                                        val buttonLabel = if (isLast) {
                                            if (readingState.grammarQuestions.isNotEmpty()) "CONTINUE TO GRAMMAR CHECK" else "VIEW RESULTS"
                                        } else {
                                            "NEXT QUESTION"
                                        }
                                        NeoButton(
                                            onClick = { viewModel.nextReadingQuestion() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("next_reading_question_button"),
                                            containerColor = com.example.ui.theme.NeoYellow
                                        ) {
                                            Text(
                                                text = buttonLabel,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ReadingPhase.GRAMMAR_INTRO -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Step header & Reading score summary
                            NeoCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reading_score_intro_card"),
                                backgroundColor = com.example.ui.theme.NeoWhite
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    NeoSectionBadge(
                                        title = "READING COMPLETED",
                                        containerColor = com.example.ui.theme.NeoGreen
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "SCORE: ${readingState.score} / ${readingState.questions.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                        color = if (readingState.score >= (readingState.questions.size * 0.8f)) com.example.ui.theme.NeoGreen else com.example.ui.theme.NeoBlack
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Comprehension questions finished!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = com.example.ui.theme.NeoBlack.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Grammar Check Intro Card
                            NeoCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grammar_check_intro_card"),
                                backgroundColor = com.example.ui.theme.NeoCyan
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    NeoSectionBadge(
                                        title = "GRAMMAR CHECK",
                                        containerColor = com.example.ui.theme.NeoWhite
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Let's check the grammar from today's passage",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.3.sp,
                                        color = com.example.ui.theme.NeoBlack,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Explore ${readingState.grammarQuestions.size} grammar structures used in the story. Each question includes a detailed rule explanation.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = com.example.ui.theme.NeoBlack.copy(alpha = 0.85f),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    NeoButton(
                                        onClick = { viewModel.startGrammarCheck() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("start_grammar_check_button"),
                                        containerColor = com.example.ui.theme.NeoYellow
                                    ) {
                                        Text(
                                            text = "START GRAMMAR CHECK",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    ReadingPhase.GRAMMAR -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                        ) {
                            // Header progress and status badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeoSectionBadge(
                                    title = "GRAMMAR ${readingState.currentGrammarIndex + 1} OF ${readingState.grammarQuestions.size}",
                                    containerColor = com.example.ui.theme.NeoCream
                                )
                                NeoSectionBadge(
                                    title = "GRAMMAR",
                                    containerColor = com.example.ui.theme.NeoCyan
                                )
                                NeoSectionBadge(
                                    title = "SCORE: ${readingState.grammarScore}/${readingState.grammarQuestions.size}",
                                    containerColor = com.example.ui.theme.NeoGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            NeoLinearProgressBar(
                                progress = if (readingState.grammarQuestions.isNotEmpty()) {
                                    (readingState.currentGrammarIndex + 1).toFloat() / readingState.grammarQuestions.size
                                } else 0f,
                                barColor = com.example.ui.theme.NeoCyan,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Passage Card
                            NeoCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grammar_passage_card"),
                                backgroundColor = com.example.ui.theme.NeoWhite
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = com.example.ui.theme.NeoBlack,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "REFERENCE PASSAGE",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp,
                                                color = com.example.ui.theme.NeoBlack
                                            )
                                        }
                                        NeoSectionBadge(
                                            title = "GRAMMAR CHECK",
                                            containerColor = com.example.ui.theme.NeoCyan
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = readingState.passage,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 26.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = com.example.ui.theme.NeoBlack
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Current Grammar Question
                            val currentGrammarQ = readingState.grammarQuestions.getOrNull(readingState.currentGrammarIndex)
                            if (currentGrammarQ != null) {
                                NeoCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("grammar_question_card"),
                                    backgroundColor = com.example.ui.theme.NeoCyan
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            NeoSectionBadge(
                                                title = "GRAMMAR",
                                                containerColor = com.example.ui.theme.NeoWhite
                                            )
                                            Text(
                                                text = "QUESTION ${readingState.currentGrammarIndex + 1} OF ${readingState.grammarQuestions.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = com.example.ui.theme.NeoBlack.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentGrammarQ.question,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = com.example.ui.theme.NeoBlack,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.3.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Options list
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    currentGrammarQ.options.forEachIndexed { index, option ->
                                        val isSelected = readingState.selectedGrammarOption == option
                                        val isAnswerCorrect = currentGrammarQ.correctAnswer.trim().equals(option.trim(), ignoreCase = true)
                                        val hasAnswered = readingState.selectedGrammarOption != null

                                        val cardBg = when {
                                            !hasAnswered -> com.example.ui.theme.NeoWhite
                                            isSelected && isAnswerCorrect -> com.example.ui.theme.NeoGreen
                                            isSelected && !isAnswerCorrect -> com.example.ui.theme.NeoRed
                                            isAnswerCorrect -> com.example.ui.theme.NeoGreen
                                            else -> com.example.ui.theme.NeoWhite.copy(alpha = 0.6f)
                                        }

                                        val textClr = when {
                                            isSelected && !isAnswerCorrect -> com.example.ui.theme.NeoWhite
                                            else -> com.example.ui.theme.NeoBlack
                                        }

                                        NeoCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("grammar_option_${index}"),
                                            backgroundColor = cardBg,
                                            onClick = if (!hasAnswered) { { viewModel.selectGrammarOption(option) } } else null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        !hasAnswered -> Icons.Outlined.Circle
                                                        isSelected && isAnswerCorrect -> Icons.Default.CheckCircle
                                                        isSelected && !isAnswerCorrect -> Icons.Default.Cancel
                                                        isAnswerCorrect -> Icons.Default.CheckCircle
                                                        else -> Icons.Outlined.Cancel
                                                    },
                                                    contentDescription = null,
                                                    tint = textClr,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = option,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = textClr
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Explanation & Next Question button
                                AnimatedVisibility(
                                    visible = readingState.selectedGrammarOption != null,
                                    enter = slideInVertically { it } + fadeIn()
                                ) {
                                    Column {
                                        NeoCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("grammar_explanation_card"),
                                            backgroundColor = com.example.ui.theme.NeoCream
                                        ) {
                                            Column(modifier = Modifier.padding(18.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    NeoSectionBadge(
                                                        title = "GRAMMAR",
                                                        containerColor = com.example.ui.theme.NeoCyan
                                                    )
                                                    Text(
                                                        text = "RULE & EXPLANATION",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 0.5.sp,
                                                        color = com.example.ui.theme.NeoBlack
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = currentGrammarQ.explanation,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        lineHeight = 24.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = com.example.ui.theme.NeoBlack
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        val isLastGrammar = readingState.currentGrammarIndex == readingState.grammarQuestions.size - 1
                                        NeoButton(
                                            onClick = { viewModel.nextGrammarQuestion() },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("next_grammar_question_button"),
                                            containerColor = com.example.ui.theme.NeoCyan
                                        ) {
                                            Text(
                                                text = if (isLastGrammar) "VIEW FINAL RESULTS" else "NEXT GRAMMAR QUESTION",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is DailyReadingUiState.Finished -> {
                NeoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .testTag("reading_finished_card"),
                    backgroundColor = com.example.ui.theme.NeoWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(com.example.ui.theme.NeoGreen, shape = RoundedCornerShape(8.dp))
                                .border(2.dp, com.example.ui.theme.NeoBlack, shape = RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = com.example.ui.theme.NeoBlack
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SESSION COMPLETED!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            color = com.example.ui.theme.NeoBlack,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Two separate scores: Reading and Grammar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NeoCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("final_reading_score_card"),
                                backgroundColor = if (readingState.score >= (readingState.total * 0.8f)) com.example.ui.theme.NeoGreen else com.example.ui.theme.NeoCream
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    NeoSectionBadge(
                                        title = "READING",
                                        containerColor = com.example.ui.theme.NeoYellow
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Reading: ${readingState.score}/${readingState.total}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.3.sp,
                                        color = com.example.ui.theme.NeoBlack,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            if (readingState.grammarTotal > 0) {
                                NeoCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("final_grammar_score_card"),
                                    backgroundColor = if (readingState.grammarScore >= (readingState.grammarTotal * 0.8f)) com.example.ui.theme.NeoGreen else com.example.ui.theme.NeoCyan
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        NeoSectionBadge(
                                            title = "GRAMMAR",
                                            containerColor = com.example.ui.theme.NeoWhite
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Grammar: ${readingState.grammarScore}/${readingState.grammarTotal}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.3.sp,
                                            color = com.example.ui.theme.NeoBlack,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Difficulty Level Tracking Result Card
                        val (levelBadgeColor, levelTitle, levelDesc) = when (readingState.levelChange) {
                            LevelChange.INCREASED -> Triple(
                                com.example.ui.theme.NeoGreen,
                                "LEVEL INCREASED: LEVEL ${readingState.newReadingLevel} (${readingState.cefrLevel})",
                                "Outstanding comprehension! Your reading level advanced from Level ${readingState.oldReadingLevel} to Level ${readingState.newReadingLevel}."
                            )
                            LevelChange.DECREASED -> Triple(
                                com.example.ui.theme.NeoYellow,
                                "LEVEL ADJUSTED: LEVEL ${readingState.newReadingLevel} (${readingState.cefrLevel})",
                                "Keep practicing! We adjusted your level to Level ${readingState.newReadingLevel} so you can build stronger mastery."
                            )
                            LevelChange.UNCHANGED -> Triple(
                                com.example.ui.theme.NeoCream,
                                "LEVEL MAINTAINED: LEVEL ${readingState.newReadingLevel} (${readingState.cefrLevel})",
                                "Solid effort! You maintained your current reading level at Level ${readingState.newReadingLevel}."
                            )
                        }

                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = levelBadgeColor
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = levelTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = com.example.ui.theme.NeoBlack,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = levelDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.example.ui.theme.NeoBlack.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NeoSecondaryButton(
                                onClick = { viewModel.restartCurrentPassageQuestions() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("review_passage_button")
                            ) {
                                Text("RETRY", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                            }
                            NeoButton(
                                onClick = { viewModel.generateDailyReading() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_passage_button"),
                                containerColor = com.example.ui.theme.NeoYellow
                            ) {
                                Text("NEW PASSAGE", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


// FlowRow is a custom wrapping row helper since we don't have standard M3 flowrow in older compose
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth
        val lines = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentLine = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentLineWidth = 0

        placeables.forEach { placeable ->
            val spacing = if (currentLine.isNotEmpty()) mainAxisSpacing.roundToPx() else 0
            if (currentLineWidth + spacing + placeable.width <= layoutWidth) {
                currentLine.add(placeable)
                currentLineWidth += spacing + placeable.width
            } else {
                lines.add(currentLine)
                currentLine = mutableListOf(placeable)
                currentLineWidth = placeable.width
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        var totalHeight = 0
        lines.forEachIndexed { index, line ->
            val maxLineHeight = line.maxOfOrNull { it.height } ?: 0
            totalHeight += maxLineHeight
            if (index < lines.size - 1) {
                totalHeight += crossAxisSpacing.roundToPx()
            }
        }

        layout(layoutWidth, totalHeight) {
            var currentY = 0
            lines.forEach { line ->
                val maxLineHeight = line.maxOfOrNull { it.height } ?: 0
                var currentX = 0
                line.forEach { placeable ->
                    placeable.placeRelative(currentX, currentY)
                    currentX += placeable.width + mainAxisSpacing.roundToPx()
                }
                currentY += maxLineHeight + crossAxisSpacing.roundToPx()
            }
        }
    }
}

fun getAIImageUrl(word: String, prompt: String): String {
    if (prompt.startsWith("http://") || prompt.startsWith("https://")) {
        return prompt
    }
    val cleanPrompt = prompt.ifEmpty { "A beautiful creative symbolic digital art vector illustration of the concept of $word" }
    val encodedPrompt = try {
        URLEncoder.encode(cleanPrompt, "UTF-8")
    } catch (e: Exception) {
        word
    }
    return "https://image.pollinations.ai/p/$encodedPrompt?width=600&height=400&nologo=true"
}

@Composable
fun rememberAppImageLoader(context: Context): ImageLoader {
    return remember(context) {
        ImageLoader.Builder(context)
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .callTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

fun getPhoneticForAccent(phonetic: String, accent: String): String {
    if (phonetic.contains("(US)") || phonetic.contains("(UK)")) {
        val chunks = Regex("(/[^/]+/)(?:\\s*\\((US|UK)\\))?").findAll(phonetic).toList()
        if (chunks.isNotEmpty()) {
            val matchingChunk = chunks.find { it.groups[2]?.value == accent }
            if (matchingChunk != null) {
                return matchingChunk.groups[1]?.value?.trim() ?: phonetic
            }
            val genericChunk = chunks.find { it.groups[2] == null }
            if (genericChunk != null) {
                return genericChunk.groups[1]?.value?.trim() ?: phonetic
            }
            return chunks.first().groups[1]?.value?.trim() ?: phonetic
        }
    }
    return phonetic.replace(" (US)", "").replace(" (UK)", "").trim()
}

fun formatPartOfSpeechAbbreviation(pos: String?): String {
    if (pos.isNullOrBlank()) return ""
    val trimmed = pos.trim().lowercase()
    return when (trimmed) {
        "noun", "n", "n." -> "n."
        "verb", "v", "v.", "vb", "vb." -> "v."
        "adjective", "adj", "adj." -> "adj."
        "adverb", "adv", "adv." -> "adv."
        "preposition", "prep", "prep." -> "prep."
        "pronoun", "pron", "pron." -> "pron."
        "conjunction", "conj", "conj." -> "conj."
        "interjection", "interj", "interj." -> "interj."
        "phrase", "phr", "phr." -> "phr."
        "abbreviation", "abbr", "abbr." -> "abbr."
        "idiom", "idm", "idm." -> "idm."
        "article", "art", "art." -> "art."
        "determiner", "det", "det." -> "det."
        "particle", "part", "part." -> "part."
        "numeral", "num", "num." -> "num."
        else -> {
            val clean = trimmed.replace(".", "").trim()
            if (clean.isEmpty()) ""
            else "${clean.take(4)}."
        }
    }
}

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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Study Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeoCream)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeoChip(
                text = "CARDS",
                selected = studyMode == FlashcardStudyMode.CARDS,
                onClick = { viewModel.setStudyMode(FlashcardStudyMode.CARDS) },
                modifier = Modifier.weight(1f).testTag("study_mode_cards")
            )
            NeoChip(
                text = "QUIZ",
                selected = studyMode == FlashcardStudyMode.QUIZ,
                onClick = { viewModel.setStudyMode(FlashcardStudyMode.QUIZ) },
                modifier = Modifier.weight(1f).testTag("study_mode_quiz")
            )
            NeoChip(
                text = "SPELLING",
                selected = studyMode == FlashcardStudyMode.SPELLING,
                onClick = { viewModel.setStudyMode(FlashcardStudyMode.SPELLING) },
                modifier = Modifier.weight(1f).testTag("study_mode_spelling")
            )
        }

        when (studyMode) {
            FlashcardStudyMode.QUIZ -> {
                NeoQuizView(viewModel = viewModel, bookmarkedWords = bookmarkedWords)
            }
            FlashcardStudyMode.SPELLING -> {
                NeoSpellingView(viewModel = viewModel, bookmarkedWords = bookmarkedWords)
            }
            FlashcardStudyMode.CARDS -> {
                if (bookmarkedWords.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NeoCard(
                            modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp),
                            backgroundColor = NeoWhite
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                NeoSectionBadge(title = "FLASHCARDS", containerColor = NeoYellow)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "NO SAVED WORDS YET",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = NeoBlack,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Search for vocabulary words and tap the bookmark icon to populate your smart flashcards deck.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeoBlack.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    var currentIndex by rememberSaveable { mutableStateOf(0) }
                    if (currentIndex >= bookmarkedWords.size) {
                        currentIndex = bookmarkedWords.size - 1
                    }
                    if (currentIndex < 0) {
                        currentIndex = 0
                    }

                    val currentWord = bookmarkedWords[currentIndex]
                    val autoPronounce by viewModel.autoPronounceEnabled.collectAsStateWithLifecycle()

                    LaunchedEffect(currentWord.word) {
                        if (autoPronounce) {
                            viewModel.speak(currentWord.word)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
            ) {
                NeoSectionBadge(
                    title = "FLASHCARDS STUDY",
                    containerColor = NeoYellow
                )
                Box(
                    modifier = Modifier
                        .background(NeoWhite, shape = RoundedCornerShape(6.dp))
                        .border(2.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CARD ${currentIndex + 1} OF ${bookmarkedWords.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeoBlack,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Flashcard
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Flashcard(word = currentWord, viewModel = viewModel)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoIconButton(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    containerColor = if (currentIndex > 0) NeoYellow else Color(0xFFE5E7EB),
                    borderWidth = 2.5.dp,
                    shadowOffset = if (currentIndex > 0) 3.dp else 1.dp,
                    modifier = Modifier.size(52.dp).testTag("flashcards_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Previous Card",
                        tint = if (currentIndex > 0) NeoBlack else NeoBlack.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                NeoButton(
                    onClick = {
                        viewModel.speak(currentWord.word)
                    },
                    containerColor = NeoCyan,
                    contentColor = NeoBlack,
                    modifier = Modifier.height(52.dp).testTag("flashcards_speak_btn"),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PRONOUNCE", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }

                NeoIconButton(
                    onClick = { if (currentIndex < bookmarkedWords.size - 1) currentIndex++ },
                    containerColor = if (currentIndex < bookmarkedWords.size - 1) NeoYellow else Color(0xFFE5E7EB),
                    borderWidth = 2.5.dp,
                    shadowOffset = if (currentIndex < bookmarkedWords.size - 1) 3.dp else 1.dp,
                    modifier = Modifier.size(52.dp).testTag("flashcards_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next Card",
                        tint = if (currentIndex < bookmarkedWords.size - 1) NeoBlack else NeoBlack.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
                }
            }
        }
    }
}

@Composable
fun NeoQuizView(
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
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            NeoCard(modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp), backgroundColor = NeoWhite) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NeoSectionBadge(title = "MULTIPLE CHOICE QUIZ", containerColor = NeoYellow)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("NO WORDS TO QUIZ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = NeoBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bookmark words from search to test your recall with interactive multiple choice questions.", textAlign = TextAlign.Center, color = NeoBlack.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(16.dp))
                    NeoButton(onClick = { viewModel.loadSampleData() }) {
                        Text("LOAD SAMPLE WORDS", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        return
    }

    val question = quizState.currentQuestion
    if (question == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NeoButton(onClick = { viewModel.generateNextQuizQuestion() }) {
                Text("START QUIZ", fontWeight = FontWeight.Black)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(NeoYellow, shape = RoundedCornerShape(6.dp))
                        .border(2.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SCORE: ${quizState.score}/${quizState.totalAnswered}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = NeoBlack
                    )
                }
                if (quizState.currentStreak > 1) {
                    Box(
                        modifier = Modifier
                            .background(NeoRed, shape = RoundedCornerShape(6.dp))
                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "STREAK: ${quizState.currentStreak} ðŸ”¥",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = NeoWhite
                        )
                    }
                }
            }
            NeoSecondaryButton(
                onClick = { viewModel.restartQuiz() },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("quiz_restart_button")
            ) {
                Text("RESTART", fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NeoCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .weight(1f),
            backgroundColor = NeoWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(NeoCyan, shape = RoundedCornerShape(4.dp))
                            .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = question.targetWord.partOfSpeech.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack
                        )
                    }
                    NeoIconButton(
                        onClick = { viewModel.speak(question.targetWord.word) },
                        containerColor = NeoYellow,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Pronounce", modifier = Modifier.size(18.dp), tint = NeoBlack)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "WHAT IS THE WORD FOR:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = question.targetWord.definition,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack,
                    textAlign = TextAlign.Center
                )

                if (question.targetWord.example.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val maskedExample = remember(question.targetWord) {
                        val word = question.targetWord.word
                        question.targetWord.example.replace(Regex("(?i)\\b$word\\b"), "_____")
                    }
                    Box(
                        modifier = Modifier
                            .background(NeoCream, shape = RoundedCornerShape(6.dp))
                            .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "\"$maskedExample\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NeoBlack.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                question.options.forEach { option ->
                    val isSelected = question.selectedAnswer == option
                    val isTarget = option.equals(question.targetWord.word, ignoreCase = true)
                    val bgColor = when {
                        question.selectedAnswer == null -> NeoWhite
                        isTarget -> NeoGreen
                        isSelected -> NeoRed
                        else -> NeoCream
                    }
                    val textColor = when {
                        question.selectedAnswer == null -> NeoBlack
                        isTarget -> NeoWhite
                        isSelected -> NeoWhite
                        else -> NeoBlack.copy(alpha = 0.6f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(bgColor, shape = RoundedCornerShape(8.dp))
                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                            .clickable(enabled = question.selectedAnswer == null) {
                                viewModel.answerQuiz(option)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("quiz_option_${option}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = option.uppercase(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Black,
                                color = textColor,
                                letterSpacing = 0.5.sp
                            )
                            if (question.selectedAnswer != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isTarget) {
                                    Icon(Icons.Default.Check, contentDescription = "Correct", tint = NeoWhite, modifier = Modifier.size(18.dp))
                                } else if (isSelected) {
                                    Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = NeoWhite, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (question.selectedAnswer != null) {
            NeoButton(
                onClick = { viewModel.generateNextQuizQuestion() },
                containerColor = NeoYellow,
                contentColor = NeoBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .height(52.dp)
                    .testTag("quiz_next_button")
            ) {
                Text("NEXT QUESTION â†’", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
fun NeoSpellingView(
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
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            NeoCard(modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp), backgroundColor = NeoWhite) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NeoSectionBadge(title = "SPELLING TEST", containerColor = NeoYellow)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("NO WORDS FOR SPELLING", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = NeoBlack)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bookmark words from search to practice listening and spelling vocabulary.", textAlign = TextAlign.Center, color = NeoBlack.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(16.dp))
                    NeoButton(onClick = { viewModel.loadSampleData() }) {
                        Text("LOAD SAMPLE WORDS", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        return
    }

    val targetWord = spellingState.targetWord
    if (targetWord == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NeoButton(onClick = { viewModel.generateNextSpellingQuestion() }) {
                Text("START SPELLING TEST", fontWeight = FontWeight.Black)
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .background(NeoYellow, shape = RoundedCornerShape(6.dp))
                        .border(2.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SCORE: ${spellingState.score}/${spellingState.totalAnswered}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = NeoBlack
                    )
                }
                if (spellingState.currentStreak > 1) {
                    Box(
                        modifier = Modifier
                            .background(NeoRed, shape = RoundedCornerShape(6.dp))
                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "STREAK: ${spellingState.currentStreak} ðŸ”¥",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = NeoWhite
                        )
                    }
                }
            }
            NeoSecondaryButton(
                onClick = { viewModel.restartSpelling() },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("spelling_restart_button")
            ) {
                Text("RESTART", fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        NeoCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .weight(1f),
            backgroundColor = NeoWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NeoSectionBadge(title = "DICTATION & SPELLING", containerColor = NeoCyan)

                Spacer(modifier = Modifier.height(16.dp))

                NeoButton(
                    onClick = { viewModel.speak(targetWord.word) },
                    containerColor = NeoYellow,
                    contentColor = NeoBlack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("spelling_pronounce_button")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("TAP TO LISTEN", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MEANING: ${targetWord.definition}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeoBlack,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                val wordText = targetWord.word.trim()
                val hintDisplay = when (spellingState.hintLevel) {
                    1 -> "${wordText.first().uppercase()} " + " _ ".repeat(wordText.length - 1) + " (${wordText.length} letters)"
                    2 -> "${wordText.first().uppercase()} " + " _ ".repeat((wordText.length - 2).coerceAtLeast(0)) + " ${wordText.last().uppercase()}"
                    else -> null
                }

                if (hintDisplay != null) {
                    Box(
                        modifier = Modifier
                            .background(NeoCream, shape = RoundedCornerShape(6.dp))
                            .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = hintDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = NeoRed
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!spellingState.isChecked && spellingState.hintLevel < 2) {
                    NeoSecondaryButton(
                        onClick = { viewModel.revealSpellingHint() },
                        modifier = Modifier.testTag("spelling_hint_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (spellingState.hintLevel == 0) "NEED A HINT?" else "SHOW LAST LETTER", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = spellingState.userInput,
                    onValueChange = { viewModel.updateSpellingInput(it) },
                    enabled = !spellingState.isChecked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spelling_input_field"),
                    placeholder = { Text("Type the word you heard...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeoBlack,
                        unfocusedBorderColor = NeoBlack,
                        focusedContainerColor = NeoWhite,
                        unfocusedContainerColor = NeoWhite
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (spellingState.isChecked) {
                    val isCorrect = spellingState.isCorrect == true
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isCorrect) NeoGreen else NeoRed, shape = RoundedCornerShape(8.dp))
                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isCorrect) "EXCELLENT! CORRECT SPELLING ðŸŽ‰" else "INCORRECT!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = NeoWhite
                            )
                            if (!isCorrect) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "CORRECT WORD: ${targetWord.word.uppercase()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = NeoWhite
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!spellingState.isChecked) {
            NeoButton(
                onClick = { viewModel.checkSpelling() },
                containerColor = NeoGreen,
                contentColor = NeoWhite,
                enabled = spellingState.userInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .height(52.dp)
                    .testTag("spelling_submit_button")
            ) {
                Text("CHECK SPELLING âœ“", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
        } else {
            NeoButton(
                onClick = { viewModel.generateNextSpellingQuestion() },
                containerColor = NeoYellow,
                contentColor = NeoBlack,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .height(52.dp)
                    .testTag("spelling_next_button")
            ) {
                Text("NEXT WORD â†’", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
fun Flashcard(word: WordEntity, viewModel: DictionaryViewModel) {
    var flipped by remember { mutableStateOf(false) }
    var showCardInfoDialog by remember { mutableStateOf(false) }

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
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 380.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { flipped = !flipped },
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            // Front side
            NeoCard(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = NeoWhite,
                shadowOffset = 5.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeoIconButton(
                            onClick = { showCardInfoDialog = true },
                            containerColor = NeoWhite,
                            contentColor = NeoBlack,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("flashcard_card_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Card Review History",
                                tint = NeoBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        if (word.isLeech) {
                            com.example.ui.theme.NeoLeechBadge(lapses = word.lapses)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        NeoCardStatusBadge(
                            repetitions = word.repetitions,
                            interval = word.interval,
                            distinctReviewDays = word.distinctReviewDays
                        )
                    }
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = word.word.uppercase(),
                                style = MaterialTheme.typography.displaySmall,
                                color = NeoBlack,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            val posAbbr = formatPartOfSpeechAbbreviation(word.partOfSpeech)
                            if (posAbbr.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "($posAbbr)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = NeoBlack
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            NeoIconButton(
                                onClick = { viewModel.speak(word.word) },
                                containerColor = NeoYellow,
                                modifier = Modifier.size(42.dp).testTag("flashcard_speak_front_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Pronounce Word",
                                    tint = NeoBlack,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .background(NeoCream, shape = RoundedCornerShape(6.dp))
                                .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "TAP TO REVEAL DEFINITION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = NeoBlack.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Back side
            NeoCard(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = 180f
                    },
                backgroundColor = NeoWhite,
                shadowOffset = 5.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (word.imagePrompt.isNotEmpty()) {
                        val imageLoader = rememberAppImageLoader(LocalContext.current)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(getAIImageUrl(word.word, word.imagePrompt))
                                    .crossfade(true)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = "Illustration for ${word.word}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(NeoCream),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.5.dp,
                                            color = NeoBlack
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(NeoCream),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ImageNotSupported,
                                                contentDescription = "Image unavailable",
                                                tint = NeoBlack.copy(alpha = 0.6f),
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "NO IMAGE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = NeoBlack.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                },
                                success = {
                                    SubcomposeAsyncImageContent(
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            )

                            // Try Another Photo button
                            TryAnotherPhotoButton(
                                word = word,
                                viewModel = viewModel,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (word.isLeech) {
                        com.example.ui.theme.NeoLeechBadge(lapses = word.lapses)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
                    val displayPhonetic = getPhoneticForAccent(word.phonetic, selectedAccent)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$displayPhonetic ($selectedAccent)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        NeoIconButton(
                            onClick = { viewModel.speak(word.word) },
                            containerColor = NeoYellow,
                            modifier = Modifier.size(36.dp).testTag("flashcard_speak_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Pronounce Word",
                                tint = NeoBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (word.definition.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = word.definition,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeoBlack,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    if (word.example.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${word.example}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            lineHeight = 18.sp,
                            color = NeoBlack,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

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
    
    // Auto-trigger session start on first compose if queue is empty and there are due cards
    LaunchedEffect(cardsDueTodayCount) {
        if (reviewQueue.isEmpty() && cardsDueTodayCount > 0) {
            viewModel.startReviewSession()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (availableTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DECK:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack
                )
                (listOf("ALL") + availableTags).forEach { deck ->
                    val isSelected = selectedReviewDeck.equals(deck, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) NeoCyan else NeoWhite, shape = RoundedCornerShape(6.dp))
                            .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                            .clickable { viewModel.setSelectedReviewDeck(deck) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("review_deck_chip_$deck")
                    ) {
                        Text(
                            text = if (deck == "ALL") "ALL DECKS" else "#$deck",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack
                        )
                    }
                }
            }
        }
        if (reviewQueue.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DailyStreakAndHeatmapBanner(
                    viewModel = viewModel,
                    initiallyExpanded = true
                )

                // Empty State (All Caught Up!)
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeoWhite
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp).fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(NeoGreen, shape = RoundedCornerShape(12.dp))
                                .border(2.5.dp, NeoBlack, shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = NeoBlack
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ALL CAUGHT UP!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You have completed all vocabulary reviews scheduled for today. Outstanding work! Return tomorrow for spaced repetition reviews.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeoBlack.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        NeoButton(
                            onClick = { viewModel.startReviewSession() },
                            containerColor = NeoYellow,
                            contentColor = NeoBlack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_review_button")
                        ) {
                            Text(
                                "RESTART STUDY SESSION (${bookmarkedWords.size} CARDS)",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        } else {
            // Active Review Session
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
            
            // Whenever currentCard changes, reset flipped to false and auto-pronounce
            LaunchedEffect(currentCard.word) {
                flipped = false
                if (autoPronounce) {
                    viewModel.speak(currentCard.word)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeoSectionBadge(
                        title = "ACTIVE REVIEW",
                        containerColor = NeoYellow
                    )
                    Box(
                        modifier = Modifier
                            .background(NeoWhite, shape = RoundedCornerShape(6.dp))
                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${reviewQueue.size} CARDS LEFT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spaced Repetition Card Display
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (flipped) 180f else 0f,
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 380.dp)
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                            }
                            .clickable { flipped = !flipped },
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            // Front side
                            NeoCard(
                                modifier = Modifier.fillMaxSize(),
                                backgroundColor = NeoWhite,
                                shadowOffset = 5.dp
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Row(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NeoIconButton(
                                            onClick = { showReviewCardInfoDialog = true },
                                            containerColor = NeoWhite,
                                            contentColor = NeoBlack,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("active_review_card_info_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Card Review History",
                                                tint = NeoBlack,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (currentCard.isLeech) {
                                            com.example.ui.theme.NeoLeechBadge(lapses = currentCard.lapses)
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        NeoCardStatusBadge(
                                            repetitions = currentCard.repetitions,
                                            interval = currentCard.interval,
                                            distinctReviewDays = currentCard.distinctReviewDays
                                        )
                                    }
                                    
                                    Column(
                                        modifier = Modifier
                                            .verticalScroll(rememberScrollState())
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = currentCard.word.uppercase(),
                                                style = MaterialTheme.typography.displaySmall,
                                                color = NeoBlack,
                                                fontWeight = FontWeight.Black,
                                                textAlign = TextAlign.Center
                                            )
                                            val posAbbr = formatPartOfSpeechAbbreviation(currentCard.partOfSpeech)
                                            if (posAbbr.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "($posAbbr)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontStyle = FontStyle.Italic,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = NeoBlack
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            NeoIconButton(
                                                onClick = { viewModel.speak(currentCard.word) },
                                                containerColor = NeoYellow,
                                                modifier = Modifier.size(42.dp).testTag("speak_review_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = "Pronounce Word",
                                                    tint = NeoBlack,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(NeoCream, shape = RoundedCornerShape(6.dp))
                                                .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "TAP TO REVEAL DETAILS",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack.copy(alpha = 0.7f),
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Back side
                            NeoCard(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        rotationY = 180f
                                    },
                                backgroundColor = NeoWhite,
                                shadowOffset = 5.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (currentCard.imagePrompt.isNotEmpty()) {
                                        val imageLoader = rememberAppImageLoader(LocalContext.current)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(4f / 3f)
                                                .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            SubcomposeAsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(getAIImageUrl(currentCard.word, currentCard.imagePrompt))
                                                    .crossfade(true)
                                                    .build(),
                                                imageLoader = imageLoader,
                                                contentDescription = "Illustration for ${currentCard.word}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                loading = {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(NeoCream),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(28.dp),
                                                            strokeWidth = 2.5.dp,
                                                            color = NeoBlack
                                                        )
                                                    }
                                                },
                                                error = {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(NeoCream),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                            modifier = Modifier.padding(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ImageNotSupported,
                                                                contentDescription = "Image unavailable",
                                                                tint = NeoBlack.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(32.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "NO IMAGE",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = NeoBlack.copy(alpha = 0.7f),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                },
                                                success = {
                                                    SubcomposeAsyncImageContent(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                            )

                                            // Try Another Photo button
                                            TryAnotherPhotoButton(
                                                word = currentCard,
                                                viewModel = viewModel,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = currentCard.word.uppercase(),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = NeoBlack,
                                            fontWeight = FontWeight.Black
                                        )
                                        val posAbbr = formatPartOfSpeechAbbreviation(currentCard.partOfSpeech)
                                        if (posAbbr.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "($posAbbr)",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontStyle = FontStyle.Italic,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = NeoBlack
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        NeoIconButton(
                                            onClick = { viewModel.speak(currentCard.word) },
                                            containerColor = NeoYellow,
                                            modifier = Modifier.size(36.dp).testTag("speak_review_back_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Pronounce Word",
                                                tint = NeoBlack,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    
                                    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
                                    val displayPhonetic = getPhoneticForAccent(currentCard.phonetic, selectedAccent)
                                    Text(
                                        text = "$displayPhonetic ($selectedAccent)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoBlack.copy(alpha = 0.8f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = currentCard.definition,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NeoBlack,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    
                                    if (currentCard.example.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "\"${currentCard.example}\"",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontStyle = FontStyle.Italic,
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            lineHeight = 18.sp,
                                            color = NeoBlack,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Interaction Area
                AnimatedContent(
                    targetState = flipped,
                    label = "interactionArea"
                ) { isFlipped ->
                    if (!isFlipped) {
                        NeoButton(
                            onClick = { flipped = true },
                            containerColor = NeoYellow,
                            contentColor = NeoBlack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("flip_card_button")
                        ) {
                            Text("REVEAL CARD", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                        }
                    } else {
                        // SM-2 Rating Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeoRatingButton(
                                text = "Again",
                                containerColor = NeoRed,
                                modifier = Modifier.weight(1f).testTag("rate_again_button"),
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Again") }
                            )
                            NeoRatingButton(
                                text = "Hard",
                                containerColor = NeoOrange,
                                modifier = Modifier.weight(1f).testTag("rate_hard_button"),
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Hard") }
                            )
                            NeoRatingButton(
                                text = "Good",
                                containerColor = NeoGreen,
                                modifier = Modifier.weight(1f).testTag("rate_good_button"),
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Good") }
                            )
                            NeoRatingButton(
                                text = "Easy",
                                containerColor = NeoCyan,
                                modifier = Modifier.weight(1f).testTag("rate_easy_button"),
                                enabled = !isRatingInProgress,
                                onClick = { viewModel.rateCard(currentCard, "Easy") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    NeoRatingButton(
        text = text,
        containerColor = color,
        modifier = modifier,
        enabled = enabled,
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DictionaryViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val autoPronounce by viewModel.autoPronounceEnabled.collectAsStateWithLifecycle()
    val pronunciationSource by viewModel.pronunciationSource.collectAsStateWithLifecycle()
    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
    val speechSpeed by viewModel.speechSpeed.collectAsStateWithLifecycle()
    val geminiApiKeys by viewModel.geminiApiKeys.collectAsStateWithLifecycle()
    val activeKeyIndex by viewModel.activeKeyIndex.collectAsStateWithLifecycle()
    val lastPronunciationSource by viewModel.lastPronunciationSource.collectAsStateWithLifecycle()

    val geminiMeaningApiKeys by viewModel.geminiMeaningApiKeys.collectAsStateWithLifecycle()
    val geminiMeaningActiveKeyIndex by viewModel.geminiMeaningActiveKeyIndex.collectAsStateWithLifecycle()
    val geminiMeaningModelChoice by viewModel.geminiMeaningModelChoice.collectAsStateWithLifecycle()
    val geminiMeaningCustomModel by viewModel.geminiMeaningCustomModel.collectAsStateWithLifecycle()
    val openRouterApiKey by viewModel.openRouterApiKey.collectAsStateWithLifecycle()
    val pexelsApiKey by viewModel.pexelsApiKey.collectAsStateWithLifecycle()

    val aiVisualizationsEnabled by viewModel.aiVisualizationsEnabled.collectAsStateWithLifecycle()
    val remindersEnabled by viewModel.remindersEnabled.collectAsStateWithLifecycle()
    val alwaysNotifyEnabled by viewModel.alwaysNotifyEnabled.collectAsStateWithLifecycle()
    val selectedThemeColor by viewModel.selectedThemeColor.collectAsStateWithLifecycle()
    val imageSourcePreference by viewModel.imageSourcePreference.collectAsStateWithLifecycle()
    val pexelsQuerySource by viewModel.pexelsQuerySource.collectAsStateWithLifecycle()

    val easyBonus by viewModel.easyBonus.collectAsStateWithLifecycle()
    val startingEase by viewModel.startingEase.collectAsStateWithLifecycle()
    val intervalModifier by viewModel.intervalModifier.collectAsStateWithLifecycle()
    val hardIntervalMultiplier by viewModel.hardIntervalMultiplier.collectAsStateWithLifecycle()
    val leechThreshold by viewModel.leechThreshold.collectAsStateWithLifecycle()
    val leechWords by viewModel.leechWordsList.collectAsStateWithLifecycle()
    val leechWordsCount by viewModel.leechWordsCount.collectAsStateWithLifecycle()

    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val longestStreak by viewModel.longestStreak.collectAsStateWithLifecycle()
    val successRate by viewModel.successRate.collectAsStateWithLifecycle()
    val reviewsToday by viewModel.reviewsToday.collectAsStateWithLifecycle()
    val reviewsThisWeek by viewModel.reviewsThisWeek.collectAsStateWithLifecycle()

    val totalWordsCount by viewModel.totalWordsCount.collectAsStateWithLifecycle()
    val wordsMasteredCount by viewModel.wordsMasteredCount.collectAsStateWithLifecycle()
    val wordsLearningCount by viewModel.wordsLearningCount.collectAsStateWithLifecycle()
    val wordsNewCount by viewModel.wordsNewCount.collectAsStateWithLifecycle()
    val detailedSrsStats by viewModel.detailedSrsStats.collectAsStateWithLifecycle()

    val userEnglishLevel by viewModel.userEnglishLevel.collectAsStateWithLifecycle()
    val readingSessions by viewModel.readingSessionsState.collectAsStateWithLifecycle()
    val currentReadingLevel by viewModel.readingLevel.collectAsStateWithLifecycle()
    val aiUsageStats by viewModel.aiUsageStats.collectAsStateWithLifecycle()
    val geminiLocked by viewModel.geminiLocked.collectAsStateWithLifecycle()
    val srsAlgorithm by viewModel.srsAlgorithm.collectAsStateWithLifecycle()
    val targetRetention by viewModel.targetRetention.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var permissionState by remember { mutableStateOf("default") }
    var settingsTab by remember { mutableStateOf(0) } // 0: Stats, 1: Prefs, 2: SRS Settings
    var showAdvancedSrsSliders by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    fun updatePermissionState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (granted) {
                permissionState = "granted"
            } else {
                val sharedPrefs = context.getSharedPreferences("vocab_ai_prefs", Context.MODE_PRIVATE)
                val hasAsked = sharedPrefs.getBoolean("has_asked_notification_permission", false)
                permissionState = if (hasAsked) "denied" else "default"
            }
        } else {
            permissionState = "granted"
        }
        Log.d("SettingsScreen", "Updated notification permission state in UI: $permissionState")
    }

    // Refresh permission state and stats on resume / startup
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                updatePermissionState()
                viewModel.refreshStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = NeoWhite,
            title = {
                Text(
                    "RESET ALL PROGRESS",
                    fontWeight = FontWeight.Black,
                    color = NeoRed,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    "This will reset the spaced repetition progress for all $totalWordsCount words in your deck. This action cannot be undone. Are you sure?",
                    color = NeoBlack,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                NeoDestructiveButton(
                    onClick = {
                        viewModel.resetAllWordsProgress()
                        showResetConfirmDialog = false
                    },
                    modifier = Modifier.testTag("confirm_reset_all_button"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("RESET ALL", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                NeoSecondaryButton(
                    onClick = { showResetConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_reset_all_button"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val sharedPrefs = context.getSharedPreferences("vocab_ai_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("has_asked_notification_permission", true).apply()
        
        updatePermissionState()
        
        if (isGranted) {
            viewModel.setRemindersEnabled(true)
        } else {
            viewModel.setRemindersEnabled(false)
        }
        Log.i("SettingsScreen", "Permission prompt requested. Result: $isGranted")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeoIconButton(
                            onClick = onNavigateBack,
                            containerColor = NeoWhite,
                            modifier = Modifier.size(42.dp).testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = NeoBlack,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SETTINGS",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge,
                            letterSpacing = 0.5.sp,
                            color = NeoBlack
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(NeoBlack)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Navigation with Neo Styling
            TabRow(
                selectedTabIndex = settingsTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NeoBlack,
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(NeoBlack)
                    )
                },
                indicator = { tabPositions ->
                    if (settingsTab < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[settingsTab])
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(NeoYellow)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = listOf(
                    "STATS" to "settings_tab_stats",
                    "PREFERENCES" to "settings_tab_prefs",
                    "SRS SETTINGS" to "settings_tab_srs"
                )
                tabs.forEachIndexed { index, (title, tag) ->
                    val isSelected = settingsTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { settingsTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = NeoBlack,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        },
                        modifier = Modifier.testTag(tag)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (settingsTab) {
                    0 -> {
                        // STREAK CARD
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoYellow
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(NeoWhite, shape = RoundedCornerShape(10.dp))
                                        .border(2.dp, NeoBlack, shape = RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Streak",
                                        tint = NeoBlack,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "CURRENT STREAK: $currentStreak DAYS",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = NeoBlack
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Longest Streak: $longestStreak days",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeoBlack.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // REVIEW HISTORY CARD
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "REVIEW HISTORY", containerColor = NeoCyan)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "TODAY",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack.copy(alpha = 0.7f)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$reviewsToday REVIEWS",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "THIS WEEK",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack.copy(alpha = 0.7f)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$reviewsThisWeek REVIEWS",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(NeoBlack.copy(alpha = 0.15f))
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "SUCCESS RATE: ${String.format("%.1f", successRate)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = NeoBlack
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Percentage of flashcard reviews rated Good or Easy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeoBlack.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // DECK COMPOSITION CARD
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "DECK COMPOSITION ($totalWordsCount WORDS)", containerColor = NeoGreen)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val total = totalWordsCount.coerceAtLeast(1)
                                val masteredPct = wordsMasteredCount.toFloat() / total
                                val learningPct = wordsLearningCount.toFloat() / total
                                val newPct = wordsNewCount.toFloat() / total
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(18.dp)
                                        .background(NeoWhite, shape = RoundedCornerShape(4.dp))
                                        .border(2.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
                                        .clip(RoundedCornerShape(4.dp))
                                ) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        if (masteredPct > 0) {
                                            Box(modifier = Modifier.fillMaxHeight().weight(masteredPct).background(NeoGreen))
                                        }
                                        if (learningPct > 0) {
                                            Box(modifier = Modifier.fillMaxHeight().weight(learningPct).background(NeoYellow))
                                        }
                                        if (newPct > 0) {
                                            Box(modifier = Modifier.fillMaxHeight().weight(newPct).background(Color(0xFF94A3B8)))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .background(NeoGreen, shape = RoundedCornerShape(3.dp))
                                                .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Mastered: $wordsMasteredCount",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = NeoBlack
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .background(NeoYellow, shape = RoundedCornerShape(3.dp))
                                                .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Learning: $wordsLearningCount",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = NeoBlack
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .background(Color(0xFF94A3B8), shape = RoundedCornerShape(3.dp))
                                                .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(3.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "New: $wordsNewCount",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                            color = NeoBlack
                                        )
                                    }
                                }
                            }
                        }

                        // READING PROGRESS CARD
                        NeoCard(
                            modifier = Modifier.fillMaxWidth().testTag("reading_progress_card"),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "READING PROGRESS", containerColor = NeoGreen)
                                Spacer(modifier = Modifier.height(16.dp))

                                val currentCefr = viewModel.getCefrLabel(currentReadingLevel)
                                val totalSessions = readingSessions.size

                                if (readingSessions.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = null,
                                                tint = NeoBlack.copy(alpha = 0.6f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Complete your first Daily Reading to start tracking progress.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = NeoBlack.copy(alpha = 0.8f),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Current Level: $currentCefr",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                } else {
                                    val totalScore = readingSessions.sumOf { it.score }
                                    val totalQuestions = readingSessions.sumOf { it.total }
                                    val avgPct = if (totalQuestions > 0) {
                                        (totalScore.toFloat() / totalQuestions.toFloat()) * 100f
                                    } else 0f

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                                .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "CURRENT LEVEL",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = NeoBlack.copy(alpha = 0.7f)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = currentCefr,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = NeoBlack
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "$totalSessions SESSIONS",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = NeoBlack.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                                .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "AVERAGE SCORE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = NeoBlack.copy(alpha = 0.7f)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${avgPct.toInt()}% average",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = NeoBlack
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "$totalScore/$totalQuestions correct",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = NeoBlack.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(2.dp)
                                            .background(NeoBlack.copy(alpha = 0.15f))
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    val lastSessions = readingSessions.takeLast(10)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RECENT SESSIONS (${lastSessions.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Text(
                                            text = "Last 10 results",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeoBlack.copy(alpha = 0.6f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            lastSessions.forEach { session ->
                                                val ratio = if (session.total > 0) session.score.toFloat() / session.total.toFloat() else 0f
                                                val barHeight = (ratio * 36).dp.coerceAtLeast(8.dp)
                                                val barColor = when {
                                                    ratio >= 0.8f -> NeoGreen
                                                    ratio >= 0.5f -> NeoYellow
                                                    else -> NeoRed
                                                }
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Bottom,
                                                    modifier = Modifier.fillMaxHeight()
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(18.dp)
                                                            .height(barHeight)
                                                            .background(barColor, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                            .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${session.score}",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = NeoBlack
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // AI USAGE TODAY CARD
                        NeoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_usage_card"),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "AI USAGE TODAY", containerColor = NeoPurple)
                                Spacer(modifier = Modifier.height(16.dp))

                                if (aiUsageStats.geminiCallsToday == 0 && aiUsageStats.openRouterCallsToday == 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No AI requests yet today",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = NeoBlack.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.testTag("ai_usage_empty_text")
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                                .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = "Gemini: ${aiUsageStats.geminiCallsToday} requests",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack,
                                                modifier = Modifier.testTag("ai_usage_gemini_row")
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                                .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = "Backup (OpenRouter): ${aiUsageStats.openRouterCallsToday} requests",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack,
                                                modifier = Modifier.testTag("ai_usage_backup_row")
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        val lastProviderLabel = if (aiUsageStats.lastProviderUsed.equals("openrouter", ignoreCase = true)) {
                                            "Backup"
                                        } else {
                                            "Gemini"
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        if (lastProviderLabel == "Backup") NeoYellow else NeoGreen,
                                                        shape = CircleShape
                                                    )
                                                    .border(1.5.dp, NeoBlack, shape = CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Last response from: $lastProviderLabel",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack.copy(alpha = 0.8f),
                                                modifier = Modifier.testTag("ai_usage_last_provider")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // LOCK GEMINI CARD (Next to AI USAGE TODAY)
                        NeoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lock_gemini_card"),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "GEMINI LOCK", containerColor = NeoYellow)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setGeminiLocked(!geminiLocked) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Lock Gemini for Today",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (geminiLocked) {
                                                "Gemini locked â€” using backup (OpenRouter)"
                                            } else {
                                                "Gemini active"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (geminiLocked) NeoRed else NeoGreen,
                                            modifier = Modifier.testTag("gemini_lock_status_text")
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Switch(
                                        checked = geminiLocked,
                                        onCheckedChange = { viewModel.setGeminiLocked(it) },
                                        modifier = Modifier.testTag("lock_gemini_switch")
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // SECTION: My English Level
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "MY ENGLISH LEVEL", containerColor = NeoGreen)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Proficiency Level",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = NeoBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Choose your CEFR level to calibrate Daily Reading passage difficulty",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeoBlack.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("A1", "A2", "B1", "B2", "C1").forEach { levelOption ->
                                        val isSelected = userEnglishLevel.equals(levelOption, ignoreCase = true)
                                        NeoChip(
                                            text = levelOption,
                                            selected = isSelected,
                                            onClick = { viewModel.setUserEnglishLevel(levelOption) },
                                            selectedColor = NeoYellow,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("english_level_chip_$levelOption")
                                        )
                                    }
                                }
                            }
                        }

                        // SECTION: Audio & Pronunciation
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "AUDIO & PRONUNCIATION", containerColor = NeoCyan)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Auto-pronounce
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setAutoPronounceEnabled(!autoPronounce) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Auto-Pronounce Words",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Speak word pronunciation automatically when cards appear",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Switch(
                                        checked = autoPronounce,
                                        onCheckedChange = { viewModel.setAutoPronounceEnabled(it) },
                                        modifier = Modifier.testTag("auto_pronounce_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Pronunciation Voice Source Selector
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Pronunciation Voice Source",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = NeoBlack
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (pronunciationSource) {
                                            "online" -> "Online (Gemini AI Voice): Neural audio generated via Google Gemini TTS"
                                            "device" -> "Device Voice (TTS): Built-in Android speech synthesizer"
                                            else -> "Auto (Gemini AI, then Device): Gemini voice first, silent fallback to device TTS"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeoBlack.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Auto" to "auto",
                                            "Online" to "online",
                                            "Device" to "device"
                                        ).forEach { (label, value) ->
                                            val isSelected = pronunciationSource == value
                                            NeoChip(
                                                text = label,
                                                selected = isSelected,
                                                onClick = { viewModel.setPronunciationSource(value) },
                                                selectedColor = NeoYellow,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("pronunciation_source_chip_$value")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    val displaySource = if (lastPronunciationSource.isNotBlank()) lastPronunciationSource else "Not used yet"
                                    val sourceColor = when (lastPronunciationSource) {
                                        "Gemini Key 1" -> NeoGreen
                                        "Gemini Key 2" -> NeoCyan
                                        "Device TTS" -> NeoYellow
                                        else -> NeoCream
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.testTag("last_pronunciation_source_audio_row")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(sourceColor, shape = CircleShape)
                                                .border(1.5.dp, NeoBlack, shape = CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Last pronunciation source: $displaySource",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack.copy(alpha = 0.8f),
                                            modifier = Modifier.testTag("last_pronunciation_source_text")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Accent Selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Pronunciation Accent",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Voice and phonetic spelling (US vs UK)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("US" to "US", "UK" to "UK").forEach { (label, value) ->
                                            val isSelected = selectedAccent == value
                                            NeoChip(
                                                text = label,
                                                selected = isSelected,
                                                onClick = { viewModel.setSelectedAccent(value) },
                                                selectedColor = NeoYellow,
                                                modifier = Modifier.testTag("accent_chip_$value")
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Speech Speed
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Speech Speed",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Playback speed of the speech engine",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Normal" to "Normal", "Slow" to "Slow").forEach { (label, value) ->
                                            val isSelected = speechSpeed == value
                                            NeoChip(
                                                text = label,
                                                selected = isSelected,
                                                onClick = { viewModel.setSpeechSpeed(value) },
                                                selectedColor = NeoYellow,
                                                modifier = Modifier.testTag("speech_speed_chip_$value")
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SECTION: Gemini TTS API Keys
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "GEMINI TTS API KEYS", containerColor = NeoGreen)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Gemini API Keys",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = NeoBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Store Gemini API keys for online pronunciation. If the active key hits rate limits (HTTP 429), remaining keys are tried in order automatically as fallback.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeoBlack.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                ApiKeyDirectLinkBanner(
                                    title = "Get Gemini API Key (Free)",
                                    subtitle = "aistudio.google.com/app/apikey",
                                    url = "https://aistudio.google.com/app/apikey",
                                    buttonText = "GET KEY â†—",
                                    badgeColor = NeoYellow,
                                    testTag = "gemini_tts_direct_link_banner"
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Active Key Status Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Active API Key",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (geminiApiKeys.isEmpty()) {
                                                "No keys added (device TTS fallback)"
                                            } else {
                                                "Key ${activeKeyIndex + 1} is currently active"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (geminiApiKeys.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    // Active Key Selection Toggle Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        geminiApiKeys.forEachIndexed { idx, _ ->
                                            NeoChip(
                                                text = if (activeKeyIndex == idx) "Active: Key ${idx + 1}" else "Select Key ${idx + 1}",
                                                selected = activeKeyIndex == idx,
                                                onClick = { viewModel.setActiveKeyIndex(idx) },
                                                selectedColor = NeoYellow,
                                                modifier = Modifier.testTag("active_key_${idx + 1}_chip")
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                val displaySourceKeys = if (lastPronunciationSource.isNotBlank()) lastPronunciationSource else "Not used yet"
                                val sourceColorKeys = when {
                                    lastPronunciationSource.startsWith("Gemini Key") -> NeoGreen
                                    lastPronunciationSource == "Device TTS" -> NeoYellow
                                    else -> NeoCream
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.testTag("last_pronunciation_source_keys_row")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(sourceColorKeys, shape = CircleShape)
                                            .border(1.5.dp, NeoBlack, shape = CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Last pronunciation source: $displaySourceKeys",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Black,
                                        color = NeoBlack.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                Spacer(modifier = Modifier.height(14.dp))

                                // Dynamic list of Gemini API Keys
                                if (geminiApiKeys.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "No Gemini API keys configured. Tap \"+ Add Gemini Key\" below to add one.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = NeoBlack.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val uriHandler = LocalUriHandler.current
                                            val context = LocalContext.current
                                            Row(
                                                modifier = Modifier
                                                    .clickable {
                                                        try {
                                                            uriHandler.openUri("https://aistudio.google.com/app/apikey")
                                                        } catch (_: Exception) {
                                                            try {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")).apply {
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                Log.e("ApiKeyLink", "Error opening link", e)
                                                            }
                                                        }
                                                    }
                                                    .padding(4.dp)
                                                    .testTag("gemini_tts_empty_get_key_link"),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0B57D0),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Get your free Gemini API key here â†—",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0B57D0),
                                                    textDecoration = TextDecoration.Underline
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                } else {
                                    geminiApiKeys.forEachIndexed { index, apiKey ->
                                        GeminiKeyRowItem(
                                            index = index,
                                            apiKey = apiKey,
                                            isActive = activeKeyIndex == index,
                                            onKeyChange = { updated ->
                                                viewModel.updateGeminiApiKey(index, updated)
                                            },
                                            onSelectActive = {
                                                viewModel.setActiveKeyIndex(index)
                                            },
                                            onDelete = {
                                                viewModel.removeGeminiApiKey(index)
                                            },
                                            directLinkUrl = "https://aistudio.google.com/app/apikey",
                                            directLinkLabel = "Get key from Google AI Studio (Free) â†—"
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                }

                                // "+ Add Gemini Key" Button
                                NeoButton(
                                    onClick = {
                                        viewModel.addGeminiApiKey("")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("add_gemini_key_button"),
                                    containerColor = NeoYellow
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Gemini Key",
                                        tint = NeoBlack,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+ Add Gemini Key",
                                        fontWeight = FontWeight.Black,
                                        color = NeoBlack
                                    )
                                }
                            }
                        }

                        // SECTION: Gemini API Keys (Meaning & Reading)
                        NeoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_meaning_keys_card"),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "GEMINI API KEYS (MEANING & READING)", containerColor = NeoGreen)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Gemini API Keys",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = NeoBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Store Gemini API keys for word definitions, explanations, examples, and Daily Reading generation. If the active key hits rate limits (HTTP 429), remaining keys are tried automatically in order before falling back to OpenRouter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeoBlack.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                ApiKeyDirectLinkBanner(
                                    title = "Get Gemini API Key (Free)",
                                    subtitle = "aistudio.google.com/app/apikey",
                                    url = "https://aistudio.google.com/app/apikey",
                                    buttonText = "GET KEY â†—",
                                    badgeColor = NeoYellow,
                                    testTag = "gemini_meaning_direct_link_banner"
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Active Key Status Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Active API Key",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (geminiMeaningApiKeys.isEmpty()) {
                                                "No keys added (falls back to OpenRouter)"
                                            } else {
                                                "Key ${geminiMeaningActiveKeyIndex + 1} is currently active"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (geminiMeaningApiKeys.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    // Active Key Selection Toggle Chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        geminiMeaningApiKeys.forEachIndexed { idx, _ ->
                                            NeoChip(
                                                text = if (geminiMeaningActiveKeyIndex == idx) "Active: Key ${idx + 1}" else "Select Key ${idx + 1}",
                                                selected = geminiMeaningActiveKeyIndex == idx,
                                                onClick = { viewModel.setGeminiMeaningActiveKeyIndex(idx) },
                                                selectedColor = NeoYellow,
                                                modifier = Modifier.testTag("active_meaning_key_${idx + 1}_chip")
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                Spacer(modifier = Modifier.height(14.dp))

                                // Dynamic list of Gemini Meaning API Keys
                                if (geminiMeaningApiKeys.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NeoCream, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "No Gemini API keys configured. Tap \"+ Add Gemini Key\" below to add one.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = NeoBlack.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val uriHandler = LocalUriHandler.current
                                            val context = LocalContext.current
                                            Row(
                                                modifier = Modifier
                                                    .clickable {
                                                        try {
                                                            uriHandler.openUri("https://aistudio.google.com/app/apikey")
                                                        } catch (_: Exception) {
                                                            try {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")).apply {
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                Log.e("ApiKeyLink", "Error opening link", e)
                                                            }
                                                        }
                                                    }
                                                    .padding(4.dp)
                                                    .testTag("gemini_meaning_empty_get_key_link"),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = null,
                                                    tint = Color(0xFF0B57D0),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Get your free Gemini API key here â†—",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0B57D0),
                                                    textDecoration = TextDecoration.Underline
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                } else {
                                    geminiMeaningApiKeys.forEachIndexed { index, apiKey ->
                                        GeminiKeyRowItem(
                                            index = index,
                                            apiKey = apiKey,
                                            isActive = geminiMeaningActiveKeyIndex == index,
                                            onKeyChange = { updated ->
                                                viewModel.updateGeminiMeaningApiKey(index, updated)
                                            },
                                            onSelectActive = {
                                                viewModel.setGeminiMeaningActiveKeyIndex(index)
                                            },
                                            onDelete = {
                                                viewModel.removeGeminiMeaningApiKey(index)
                                            },
                                            labelPrefix = "Gemini Key",
                                            testTagPrefix = "gemini_meaning_key",
                                            directLinkUrl = "https://aistudio.google.com/app/apikey",
                                            directLinkLabel = "Get key from Google AI Studio (Free) â†—"
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                }

                                // "+ Add Gemini Key" Button
                                NeoButton(
                                    onClick = {
                                        viewModel.addGeminiMeaningApiKey("")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("add_gemini_meaning_key_button"),
                                    containerColor = NeoYellow
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Gemini Key",
                                        tint = NeoBlack,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+ Add Gemini Key",
                                        fontWeight = FontWeight.Black,
                                        color = NeoBlack
                                    )
                                }

                                val hasAnyValidMeaningKey = geminiMeaningApiKeys.any { it.trim().isNotBlank() }
                                if (!hasAnyValidMeaningKey) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NeoCream, shape = RoundedCornerShape(6.dp))
                                            .border(1.5.dp, NeoBlack.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "No key set â€” this feature is disabled until you add one.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NeoRed,
                                            modifier = Modifier.testTag("gemini_meaning_disabled_hint")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Gemini Model Selector
                                Text(
                                    text = "Gemini Model",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = NeoBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Select model for word lookup and Daily Reading generation",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeoBlack.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                var meaningModelMenuExpanded by remember { mutableStateOf(false) }
                                val commonGeminiModels = listOf(
                                    "gemini-3.5-flash-lite",
                                    "gemini-4-hydrogen",
                                    "gemini-3.5-flash",
                                    "gemini-3.5-pro",
                                    "gemini-2.5-flash",
                                    "gemini-2.5-pro",
                                    "gemini-2.0-flash",
                                    "gemini-2.0-flash-lite",
                                    "gemini-1.5-flash",
                                    "gemini-1.5-pro",
                                    "Custom..."
                                )

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .neoShadow(offsetX = 2.dp, offsetY = 2.dp, shadowColor = NeoBlack, shape = RoundedCornerShape(6.dp))
                                            .background(NeoWhite, shape = RoundedCornerShape(6.dp))
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                            .clickable { meaningModelMenuExpanded = true }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                            .testTag("gemini_model_selector_dropdown")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (geminiMeaningModelChoice == "Custom..." || geminiMeaningModelChoice.equals("custom", ignoreCase = true)) {
                                                    "Custom (${geminiMeaningCustomModel.ifBlank { "None" }})"
                                                } else {
                                                    when (geminiMeaningModelChoice) {
                                                        "gemini-3.5-flash-lite" -> "gemini-3.5-flash-lite (Flash Lite 3.5)"
                                                        "gemini-4-hydrogen" -> "gemini-4-hydrogen (Hydrogen)"
                                                        "gemini-3.5-flash" -> "gemini-3.5-flash (Flash 3.5)"
                                                        "gemini-3.5-pro" -> "gemini-3.5-pro (Pro 3.5)"
                                                        "gemini-2.5-flash" -> "gemini-2.5-flash (Default)"
                                                        else -> geminiMeaningModelChoice
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = NeoBlack
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Gemini Model",
                                                tint = NeoBlack
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = meaningModelMenuExpanded,
                                        onDismissRequest = { meaningModelMenuExpanded = false },
                                        modifier = Modifier
                                            .background(NeoWhite)
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                    ) {
                                        commonGeminiModels.forEach { modelOption ->
                                            val isSelected = geminiMeaningModelChoice.equals(modelOption, ignoreCase = true)
                                            val labelBadge = when (modelOption) {
                                                "gemini-3.5-flash-lite" -> "Flash Lite 3.5"
                                                "gemini-4-hydrogen" -> "Hydrogen"
                                                "gemini-3.5-flash" -> "Flash 3.5"
                                                "gemini-3.5-pro" -> "Pro 3.5"
                                                "gemini-2.5-flash" -> "Flash 2.5 (Default)"
                                                else -> null
                                            }
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = modelOption,
                                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                                                color = NeoBlack
                                                            )
                                                            if (labelBadge != null) {
                                                                Text(
                                                                    text = labelBadge,
                                                                    fontSize = 11.sp,
                                                                    color = NeoBlack.copy(alpha = 0.6f),
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                        if (isSelected) {
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Text(
                                                                text = "âœ“",
                                                                fontWeight = FontWeight.Black,
                                                                color = NeoGreen
                                                            )
                                                        }
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setGeminiMeaningModelChoice(modelOption)
                                                    meaningModelMenuExpanded = false
                                                },
                                                modifier = Modifier.testTag("model_option_$modelOption")
                                            )
                                        }
                                    }
                                }

                                if (geminiMeaningModelChoice == "Custom..." || geminiMeaningModelChoice.equals("custom", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(NeoWhite, shape = RoundedCornerShape(6.dp))
                                            .border(1.5.dp, NeoBlack.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
                                    ) {
                                        OutlinedTextField(
                                            value = geminiMeaningCustomModel,
                                            onValueChange = { viewModel.setGeminiMeaningCustomModel(it) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("custom_gemini_model_input"),
                                            placeholder = {
                                                Text(
                                                    "Enter custom model ID (e.g. gemini-3.5-flash-lite, gemini-4-hydrogen)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(color = NeoBlack.copy(alpha = 0.4f))
                                                )
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = NeoWhite,
                                                unfocusedContainerColor = NeoWhite,
                                                focusedTextColor = NeoBlack,
                                                unfocusedTextColor = NeoBlack,
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                cursorColor = NeoBlack
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // SECTION: OpenRouter API Key
                        SingleApiKeyCard(
                            title = "OPENROUTER API KEY",
                            badgeColor = NeoYellow,
                            heading = "OpenRouter API Key",
                            description = "Fallback API key used when Gemini hits rate limits (HTTP 429) or is manually locked. Automatically serves word lookup and Daily Reading.",
                            currentKey = openRouterApiKey,
                            placeholder = "Enter OpenRouter API Key",
                            testTagPrefix = "openrouter_key",
                            directLinkUrl = "https://openrouter.ai/keys",
                            directLinkLabel = "Get OpenRouter API Key (Free)",
                            directLinkSubtitle = "openrouter.ai/keys (Free tier models supported)",
                            onSaveKey = { viewModel.setOpenRouterApiKey(it) },
                            onClearKey = { viewModel.setOpenRouterApiKey("") }
                        )

                        // SECTION: Pexels API Key
                        SingleApiKeyCard(
                            title = "PEXELS API KEY",
                            badgeColor = NeoCyan,
                            heading = "Pexels API Key",
                            description = "API key used when Image Source is set to 'Real Photo' to find real contextual photographs for English vocabulary.",
                            currentKey = pexelsApiKey,
                            placeholder = "Enter Pexels API Key",
                            testTagPrefix = "pexels_key",
                            directLinkUrl = "https://www.pexels.com/api/",
                            directLinkLabel = "Get Pexels API Key (Free)",
                            directLinkSubtitle = "pexels.com/api (Free developer access)",
                            onSaveKey = { viewModel.setPexelsApiKey(it) },
                            onClearKey = { viewModel.setPexelsApiKey("") }
                        )

                        // SECTION: Appearance & Personalization
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "APPEARANCE", containerColor = NeoYellow)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Accent Color
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Text(
                                        text = "Primary Accent Color",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Black,
                                        color = NeoBlack
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Customize highlight colors across the app",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeoBlack.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    val colorOptions = listOf(
                                        "yellow" to NeoYellow,
                                        "blue" to Color(0xFF0B57D0),
                                        "green" to Color(0xFF137333),
                                        "orange" to Color(0xFFE65100),
                                        "pink" to Color(0xFFB32656),
                                        "teal" to Color(0xFF00796B)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        colorOptions.forEach { (name, color) ->
                                            val isSelected = selectedThemeColor.lowercase() == name
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .neoShadow(offsetX = 2.dp, offsetY = 2.dp, shadowColor = NeoBlack, shape = CircleShape)
                                                    .background(color, shape = CircleShape)
                                                    .border(2.dp, NeoBlack, shape = CircleShape)
                                                    .clickable { viewModel.setSelectedThemeColor(name) }
                                                    .testTag("color_theme_$name"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = NeoBlack,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                Spacer(modifier = Modifier.height(12.dp))

                                // AI Visualizations
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setAiVisualizationsEnabled(!aiVisualizationsEnabled) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "AI Visualizations",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Enable generated mnemonic image illustrations for words",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Switch(
                                        checked = aiVisualizationsEnabled,
                                        onCheckedChange = { viewModel.setAiVisualizationsEnabled(it) },
                                        modifier = Modifier.testTag("ai_visualizations_switch")
                                    )
                                }
                            }
                        }

                        // SECTION: Word Image Source
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "WORD IMAGE SOURCE", containerColor = NeoPurple)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Vocabulary Image Source",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Select whether new words show AI-generated illustrations or real photos",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "AI Generated" to "ai",
                                        "Real Photo" to "web"
                                    ).forEach { (label, value) ->
                                        val isSelected = imageSourcePreference == value
                                        NeoChip(
                                            text = label,
                                            selected = isSelected,
                                            onClick = { viewModel.setImageSourcePreference(value) },
                                            selectedColor = NeoYellow,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("image_source_chip_$value")
                                        )
                                    }
                                }
                            }
                        }

                        // SECTION: Real Photo Search Source
                        NeoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pexels_query_source_card"),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "REAL PHOTO SEARCH SOURCE", containerColor = NeoCyan)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Query Source Field",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = when (pexelsQuerySource) {
                                                "word" -> "Search using the word/phrase itself"
                                                "definition" -> "Search using keywords from the definition text"
                                                else -> "Search using keywords from the AI visual scene description"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "Word" to "word",
                                        "Definition" to "definition",
                                        "AI Description" to "image_prompt"
                                    ).forEach { (label, value) ->
                                        val isSelected = pexelsQuerySource == value
                                        NeoChip(
                                            text = label,
                                            selected = isSelected,
                                            onClick = { viewModel.setPexelsQuerySource(value) },
                                            selectedColor = NeoYellow,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("pexels_query_source_chip_$value")
                                        )
                                    }
                                }
                            }
                        }

                        // SECTION: Notifications & Reminders
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "STUDY REMINDERS", containerColor = NeoOrange)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val nextState = !remindersEnabled
                                            if (nextState) {
                                                if (permissionState == "granted") {
                                                    viewModel.setRemindersEnabled(true)
                                                } else {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    } else {
                                                        viewModel.setRemindersEnabled(true)
                                                    }
                                                }
                                            } else {
                                                viewModel.setRemindersEnabled(false)
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Daily Reminder Notifications",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Receive daily alerts for vocabulary due for review",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Switch(
                                        checked = remindersEnabled && permissionState == "granted",
                                        onCheckedChange = { nextState ->
                                            if (nextState) {
                                                if (permissionState == "granted") {
                                                    viewModel.setRemindersEnabled(true)
                                                } else {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    } else {
                                                        viewModel.setRemindersEnabled(true)
                                                    }
                                                }
                                            } else {
                                                viewModel.setRemindersEnabled(false)
                                            }
                                        },
                                        modifier = Modifier.testTag("study_reminders_switch")
                                    )
                                }

                                // UI Feedback for Permission Status
                                if (remindersEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    when (permissionState) {
                                        "granted" -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(NeoGreen.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp))
                                                    .border(2.dp, NeoGreen, shape = RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = NeoBlack,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Daily study reminders are active",
                                                        color = NeoBlack,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                        "denied" -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFFFFECEB), shape = RoundedCornerShape(8.dp))
                                                    .border(2.dp, NeoRed, shape = RoundedCornerShape(8.dp))
                                                    .padding(16.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "NOTIFICATIONS ARE BLOCKED",
                                                        color = NeoRed,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Please manually enable notifications in Android Settings > Apps > Redeus Dictionary > Notifications.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = NeoBlack
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    NeoDestructiveButton(
                                                        onClick = {
                                                            try {
                                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                                    data = Uri.fromParts("package", context.packageName, null)
                                                                }
                                                                context.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                Log.e("SettingsScreen", "Failed to open settings", e)
                                                            }
                                                        },
                                                        modifier = Modifier.testTag("open_settings_btn")
                                                    ) {
                                                        Text("OPEN SYSTEM SETTINGS", fontWeight = FontWeight.Black)
                                                    }
                                                }
                                            }
                                        }
                                        "default" -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                NeoButton(
                                                    onClick = {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        }
                                                    },
                                                    containerColor = NeoYellow,
                                                    modifier = Modifier.testTag("enable_notifications_btn")
                                                ) {
                                                    Text("ENABLE NOTIFICATIONS", fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (remindersEnabled && permissionState == "granted") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.setAlwaysNotifyEnabled(!alwaysNotifyEnabled) }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Always Notify Daily",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Black,
                                                color = NeoBlack
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Send notification even if no cards are currently due for review",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NeoBlack.copy(alpha = 0.7f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Switch(
                                            checked = alwaysNotifyEnabled,
                                            onCheckedChange = { viewModel.setAlwaysNotifyEnabled(it) },
                                            modifier = Modifier.testTag("always_notify_switch")
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // DETAILED STATISTICS SECTION HEADER
                        NeoCard(
                            modifier = Modifier.fillMaxWidth().testTag("detailed_stats_header_card"),
                            backgroundColor = NeoYellow
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(NeoWhite, shape = RoundedCornerShape(8.dp))
                                        .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = "Analytics",
                                        tint = NeoBlack,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "DETAILED SRS STATISTICS",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = NeoBlack
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Comprehensive spaced repetition memory performance",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeoBlack.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // 1. RETENTION RATE CARD
                        DetailedRetentionRateCard(stats = detailedSrsStats)

                        // 2. REVIEWS PER DAY CARD (LAST 30 DAYS)
                        ReviewsPerDayChartCard(stats = detailedSrsStats)

                        // 3. RATING DISTRIBUTION CARD (ALL-TIME)
                        RatingDistributionCard(dist = detailedSrsStats.ratingDistribution)

                        // 4. EASE FACTOR DISTRIBUTION CARD
                        EaseFactorDistributionCard(dist = detailedSrsStats.easeFactorDistribution)

                        // 5. 14-DAY FORECAST CARD
                        ForecastUpcomingReviewsCard(forecast = detailedSrsStats.forecastNext14Days)

                        // 6. STREAK CALENDAR HEATMAP CARD
                        StreakHeatmapCard(stats = detailedSrsStats)

                        // 7. LEECH DETECTION & MANAGEMENT CARD
                        LeechManagementCard(
                            leechWords = leechWords,
                            onClearLeechFlag = { viewModel.clearLeechFlag(it) }
                        )

                        // 8. SCHEDULING ALGORITHM SETTING CARD
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(NeoYellow, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = NeoBlack,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "SCHEDULING ALGORITHM",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Text(
                                            text = "Select algorithm used for spaced repetition reviews",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Algorithm Choice Row: SM-2 (Classic) vs FSRS (Experimental)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isSm2 = srsAlgorithm == "sm2"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSm2) NeoYellow else NeoWhite, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setSrsAlgorithm("sm2") }
                                            .padding(vertical = 12.dp, horizontal = 8.dp)
                                            .testTag("srs_algorithm_sm2_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "SM-2 (Classic)",
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = NeoBlack
                                            )
                                            Text(
                                                text = "Battle-tested standard",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeoBlack.copy(alpha = 0.6f)
                                            )
                                        }
                                    }

                                    val isFsrs = srsAlgorithm == "fsrs"
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isFsrs) NeoCyan else NeoWhite, shape = RoundedCornerShape(8.dp))
                                            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                                            .clickable { viewModel.setSrsAlgorithm("fsrs") }
                                            .padding(vertical = 12.dp, horizontal = 8.dp)
                                            .testTag("srs_algorithm_fsrs_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "FSRS (Experimental)",
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = NeoBlack
                                            )
                                            Text(
                                                text = "Modern DSR model",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeoBlack.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "New and less tested â€” you can switch back anytime.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    color = NeoBlack.copy(alpha = 0.7f)
                                )

                                // Target Retention setting (shown only when FSRS is active)
                                if (srsAlgorithm == "fsrs") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Target Retention",
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = NeoBlack
                                                )
                                                Text(
                                                    text = "Desired recall probability for scheduled reviews",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NeoBlack.copy(alpha = 0.7f)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(NeoCyan, shape = RoundedCornerShape(6.dp))
                                                    .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "$targetRetention%",
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = NeoBlack
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Slider(
                                            value = targetRetention.toFloat(),
                                            onValueChange = { viewModel.setTargetRetention(it.toInt()) },
                                            valueRange = 80f..97f,
                                            steps = 16,
                                            modifier = Modifier.fillMaxWidth().testTag("srs_target_retention_slider")
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("80% (Fewer reviews)", style = MaterialTheme.typography.labelSmall, color = NeoBlack.copy(alpha = 0.6f))
                                            Text("90% (Standard)", style = MaterialTheme.typography.labelSmall, color = NeoBlack.copy(alpha = 0.6f))
                                            Text("97% (High retention)", style = MaterialTheme.typography.labelSmall, color = NeoBlack.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        }

                        // ADVANCED ALGORITHM TOGGLE
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoWhite
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAdvancedSrsSliders = !showAdvancedSrsSliders }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Advanced SRS Parameters",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Fine-tune SM-2 repetition scheduling multipliers",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Switch(
                                        checked = showAdvancedSrsSliders,
                                        onCheckedChange = { showAdvancedSrsSliders = it },
                                        modifier = Modifier.testTag("advanced_srs_sliders_switch")
                                    )
                                }

                                if (showAdvancedSrsSliders) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.5.dp).background(NeoBlack.copy(alpha = 0.15f)))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Slider 1: Easy Bonus
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Easy Bonus", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge, color = NeoBlack)
                                            Box(
                                                modifier = Modifier
                                                    .background(NeoYellow, shape = RoundedCornerShape(6.dp))
                                                    .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = String.format("%.2f", easyBonus),
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = NeoBlack,
                                                    modifier = Modifier.testTag("easy_bonus_value")
                                                )
                                            }
                                        }
                                        Slider(
                                            value = easyBonus,
                                            onValueChange = { viewModel.setEasyBonus(it) },
                                            valueRange = 1.0f..3.0f,
                                            modifier = Modifier.testTag("easy_bonus_slider")
                                        )
                                        Text(
                                            text = "Extra multiplier applied to calculated interval when you tap 'Easy'.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NeoBlack.copy(alpha = 0.1f)))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Slider 2: Starting Ease
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Starting Ease", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge, color = NeoBlack)
                                            Box(
                                                modifier = Modifier
                                                    .background(NeoYellow, shape = RoundedCornerShape(6.dp))
                                                    .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = String.format("%.2f", startingEase),
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = NeoBlack,
                                                    modifier = Modifier.testTag("starting_ease_value")
                                                )
                                            }
                                        }
                                        Slider(
                                            value = startingEase,
                                            onValueChange = { viewModel.setStartingEase(it) },
                                            valueRange = 1.3f..4.0f,
                                            modifier = Modifier.testTag("starting_ease_slider")
                                        )
                                        Text(
                                            text = "The starting ease factor assigned to brand-new cards.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NeoBlack.copy(alpha = 0.1f)))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Slider 3: Interval Modifier
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Interval Modifier", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge, color = NeoBlack)
                                            Box(
                                                modifier = Modifier
                                                    .background(NeoYellow, shape = RoundedCornerShape(6.dp))
                                                    .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = String.format("%.2f", intervalModifier),
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = NeoBlack,
                                                    modifier = Modifier.testTag("interval_modifier_value")
                                                )
                                            }
                                        }
                                        Slider(
                                            value = intervalModifier,
                                            onValueChange = { viewModel.setIntervalModifier(it) },
                                            valueRange = 0.2f..2.0f,
                                            modifier = Modifier.testTag("interval_modifier_slider")
                                        )
                                        Text(
                                            text = "Global multiplier applied to all calculated intervals to compress or stretch scheduling.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NeoBlack.copy(alpha = 0.1f)))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Slider 4: Hard Interval Multiplier
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Hard Interval Multiplier", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge, color = NeoBlack)
                                            Box(
                                                modifier = Modifier
                                                    .background(NeoYellow, shape = RoundedCornerShape(6.dp))
                                                    .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = String.format("%.2f", hardIntervalMultiplier),
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = NeoBlack,
                                                    modifier = Modifier.testTag("hard_interval_multiplier_value")
                                                )
                                            }
                                        }
                                        Slider(
                                            value = hardIntervalMultiplier,
                                            onValueChange = { viewModel.setHardIntervalMultiplier(it) },
                                            valueRange = 1.0f..2.0f,
                                            modifier = Modifier.testTag("hard_interval_multiplier_slider")
                                        )
                                        Text(
                                            text = "Controls how much intervals grow when rating a card as 'Hard'.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(NeoBlack.copy(alpha = 0.1f)))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Slider 5: Leech Threshold
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Leech Threshold", fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge, color = NeoBlack)
                                            Box(
                                                modifier = Modifier
                                                    .background(NeoYellow, shape = RoundedCornerShape(6.dp))
                                                    .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "$leechThreshold lapses",
                                                    fontWeight = FontWeight.Black,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = NeoBlack,
                                                    modifier = Modifier.testTag("leech_threshold_value")
                                                )
                                            }
                                        }
                                        Slider(
                                            value = leechThreshold.toFloat(),
                                            onValueChange = { viewModel.setLeechThreshold(it.toInt()) },
                                            valueRange = 1f..20f,
                                            steps = 18,
                                            modifier = Modifier.testTag("leech_threshold_slider")
                                        )
                                        Text(
                                            text = "Number of lapses (Again ratings on learned cards) before a card is flagged as a Leech (Anki default: 8).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeoBlack.copy(alpha = 0.7f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // RESET SRS DEFAULT BUTTON
                                    NeoSecondaryButton(
                                        onClick = { viewModel.resetSrsSettingsToDefault() },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reset_srs_defaults_button")
                                    ) {
                                        Text("RESET SRS SETTINGS TO DEFAULTS", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                    }
                                }
                            }
                        }

                        // RESET ALL PROGRESS CARD
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFFFFECEB),
                            borderColor = NeoRed
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                NeoSectionBadge(title = "DANGER ZONE", containerColor = NeoRed, contentColor = NeoWhite)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Resetting progress will return all vocabulary cards to their initial learning stage.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeoBlack
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                NeoDestructiveButton(
                                    onClick = { showResetConfirmDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("reset_all_progress_button")
                                ) {
                                    Text("RESET ALL PROGRESS", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    viewModel: DictionaryViewModel,
    onDismiss: () -> Unit
) {
    SettingsScreen(viewModel = viewModel, onNavigateBack = onDismiss)
}

@Composable
fun ApiKeyDirectLinkBanner(
    title: String,
    subtitle: String,
    url: String,
    buttonText: String = "GET KEY â†—",
    containerColor: Color = NeoCream,
    badgeColor: Color = NeoYellow,
    testTag: String = "api_key_direct_link_banner"
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, shadowColor = NeoBlack, shape = RoundedCornerShape(8.dp))
            .background(containerColor, shape = RoundedCornerShape(8.dp))
            .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
            .clickable {
                try {
                    uriHandler.openUri(url)
                } catch (_: Exception) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("ApiKeyLink", "Error opening $url", e)
                    }
                }
            }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(NeoWhite, shape = RoundedCornerShape(6.dp))
                    .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open Link",
                    tint = NeoBlack,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = NeoBlack.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(badgeColor, shape = RoundedCornerShape(6.dp))
                .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = NeoBlack
            )
        }
    }
}

@Composable
fun GeminiKeyRowItem(
    index: Int,
    apiKey: String,
    isActive: Boolean,
    onKeyChange: (String) -> Unit,
    onSelectActive: () -> Unit,
    onDelete: () -> Unit,
    labelPrefix: String = "Gemini API Key",
    testTagPrefix: String = "gemini_key",
    directLinkUrl: String? = null,
    directLinkLabel: String? = null
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    var textInput by remember(apiKey) { mutableStateOf(apiKey) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neoShadow(offsetX = 3.dp, offsetY = 3.dp, shadowColor = NeoBlack, shape = RoundedCornerShape(8.dp))
            .background(if (isActive) NeoCream else NeoWhite, shape = RoundedCornerShape(8.dp))
            .border(2.dp, if (isActive) NeoBlack else NeoBlack.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectActive() }
            ) {
                RadioButton(
                    selected = isActive,
                    onClick = onSelectActive,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = NeoBlack,
                        unselectedColor = NeoBlack.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag("${testTagPrefix}_${index + 1}_radio")
                )
                Text(
                    text = "$labelPrefix ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                    color = NeoBlack
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .background(NeoGreen, shape = RoundedCornerShape(4.dp))
                            .border(1.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack
                        )
                    }
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_${testTagPrefix}_${index + 1}_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete $labelPrefix ${index + 1}",
                    tint = NeoBlack.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeoWhite, shape = RoundedCornerShape(6.dp))
                .border(1.5.dp, NeoBlack.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    onKeyChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_${index + 1}_input"),
                placeholder = {
                    Text(
                        "Enter $labelPrefix ${index + 1}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = NeoBlack.copy(alpha = 0.4f))
                    )
                },
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { isVisible = !isVisible },
                        modifier = Modifier.testTag("toggle_${testTagPrefix}_${index + 1}_visibility")
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isVisible) "Hide $labelPrefix ${index + 1}" else "Show $labelPrefix ${index + 1}",
                            tint = NeoBlack
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NeoWhite,
                    unfocusedContainerColor = NeoWhite,
                    focusedTextColor = NeoBlack,
                    unfocusedTextColor = NeoBlack,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = NeoBlack
                )
            )
        }

        if (!directLinkUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            val uriHandler = LocalUriHandler.current
            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            uriHandler.openUri(directLinkUrl)
                        } catch (_: Exception) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directLinkUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("ApiKeyLink", "Error opening $directLinkUrl", e)
                            }
                        }
                    }
                    .padding(vertical = 2.dp)
                    .testTag("${testTagPrefix}_${index + 1}_direct_link"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Color(0xFF0B57D0),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = directLinkLabel ?: "Get API Key â†—",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B57D0),
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
fun SingleApiKeyCard(
    title: String,
    badgeColor: Color,
    heading: String,
    description: String,
    currentKey: String,
    placeholder: String,
    testTagPrefix: String,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    disabledHint: String = "No key set â€” this feature is disabled until you add one.",
    directLinkUrl: String? = null,
    directLinkLabel: String? = null,
    directLinkSubtitle: String? = null
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }
    var textInput by remember(currentKey) { mutableStateOf(currentKey) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_card"),
        backgroundColor = NeoWhite
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            NeoSectionBadge(title = title, containerColor = badgeColor)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = heading,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = NeoBlack
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = NeoBlack.copy(alpha = 0.7f)
            )

            if (!directLinkUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                ApiKeyDirectLinkBanner(
                    title = directLinkLabel ?: "Get API Key",
                    subtitle = directLinkSubtitle ?: directLinkUrl.removePrefix("https://").removeSuffix("/"),
                    url = directLinkUrl,
                    buttonText = "GET KEY â†—",
                    badgeColor = badgeColor,
                    testTag = "${testTagPrefix}_direct_link_banner"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoWhite, shape = RoundedCornerShape(6.dp))
                    .border(1.5.dp, NeoBlack.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_input"),
                    placeholder = {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(color = NeoBlack.copy(alpha = 0.4f))
                        )
                    },
                    visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        IconButton(
                            onClick = { isVisible = !isVisible },
                            modifier = Modifier.testTag("${testTagPrefix}_visibility_toggle")
                        ) {
                            Icon(
                                imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isVisible) "Hide API Key" else "Show API Key",
                                tint = NeoBlack
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeoWhite,
                        unfocusedContainerColor = NeoWhite,
                        focusedTextColor = NeoBlack,
                        unfocusedTextColor = NeoBlack,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = NeoBlack
                    )
                )
            }

            if (!directLinkUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                uriHandler.openUri(directLinkUrl)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(directLinkUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("ApiKeyLink", "Error opening $directLinkUrl", e)
                                }
                            }
                        }
                        .padding(vertical = 2.dp)
                        .testTag("${testTagPrefix}_direct_link_under_input"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF0B57D0),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Get key directly: ${directLinkSubtitle ?: directLinkUrl} â†—",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B57D0),
                        textDecoration = TextDecoration.Underline
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoButton(
                    onClick = { onSaveKey(textInput.trim()) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_save_button"),
                    containerColor = NeoGreen
                ) {
                    Text("Save Key", fontWeight = FontWeight.Black, color = NeoBlack)
                }

                NeoButton(
                    onClick = {
                        textInput = ""
                        onClearKey()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${testTagPrefix}_clear_button"),
                    containerColor = NeoPink
                ) {
                    Text("Clear", fontWeight = FontWeight.Black, color = NeoBlack)
                }
            }

            if (currentKey.isBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoCream, shape = RoundedCornerShape(6.dp))
                        .border(1.5.dp, NeoBlack.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = disabledHint,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = NeoRed,
                        modifier = Modifier.testTag("${testTagPrefix}_disabled_hint")
                    )
                }
            }
        }
    }
}

@Composable
fun DetailedRetentionRateCard(stats: DetailedSrsStats) {
    NeoCard(
        modifier = Modifier.fillMaxWidth().testTag("retention_rate_card"),
        backgroundColor = NeoWhite
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            NeoSectionBadge(title = "RETENTION RATE", containerColor = NeoGreen)
            Spacer(modifier = Modifier.height(14.dp))

            // Main highlight box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoCream, shape = RoundedCornerShape(8.dp))
                    .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RETENTION: ${String.format("%.1f", stats.last30DaysRetentionRate)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Last 30 Days (${stats.totalReviewsLast30Days} reviews)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack.copy(alpha = 0.7f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(NeoGreen, shape = RoundedCornerShape(6.dp))
                            .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${stats.last30DaysRetentionRate.toInt()}%",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = NeoBlack
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Comparison Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(NeoWhite, shape = RoundedCornerShape(8.dp))
                        .border(1.5.dp, NeoBlack.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "LAST 30 DAYS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.1f", stats.last30DaysRetentionRate)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack
                        )
                        Text(
                            text = "${stats.totalReviewsLast30Days} reviews",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeoBlack.copy(alpha = 0.6f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(NeoWhite, shape = RoundedCornerShape(8.dp))
                        .border(1.5.dp, NeoBlack.copy(alpha = 0.4f), shape = RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "ALL-TIME",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${String.format("%.1f", stats.overallRetentionRate)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack
                        )
                        Text(
                            text = "${stats.totalReviewsAllTime} reviews",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeoBlack.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Percentage of flashcard reviews where cards were recalled successfully (Hard, Good, or Easy).",
                style = MaterialTheme.typography.bodySmall,
                color = NeoBlack.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ReviewsPerDayChartCard(stats: DetailedSrsStats) {
    val dailyList = stats.reviewsPerDayLast30Days
    val maxCount = dailyList.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    NeoCard(
        modifier = Modifier.fillMaxWidth().testTag("reviews_per_day_chart_card"),
        backgroundColor = NeoWhite
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            NeoSectionBadge(title = "REVIEWS PER DAY (LAST 30 DAYS)", containerColor = NeoCyan)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total 30-Day Reviews: ${stats.totalReviewsLast30Days}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack
                )
                Text(
                    text = "Peak: $maxCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeoBlack.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bar chart container with horizontal scrolling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoCream, shape = RoundedCornerShape(8.dp))
                    .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyList.forEachIndexed { index, item ->
                        val isToday = index == dailyList.size - 1
                        val barHeight = ((item.count.toFloat() / maxCount) * 50).dp.coerceAtLeast(if (item.count > 0) 6.dp else 2.dp)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.height(84.dp)
                        ) {
                            if (item.count > 0) {
                                Text(
                                    text = "${item.count}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeoBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(barHeight)
                                    .background(
                                        if (isToday) NeoYellow else if (item.count > 0) NeoCyan else NeoBlack.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (item.count > 0) NeoBlack else Color.Transparent,
                                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.dateLabel,
                                fontSize = 8.sp,
                                fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal,
                                color = if (isToday) NeoBlack else NeoBlack.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDistributionCard(dist: SrsRatingDistribution) {
    val total = dist.totalCount.coerceAtLeast(1)
    val againRatio = dist.againCount.toFloat() / total
    val hardRatio = dist.hardCount.toFloat() / total
    val goodRatio = dist.goodCount.toFloat() / total
    val easyRatio = dist.easyCount.toFloat() / total

    NeoCard(
        modifier = Modifier.fillMaxWidth().testTag("rating_distribution_card"),
        backgroundColor = NeoWhite
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            NeoSectionBadge(title = "RATING BREAKDOWN (ALL-TIME)", containerColor = NeoPurple)
            Spacer(modifier = Modifier.height(14.dp))

            // Stacked Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(NeoCream, shape = RoundedCornerShape(4.dp))
                    .border(2.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
            ) {
                if (dist.totalCount == 0) {
                    Box(modifier = Modifier.fillMaxSize().background(NeoCream))
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (againRatio > 0) Box(modifier = Modifier.fillMaxHeight().weight(againRatio).background(NeoRed))
                        if (hardRatio > 0) Box(modifier = Modifier.fillMaxHeight().weight(hardRatio).background(NeoOrange))
                        if (goodRatio > 0) Box(modifier = Modifier.fillMaxHeight().weight(goodRatio).background(NeoGreen))
                        if (easyRatio > 0) Box(modifier = Modifier.fillMaxHeight().weight(easyRatio).background(NeoCyan))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2x2 Grid of Ratings
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingStatChip(
                        label = "AGAIN",
                        count = dist.againCount,
                        pct = dist.againPct,
                        color = NeoRed,
                        modifier = Modifier.weight(1f)
                    )
                    RatingStatChip(
                        label = "HARD",
                        count = dist.hardCount,
                        pct = dist.hardPct,
                        color = NeoOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RatingStatChip(
                        label = "GOOD",
                        count = dist.goodCount,
                        pct = dist.goodPct,
                        color = NeoGreen,
                        modifier = Modifier.weight(1f)
                    )
                    RatingStatChip(
                        label = "EASY",
                        count = dist.easyCount,
                        pct = dist.easyPct,
                        color = NeoCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingStatChip(
    label: String,
    count: Int,
    pct: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(NeoCream, shape = RoundedCornerShape(6.dp))
            .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, shape = RoundedCornerShape(3.dp))
                        .border(1.dp, NeoBlack, shape = RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack
                )
            }
            Text(
                text = "$count (${String.format("%.1f", pct)}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeoBlack.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun EaseFactorDistributionCard(dist: EaseFactorDistribution) {
    val total = dist.totalWords.coerceAtLeast(1)

    val buckets = listOf(
        Triple("< 1.8 (Very Hard)", dist.bucketUnder1_8, NeoRed),
        Triple("1.8 - 2.2 (Hard)", dist.bucket1_8to2_2, NeoOrange),
        Triple("2.2 - 2.6 (Moderate)", dist.bucket2_2to2_6, NeoYellow),
        Triple("2.6 - 3.0 (Easy)", dist.bucket2_6to3_0, NeoGreen),
        Triple("> 3.0 (Very Easy)", dist.bucketOver3_0, NeoCyan)
    )

    NeoCard(
        modifier = Modifier.fillMaxWidth().testTag("ease_factor_dist_card"),
        backgroundColor = NeoWhite
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            NeoSectionBadge(title = "EASE FACTOR DISTRIBUTION", containerColor = NeoYellow)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Breakdown of card difficulty across your total deck (${dist.totalWords} words).",
                style = MaterialTheme.typography.bodySmall,
                color = NeoBlack.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                buckets.forEach { (label, count, color) ->
                    val ratio = count.toFloat() / total
                    val pct = ratio * 100f

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoBlack
                            )
                            Text(
                                text = "$count words (${String.format("%.1f", pct)}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoBlack.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(NeoCream, shape = RoundedCornerShape(3.dp))
                                .border(1.dp, NeoBlack, shape = RoundedCornerShape(3.dp))
                                .clip(RoundedCornerShape(3.dp))
                        ) {
                            if (ratio > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(ratio)
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastUpcomingReviewsCard(forecast: List<ForecastDayStat>) {
    val maxCount = forecast.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val totalUpcoming = forecast.sumOf { it.count }

    NeoCard(
        modifier = Modifier.fillMaxWidth().testTag("forecast_chart_card"),
        backgroundColor = NeoWhite
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            NeoSectionBadge(title = "14-DAY REVIEW FORECAST", containerColor = NeoOrange)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Upcoming review load scheduled for the next 14 days ($totalUpcoming total due).",
                style = MaterialTheme.typography.bodySmall,
                color = NeoBlack.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoCream, shape = RoundedCornerShape(8.dp))
                    .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    forecast.forEach { item ->
                        val isToday = item.dayOffset == 0
                        val barHeight = ((item.count.toFloat() / maxCount) * 50).dp.coerceAtLeast(if (item.count > 0) 6.dp else 2.dp)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.height(84.dp)
                        ) {
                            if (item.count > 0) {
                                Text(
                                    text = "${item.count}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeoBlack
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(barHeight)
                                    .background(
                                        if (isToday) NeoOrange else if (item.count > 0) NeoCyan else NeoBlack.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (item.count > 0) NeoBlack else Color.Transparent,
                                        shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.dayOfWeek,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoBlack.copy(alpha = 0.7f)
                            )
                            Text(
                                text = item.dateLabel,
                                fontSize = 8.sp,
                                fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal,
                                color = if (isToday) NeoBlack else NeoBlack.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreakHeatmapCard(stats: DetailedSrsStats) {
    val streakDays = stats.streakHistoryLast30Days

    NeoCard(
        modifier = Modifier.fillMaxWidth().testTag("streak_heatmap_card"),
        backgroundColor = NeoWhite
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            NeoSectionBadge(title = "30-DAY STREAK HEATMAP", containerColor = NeoGreen)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current: ${stats.currentStreak} days",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack
                )
                Text(
                    text = "Longest: ${stats.longestStreak} days",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeoBlack.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 30 days grid (5 rows x 6 columns)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoCream, shape = RoundedCornerShape(8.dp))
                    .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    streakDays.chunked(6).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowDays.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (day.hasReview) NeoGreen else NeoWhite,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            if (day.isToday) 2.5.dp else 1.5.dp,
                                            if (day.isToday) NeoBlack else if (day.hasReview) NeoBlack else NeoBlack.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = day.dateLabel.substringAfter('/'),
                                            fontSize = 9.sp,
                                            fontWeight = if (day.isToday || day.hasReview) FontWeight.Black else FontWeight.Normal,
                                            color = NeoBlack
                                        )
                                        if (day.isToday) {
                                            Text("â˜…", fontSize = 7.sp, fontWeight = FontWeight.Black, color = NeoBlack)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(NeoGreen, shape = RoundedCornerShape(3.dp))
                            .border(1.dp, NeoBlack, shape = RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reviewed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoBlack
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(NeoWhite, shape = RoundedCornerShape(3.dp))
                            .border(1.dp, NeoBlack.copy(alpha = 0.4f), shape = RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rest Day",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoBlack.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Neo-Brutalist Leech Management Card in SRS Settings tab
 */
@Composable
fun LeechManagementCard(
    leechWords: List<WordEntity>,
    onClearLeechFlag: (String) -> Unit
) {
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leech_management_card"),
        backgroundColor = if (leechWords.isNotEmpty()) Color(0xFFFFECEB) else NeoWhite,
        borderColor = if (leechWords.isNotEmpty()) NeoRed else NeoBlack
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (leechWords.isNotEmpty()) NeoRed else NeoCream, shape = RoundedCornerShape(6.dp))
                            .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Leeches",
                            tint = if (leechWords.isNotEmpty()) NeoWhite else NeoBlack,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LEECH DETECTION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack
                        )
                        Text(
                            text = if (leechWords.isNotEmpty()) "${leechWords.size} words repeatedly failing review" else "Cards with high failure rates",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeoBlack.copy(alpha = 0.7f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .background(if (leechWords.isNotEmpty()) NeoRed else NeoGreen, shape = RoundedCornerShape(6.dp))
                        .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (leechWords.isNotEmpty()) "${leechWords.size} LEECHES" else "0 LEECHES",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeoWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (leechWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                        .border(1.5.dp, NeoGreen, shape = RoundedCornerShape(6.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeoBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "No leech words detected! Your retention is solid.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = NeoBlack
                        )
                    }
                }
            } else {
                Text(
                    text = "Leech cards require better mnemonics, simpler definitions, or concrete examples.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeoBlack.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    leechWords.forEach { word ->
                        NeoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("leech_item_${word.word}"),
                            backgroundColor = NeoWhite,
                            borderWidth = 1.5.dp,
                            shadowOffset = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = word.word.uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Black,
                                            color = NeoBlack
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        com.example.ui.theme.NeoLeechBadge(lapses = word.lapses)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${word.lapses} lapses â€¢ Ease: ${String.format("%.2f", word.easeFactor)} â€¢ Interval: ${word.interval}d",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeoBlack.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                NeoButton(
                                    onClick = { onClearLeechFlag(word.word) },
                                    containerColor = NeoCream,
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("clear_leech_${word.word}")
                                ) {
                                    Text(
                                        text = "CLEAR FLAG",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
