package com.example.ui.screens.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.DictionaryViewModel

private enum class CourseCatalogFilter(val label: String) {
    ALL("All (9 Books)"),
    ESSENTIAL("6 Essential Books"),
    IELTS("IELTS (3 Books)"),
    TOPICS("26 Topics")
}

@Composable
fun CoursesExplorerContent(
    viewModel: DictionaryViewModel,
    bookmarkedWords: List<WordEntity>,
    onNavigateToSearch: () -> Unit,
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val books by viewModel.offlineBooks.collectAsStateWithLifecycle()
    val topics by viewModel.offlineTopics.collectAsStateWithLifecycle()
    val bookWordLowersMap by viewModel.bookWordLowersMap.collectAsStateWithLifecycle()
    val topicWordLowersMap by viewModel.topicWordLowersMap.collectAsStateWithLifecycle()
    val selectedBook by viewModel.selectedCourseBook.collectAsStateWithLifecycle()
    val courseUnits by viewModel.courseUnits.collectAsStateWithLifecycle()
    val unitWordLowersMap by viewModel.unitWordLowersMap.collectAsStateWithLifecycle()
    val selectedUnit by viewModel.selectedCourseUnit.collectAsStateWithLifecycle()
    val selectedTopic by viewModel.selectedCourseTopic.collectAsStateWithLifecycle()
    val courseWords by viewModel.courseWords.collectAsStateWithLifecycle()
    val courseSearchQuery by viewModel.courseSearchQuery.collectAsStateWithLifecycle()
    val courseSearchResults by viewModel.courseSearchResults.collectAsStateWithLifecycle()
    val isAdding by viewModel.isAddingCourseWords.collectAsStateWithLifecycle()

    var catalogFilter by rememberSaveable { mutableStateOf(CourseCatalogFilter.ALL) }

    val deckWordMap = remember(bookmarkedWords) {
        bookmarkedWords.associateBy { it.word.trim().lowercase() }
    }
    val masteredWordSet = remember(bookmarkedWords) {
        bookmarkedWords
            .filter { it.distinctReviewDays >= 3 && it.interval >= 21 && !it.isLeech }
            .map { it.word.trim().lowercase() }
            .toSet()
    }

    LaunchedEffect(Unit) {
        if (books.isEmpty()) {
            viewModel.loadOfflineCatalog()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            selectedTopic != null -> {
                val topic = selectedTopic!!
                val extraTags = listOf("Topic: ${topic.nameEn}")
                CourseWordsListPane(
                    title = "${topic.nameEn} • ${topic.nameAr}",
                    subtitle = "${courseWords.size} words • Essential 6 Books",
                    deckTagForReview = "Topic: ${topic.nameEn}",
                    words = courseWords,
                    deckWordMap = deckWordMap,
                    masteredWordSet = masteredWordSet,
                    isAdding = isAdding,
                    onBack = { viewModel.selectCourseTopic(null) },
                    onAddBatch = { batchSize ->
                        viewModel.addNextBatchToDeck(courseWords, batchSize, extraTags, topic.nameEn)
                    },
                    onAddAll = {
                        viewModel.addAllWordsToDeck(courseWords, extraTags, topic.nameEn)
                    },
                    onReviewDeck = {
                        viewModel.studyDeckTagInReview("Topic: ${topic.nameEn}")
                        onNavigateToReview()
                    },
                    onWordClick = { item ->
                        viewModel.openCourseWord(item, extraTags) { onNavigateToSearch() }
                    },
                    onWordToggleDeck = { item, inDeck ->
                        viewModel.toggleCourseWordInDeck(item, inDeck, extraTags)
                    }
                )
            }

            selectedBook != null && selectedUnit != null -> {
                val book = selectedBook!!
                val unit = selectedUnit!!
                val unitDeckTag = OfflineDictionaryDatabase.buildDeckTagForUnit(book.shortTag, unit.unitNum)
                CourseWordsListPane(
                    title = unit.title,
                    subtitle = "${book.shortTag} (${book.cefr}) • ${courseWords.size} words",
                    deckTagForReview = unitDeckTag,
                    words = courseWords,
                    deckWordMap = deckWordMap,
                    masteredWordSet = masteredWordSet,
                    isAdding = isAdding,
                    onBack = { viewModel.selectCourseUnit(null) },
                    onAddBatch = { batchSize ->
                        viewModel.addNextBatchToDeck(courseWords, batchSize, label = "${book.shortTag} U${unit.unitNum}")
                    },
                    onAddAll = {
                        viewModel.addAllWordsToDeck(courseWords, label = "${book.shortTag} U${unit.unitNum}")
                    },
                    onReviewDeck = {
                        viewModel.studyDeckTagInReview(unitDeckTag)
                        onNavigateToReview()
                    },
                    onWordClick = { item ->
                        viewModel.openCourseWord(item) { onNavigateToSearch() }
                    },
                    onWordToggleDeck = { item, inDeck ->
                        viewModel.toggleCourseWordInDeck(item, inDeck)
                    }
                )
            }

            selectedBook != null -> {
                val book = selectedBook!!
                val bookWordsSet = bookWordLowersMap[book.bookNum].orEmpty()
                val inDeckCount = bookWordsSet.count { it in deckWordMap }
                val masteredCount = bookWordsSet.count { it in masteredWordSet }

                CourseBookUnitsPane(
                    book = book,
                    units = courseUnits,
                    unitWordLowersMap = unitWordLowersMap,
                    deckWordMap = deckWordMap,
                    masteredWordSet = masteredWordSet,
                    bookInDeckCount = inDeckCount,
                    bookMasteredCount = masteredCount,
                    onBack = { viewModel.selectCourseBook(null) },
                    onSelectUnit = { viewModel.selectCourseUnit(it) },
                    onReviewBook = {
                        viewModel.studyDeckTagInReview(book.shortTag)
                        onNavigateToReview()
                    }
                )
            }

            else -> {
                // Root Courses Catalog View (6 Essential Books + 3 IELTS Books + 26 Topics)
                OutlinedTextField(
                    value = courseSearchQuery,
                    onValueChange = { q ->
                        val cat = when (catalogFilter) {
                            CourseCatalogFilter.ESSENTIAL -> "essential"
                            CourseCatalogFilter.IELTS -> "ielts"
                            else -> null
                        }
                        viewModel.setCourseSearchQuery(q, cat)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("courses_search_input"),
                    placeholder = { Text("Search 9,591 Book & IELTS words (EN / عربي)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (courseSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setCourseSearchQuery("") },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CourseCatalogFilter.entries.forEach { filter ->
                        val selected = catalogFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = {
                                catalogFilter = filter
                                if (courseSearchQuery.trim().length >= 2) {
                                    val cat = when (filter) {
                                        CourseCatalogFilter.ESSENTIAL -> "essential"
                                        CourseCatalogFilter.IELTS -> "ielts"
                                        else -> null
                                    }
                                    viewModel.setCourseSearchQuery(courseSearchQuery, cat)
                                }
                            },
                            label = { Text(filter.label) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("course_filter_${filter.name.lowercase()}")
                        )
                    }
                }

                if (courseSearchQuery.trim().length >= 2) {
                    if (courseSearchResults.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Text(
                                text = "No curriculum words matched \"$courseSearchQuery\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(courseSearchResults, key = { it.id }) { item ->
                                val inDeck = deckWordMap.containsKey(item.word.lowercase())
                                val isMastered = item.word.lowercase() in masteredWordSet
                                CurriculumWordRow(
                                    item = item,
                                    inDeck = inDeck,
                                    isMastered = isMastered,
                                    showSourceBadge = true,
                                    onClick = { viewModel.openCourseWord(item) { onNavigateToSearch() } },
                                    onToggleDeck = { viewModel.toggleCourseWordInDeck(item, inDeck) }
                                )
                            }
                        }
                    }
                } else if (catalogFilter == CourseCatalogFilter.TOPICS) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(topics, key = { it.id }) { topic ->
                            val wordsSet = topicWordLowersMap[topic.id].orEmpty()
                            val inDeck = wordsSet.count { it in deckWordMap }
                            val mastered = wordsSet.count { it in masteredWordSet }
                            TopicCourseCard(
                                topic = topic,
                                inDeckCount = inDeck,
                                masteredCount = mastered,
                                onClick = { viewModel.selectCourseTopic(topic) }
                            )
                        }
                    }
                } else {
                    val ieltsBooks = books.filter { it.category == "ielts" }
                    val essentialBooks = books.filter { it.category == "essential" }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item(key = "anki_banner") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Anki-Style FSRS Study Decks",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Pick a Book or Unit, add 10–20 new cards per day to your Deck, and review them on schedule with FSRS.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }

                        if (catalogFilter == CourseCatalogFilter.ALL || catalogFilter == CourseCatalogFilter.IELTS) {
                            item(key = "header_ielts") {
                                Text(
                                    text = "IELTS Preparation (${ieltsBooks.sumOf { it.wordCount }} words)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            items(ieltsBooks, key = { "book_${it.bookNum}" }) { book ->
                                val wordsSet = bookWordLowersMap[book.bookNum].orEmpty()
                                val inDeck = wordsSet.count { it in deckWordMap }
                                val mastered = wordsSet.count { it in masteredWordSet }
                                BookCourseCard(
                                    book = book,
                                    inDeckCount = inDeck,
                                    masteredCount = mastered,
                                    onClick = { viewModel.selectCourseBook(book) },
                                    onReviewBook = if (inDeck > 0) {
                                        {
                                            viewModel.studyDeckTagInReview(book.shortTag)
                                            onNavigateToReview()
                                        }
                                    } else null
                                )
                            }
                        }

                        if (catalogFilter == CourseCatalogFilter.ALL || catalogFilter == CourseCatalogFilter.ESSENTIAL) {
                            item(key = "header_essential") {
                                Text(
                                    text = "6 Essential English Books (${essentialBooks.sumOf { it.wordCount }} words)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(essentialBooks, key = { "book_${it.bookNum}" }) { book ->
                                val wordsSet = bookWordLowersMap[book.bookNum].orEmpty()
                                val inDeck = wordsSet.count { it in deckWordMap }
                                val mastered = wordsSet.count { it in masteredWordSet }
                                BookCourseCard(
                                    book = book,
                                    inDeckCount = inDeck,
                                    masteredCount = mastered,
                                    onClick = { viewModel.selectCourseBook(book) },
                                    onReviewBook = if (inDeck > 0) {
                                        {
                                            viewModel.studyDeckTagInReview(book.shortTag)
                                            onNavigateToReview()
                                        }
                                    } else null
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
private fun BookCourseCard(
    book: OfflineBook,
    inDeckCount: Int,
    masteredCount: Int,
    onClick: () -> Unit,
    onReviewBook: (() -> Unit)?
) {
    val isIelts = book.category == "ielts"
    val progress = if (book.wordCount > 0) inDeckCount.toFloat() / book.wordCount.toFloat() else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_book_card_${book.bookNum}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = if (isIelts) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (isIelts) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = if (isIelts) "IELTS" else "Essential",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (book.cefr.isNotBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = book.cefr,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${book.unitCount} units • ${book.wordCount} words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open book",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$inDeckCount / ${book.wordCount} in Deck • $masteredCount mastered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onReviewBook != null) {
                    TextButton(
                        onClick = onReviewBook,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Review", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicCourseCard(
    topic: OfflineTopic,
    inDeckCount: Int,
    masteredCount: Int,
    onClick: () -> Unit
) {
    val progress = if (topic.wordCount > 0) inDeckCount.toFloat() / topic.wordCount.toFloat() else 0f
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_topic_card_${topic.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.nameEn,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${topic.nameAr} • ${topic.wordCount} words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open topic",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
            )
            Text(
                text = "$inDeckCount / ${topic.wordCount} in Deck • $masteredCount mastered",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CourseBookUnitsPane(
    book: OfflineBook,
    units: List<OfflineUnit>,
    unitWordLowersMap: Map<Int, Set<String>>,
    deckWordMap: Map<String, WordEntity>,
    masteredWordSet: Set<String>,
    bookInDeckCount: Int,
    bookMasteredCount: Int,
    onBack: () -> Unit,
    onSelectUnit: (OfflineUnit) -> Unit,
    onReviewBook: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("courses_back_to_books")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to books")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${book.cefr} • $bookInDeckCount / ${book.wordCount} in Deck • $bookMasteredCount mastered",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (bookInDeckCount > 0) {
                        FilledTonalButton(onClick = onReviewBook) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Review")
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(units, key = { it.id }) { unit ->
                val unitWords = unitWordLowersMap[unit.unitNum].orEmpty()
                val inDeck = unitWords.count { it in deckWordMap }
                val mastered = unitWords.count { it in masteredWordSet }
                val progress = if (unit.wordCount > 0) inDeck.toFloat() / unit.wordCount.toFloat() else 0f

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_unit_card_${unit.id}"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    onClick = { onSelectUnit(unit) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = unit.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${unit.wordCount} words • $inDeck in Deck • $mastered mastered",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open unit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseWordsListPane(
    title: String,
    subtitle: String,
    deckTagForReview: String,
    words: List<OfflineCurriculumWord>,
    deckWordMap: Map<String, WordEntity>,
    masteredWordSet: Set<String>,
    isAdding: Boolean,
    onBack: () -> Unit,
    onAddBatch: (Int) -> Unit,
    onAddAll: () -> Unit,
    onReviewDeck: () -> Unit,
    onWordClick: (OfflineCurriculumWord) -> Unit,
    onWordToggleDeck: (OfflineCurriculumWord, Boolean) -> Unit
) {
    val inDeckCount = remember(words, deckWordMap) {
        words.count { deckWordMap.containsKey(it.word.lowercase()) }
    }
    val remainingCount = (words.size - inDeckCount).coerceAtLeast(0)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("courses_back_to_units")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$subtitle • $inDeckCount/${words.size} in Deck",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Anki-style batch study actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (remainingCount > 0) {
                        Button(
                            onClick = { onAddBatch(10) },
                            enabled = !isAdding,
                            modifier = Modifier.testTag("add_next_10_cards_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+10 to Deck")
                        }
                        if (remainingCount > 10) {
                            FilledTonalButton(
                                onClick = { onAddBatch(20) },
                                enabled = !isAdding
                            ) {
                                Text("+20")
                            }
                        }
                        OutlinedButton(
                            onClick = onAddAll,
                            enabled = !isAdding,
                            modifier = Modifier.testTag("add_all_unit_cards_button")
                        ) {
                            Text("Add all ($remainingCount)")
                        }
                    }
                    if (inDeckCount > 0) {
                        FilledTonalButton(
                            onClick = onReviewDeck,
                            modifier = Modifier.testTag("review_unit_deck_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Review ($inDeckCount)")
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(words, key = { it.id }) { item ->
                val inDeck = deckWordMap.containsKey(item.word.lowercase())
                val isMastered = item.word.lowercase() in masteredWordSet
                CurriculumWordRow(
                    item = item,
                    inDeck = inDeck,
                    isMastered = isMastered,
                    showSourceBadge = false,
                    onClick = { onWordClick(item) },
                    onToggleDeck = { onWordToggleDeck(item, inDeck) }
                )
            }
        }
    }
}

@Composable
private fun CurriculumWordRow(
    item: OfflineCurriculumWord,
    inDeck: Boolean,
    isMastered: Boolean,
    showSourceBadge: Boolean,
    onClick: () -> Unit,
    onToggleDeck: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_word_row_${item.word.lowercase()}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.word.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.ipa.isNotBlank()) {
                        Text(
                            text = item.ipa,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                    if (isMastered) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Text(
                                text = "Mastered",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    } else if (showSourceBadge && item.bookShortTag.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = item.bookShortTag,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                if (item.translationAr.isNotBlank()) {
                    Text(
                        text = item.translationAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.definitionEn.isNotBlank()) {
                    Text(
                        text = item.definitionEn,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onToggleDeck,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("course_word_bookmark_${item.word.lowercase()}")
            ) {
                Icon(
                    imageVector = if (inDeck) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (inDeck) "Remove from Deck" else "Add to Anki FSRS Deck",
                    tint = if (inDeck) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
