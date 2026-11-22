package com.example.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.DictionaryViewModel
import com.example.ui.components.*

private enum class CourseCatalogFilter(val label: String) {
    ALL("All"),
    IELTS("IELTS"),
    ESSENTIAL("6 Books"),
    TOPICS("Topics")
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
    val haptics = LocalHapticFeedback.current

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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            selectedTopic != null -> {
                val topic = selectedTopic!!
                val extraTags = listOf("Topic: ${topic.nameEn}")
                CourseWordsListPane(
                    title = "${topic.nameEn} • ${topic.nameAr}",
                    subtitle = "${courseWords.size} words • Essential 6 Books",
                    words = courseWords,
                    deckWordMap = deckWordMap,
                    masteredWordSet = masteredWordSet,
                    isAdding = isAdding,
                    onBack = { viewModel.selectCourseTopic(null) },
                    onAddBatch = { batchSize ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.addNextBatchToDeck(courseWords, batchSize, extraTags, topic.nameEn)
                    },
                    onAddAll = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    words = courseWords,
                    deckWordMap = deckWordMap,
                    masteredWordSet = masteredWordSet,
                    isAdding = isAdding,
                    onBack = { viewModel.selectCourseUnit(null) },
                    onAddBatch = { batchSize ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.addNextBatchToDeck(courseWords, batchSize, label = "${book.shortTag} U${unit.unitNum}")
                    },
                    onAddAll = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                // Apple HIG Spotlight-style inline search field
                AppleInlineSearchField(
                    query = courseSearchQuery,
                    placeholder = "Search 9,591 Book & IELTS words...",
                    onQueryChange = { q ->
                        val cat = when (catalogFilter) {
                            CourseCatalogFilter.ESSENTIAL -> "essential"
                            CourseCatalogFilter.IELTS -> "ielts"
                            else -> null
                        }
                        viewModel.setCourseSearchQuery(q, cat)
                    },
                    onClear = { viewModel.setCourseSearchQuery("") },
                    testTag = "courses_search_input"
                )

                // Clean Apple pill filter bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CourseCatalogFilter.entries.forEach { filter ->
                        val selected = catalogFilter == filter
                        val interaction = remember { MutableInteractionSource() }
                        Surface(
                            modifier = Modifier
                                .applePressScale(interaction, 0.96f)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null
                                ) {
                                    catalogFilter = filter
                                    if (courseSearchQuery.trim().length >= 2) {
                                        val cat = when (filter) {
                                            CourseCatalogFilter.ESSENTIAL -> "essential"
                                            CourseCatalogFilter.IELTS -> "ielts"
                                            else -> null
                                        }
                                        viewModel.setCourseSearchQuery(courseSearchQuery, cat)
                                    }
                                }
                                .testTag("course_filter_${filter.name.lowercase()}"),
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                if (courseSearchQuery.trim().length >= 2) {
                    if (courseSearchResults.isEmpty()) {
                        AppleInsetGroupedCard {
                            Text(
                                text = "No matches for \"$courseSearchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            item(key = "search_results_group") {
                                AppleInsetGroupedCard {
                                    courseSearchResults.forEachIndexed { idx, item ->
                                        val inDeck = deckWordMap.containsKey(item.word.lowercase())
                                        val isMastered = item.word.lowercase() in masteredWordSet
                                        CurriculumWordInsetRow(
                                            item = item,
                                            inDeck = inDeck,
                                            isMastered = isMastered,
                                            showSourceBadge = true,
                                            onClick = { viewModel.openCourseWord(item) { onNavigateToSearch() } },
                                            onToggleDeck = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.toggleCourseWordInDeck(item, inDeck)
                                            }
                                        )
                                        if (idx < courseSearchResults.lastIndex) {
                                            AppleInsetDivider(startIndent = 16.dp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (catalogFilter == CourseCatalogFilter.TOPICS) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item(key = "topics_header") {
                            AppleSectionHeader(title = "26 Life Topics")
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        item(key = "topics_group") {
                            AppleInsetGroupedCard {
                                topics.forEachIndexed { idx, topic ->
                                    val wordsSet = topicWordLowersMap[topic.id].orEmpty()
                                    val inDeck = wordsSet.count { it in deckWordMap }
                                    val mastered = wordsSet.count { it in masteredWordSet }
                                    TopicInsetRow(
                                        topic = topic,
                                        inDeckCount = inDeck,
                                        masteredCount = mastered,
                                        onClick = { viewModel.selectCourseTopic(topic) }
                                    )
                                    if (idx < topics.lastIndex) {
                                        AppleInsetDivider(startIndent = 60.dp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val ieltsBooks = books.filter { it.category == "ielts" }
                    val essentialBooks = books.filter { it.category == "essential" }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        if (catalogFilter == CourseCatalogFilter.ALL || catalogFilter == CourseCatalogFilter.IELTS) {
                            item(key = "ielts_section") {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AppleSectionHeader(
                                        title = "IELTS Preparation • ${ieltsBooks.sumOf { it.wordCount }} Words"
                                    )
                                    AppleInsetGroupedCard {
                                        ieltsBooks.forEachIndexed { idx, book ->
                                            val wordsSet = bookWordLowersMap[book.bookNum].orEmpty()
                                            val inDeck = wordsSet.count { it in deckWordMap }
                                            val mastered = wordsSet.count { it in masteredWordSet }
                                            BookInsetRow(
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
                                            if (idx < ieltsBooks.lastIndex) {
                                                AppleInsetDivider(startIndent = 62.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (catalogFilter == CourseCatalogFilter.ALL || catalogFilter == CourseCatalogFilter.ESSENTIAL) {
                            item(key = "essential_section") {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AppleSectionHeader(
                                        title = "6 Essential English Books • ${essentialBooks.sumOf { it.wordCount }} Words"
                                    )
                                    AppleInsetGroupedCard {
                                        essentialBooks.forEachIndexed { idx, book ->
                                            val wordsSet = bookWordLowersMap[book.bookNum].orEmpty()
                                            val inDeck = wordsSet.count { it in deckWordMap }
                                            val mastered = wordsSet.count { it in masteredWordSet }
                                            BookInsetRow(
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
                                            if (idx < essentialBooks.lastIndex) {
                                                AppleInsetDivider(startIndent = 62.dp)
                                            }
                                        }
                                    }
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
private fun AppleInlineSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookInsetRow(
    book: OfflineBook,
    inDeckCount: Int,
    masteredCount: Int,
    onClick: () -> Unit,
    onReviewBook: (() -> Unit)?
) {
    val badgeColor = when (book.bookNum) {
        7 -> Color(0xFFFF9F0A) // Barron's IELTS — Gold
        8 -> Color(0xFF32ADE6) // IELTS Listening — Sky Teal
        9 -> Color(0xFF5E5CE6) // IELTS Vocab Bank — Indigo
        1, 2 -> Color(0xFF30D158) // A1-A2 — Mint Green
        3, 4 -> Color(0xFF0A84FF) // B1-B2 — Apple Blue
        else -> Color(0xFFBF5AF2) // B2-C1 — Deep Violet
    }
    val icon = when (book.bookNum) {
        7 -> Icons.Default.WorkspacePremium
        8 -> Icons.Default.Headphones
        9 -> Icons.Default.LibraryBooks
        else -> Icons.Default.MenuBook
    }
    val progress = if (book.wordCount > 0) inDeckCount.toFloat() / book.wordCount.toFloat() else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("course_book_card_${book.bookNum}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppleIconBadge(
            icon = icon,
            tintColor = badgeColor,
            size = 36.dp,
            iconSize = 20.dp
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${book.cefr} • ${book.unitCount} units • ${book.wordCount} words",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (inDeckCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        color = badgeColor,
                        trackColor = badgeColor.copy(alpha = 0.16f),
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "$inDeckCount in Deck • $masteredCount mastered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (onReviewBook != null) {
            Surface(
                onClick = onReviewBook,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Review",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TopicInsetRow(
    topic: OfflineTopic,
    inDeckCount: Int,
    masteredCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("course_topic_card_${topic.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppleIconBadge(
            icon = Icons.Default.Category,
            tintColor = Color(0xFF30D158),
            size = 34.dp,
            iconSize = 18.dp
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = topic.nameEn,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${topic.nameAr} • ${topic.wordCount} words" +
                    if (inDeckCount > 0) " • $inDeckCount in Deck ($masteredCount mastered)" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open topic",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("courses_back_to_books")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to books",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${book.cefr} • $bookInDeckCount/${book.wordCount} in Deck • $bookMasteredCount mastered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (bookInDeckCount > 0) {
                Surface(
                    onClick = onReviewBook,
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Text(
                        text = "Review",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item(key = "units_group") {
                AppleInsetGroupedCard {
                    units.forEachIndexed { idx, unit ->
                        val unitWords = unitWordLowersMap[unit.unitNum].orEmpty()
                        val inDeck = unitWords.count { it in deckWordMap }
                        val mastered = unitWords.count { it in masteredWordSet }
                        val progress = if (unit.wordCount > 0) inDeck.toFloat() / unit.wordCount.toFloat() else 0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectUnit(unit) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("course_unit_card_${unit.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${unit.unitNum}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = unit.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${unit.wordCount} words" +
                                        if (inDeck > 0) " • $inDeck in Deck • $mastered mastered" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (inDeck > 0) {
                                    LinearProgressIndicator(
                                        progress = { progress.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Open unit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (idx < units.lastIndex) {
                            AppleInsetDivider(startIndent = 58.dp)
                        }
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("courses_back_to_units")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$subtitle • $inDeckCount/${words.size} in Deck",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Anki-style batch study action bar (Apple pill hierarchy: 1 prominent action + calm secondary pills)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (remainingCount > 0) {
                val add10Interaction = remember { MutableInteractionSource() }
                Button(
                    onClick = { onAddBatch(10) },
                    enabled = !isAdding,
                    interactionSource = add10Interaction,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .applePressScale(add10Interaction, 0.96f)
                        .testTag("add_next_10_cards_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+10 to Deck", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
                }
                if (remainingCount > 10) {
                    FilledTonalButton(
                        onClick = { onAddBatch(20) },
                        enabled = !isAdding,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text("+20", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp))
                    }
                }
                OutlinedButton(
                    onClick = onAddAll,
                    enabled = !isAdding,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("add_all_unit_cards_button")
                ) {
                    Text("All ($remainingCount)", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp))
                }
            }
            if (inDeckCount > 0) {
                val reviewInteraction = remember { MutableInteractionSource() }
                Surface(
                    onClick = onReviewDeck,
                    interactionSource = reviewInteraction,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF30D158).copy(alpha = 0.16f),
                    contentColor = Color(0xFF30D158),
                    border = BorderStroke(0.75.dp, Color(0xFF30D158).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .height(38.dp)
                        .applePressScale(reviewInteraction, 0.96f)
                        .testTag("review_unit_deck_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Review ($inDeckCount)",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item(key = "words_group") {
                AppleInsetGroupedCard {
                    words.forEachIndexed { idx, item ->
                        val inDeck = deckWordMap.containsKey(item.word.lowercase())
                        val isMastered = item.word.lowercase() in masteredWordSet
                        CurriculumWordInsetRow(
                            item = item,
                            inDeck = inDeck,
                            isMastered = isMastered,
                            showSourceBadge = false,
                            onClick = { onWordClick(item) },
                            onToggleDeck = { onWordToggleDeck(item, inDeck) }
                        )
                        if (idx < words.lastIndex) {
                            AppleInsetDivider(startIndent = 16.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurriculumWordInsetRow(
    item: OfflineCurriculumWord,
    inDeck: Boolean,
    isMastered: Boolean,
    showSourceBadge: Boolean,
    onClick: () -> Unit,
    onToggleDeck: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 6.dp, top = 11.dp, bottom = 11.dp)
            .testTag("course_word_row_${item.word.lowercase()}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.word.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.ipa.isNotBlank()) {
                    Text(
                        text = item.ipa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (isMastered) {
                    Text(
                        text = "• Mastered",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF30D158)
                    )
                } else if (showSourceBadge && item.bookShortTag.isNotBlank()) {
                    Text(
                        text = "• ${item.bookShortTag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (item.translationAr.isNotBlank()) {
                Text(
                    text = item.translationAr,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.definitionEn.isNotBlank()) {
                Text(
                    text = item.definitionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onToggleDeck,
            modifier = Modifier
                .size(44.dp)
                .testTag("course_word_bookmark_${item.word.lowercase()}")
        ) {
            Icon(
                imageVector = if (inDeck) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (inDeck) "Remove from Deck" else "Add to FSRS Deck",
                tint = if (inDeck) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(21.dp)
            )
        }
    }
}
