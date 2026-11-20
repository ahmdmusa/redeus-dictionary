package com.example.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WordEntity
import com.example.ui.DictionaryViewModel
import com.example.ui.WordStatusFilter
import com.example.ui.components.CardStatusBadge
import com.example.ui.components.DailyStreakAndHeatmapBanner
import com.example.ui.components.LeechBadge
import com.example.ui.components.WordTagsRow
import com.example.ui.navigation.LocalWindowWidthSizeClass
import com.example.ui.screens.mining.SentenceMiningHubView
import com.example.ui.screens.search.WordDetailsCard
import com.example.ui.theme.LocalReducedMotionEnabled
import com.example.ui.theme.RedeusMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTabContent(
    viewModel: DictionaryViewModel,
    historyWords: List<WordEntity>,
    bookmarkedWords: List<WordEntity>,
    innerPadding: PaddingValues,
    onNavigateToSearch: () -> Unit
) {
    var selectedSubTab by rememberSaveable { mutableStateOf(0) } // 0: Deck, 1: Sentences, 2: History
    val filterSearchQuery by viewModel.filterSearchQuery.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val selectedPosFilter by viewModel.selectedPosFilter.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    val availableTags by viewModel.allAvailableTags.collectAsStateWithLifecycle()
    val minedSentences by viewModel.minedSentencesState.collectAsStateWithLifecycle()

    val widthSizeClass = LocalWindowWidthSizeClass.current
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
    var selectedDetailWordKey by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // PrimaryTabRow for the 3 peer Library views
        PrimaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Text(
                        text = "Deck (${bookmarkedWords.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("library_subtab_deck")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Text(
                        text = "Sentences (${minedSentences.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("library_subtab_sentences")
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = {
                    Text(
                        text = "History (${historyWords.size})",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.testTag("library_subtab_history")
            )
        }

        if (selectedSubTab == 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                SentenceMiningHubView(viewModel = viewModel)
            }
            return
        }

        val isDeck = selectedSubTab == 0
        val rawList = if (isDeck) bookmarkedWords else historyWords
        val listToDisplay = remember(
            rawList,
            filterSearchQuery,
            selectedStatusFilter,
            selectedPosFilter,
            selectedTagFilter
        ) {
            rawList.filter { wordEntity ->
                val query = filterSearchQuery.trim().lowercase()
                val matchesQuery = query.isEmpty() ||
                    wordEntity.word.lowercase().contains(query) ||
                    wordEntity.definition.lowercase().contains(query) ||
                    wordEntity.tags.lowercase().contains(query)

                val matchesStatus = when (selectedStatusFilter) {
                    WordStatusFilter.ALL -> true
                    WordStatusFilter.NEW -> wordEntity.repetitions == 0 && wordEntity.interval == 0
                    WordStatusFilter.LEARNING -> (wordEntity.repetitions > 0 || wordEntity.interval > 0) &&
                        (wordEntity.interval < 21 || wordEntity.distinctReviewDays < 3) && !wordEntity.isLeech
                    WordStatusFilter.MASTERED -> wordEntity.distinctReviewDays >= 3 && wordEntity.interval >= 21 && !wordEntity.isLeech
                    WordStatusFilter.LEECH -> wordEntity.isLeech
                }

                val matchesPos = selectedPosFilter.equals("ALL", ignoreCase = true) ||
                    wordEntity.partOfSpeech.equals(selectedPosFilter, ignoreCase = true)

                val matchesTag = selectedTagFilter.isBlank() ||
                    wordEntity.tags.split(",").map { it.trim().lowercase() }.contains(selectedTagFilter.trim().lowercase())

                matchesQuery && matchesStatus && matchesPos && matchesTag
            }
        }

        val selectedDetailWord = remember(listToDisplay, selectedDetailWordKey) {
            listToDisplay.firstOrNull { it.word.equals(selectedDetailWordKey, ignoreCase = true) }
                ?: listToDisplay.firstOrNull()
        }

        val listPaneContent: @Composable (Modifier) -> Unit = { paneModifier ->
            Column(
                modifier = paneModifier,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DailyStreakAndHeatmapBanner(viewModel = viewModel)

                if (rawList.isNotEmpty()) {
                    LibraryFilterBar(
                        searchQuery = filterSearchQuery,
                        onSearchQueryChange = { viewModel.setFilterSearchQuery(it) },
                        statusFilter = selectedStatusFilter,
                        onStatusFilterChange = { viewModel.setSelectedStatusFilter(it) },
                        posFilter = selectedPosFilter,
                        onPosFilterChange = { viewModel.setSelectedPosFilter(it) },
                        tagFilter = selectedTagFilter,
                        onTagFilterChange = { viewModel.setSelectedTagFilter(it) },
                        availableTags = availableTags
                    )
                }

                if (rawList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        LibraryEmptyState(
                            isDeck = isDeck,
                            onLoadSampleData = { viewModel.loadSampleData() }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDeck) "Saved vocabulary (${listToDisplay.size})" else "Recent lookups (${listToDisplay.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDeck) {
                                FilledTonalButton(
                                    onClick = { viewModel.loadSampleData() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("load_sample_data_header_button")
                                ) {
                                    Text("Sample")
                                }
                            }
                            TextButton(
                                onClick = {
                                    if (isDeck) viewModel.clearBookmarks() else viewModel.clearHistory()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Clear all")
                            }
                        }
                    }

                    if (listToDisplay.isEmpty()) {
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
                                    text = "No matching words",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "No words match your active search or filter criteria.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(
                                    onClick = {
                                        viewModel.setFilterSearchQuery("")
                                        viewModel.setSelectedStatusFilter(WordStatusFilter.ALL)
                                        viewModel.setSelectedPosFilter("ALL")
                                        viewModel.setSelectedTagFilter("")
                                    }
                                ) {
                                    Text("Reset filters")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(listToDisplay, key = { it.word }) { wordEntity ->
                                val isSelected = isExpanded && selectedDetailWord?.word == wordEntity.word
                                LibraryItemRow(
                                    wordEntity = wordEntity,
                                    viewModel = viewModel,
                                    isSelected = isSelected,
                                    onSelect = {
                                        if (isExpanded) {
                                            selectedDetailWordKey = wordEntity.word
                                        } else {
                                            viewModel.selectHistoryWord(wordEntity)
                                            onNavigateToSearch()
                                        }
                                    },
                                    onDelete = { viewModel.deleteFromHistory(wordEntity) },
                                    onBookmarkToggle = { viewModel.toggleBookmark(wordEntity) },
                                    onResetProgress = { viewModel.resetWordProgress(wordEntity.word) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                listPaneContent(
                    Modifier
                        .weight(0.46f)
                        .fillMaxHeight()
                )
                Box(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedDetailWord != null) {
                        WordDetailsCard(
                            wordEntity = selectedDetailWord,
                            onSpeak = { viewModel.speak(selectedDetailWord.word) },
                            onBookmarkToggle = { viewModel.toggleBookmark(selectedDetailWord) },
                            viewModel = viewModel
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Select a word to inspect details",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                listPaneContent(
                    Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                )
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    isDeck: Boolean,
    onLoadSampleData: () -> Unit
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
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDeck) Icons.Outlined.BookmarkBorder else Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = if (isDeck) "No bookmarks yet" else "History is empty",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isDeck) {
                    "Bookmark any looked-up word to save it for offline flashcards and spaced-repetition reviews."
                } else {
                    "Words you look up with Gemini AI appear here automatically."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isDeck) {
                Button(
                    onClick = onLoadSampleData,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("load_sample_data_empty_button")
                ) {
                    Text("Load sample words")
                }
            }
        }
    }
}

@Composable
fun LibraryFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    statusFilter: WordStatusFilter,
    onStatusFilterChange: (WordStatusFilter) -> Unit,
    posFilter: String,
    onPosFilterChange: (String) -> Unit,
    tagFilter: String,
    onTagFilterChange: (String) -> Unit,
    availableTags: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotionEnabled.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filter_search_input"),
                label = { Text("Filter words") },
                placeholder = { Text("Search by word, meaning, or tag...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear filter search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeSummary = buildList {
                    if (statusFilter != WordStatusFilter.ALL) add(statusFilter.name.lowercase().replaceFirstChar { it.uppercase() })
                    if (posFilter != "ALL") add(posFilter.lowercase())
                    if (tagFilter.isNotBlank()) add("#$tagFilter")
                }.joinToString(" • ").ifEmpty { "All words" }

                Text(
                    text = "Filter: $activeSummary",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("toggle_filters_button")
                ) {
                    Text(if (expanded) "Hide filters" else "More filters")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(RedeusMotion.spec(reducedMotion)) + expandVertically(RedeusMotion.spec(reducedMotion)),
                exit = fadeOut(RedeusMotion.spec(reducedMotion)) + shrinkVertically(RedeusMotion.spec(reducedMotion))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Status filter chips
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WordStatusFilter.entries.forEach { filter ->
                            val isSelected = statusFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { onStatusFilterChange(filter) },
                                label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.testTag("filter_status_${filter.name.lowercase()}")
                            )
                        }
                    }

                    // Part of speech chips
                    Text(
                        text = "Part of speech",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL", "NOUN", "VERB", "ADJECTIVE", "ADVERB", "OTHER").forEach { pos ->
                            val isSelected = posFilter.equals(pos, ignoreCase = true)
                            val label = when (pos) {
                                "ALL" -> "All"
                                "NOUN" -> "n."
                                "VERB" -> "v."
                                "ADJECTIVE" -> "adj."
                                "ADVERB" -> "adv."
                                else -> "Other"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onPosFilterChange(pos) },
                                label = { Text(label) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.testTag("filter_pos_${pos.lowercase()}")
                            )
                        }
                    }

                    if (availableTags.isNotEmpty()) {
                        Text(
                            text = "Decks & tags",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isAllSelected = tagFilter.isBlank() || tagFilter.equals("ALL", ignoreCase = true)
                            FilterChip(
                                selected = isAllSelected,
                                onClick = { onTagFilterChange("") },
                                label = { Text("All decks") },
                                leadingIcon = if (isAllSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                modifier = Modifier.testTag("filter_tag_all")
                            )
                            availableTags.forEach { tag ->
                                val isSelected = tagFilter.equals(tag, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onTagFilterChange(if (isSelected) "" else tag) },
                                    label = { Text("#$tag") },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.testTag("filter_tag_$tag")
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
fun LibraryItemRow(
    wordEntity: WordEntity,
    viewModel: DictionaryViewModel,
    isSelected: Boolean = false,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onResetProgress: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("library_item_${wordEntity.word}"),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Initial letter avatar
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = wordEntity.word.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = wordEntity.word.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (wordEntity.partOfSpeech.isNotBlank()) {
                            Text(
                                text = wordEntity.partOfSpeech,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = wordEntity.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Badges + Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CardStatusBadge(wordEntity = wordEntity)
                    if (wordEntity.isLeech) {
                        LeechBadge(lapses = wordEntity.lapses)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (wordEntity.repetitions > 0 || wordEntity.interval > 0) {
                        IconButton(
                            onClick = onResetProgress,
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("reset_progress_${wordEntity.word}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset review progress",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = if (wordEntity.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (wordEntity.isBookmarked) "Remove bookmark" else "Bookmark word",
                            tint = if (wordEntity.isBookmarked) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete word",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            WordTagsRow(word = wordEntity, viewModel = viewModel)
        }
    }
}
