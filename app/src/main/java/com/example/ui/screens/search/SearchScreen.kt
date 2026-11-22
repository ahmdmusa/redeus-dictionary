package com.example.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.OfflineCurriculumWord
import com.example.data.OfflineSearchSuggestion
import com.example.data.WordEntity
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.screens.mining.ExampleSentenceMiningActionsRow
import com.example.ui.theme.RedeusExpressiveType

private enum class DictionaryScope(val label: String) {
    ALL("All"),
    BOOKS("Books & IELTS"),
    MY_DECK("My Deck"),
    HISTORY("History")
}

/**
 * Tab 3: Completely Overhauled Offline Bilingual Dictionary & Search Experience.
 *
 * Features:
 *   - Sticky 52dp Spotlight Search Bar (EN <-> AR, 48dp Clear & Go actions)
 *   - Apple Scope Bar (All / Books & IELTS / My Deck / History)
 *   - Normalized Arabic search (tashkeel & hamza insensitive) + English lemmatization + Fuzzy Did-You-Mean
 *   - Exact Match Hero Card while typing + instant 48dp Pronounce & Add-to-Study controls
 *   - Landing: Recent Searches + Deterministic Offline Word of the Day + CEFR Quick Browse (A1..IELTS)
 *   - Word Detail View: In-dictionary Back Stack, Local Permanent Photo, RTL Arabic, Highlighted Example + Audio,
 *     Found-in-Books links, tappable Synonyms/Antonyms, and persistent ✨ AI Helper sheet.
 */
