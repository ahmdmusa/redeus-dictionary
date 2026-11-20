package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MinedSentenceEntity
import com.example.data.WordEntity
import com.example.ui.theme.*


/**
 * Quick action buttons embedded inside WordDetailsCard Usage Example Card
 */
@Composable
fun ExampleSentenceMiningActionsRow(
    wordEntity: WordEntity,
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeoSecondaryButton(
            onClick = { viewModel.speakSentence(wordEntity.example) },
            containerColor = NeoWhite,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("listen_example_sentence_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Listen Sentence",
                tint = NeoBlack,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "LISTEN SENTENCE",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp
            )
        }

        NeoButton(
            onClick = { viewModel.mineSentenceFromWord(wordEntity) },
            containerColor = NeoYellow,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("mine_example_sentence_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Mine Sentence",
                tint = NeoBlack,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "MINE SENTENCE",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp
            )
        }
    }
}

/**
 * Full Sentence Mining Hub for Everyday Conversation & Context Learning
 */
@Composable
fun SentenceMiningHubView(
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    val allSentences by viewModel.minedSentencesState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedSentenceCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.sentenceSearchQuery.collectAsStateWithLifecycle()

    var isClozeMode by rememberSaveable { mutableStateOf(false) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val categories = listOf(
        "ALL" to "ALL (${allSentences.size})",
        "Casual" to "CASUAL CHAT",
        "Workplace" to "WORKPLACE",
        "Dining" to "DINING & CAFE",
        "Travel" to "TRAVEL & ERRANDS",
        "Idioms" to "MODERN IDIOMS",
        "FAVORITES" to "â˜… SAVED"
    )

    val filteredSentences = remember(allSentences, selectedCategory, searchQuery) {
        allSentences.filter { item ->
            val matchesCategory = when (selectedCategory) {
                "ALL" -> true
                "FAVORITES" -> item.isFavorite
                else -> item.category.equals(selectedCategory, ignoreCase = true)
            }
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                item.sentence.lowercase().contains(q) ||
                item.targetWord.lowercase().contains(q) ||
                item.meaning.lowercase().contains(q)
            matchesCategory && matchesSearch
        }
    }

    if (showAddDialog) {
        AddCustomSentenceDialog(
            onDismiss = { showAddDialog = false },
            onSave = { sentence, targetWord, meaning, category, notes ->
                viewModel.addCustomMinedSentence(
                    sentence = sentence,
                    targetWord = targetWord,
                    meaning = meaning,
                    category = category,
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 600.dp)
    ) {
        // Mode & Action Bar: Browse vs Cloze Practice + Mine Custom Sentence
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoChip(
                text = if (isClozeMode) "â† SENTENCE LIST" else "ðŸ§  CLOZE PRACTICE",
                selected = isClozeMode,
                selectedColor = NeoGreen,
                unselectedColor = NeoYellowLight,
                onClick = { isClozeMode = !isClozeMode },
                modifier = Modifier.weight(1f).testTag("sentence_cloze_mode_toggle")
            )

            NeoButton(
                onClick = { showAddDialog = true },
                containerColor = NeoCyan,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                shadowOffset = 2.5.dp,
                modifier = Modifier.testTag("add_custom_sentence_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Sentence",
                    tint = NeoBlack,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "MINE NEW",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }

        // Category Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (key, label) ->
                NeoChip(
                    text = label,
                    selected = selectedCategory == key,
                    selectedColor = NeoYellow,
                    unselectedColor = NeoWhite,
                    onClick = { viewModel.setSelectedSentenceCategory(key) },
                    modifier = Modifier.testTag("sentence_cat_$key")
                )
            }
        }

        if (isClozeMode) {
            SentenceClozePracticeView(
                sentences = filteredSentences,
                viewModel = viewModel,
                onExitCloze = { isClozeMode = false }
            )
        } else {
            // Search Box for Mined Sentences
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSentenceSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search phrases, idioms, or context sentences...",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeoBlack.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Sentences",
                        tint = NeoBlack,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSentenceSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = NeoBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = NeoWhite,
                    unfocusedContainerColor = NeoWhite,
                    focusedBorderColor = NeoBlack,
                    unfocusedBorderColor = NeoBlack,
                    focusedTextColor = NeoBlack,
                    unfocusedTextColor = NeoBlack
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("sentence_search_input")
            )

            if (filteredSentences.isEmpty()) {
                NeoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    backgroundColor = NeoWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO SENTENCES FOUND",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = NeoBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mine sentences from any dictionary lookup or tap '+ MINE NEW' to save phrases from YouTube, Reddit, or daily conversations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeoBlack.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredSentences, key = { it.id }) { item ->
                        MinedSentenceCard(
                            item = item,
                            onSpeakSentence = { viewModel.speakSentence(item.sentence) },
                            onSpeakTargetWord = { viewModel.speak(item.targetWord) },
                            onToggleFavorite = { viewModel.toggleSentenceFavorite(item) },
                            onPractice = { viewModel.practiceMinedSentence(item, true) },
                            onDelete = { viewModel.deleteMinedSentence(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MinedSentenceCard(
    item: MinedSentenceEntity,
    onSpeakSentence: () -> Unit,
    onSpeakTargetWord: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPractice: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (item.category.lowercase()) {
        "casual" -> NeoCyan
        "workplace" -> NeoPurple
        "dining" -> NeoOrange
        "travel" -> NeoGreen
        "idioms" -> NeoPink
        else -> NeoYellow
    }

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mined_sentence_card_${item.id}"),
        backgroundColor = NeoWhite
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top bar: Category Badge + Practice count + Favorite & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(categoryColor, shape = RoundedCornerShape(4.dp))
                            .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            ),
                            color = NeoBlack
                        )
                    }

                    if (item.practiceCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(NeoGreen, shape = RoundedCornerShape(4.dp))
                                .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "âœ“ ${item.practiceCount}X",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                ),
                                color = NeoBlack
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NeoIconButton(
                        onClick = onToggleFavorite,
                        containerColor = if (item.isFavorite) NeoYellow else NeoCream,
                        modifier = Modifier.size(34.dp),
                        shadowOffset = 2.dp
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = NeoBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    NeoIconButton(
                        onClick = onDelete,
                        containerColor = NeoRed,
                        contentColor = NeoWhite,
                        modifier = Modifier.size(34.dp),
                        shadowOffset = 2.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Sentence",
                            tint = NeoWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Highlighted Context Sentence
            val highlightedText = remember(item.sentence, item.targetWord) {
                buildHighlightedSentence(item.sentence, item.targetWord)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoCream, shape = RoundedCornerShape(8.dp))
                    .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                    .clickable { onSpeakSentence() }
                    .padding(14.dp)
            ) {
                Text(
                    text = highlightedText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    ),
                    color = NeoBlack
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Target Expression + Meaning Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoYellowLight.copy(alpha = 0.55f), shape = RoundedCornerShape(6.dp))
                    .border(1.5.dp, NeoBlack, shape = RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "KEY PHRASE: \"${item.targetWord.uppercase()}\"",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp
                        ),
                        color = NeoRed
                    )
                    Text(
                        text = "ðŸ”Š TAP PHRASE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        ),
                        color = NeoBlack,
                        modifier = Modifier
                            .background(NeoWhite, shape = RoundedCornerShape(4.dp))
                            .border(1.dp, NeoBlack, shape = RoundedCornerShape(4.dp))
                            .clickable { onSpeakTargetWord() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.meaning,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = NeoBlack
                )
                if (item.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ðŸ’¡ Context Tip: ${item.notes}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold
                        ),
                        color = NeoBlack.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Action Buttons: Listen Full Sentence + Mark Practiced
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoButton(
                    onClick = onSpeakSentence,
                    containerColor = NeoYellow,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("speak_sentence_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Listen Full Sentence",
                        tint = NeoBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LISTEN SENTENCE",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                NeoSecondaryButton(
                    onClick = {
                        onSpeakSentence()
                        onPractice()
                    },
                    containerColor = NeoGreen,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("practice_sentence_btn_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Practice",
                        tint = NeoBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PRACTICE +1",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/**
 * Interactive Cloze Deletion (Fill-in-the-blank in context) Practice Mode
 */
@Composable
fun SentenceClozePracticeView(
    sentences: List<MinedSentenceEntity>,
    viewModel: DictionaryViewModel,
    onExitCloze: () -> Unit
) {
    if (sentences.isEmpty()) {
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoWhite
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NO SENTENCES IN THIS CATEGORY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = NeoBlack
                )
                Spacer(modifier = Modifier.height(12.dp))
                NeoButton(onClick = onExitCloze) {
                    Text("BACK TO SENTENCE LIST", fontWeight = FontWeight.Black)
                }
            }
        }
        return
    }

    var currentIndex by rememberSaveable(sentences.size) { mutableStateOf(0) }
    var isRevealed by rememberSaveable(currentIndex) { mutableStateOf(false) }

    val safeIndex = currentIndex.coerceIn(0, sentences.lastIndex)
    val currentItem = sentences[safeIndex]

    val clozeSentence = remember(currentItem.sentence, currentItem.targetWord, isRevealed) {
        if (isRevealed) {
            buildHighlightedSentence(currentItem.sentence, currentItem.targetWord)
        } else {
            val regex = Regex(Regex.escape(currentItem.targetWord), RegexOption.IGNORE_CASE)
            val masked = if (regex.containsMatchIn(currentItem.sentence)) {
                regex.replace(currentItem.sentence, "[  _______  ]")
            } else {
                "${currentItem.sentence}  [  _______  ]"
            }
            buildAnnotatedString { append(masked) }
        }
    }

    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sentence_cloze_practice_card"),
        backgroundColor = NeoWhite
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoSectionBadge(
                    title = "CONTEXT CLOZE CHALLENGE",
                    containerColor = NeoCyan
                )
                Text(
                    text = "${safeIndex + 1} / ${sentences.size}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = NeoBlack
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cloze sentence box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoCream, shape = RoundedCornerShape(8.dp))
                    .border(2.5.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = clozeSentence,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        lineHeight = 24.sp
                    ),
                    color = NeoBlack,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Meaning Clue Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoYellowLight, shape = RoundedCornerShape(8.dp))
                    .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "MEANING CLUE (${currentItem.category.uppercase()}):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = NeoRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentItem.meaning,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeoBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!isRevealed) {
                NeoButton(
                    onClick = {
                        isRevealed = true
                        viewModel.speakSentence(currentItem.sentence)
                    },
                    containerColor = NeoYellow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("reveal_cloze_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Reveal Answer",
                        tint = NeoBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REVEAL PHRASE & LISTEN ðŸ”Š",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            } else {
                // Revealed Answer Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoGreen.copy(alpha = 0.25f), shape = RoundedCornerShape(8.dp))
                        .border(2.dp, NeoBlack, shape = RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ANSWER: ${currentItem.targetWord.uppercase()}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = NeoBlack
                        )
                        NeoIconButton(
                            onClick = { viewModel.speakSentence(currentItem.sentence) },
                            containerColor = NeoYellow,
                            modifier = Modifier.size(34.dp),
                            shadowOffset = 2.dp
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Replay Sentence Audio",
                                tint = NeoBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeoSecondaryButton(
                        onClick = {
                            viewModel.practiceMinedSentence(currentItem, gotIt = false)
                            isRevealed = false
                            currentIndex = (safeIndex + 1) % sentences.size
                        },
                        containerColor = NeoRedLight,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cloze_again_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Review Again",
                            tint = NeoBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PRACTICE AGAIN", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }

                    NeoButton(
                        onClick = {
                            viewModel.practiceMinedSentence(currentItem, gotIt = true)
                            isRevealed = false
                            currentIndex = (safeIndex + 1) % sentences.size
                        },
                        containerColor = NeoGreen,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cloze_got_it_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Got It",
                            tint = NeoBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GOT IT (+1 STREAK)", fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Dialog to add a custom mined sentence from YouTube, Reddit, podcasts, or daily conversations
 */
@Composable
fun AddCustomSentenceDialog(
    onDismiss: () -> Unit,
    onSave: (sentence: String, targetWord: String, meaning: String, category: String, notes: String) -> Unit
) {
    var sentence by rememberSaveable { mutableStateOf("") }
    var targetWord by rememberSaveable { mutableStateOf("") }
    var meaning by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Casual") }
    var notes by rememberSaveable { mutableStateOf("") }

    val categories = listOf("Casual", "Workplace", "Dining", "Travel", "Idioms")

    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_custom_sentence_dialog"),
            backgroundColor = NeoCream
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeoSectionBadge(
                        title = "â›ï¸ MINE NEW SENTENCE",
                        containerColor = NeoYellow
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NeoBlack)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sentence,
                    onValueChange = { sentence = it },
                    label = { Text("Full Context Sentence *", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g., Let's wrap things up and head home.") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeoWhite,
                        unfocusedContainerColor = NeoWhite,
                        focusedBorderColor = NeoBlack,
                        unfocusedBorderColor = NeoBlack,
                        focusedTextColor = NeoBlack,
                        unfocusedTextColor = NeoBlack
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetWord,
                    onValueChange = { targetWord = it },
                    label = { Text("Target Word or Phrase *", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g., wrap things up") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeoWhite,
                        unfocusedContainerColor = NeoWhite,
                        focusedBorderColor = NeoBlack,
                        unfocusedBorderColor = NeoBlack,
                        focusedTextColor = NeoBlack,
                        unfocusedTextColor = NeoBlack
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text("Meaning in Context *", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g., To finish a meeting or task") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeoWhite,
                        unfocusedContainerColor = NeoWhite,
                        focusedBorderColor = NeoBlack,
                        unfocusedBorderColor = NeoBlack,
                        focusedTextColor = NeoBlack,
                        unfocusedTextColor = NeoBlack
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "CONVERSATION CATEGORY:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = NeoBlack
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        NeoChip(
                            text = cat,
                            selected = selectedCategory == cat,
                            selectedColor = NeoCyan,
                            unselectedColor = NeoWhite,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Source / Usage Note (Optional)", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("e.g., Heard on YouTube vlog / Reddit thread") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = NeoWhite,
                        unfocusedContainerColor = NeoWhite,
                        focusedBorderColor = NeoBlack,
                        unfocusedBorderColor = NeoBlack,
                        focusedTextColor = NeoBlack,
                        unfocusedTextColor = NeoBlack
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeoSecondaryButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Black)
                    }
                    NeoButton(
                        onClick = {
                            onSave(sentence, targetWord, meaning, selectedCategory, notes)
                        },
                        containerColor = NeoGreen,
                        modifier = Modifier.weight(1f).testTag("save_mined_sentence_button")
                    ) {
                        Text("SAVE SENTENCE", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/**
 * Helper to highlight the target word or phrase inside a context sentence
 */
private fun buildHighlightedSentence(sentence: String, targetWord: String) = buildAnnotatedString {
    val cleanTarget = targetWord.trim()
    if (cleanTarget.isEmpty()) {
        append(sentence)
        return@buildAnnotatedString
    }
    val regex = Regex(Regex.escape(cleanTarget), RegexOption.IGNORE_CASE)
    var lastIndex = 0
    for (match in regex.findAll(sentence)) {
        val start = match.range.first
        val end = match.range.last + 1
        if (start > lastIndex) {
            append(sentence.substring(lastIndex, start))
        }
        pushStyle(
            SpanStyle(
                background = NeoYellow,
                color = NeoBlack,
                fontWeight = FontWeight.Black
            )
        )
        append(" ${sentence.substring(start, end)} ")
        pop()
        lastIndex = end
    }
    if (lastIndex < sentence.length) {
        append(sentence.substring(lastIndex))
    }
}
