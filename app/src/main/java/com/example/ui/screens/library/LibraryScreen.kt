package com.example.ui.screens.library

import androidx.compose.foundation.background
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
import com.example.ui.components.LeechBadge
import com.example.ui.navigation.LocalWindowWidthSizeClass
import com.example.ui.screens.search.WordDetailsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTabContent(
    viewModel: DictionaryViewModel,
    historyWords: List<WordEntity>,
    bookmarkedWords: List<WordEntity>,
    innerPadding: PaddingValues,
    onNavigateToSearch: () -> Unit
) {
    var selectedSubTab by rememberSaveable { mutableStateOf(0) } // 0: Deck, 1: History
    val safeSubTab = selectedSubTab.coerceIn(0, 1)
    val filterSearchQuery by viewModel.filterSearchQuery.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()

    val widthSizeClass = LocalWindowWidthSizeClass.current
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
    var selectedDetailWordKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val isDeck = safeSubTab == 0

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(if (isDeck) "Clear all bookmarks?" else "Clear search history?") },
            text = {
                Text(
                    if (isDeck) {
                        "This will remove all ${bookmarkedWords.size} words from your Deck."
                    } else {
                        "This will delete all ${historyWords.size} words from your History."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isDeck) viewModel.clearBookmarks() else viewModel.clearHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Clear all")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // PrimaryTabRow for Deck & History
        PrimaryTabRow(
            selectedTabIndex = safeSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = safeSubTab == 0,
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
                selected = safeSubTab == 1,
                onClick = { selectedSubTab = 1 },
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

        val rawList = if (isDeck) bookmarkedWords else historyWords
        val listToDisplay = remember(
            rawList,
            isDeck,
            filterSearchQuery,
            selectedStatusFilter
        ) {
            rawList.filter { wordEntity ->
                val query = filterSearchQuery.trim().lowercase()
                val matchesQuery = query.isEmpty() ||
                    wordEntity.word.lowercase().contains(query) ||
                    wordEntity.definition.lowercase().contains(query) ||
                    wordEntity.tags.lowercase().contains(query)

                val matchesStatus = !isDeck || when (selectedStatusFilter) {
                    WordStatusFilter.ALL -> true
                    WordStatusFilter.NEW -> wordEntity.repetitions == 0 && wordEntity.interval == 0
                    WordStatusFilter.LEARNING -> (wordEntity.repetitions > 0 || wordEntity.interval > 0) &&
                        (wordEntity.interval < 21 || wordEntity.distinctReviewDays < 3) && !wordEntity.isLeech
                    WordStatusFilter.MASTERED -> wordEntity.distinctReviewDays >= 3 && wordEntity.interval >= 21 && !wordEntity.isLeech
                    WordStatusFilter.LEECH -> wordEntity.isLeech
                }

                matchesQuery && matchesStatus
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
                if (rawList.isNotEmpty()) {
                    LibraryFilterBar(
                        searchQuery = filterSearchQuery,
                        onSearchQueryChange = { viewModel.setFilterSearchQuery(it) },
                        showStatusChips = isDeck,
                        statusFilter = selectedStatusFilter,
                        onStatusFilterChange = { viewModel.setSelectedStatusFilter(it) },
                        onRequestClearAll = { showClearConfirmDialog = true }
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
                } else if (listToDisplay.isEmpty()) {
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
                                text = "No words match your search or status filter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            TextButton(
                                onClick = {
                                    viewModel.setFilterSearchQuery("")
                                    viewModel.setSelectedStatusFilter(WordStatusFilter.ALL)
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
    showStatusChips: Boolean,
    statusFilter: WordStatusFilter,
    onStatusFilterChange: (WordStatusFilter) -> Unit,
    onRequestClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var overflowExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("filter_search_input"),
                placeholder = { Text("Search saved words...") },
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

            Box {
                IconButton(
                    onClick = { overflowExpanded = true },
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("library_overflow_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Clear all", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            overflowExpanded = false
                            onRequestClearAll()
                        }
                    )
                }
            }
        }

        if (showStatusChips) {
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
        }
    }
}

@Composable
fun LibraryItemRow(
    wordEntity: WordEntity,
    isSelected: Boolean = false,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onResetProgress: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    CardStatusBadge(wordEntity = wordEntity)
                    if (wordEntity.isLeech) {
                        LeechBadge(lapses = wordEntity.lapses)
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

            Row(verticalAlignment = Alignment.CenterVertically) {
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

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Word options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (wordEntity.repetitions > 0 || wordEntity.interval > 0) {
                            DropdownMenuItem(
                                text = { Text("Reset review progress") },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onResetProgress()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete word", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