@Composable
fun SearchTabContent(
    viewModel: DictionaryViewModel,
    searchQuery: String,
    uiState: SearchUiState,
    innerPadding: PaddingValues,
    onOpenApiSettings: () -> Unit = {},
    onOpenCourses: () -> Unit = {},
    onOpenCourseOccurrence: (bookNum: Int, unitNum: Int) -> Unit = { _, _ -> onOpenCourses() },
    onOpenReview: () -> Unit = {}
) {
    var scope by rememberSaveable { mutableStateOf(DictionaryScope.ALL) }
    var deckStatusFilter by rememberSaveable { mutableStateOf(WordStatusFilter.ALL) }
    var showClearHistoryConfirm by rememberSaveable { mutableStateOf(false) }

    val suggestions by viewModel.searchSuggestions.collectAsStateWithLifecycle()
    val fuzzySuggestions by viewModel.fuzzySuggestions.collectAsStateWithLifecycle()
    val wordOfTheDay by viewModel.wordOfTheDay.collectAsStateWithLifecycle()
    val selectedCefr by viewModel.selectedCefrBrowse.collectAsStateWithLifecycle()
    val cefrWords by viewModel.cefrBrowseWords.collectAsStateWithLifecycle()
    val historyWords by viewModel.historyState.collectAsStateWithLifecycle()
    val bookmarkedWords by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val backStack by viewModel.searchWordBackStack.collectAsStateWithLifecycle()
    val occurrences by viewModel.currentWordOccurrences.collectAsStateWithLifecycle()

    val bookmarkedSet = remember(bookmarkedWords) {
        bookmarkedWords.map { it.word.trim().lowercase() }.toSet()
    }

    LaunchedEffect(Unit) {
        viewModel.loadWordOfTheDay()
    }

    // Native Android back gesture pops dictionary back stack before exiting tab
    BackHandler(enabled = uiState !is SearchUiState.Idle || !selectedCefr.isNullOrBlank()) {
        if (uiState !is SearchUiState.Idle) {
            viewModel.navigateBackInDictionary()
        } else if (!selectedCefr.isNullOrBlank()) {
            viewModel.selectCefrBrowse(null)
        }
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear search history?") },
            text = { Text("This removes your recent lookup list (${historyWords.size} entries) without removing your Study Deck.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("dictionary_search_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Sticky 52dp Spotlight Search Bar (48dp Clear & Go touch targets)
            StickyDictionarySearchBar(
                query = searchQuery,
                onQueryChange = { newQ ->
                    if (uiState !is SearchUiState.Idle) {
                        viewModel.clearSearchState()
                    }
                    viewModel.searchSuggestionsWithScope(newQ, booksOnly = scope == DictionaryScope.BOOKS)
                },
                onClear = {
                    viewModel.clearSearchState()
                    viewModel.searchSuggestionsWithScope("", booksOnly = scope == DictionaryScope.BOOKS)
                },
                onSubmit = {
                    if (searchQuery.trim().isNotEmpty()) {
                        viewModel.openWordFromSearch(searchQuery.trim(), pushToBackStack = false)
                    }
                }
            )

            // 2. When viewing a Word Detail (Success / Loading / Error) -> show Editorial Detail with Back Bar
            if (uiState !is SearchUiState.Idle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { viewModel.navigateBackInDictionary() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("dictionary_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (backStack.isNotEmpty()) {
                                backStack.last().replaceFirstChar { it.uppercase() }
                            } else {
                                "Back to Search"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (uiState) {
                        is SearchUiState.Loading -> {
                            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                    Text(
                                        text = "Looking up \"$searchQuery\"...",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                        is SearchUiState.Success -> {
                            WordDetailsCard(
                                wordEntity = uiState.word,
                                onSpeak = { viewModel.speak(uiState.word.word) },
                                onBookmarkToggle = { viewModel.toggleBookmark(uiState.word) },
                                viewModel = viewModel,
                                curriculumOccurrences = occurrences,
                                onOpenCourses = onOpenCourses,
                                onOpenCourseOccurrence = onOpenCourseOccurrence,
                                onSelectRelatedWord = { related -> viewModel.openWordFromSearch(related, pushToBackStack = true) }
                            )
                        }
                        is SearchUiState.Error -> {
                            ErrorStateCard(
                                message = uiState.message,
                                onRetry = { viewModel.performSearch(searchQuery) },
                                onOpenApiSettings = onOpenApiSettings
                            )
                            if (fuzzySuggestions.isNotEmpty()) {
                                DidYouMeanSection(
                                    fuzzy = fuzzySuggestions,
                                    onPick = { viewModel.openWordFromSearch(it.word, pushToBackStack = false) }
                                )
                            }
                        }
                        is SearchUiState.Idle -> Unit
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                return@Column
            }

            // 3. Apple Scope Bar (All / Books & IELTS / My Deck / History)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DictionaryScope.entries.forEach { item ->
                    val selected = scope == item
                    val label = when (item) {
                        DictionaryScope.MY_DECK -> "${item.label} (${bookmarkedWords.size})"
                        DictionaryScope.HISTORY -> "${item.label} (${historyWords.size})"
                        else -> item.label
                    }
                    Surface(
                        onClick = {
                            scope = item
                            if (searchQuery.trim().length >= 2 && (item == DictionaryScope.ALL || item == DictionaryScope.BOOKS)) {
                                viewModel.searchSuggestionsWithScope(searchQuery, booksOnly = item == DictionaryScope.BOOKS)
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("dict_scope_${item.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Scrollable Body based on Scope & Query
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (scope) {
                    DictionaryScope.MY_DECK -> {
                        MyDeckScopeBody(
                            query = searchQuery,
                            bookmarkedWords = bookmarkedWords,
                            statusFilter = deckStatusFilter,
                            onSelectStatus = { deckStatusFilter = it },
                            onOpenWord = { viewModel.openWordFromSearch(it.word, pushToBackStack = false) },
                            onSpeak = { viewModel.speak(it.word) },
                            onOpenCourses = onOpenCourses
                        )
                    }
                    DictionaryScope.HISTORY -> {
                        HistoryScopeBody(
                            query = searchQuery,
                            historyWords = historyWords,
                            bookmarkedSet = bookmarkedSet,
                            onOpenWord = { viewModel.openWordFromSearch(it.word, pushToBackStack = false) },
                            onSpeak = { viewModel.speak(it.word) },
                            onToggleDeck = { viewModel.quickToggleStudyWord(it.word, it.arabicMeaning) },
                            onRequestClear = { showClearHistoryConfirm = true }
                        )
                    }
                    DictionaryScope.ALL, DictionaryScope.BOOKS -> {
                        val trimmed = searchQuery.trim()
                        if (trimmed.length >= 2) {
                            LiveSearchResultsBody(
                                query = trimmed,
                                suggestions = suggestions,
                                fuzzySuggestions = fuzzySuggestions,
                                bookmarkedSet = bookmarkedSet,
                                onOpenWord = { viewModel.openWordFromSearch(it, pushToBackStack = false) },
                                onSpeak = { viewModel.speak(it) },
                                onToggleDeck = { w, ar -> viewModel.quickToggleStudyWord(w, ar) },
                                onOnlineLookup = { viewModel.openWordFromSearch(trimmed, pushToBackStack = false) }
                            )
                        } else {
                            DictionaryLandingBody(
                                recentWords = historyWords.take(8),
                                wordOfTheDay = wordOfTheDay,
                                selectedCefr = selectedCefr,
                                cefrWords = cefrWords,
                                bookmarkedSet = bookmarkedSet,
                                viewModel = viewModel,
                                onOpenWord = { viewModel.openWordFromSearch(it, pushToBackStack = false) },
                                onToggleDeck = { w, ar -> viewModel.quickToggleStudyWord(w, ar) },
                                onSelectCefr = { viewModel.selectCefrBrowse(it) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StickyDictionarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.75.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search English or Arabic (97K offline)...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSubmit()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_word_input")
                )
            }

            if (query.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "EN⇄AR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        onSubmit()
                        focusManager.clearFocus()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("search_word_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Look up word",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryLandingBody(
    recentWords: List<WordEntity>,
    wordOfTheDay: OfflineCurriculumWord?,
    selectedCefr: String?,
    cefrWords: List<OfflineCurriculumWord>,
    bookmarkedSet: Set<String>,
    viewModel: DictionaryViewModel,
    onOpenWord: (String) -> Unit,
    onToggleDeck: (String, String) -> Unit,
    onSelectCefr: (String?) -> Unit
) {
    // 1. Recent Searches Chips
    if (recentWords.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppleSectionHeader(title = "Recent Searches")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentWords.forEach { item ->
                    Surface(
                        onClick = { onOpenWord(item.word) },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = item.word.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
            }
        }
    }

    // 2. Deterministic Offline Word of the Day (with Local Photo + Arabic + Pronounce + Add)
    if (wordOfTheDay != null) {
        val inDeck = wordOfTheDay.word.lowercase() in bookmarkedSet
        val previewEntity = remember(wordOfTheDay.id) {
            WordEntity(
                word = wordOfTheDay.word.lowercase(),
                phonetic = wordOfTheDay.ipa,
                partOfSpeech = wordOfTheDay.pos,
                definition = wordOfTheDay.definitionEn,
                example = wordOfTheDay.exampleEn,
                arabicMeaning = wordOfTheDay.translationAr,
                isBookmarked = inDeck
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AppleSectionHeader(title = "Word of the Day · Offline")
            AppleInsetGroupedCard(
                cornerRadius = 20.dp,
                modifier = Modifier.testTag("word_of_the_day_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenWord(wordOfTheDay.word) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WordLocalImageBanner(
                        wordEntity = previewEntity,
                        viewModel = viewModel,
                        height = 128.dp,
                        showRefreshButton = false
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = wordOfTheDay.word.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (wordOfTheDay.translationAr.isNotBlank()) {
                                Text(
                                    text = wordOfTheDay.translationAr,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        textDirection = TextDirection.Rtl
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.speak(wordOfTheDay.word) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Pronounce word of the day",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        FilledTonalButton(
                            onClick = { onToggleDeck(wordOfTheDay.word, wordOfTheDay.translationAr) },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(if (inDeck) "In Deck ✓" else "+ Study", maxLines = 1)
                        }
                    }
                    Text(
                        text = wordOfTheDay.definitionEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // 3. Browse by CEFR / IELTS Level
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleSectionHeader(title = "Browse Offline by Level")
            if (selectedCefr != null) {
                TextButton(onClick = { onSelectCefr(null) }) { Text("Clear") }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("A1", "A2", "B1", "B2", "C1", "IELTS").forEach { level ->
                val isSelected = selectedCefr == level
                Surface(
                    onClick = { onSelectCefr(if (isSelected) null else level) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.height(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        if (selectedCefr != null && cefrWords.isNotEmpty()) {
            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                cefrWords.forEachIndexed { idx, cw ->
                    val inDeck = cw.word.lowercase() in bookmarkedSet
                    QuickDictionaryRow(
                        word = cw.word,
                        arabic = cw.translationAr,
                        badge = cw.bookShortTag,
                        inDeck = inDeck,
                        onOpen = { onOpenWord(cw.word) },
                        onSpeak = { viewModel.speak(cw.word) },
                        onToggleDeck = { onToggleDeck(cw.word, cw.translationAr) }
                    )
                    if (idx < cefrWords.lastIndex) {
                        AppleInsetDivider(startIndent = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSearchResultsBody(
    query: String,
    suggestions: List<OfflineSearchSuggestion>,
    fuzzySuggestions: List<OfflineSearchSuggestion>,
    bookmarkedSet: Set<String>,
    onOpenWord: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onToggleDeck: (String, String) -> Unit,
    onOnlineLookup: () -> Unit
) {
    if (suggestions.isEmpty()) {
        AppleInsetGroupedCard(cornerRadius = 20.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "No exact offline match for \"$query\"",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center
                )
                if (fuzzySuggestions.isNotEmpty()) {
                    DidYouMeanSection(
                        fuzzy = fuzzySuggestions,
                        onPick = { onOpenWord(it.word) }
                    )
                }
                Button(
                    onClick = onOnlineLookup,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Look up \"$query\" with Online AI")
                }
            }
        }
        return
    }

    // Exact / Stem Match Hero Card right at top for instant answer without extra tap
    val top = suggestions.first()
    val isTopHero = top.word.equals(query, ignoreCase = true) || top.badge.contains("Stem", ignoreCase = true)
    if (isTopHero) {
        val inDeck = top.word.lowercase() in bookmarkedSet
        Surface(
            onClick = { onOpenWord(top.word) },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exact_match_hero_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = if (top.badge.contains("Stem")) "Stem Match · ${top.badge}" else "Exact Match · ${top.badge}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text("Tap for full card ›", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = top.word.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                textDirection = TextDirection.Ltr
                            )
                        )
                        if (top.translationAr.isNotBlank()) {
                            Text(
                                text = top.translationAr,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    textDirection = TextDirection.Rtl
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { onSpeak(top.word) }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronounce", tint = MaterialTheme.colorScheme.primary)
                    }
                    FilledTonalButton(
                        onClick = { onToggleDeck(top.word, top.translationAr) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(if (inDeck) "✓" else "+ Add")
                    }
                }
                if (top.previewDef.isNotBlank()) {
                    Text(
                        text = top.previewDef,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    // All matching rows with instant 48dp Speak & Add buttons
    AppleInsetGroupedCard(
        cornerRadius = 18.dp,
        modifier = Modifier.testTag("offline_suggestions_card")
    ) {
        suggestions.forEachIndexed { idx, s ->
            val inDeck = s.word.lowercase() in bookmarkedSet
            QuickDictionaryRow(
                word = s.word,
                arabic = s.translationAr,
                badge = s.badge,
                inDeck = inDeck,
                onOpen = { onOpenWord(s.word) },
                onSpeak = { onSpeak(s.word) },
                onToggleDeck = { onToggleDeck(s.word, s.translationAr) }
            )
            if (idx < suggestions.lastIndex) {
                AppleInsetDivider(startIndent = 16.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DidYouMeanSection(
    fuzzy: List<OfflineSearchSuggestion>,
    onPick: (OfflineSearchSuggestion) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Did you mean:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fuzzy.forEach { s ->
                Surface(
                    onClick = { onPick(s) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = s.word.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickDictionaryRow(
    word: String,
    arabic: String,
    badge: String,
    inDeck: Boolean,
    onOpen: () -> Unit,
    onSpeak: () -> Unit,
    onToggleDeck: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
            .testTag("offline_suggestion_${word.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = word.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDirection = TextDirection.Ltr
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badge.isNotBlank()) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (arabic.isNotBlank()) {
                Text(
                    text = arabic,
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onSpeak, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Pronounce $word",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onToggleDeck, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = if (inDeck) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                contentDescription = if (inDeck) "In study deck" else "Add to study deck",
                tint = if (inDeck) Color(0xFF30D158) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MyDeckScopeBody(
    query: String,
    bookmarkedWords: List<WordEntity>,
    statusFilter: WordStatusFilter,
    onSelectStatus: (WordStatusFilter) -> Unit,
    onOpenWord: (WordEntity) -> Unit,
    onSpeak: (WordEntity) -> Unit,
    onOpenCourses: () -> Unit
) {
    // Status Filter Pills
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WordStatusFilter.entries.forEach { st ->
            val selected = statusFilter == st
            Surface(
                onClick = { onSelectStatus(st) },
                shape = RoundedCornerShape(18.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.heightIn(min = 40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(
                        text = st.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val filtered = remember(bookmarkedWords, query, statusFilter) {
        filterWordList(
            list = bookmarkedWords,
            searchQuery = query,
            statusFilter = statusFilter,
            posFilter = "ALL",
            tagFilter = ""
        )
    }

    if (filtered.isEmpty()) {
        AppleInsetGroupedCard(cornerRadius = 18.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (bookmarkedWords.isEmpty()) "Your Study Deck is Empty" else "No matching cards",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Add words from Courses or Search to build your spaced-repetition deck.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onOpenCourses, shape = RoundedCornerShape(14.dp), modifier = Modifier.height(48.dp)) {
                    Text("Open Courses")
                }
            }
        }
        return
    }

    AppleInsetGroupedCard(cornerRadius = 18.dp) {
        filtered.take(100).forEachIndexed { idx, w ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenWord(w) }
                    .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = w.word.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        CardStatusBadge(wordEntity = w)
                    }
                    if (w.arabicMeaning.isNotBlank()) {
                        Text(
                            text = w.arabicMeaning,
                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = { onSpeak(w) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Pronounce", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (idx < filtered.take(100).lastIndex) {
                AppleInsetDivider(startIndent = 16.dp)
            }
        }
    }
}

@Composable
private fun HistoryScopeBody(
    query: String,
    historyWords: List<WordEntity>,
    bookmarkedSet: Set<String>,
    onOpenWord: (WordEntity) -> Unit,
    onSpeak: (WordEntity) -> Unit,
    onToggleDeck: (WordEntity) -> Unit,
    onRequestClear: () -> Unit
) {
    val filtered = remember(historyWords, query) {
        val q = query.trim().lowercase()
        val normAr = com.example.data.ArabicNormalizer.normalize(q)
        if (q.isEmpty()) historyWords else historyWords.filter {
            it.word.lowercase().contains(q) ||
                it.arabicMeaning.contains(q) ||
                (normAr.isNotEmpty() && com.example.data.ArabicNormalizer.normalize(it.arabicMeaning).contains(normAr))
        }
    }

    if (historyWords.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleSectionHeader(title = "Lookup History (${filtered.size})")
            TextButton(onClick = onRequestClear) {
                Text("Clear", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (filtered.isEmpty()) {
        AppleInsetGroupedCard(cornerRadius = 18.dp) {
            Text(
                text = "No lookup history yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        }
        return
    }

    AppleInsetGroupedCard(cornerRadius = 18.dp) {
        filtered.take(80).forEachIndexed { idx, w ->
            QuickDictionaryRow(
                word = w.word,
                arabic = w.arabicMeaning,
                badge = w.partOfSpeech,
                inDeck = w.word.lowercase() in bookmarkedSet,
                onOpen = { onOpenWord(w) },
                onSpeak = { onSpeak(w) },
                onToggleDeck = { onToggleDeck(w) }
            )
            if (idx < filtered.take(80).lastIndex) {
                AppleInsetDivider(startIndent = 16.dp)
            }
        }
    }
}

/**
 * Preserved for GreetingScreenshotTest compatibility.
 */
@Composable
fun HeroBannerSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Redeus Dictionary",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "6 Essential Books • 3 IELTS Books • 87K Arabic • FSRS",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverySuggestions(onSelectWord: (String) -> Unit) {
    val list = listOf("aquatic", "resilient", "serendipity", "ephemeral", "benevolent", "pragmatic")
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppleSectionHeader(title = "Try a Word")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            list.forEach { word ->
                val interaction = remember { MutableInteractionSource() }
                Surface(
                    onClick = { onSelectWord(word) },
                    interactionSource = interaction,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.5.dp, borderColor),
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .applePressScale(interaction, 0.95f)
                        .testTag("seed_word_$word")
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = word.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TryAnotherPhotoButton(
    word: WordEntity,
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val imageSource by viewModel.imageSourcePreference.collectAsStateWithLifecycle()
    if (imageSource != "web") return

    val cyclingWord by viewModel.cyclingPhotoWord.collectAsStateWithLifecycle()
    val isCycling = cyclingWord.equals(word.word.trim(), ignoreCase = true)

    Surface(
        onClick = { viewModel.cycleNextPhoto(word) },
        enabled = !isCycling,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
        modifier = modifier
            .size(if (compact) 48.dp else 48.dp)
            .testTag("try_another_photo_button")
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isCycling) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Try another photo",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordDetailsCard(
    wordEntity: WordEntity,
    onSpeak: () -> Unit,
    onBookmarkToggle: () -> Unit,
    viewModel: DictionaryViewModel,
    curriculumOccurrences: List<OfflineCurriculumWord> = emptyList(),
    onOpenCourses: () -> Unit = {},
    onOpenCourseOccurrence: (bookNum: Int, unitNum: Int) -> Unit = { _, _ -> onOpenCourses() },
    onSelectRelatedWord: (String) -> Unit = { viewModel.openWordFromSearch(it, pushToBackStack = true) }
) {
    var showCardInfoDialog by remember { mutableStateOf(false) }
    var showAiHelperSheet by remember { mutableStateOf(false) }
    val selectedAccent by viewModel.selectedAccent.collectAsStateWithLifecycle()
    val displayPhonetic = getPhoneticForAccent(wordEntity.phonetic, selectedAccent)
    val haptics = LocalHapticFeedback.current

    if (showCardInfoDialog) {
        WordCardInfoDialog(
            word = wordEntity,
            viewModel = viewModel,
            onDismiss = { showCardInfoDialog = false }
        )
    }

    if (showAiHelperSheet) {
        WordAiHelperSheet(
            wordEntity = wordEntity,
            viewModel = viewModel,
            onDismiss = { showAiHelperSheet = false },
            onSelectRelatedWord = { related ->
                showAiHelperSheet = false
                onSelectRelatedWord(related)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_details_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Editorial Headword Header + Clear 48dp Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = wordEntity.word.replaceFirstChar { it.uppercase() },
                        style = RedeusExpressiveType.headword.copy(
                            fontSize = 34.sp,
                            textDirection = TextDirection.Ltr
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (displayPhonetic.isNotBlank()) {
                            Text(
                                text = displayPhonetic,
                                style = RedeusExpressiveType.phonetic.copy(textDirection = TextDirection.Ltr),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (wordEntity.partOfSpeech.isNotBlank()) {
                            Text(
                                text = "• ${wordEntity.partOfSpeech}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val speakInteraction = remember { MutableInteractionSource() }
                    Surface(
                        onClick = onSpeak,
                        interactionSource = speakInteraction,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(48.dp)
                            .applePressScale(speakInteraction, 0.92f)
                            .testTag("speak_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Pronounce word",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showCardInfoDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("word_card_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Card review history",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            // Primary Action Row: Clear "+ Add to Study Deck" / "✓ In Study Deck" + "✨ AI Helper"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val deckInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBookmarkToggle()
                    },
                    interactionSource = deckInteraction,
                    shape = RoundedCornerShape(14.dp),
                    colors = if (wordEntity.isBookmarked) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF30D158).copy(alpha = 0.18f),
                            contentColor = Color(0xFF30D158)
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .applePressScale(deckInteraction, 0.97f)
                        .testTag("bookmark_toggle")
                ) {
                    Icon(
                        imageVector = if (wordEntity.isBookmarked) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (wordEntity.isBookmarked) "In Study Deck" else "Add to Study",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                }

                val helperInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAiHelperSheet = true
                    },
                    interactionSource = helperInteraction,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier
                        .height(48.dp)
                        .applePressScale(helperInteraction, 0.96f)
                        .testTag("enrich_with_ai_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("AI Helper", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CardStatusBadge(wordEntity = wordEntity)
                if (wordEntity.isLeech) {
                    LeechBadge(lapses = wordEntity.lapses)
                }
            }

            WordTagsRow(word = wordEntity, viewModel = viewModel)
        }

        // 2. Local Permanent Word Photo (Bundled 3,586 WebP or User-Saved Offline Asset)
        WordLocalImageBanner(
            wordEntity = wordEntity,
            viewModel = viewModel,
            height = 196.dp,
            showRefreshButton = true
        )

        // 3. Bilingual Meaning Card (RTL Arabic + English Definition + Found in Books)
        AppleInsetGroupedCard(cornerRadius = 18.dp) {
            val arabicEnabled by viewModel.arabicTranslationEnabled.collectAsStateWithLifecycle()
            val translations by viewModel.wordArabicTranslations.collectAsStateWithLifecycle()
            val wordKey = wordEntity.word.trim().lowercase()
            val arabicText = wordEntity.arabicMeaning.ifBlank { translations[wordKey] ?: "" }

            LaunchedEffect(wordKey, arabicEnabled) {
                if (arabicEnabled && arabicText.isBlank()) {
                    viewModel.ensureOfflineArabicLoaded(wordEntity)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (arabicEnabled && arabicText.isNotBlank()) {
                    Text(
                        text = arabicText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("arabic_translation_text")
                    )
                    if (wordEntity.definition.isNotBlank() && wordEntity.definition != arabicText) {
                        AppleInsetDivider(startIndent = 0.dp)
                    }
                }

                if (wordEntity.definition.isNotBlank() && wordEntity.definition != arabicText) {
                    Text(
                        text = wordEntity.definition,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.5.sp,
                            lineHeight = 24.sp,
                            textDirection = TextDirection.Ltr
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (curriculumOccurrences.isNotEmpty()) {
                    AppleInsetDivider(startIndent = 0.dp)
                    Text(
                        text = "FOUND IN CURRICULUM",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        curriculumOccurrences.distinctBy { "${it.bookNum}-${it.unitNum}" }.forEach { occ ->
                            Surface(
                                onClick = { onOpenCourseOccurrence(occ.bookNum, occ.unitNum) },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "📘 ${occ.bookShortTag} · Unit ${occ.unitNum}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Highlighted Example Card + 48dp Listen Example + Mine Sentence
        if (wordEntity.example.isNotBlank()) {
            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EXAMPLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        HighlightedExampleText(
                            example = wordEntity.example,
                            targetWord = wordEntity.word,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ExampleSentenceMiningActionsRow(
                        wordEntity = wordEntity,
                        viewModel = viewModel
                    )
                }
            }
        }

        // 5. Synonyms & Antonyms (tapping pushes onto Back Stack!)
        if (wordEntity.synonyms.isNotBlank() || wordEntity.antonyms.isNotBlank()) {
            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (wordEntity.synonyms.isNotBlank()) {
                        Text(
                            text = "SYNONYMS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wordEntity.synonyms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { syn ->
                                Surface(
                                    onClick = { onSelectRelatedWord(syn) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(text = syn, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                    }
                                }
                            }
                        }
                    }

                    if (wordEntity.antonyms.isNotBlank()) {
                        if (wordEntity.synonyms.isNotBlank()) AppleInsetDivider(startIndent = 0.dp)
                        Text(
                            text = "ANTONYMS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wordEntity.antonyms.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { ant ->
                                Surface(
                                    onClick = { onSelectRelatedWord(ant) },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(text = ant, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Memory Aid (if saved offline)
        if (wordEntity.mnemonic.isNotBlank()) {
            AppleInsetGroupedCard(cornerRadius = 18.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AppleIconBadge(
                        icon = Icons.Default.Lightbulb,
                        tintColor = Color(0xFFFF9F0A),
                        size = 34.dp,
                        iconSize = 18.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "MEMORY AID",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFFFF9F0A)
                        )
                        Text(
                            text = wordEntity.mnemonic,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    onOpenApiSettings: (() -> Unit)? = null
) {
    val isOffline = remember(message) {
        val lower = message.lowercase()
        lower.contains("offline") ||
            lower.contains("network") ||
            lower.contains("internet") ||
            lower.contains("unable to resolve host") ||
            lower.contains("timeout")
    }

    AppleInsetGroupedCard(
        modifier = Modifier.testTag("error_state_card"),
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppleIconBadge(
                icon = if (isOffline) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                tintColor = MaterialTheme.colorScheme.error,
                size = 44.dp,
                iconSize = 24.dp
            )
            Text(
                text = if (isOffline) "You Appear to Be Offline" else "Word Not Found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Try Again")
                }
                if (onOpenApiSettings != null && !isOffline) {
                    OutlinedButton(
                        onClick = onOpenApiSettings,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("error_open_api_settings_button")
                    ) {
                        Text("API Settings")
                    }
                }
            }
        }
    }
}
