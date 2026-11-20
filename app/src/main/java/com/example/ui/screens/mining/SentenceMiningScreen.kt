package com.example.ui.screens.mining

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MinedSentenceEntity
import com.example.data.WordEntity
import com.example.ui.DictionaryViewModel

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
        OutlinedButton(
            onClick = { viewModel.speakSentence(wordEntity.example) },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("listen_example_sentence_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Listen", style = MaterialTheme.typography.labelLarge)
        }

        FilledTonalButton(
            onClick = { viewModel.mineSentenceFromWord(wordEntity) },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("mine_example_sentence_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mine sentence", style = MaterialTheme.typography.labelLarge)
        }
    }
}

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
        "ALL" to "All (${allSentences.size})",
        "Casual" to "Casual",
        "Workplace" to "Workplace",
        "Dining" to "Dining",
        "Travel" to "Travel",
        "Idioms" to "Idioms",
        "FAVORITES" to "Saved"
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
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Action Bar: Cloze Practice Toggle + Add Custom Sentence
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { isClozeMode = !isClozeMode },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("sentence_cloze_mode_toggle")
            ) {
                Icon(
                    imageVector = if (isClozeMode) Icons.Default.FormatListBulleted else Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isClozeMode) "Sentence list" else "Cloze practice")
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("add_custom_sentence_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mine new")
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (key, label) ->
                val isSelected = selectedCategory == key
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedSentenceCategory(key) },
                    label = { Text(label) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSentenceSearchQuery(it) },
                label = { Text("Search sentences") },
                placeholder = { Text("Search phrases, idioms, or context...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSentenceSearchQuery("") },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear sentence search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sentence_search_input")
            )

            if (filteredSentences.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No sentences found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mine sentences from any dictionary lookup or tap 'Mine new' to save phrases from daily conversations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val highlightBg = MaterialTheme.colorScheme.tertiaryContainer
    val highlightFg = MaterialTheme.colorScheme.onTertiaryContainer

    val highlightedText = remember(item.sentence, item.targetWord, highlightBg, highlightFg) {
        buildHighlightedSentence(item.sentence, item.targetWord, highlightBg, highlightFg)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mined_sentence_card_${item.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top bar: category + practice count + favorite/delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (item.practiceCount > 0) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = "Practiced ${item.practiceCount}×",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (item.isFavorite) "Remove favorite" else "Mark favorite",
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete sentence",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Highlighted sentence block
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onSpeakSentence() }
            ) {
                Text(
                    text = highlightedText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Target phrase + meaning + notes
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Key phrase: \"${item.targetWord}\"",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onSpeakTargetWord,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Phrase")
                        }
                    }
                    Text(
                        text = item.meaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.notes.isNotBlank()) {
                        Text(
                            text = "Context tip: ${item.notes}",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSpeakSentence,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("speak_sentence_btn_${item.id}")
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Listen")
                }

                FilledTonalButton(
                    onClick = {
                        onSpeakSentence()
                        onPractice()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("practice_sentence_btn_${item.id}")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Practice +1")
                }
            }
        }
    }
}

@Composable
fun SentenceClozePracticeView(
    sentences: List<MinedSentenceEntity>,
    viewModel: DictionaryViewModel,
    onExitCloze: () -> Unit
) {
    if (sentences.isEmpty()) {
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
                Text(
                    text = "No sentences in this category",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onExitCloze,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Back to sentence list")
                }
            }
        }
        return
    }

    var currentIndex by rememberSaveable(sentences.size) { mutableStateOf(0) }
    var isRevealed by rememberSaveable(currentIndex) { mutableStateOf(false) }

    val safeIndex = currentIndex.coerceIn(0, sentences.lastIndex)
    val currentItem = sentences[safeIndex]
    val highlightBg = MaterialTheme.colorScheme.tertiaryContainer
    val highlightFg = MaterialTheme.colorScheme.onTertiaryContainer

    val clozeSentence = remember(currentItem.sentence, currentItem.targetWord, isRevealed, highlightBg, highlightFg) {
        if (isRevealed) {
            buildHighlightedSentence(currentItem.sentence, currentItem.targetWord, highlightBg, highlightFg)
        } else {
            val regex = Regex(Regex.escape(currentItem.targetWord), RegexOption.IGNORE_CASE)
            val masked = if (regex.containsMatchIn(currentItem.sentence)) {
                regex.replace(currentItem.sentence, "[ _______ ]")
            } else {
                "${currentItem.sentence} [ _______ ]"
            }
            buildAnnotatedString { append(masked) }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("sentence_cloze_practice_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Context cloze",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(
                        text = "${safeIndex + 1} / ${sentences.size}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = clozeSentence,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(18.dp)
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Meaning clue (${currentItem.category})",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = currentItem.meaning,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!isRevealed) {
                Button(
                    onClick = {
                        isRevealed = true
                        viewModel.speakSentence(currentItem.sentence)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("reveal_cloze_button")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reveal phrase & listen")
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Answer: ${currentItem.targetWord}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.speakSentence(currentItem.sentence) },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Replay sentence audio")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.practiceMinedSentence(currentItem, gotIt = false)
                            isRevealed = false
                            currentIndex = (safeIndex + 1) % sentences.size
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("cloze_again_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Again")
                    }

                    Button(
                        onClick = {
                            viewModel.practiceMinedSentence(currentItem, gotIt = true)
                            isRevealed = false
                            currentIndex = (safeIndex + 1) % sentences.size
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("cloze_got_it_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Got it")
                    }
                }
            }
        }
    }
}

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .testTag("add_custom_sentence_dialog"),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mine new sentence",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                OutlinedTextField(
                    value = sentence,
                    onValueChange = { sentence = it },
                    label = { Text("Full context sentence *") },
                    placeholder = { Text("e.g., Let's wrap things up and head home.") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetWord,
                    onValueChange = { targetWord = it },
                    label = { Text("Target word or phrase *") },
                    placeholder = { Text("e.g., wrap things up") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text("Meaning in context *") },
                    placeholder = { Text("e.g., To finish a meeting or task") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Source / usage note (optional)") },
                    placeholder = { Text("e.g., Heard on a podcast") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(sentence, targetWord, meaning, selectedCategory, notes) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("save_mined_sentence_button")
                    ) {
                        Text("Save sentence")
                    }
                }
            }
        }
    }
}

private fun buildHighlightedSentence(
    sentence: String,
    targetWord: String,
    highlightBg: Color,
    highlightFg: Color
) = buildAnnotatedString {
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
                background = highlightBg,
                color = highlightFg,
                fontWeight = FontWeight.SemiBold
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
